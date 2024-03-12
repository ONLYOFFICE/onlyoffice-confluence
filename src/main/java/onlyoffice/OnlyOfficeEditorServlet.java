/**
 *
 * (c) Copyright Ascensio System SIA 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package onlyoffice;

import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.pages.BlogPost;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.sal.api.message.I18nResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import onlyoffice.managers.auth.AuthContext;
import com.atlassian.confluence.pages.Attachment;
import onlyoffice.sdk.manager.document.DocumentManager;
import onlyoffice.sdk.manager.url.UrlManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Map;

import static onlyoffice.sdk.manager.url.UrlManagerImpl.DOC_EDITOR_SERVLET;

public class OnlyOfficeEditorServlet extends HttpServlet {
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeEditorServlet");
    private final long serialVersionUID = 1L;

    private final I18nResolver i18n;
    private final UrlManager urlManager;
    private final AuthContext authContext;
    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;
    private final ConfigService configService;
    private final SettingsManager settingsManager;

    private final LocaleManager localeManager;

    public OnlyOfficeEditorServlet(final I18nResolver i18n, final UrlManager urlManager, final AuthContext authContext,
                                   final DocumentManager documentManager, final AttachmentUtil attachmentUtil,
                                   final ConfigService configService, final SettingsManager settingsManager,
                                   final LocaleManager localeManager) {
        this.i18n = i18n;
        this.urlManager = urlManager;
        this.authContext = authContext;
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
        this.configService = configService;
        this.settingsManager = settingsManager;
        this.localeManager = localeManager;
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        String attachmentIdString = request.getParameter("attachmentId");
        String actionDataString = request.getParameter("actionData");
        String modeString = request.getParameter("mode");
        if (attachmentIdString == null || attachmentIdString.isEmpty()) {
            if (!authContext.checkUserAuthorization(request, response)) {
                return;
            }

            String fileName = request.getParameter("fileName");
            String fileExt = request.getParameter("fileExt");
            String pageId = request.getParameter("pageId");
            if (pageId != null && !pageId.equals("")) {

                if (!attachmentUtil.checkAccessCreate(user, Long.parseLong(pageId))) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                String extension = fileExt == null
                        || !fileExt.equals("xlsx")
                        && !fileExt.equals("pptx")
                        && !fileExt.equals("docxf")
                        ? "docx" : fileExt.trim();

                String name = fileName == null || fileName.equals("")
                        ? i18n.getText("onlyoffice.editor.dialog.filecreate." + extension) : fileName;

                name = attachmentUtil.getCorrectName(name, extension, Long.parseLong(pageId));
                String mimeType = documentManager.getMimeType(name);
                InputStream newBlankFile = documentManager.getNewBlankFile(extension, localeManager.getLocale(user));

                Attachment attachment = attachmentUtil.createNewAttachment(
                        name,
                        mimeType,
                        newBlankFile,
                        newBlankFile.available(),
                        Long.parseLong(pageId),
                        user
                );

                response.sendRedirect(request.getContextPath() + DOC_EDITOR_SERVLET + "?attachmentId="
                        + URLEncoder.encode(String.valueOf(attachment.getId()), "UTF-8"));
                return;
            }
        }

        try {
            Long attachmentId = Long.parseLong(attachmentIdString);
            Long pageId = attachmentUtil.getAttachmentPageId(attachmentId);
            Attachment attachment = attachmentUtil.getAttachment(attachmentId);
            String fileName = documentManager.getDocumentName(String.valueOf(attachmentId));
            DocumentType documentType = documentManager.getDocumentType(fileName);

            if (attachment == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (!attachmentUtil.checkAccess(attachmentId, user, false)) {
                response.sendRedirect(authContext.getLoginUrl(request));
                return;
            }

            Map<String, Object> context = MacroUtils.defaultVelocityContext();
            context.put("docserviceApiUrl", urlManager.getDocumentServerApiUrl());
            context.put("docTitle", documentManager.getDocumentName(String.valueOf(attachmentId)));
            context.put("favicon", urlManager.getFaviconUrl(documentType));
            context.put("pageId", pageId);
            context.put("pageTitle", attachmentUtil.getAttachmentPageTitle(attachmentId));
            context.put("spaceKey", attachmentUtil.getAttachmentSpaceKey(attachmentId));
            context.put("spaceName", attachmentUtil.getAttachmentSpaceName(attachmentId));
            context.put("isBlogPost", String.valueOf(attachmentUtil.getContainer(pageId) instanceof BlogPost));

            if (documentType != null) {
                JSONObject actionData = null;

                if (actionDataString != null && !actionDataString.isEmpty()) {
                    actionData = new JSONObject(actionDataString);
                }

                Mode mode = Mode.EDIT;

                if (modeString != null && modeString.equals("view")) {
                    mode = Mode.VIEW;
                }

                Config config = configService.createConfig(
                        attachmentId.toString(),
                        mode,
                        request.getHeader("USER-AGENT")
                );

                config.getEditorConfig().setLang(localeManager.getLocale(user).toLanguageTag());
                config.getEditorConfig().setActionLink(actionData);

                ObjectMapper mapper = new ObjectMapper();

                context.put("configAsHtml", mapper.writeValueAsString(config));
                context.put("historyInfoUriAsHtml", urlManager.getHistoryInfoUri(attachmentId));
                context.put("historyDataUriAsHtml", urlManager.getHistoryDataUri(attachmentId));
                context.put("attachmentDataAsHtml", urlManager.getAttachmentDataUri());
                context.put("saveAsUriAsHtml", urlManager.getSaveAsUri());
                context.put("referenceDataUriAsHtml", urlManager.getReferenceDataUri(pageId));
                context.put("insertImageTypesAsHtml",
                        new JSONArray(documentManager.getInsertImageExtensions()).toString());
                context.put("compareFileTypesAsHtml",
                        new JSONArray(documentManager.getCompareFileExtensions()).toString());
                context.put("mailMergeTypesAsHtml", new JSONArray(documentManager.getMailMergeExtensions()).toString());
                context.put("demo", settingsManager.isDemoActive());
                context.put("usersInfoUrlAsHtml", urlManager.getUsersInfoUrl());
            } else {
                context.put("errorMessage", i18n.getText("onlyoffice.editor.message.error.unsupported") + "(."
                        + documentManager.getExtension(fileName) + ")");
            }

            response.setContentType("text/html;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            writer.write(VelocityUtils.getRenderedTemplate("templates/editor.vm", context));

        } catch (Exception e) {
            throw new ServletException(e.getMessage(), e);
        }
    }
}
