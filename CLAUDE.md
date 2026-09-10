# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Atlassian Confluence Server/Data Center plugin (P2, `packaging: atlassian-plugin`) that embeds ONLYOFFICE Docs
editors for Confluence attachments. Java 8 source/target level, built with the Atlassian Plugin SDK (AMPS).
Most document-service logic lives in the external `com.onlyoffice:docs-integration-sdk` — this repo mainly supplies
Confluence-specific implementations of SDK interfaces.

## Commands

Requires the Atlassian Plugin SDK on PATH (`atlas-*` wrappers); CI uses Temurin JDK 21 even though bytecode targets 8.

```bash
atlas-package                # build target/onlyoffice-confluence-plugin-<version>.jar (runs checkstyle in validate)
atlas-run                    # start a local Confluence with the plugin installed
atlas-debug                  # same, with a remote debugger on 5005
mvn checkstyle:check         # lint only — this is what the Lint workflow runs
```

There are no unit tests in this repo (`src/test` does not exist). End-to-end tests live in
[`ONLYOFFICE/integration-tests`](https://github.com/ONLYOFFICE/integration-tests) and are invoked by
`.github/workflows/e2e.yml`, which builds the jar and passes it as an artifact plus a Confluence version taken from
the `max` attribute in `src/main/resources/atlassian-plugin-marketing.xml`. Run them via the `E2E` workflow
(`workflow_dispatch` accepts a Confluence image tag and a Document Server image).

Checkstyle is strict and fails the build on warnings: 120-char lines for `.java`, no trailing whitespace, no star
imports, no unused imports, `MagicNumber`, `FinalParameters` (hence the pervasive `final` on method parameters),
`NewlineAtEndOfFile`. `checkstyle-suppressions.xml` disables Javadoc/`HiddenField`/`MethodLength`/`ParameterNumber`.
Every `.java`/`.vm` file carries the Apache-2.0 Ascensio System SIA header — copy it into new files.

## Versioning and branches

- `develop` is the working branch; `master` triggers tag creation from the top version in `CHANGELOG.md`, and a
  `v*` tag triggers the `Release` workflow, which builds the jar and cuts a GitHub release with release notes
  extracted from the newest `CHANGELOG.md` section. Keep `CHANGELOG.md` and `pom.xml` `<version>` in sync.
- Major version tracks the supported Confluence major: 6.x targets Confluence 9 (`confluence.version` 9.0.2,
  marketing compatibility 9.0.1–9.5.4); 5.x was the Confluence 8 line. New Confluence majors are developed on
  `feature/confluence-N` branches.

## Architecture

### Wiring

`src/main/resources/atlassian-plugin.xml` is the single source of truth for the plugin: it declares every servlet,
`<component>` (constructor-injected Spring beans), `<component-import>` (host Confluence/SAL services),
`web-resource` bundle, `web-item` menu entry with its `Condition` class, the macro, and the XWork action.
`META-INF/spring/plugin-context.xml` only enables the Atlassian scanner. **Any new class that needs injection or any
new URL must be registered there** — there is no annotation-based auto-registration.

### SDK integration layer (`onlyoffice.sdk.*`)

Each class extends a `Default*` from `docs-integration-sdk` and overrides only the Confluence-specific parts:

| Class | Base | Confluence specifics |
| --- | --- | --- |
| `manager/settings/SettingsManagerImpl` | `DefaultSettingsManager` | stores settings in SAL `PluginSettings` under the `onlyoffice.` prefix; `customization.forcesave` is forced to `null` |
| `manager/document/DocumentManagerImpl` | `DefaultDocumentManager` | `fileId` **is the attachment id as a string**; document key derives from `AttachmentUtil.getHashCode` truncated to 20 chars |
| `manager/url/UrlManagerImpl` | `DefaultUrlManager` | builds all plugin URLs; holds the servlet path constants; `getConfluenceBaseUrl(inner)` picks the "inner" product URL setting for Document Server → Confluence calls |
| `manager/security/JwtManagerImpl` | `DefaultJwtManager` | adds *internal* tokens (HMAC256 over an auto-generated `onlyoffice.plugin-secret`) and legacy SHA-256 `createHash`/`readHash` used for history links |
| `service/ConfigServiceImpl` | `DefaultConfigService` | permissions from `AttachmentUtil.checkAccess` ∩ SDK editability, user from `AuthenticatedUserThreadLocal` |
| `service/CallbackServiceImpl` | `DefaultCallbackService` | `handlerSave` re-checks edit access, converts back to the original extension when the editor returns a different type, then saves a new attachment version |
| `service/SettingsValidationServiceImpl` | `DefaultSettingsValidationServiceV2` | wraps document-server/command/convert checks and maps failures to i18n messages |

There are two `SettingsManager` types in scope — Confluence's `com.atlassian.confluence.setup.settings.SettingsManager`
(base URL) and the SDK's `com.onlyoffice.manager.settings.SettingsManager` (plugin settings). Constructors often take
both; watch the fully-qualified names.

### Servlets (`/plugins/servlet/onlyoffice/...`)

- `doceditor` (`OnlyOfficeEditorServlet`) — the entry point. With `pageId`+`fileExt` it creates a blank attachment
  and redirects; with `attachmentId` it builds a `Config` via `ConfigService`, signs it when JWT is on, and renders
  `templates/editor.vm`, which loads the Document Server API script and calls `DocEditor`.
- `save` (`OnlyOfficeSaveFileServlet`) — Document Server callback. `@UnrestrictedAccess`: authenticates by verifying
  the internal token in `?token=`, restores the user into `AuthenticatedUserThreadLocal`, then delegates to
  `CallbackService`. Always answers `{"error":0}` / `{"error":1,...}`.
- `file-provider` (`OnlyOfficeFileProviderServlet`) — streams attachment bytes to Document Server; verifies both the
  Document Server JWT header and the internal `?token=`.
- `api` (`OnlyOfficeAPIServlet`, POST) — dispatches on `?type=`: `save-as`, `attachment-data`, `reference-data`,
  `users-info` (browser-side editor callbacks).
- `history` (GET, `?type=info|data`) — version history, addressed by the `vkey` hash, not a raw id.
- `convert` — converts an attachment to OOXML, or a DOCX to a PDF form (`?createFrom=true`).
- `configure` — admin page (`configure.vm` + `settings.js`); GET/POST are gated on `UserManager.isSystemAdmin`, POST
  deserializes the SDK `Settings` model and returns validation results.
- `formats`, `test`, `confluence/previews/plugin/access` — supported-format list, convert self-test used by settings
  validation, and the visibility check for the ONLYOFFICE button in Confluence's media viewer.

Two token families coexist and must not be confused: the **Document Server JWT** (shared secret from settings, sent
in the configured `Authorization` header, verified with `JwtManager.verify`) and the **internal token** (plugin-owned
secret, carried in `?token=`, with an `action` claim of `download`/`callback` that each servlet must assert).

### Confluence-side pieces

- `utils/attachment/AttachmentUtil` — the only place that touches `AttachmentManager`/permissions. All access checks
  (`checkAccess(id, user, forEdit)`, `checkAccessCreate`) go through it; reuse it instead of querying Confluence
  directly.
- `conditions/` — `Condition` implementations deciding which attachment menu items appear (viewable vs. editable vs.
  convertible, size limit, permissions). Parameters like `forEdit` come from `<param>` in the plugin descriptor.
- `macro/OnlyOfficePreviewMacro` — the `onlyoffice-preview` XHTML macro; renders an embedded editor via
  `preview.vm`, resolves the target page through `ContentResolver`, and supplies format-specific placeholder SVGs in
  the editor.
- `action/DownloadAsAction` — XWork action behind `onlyoffice-download-as-view.action` (dialog + JSON result).
- Front end is Velocity templates + Soy + plain JS/AUI under `src/main/resources/{templates,js,css}`, bundled by the
  `web-resource` declarations; the `jsI18n` transformer resolves `AJS.I18n` keys at build time.

### i18n

All user-visible strings live in `lang-resource.properties` (+ `_de`, `_es`, `_fr`, `_it`, `_ru`, `_zh_CN`) under the
`onlyoffice.` key namespace. Checkstyle's `Translation` module compares key sets, so add a key to every locale file.
Error messages in settings validation are looked up by convention, e.g.
`onlyoffice.service.convert.error.<lowercased SDK error enum>` — new SDK error values need matching keys.
