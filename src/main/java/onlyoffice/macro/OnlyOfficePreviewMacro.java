/**
 *
 * (c) Copyright Ascensio System SIA 2023
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

package onlyoffice.macro;

import com.atlassian.confluence.content.render.image.ImageDimensions;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ContentEntityObject;

import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.ResourceAware;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.plugin.services.VelocityHelperService;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.HtmlUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import onlyoffice.macro.components.ContentResolver;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class OnlyOfficePreviewMacro implements Macro, EditorImagePlaceholder, ResourceAware {

    private final Logger log = LogManager.getLogger("onlyoffice.macro.OnlyOfficePreviewMacro");

    public static final int DOT_INDEX = 46;
    public static final int DEFAULT_PLACEHOLDER_WIDTH = 380;
    public static final int DEFAULT_PLACEHOLDER_HEIGHT = 300;
    public static final String DEFAULT_WIDTH = "100%";
    public static final String DEFAULT_HEIGHT = "720";
    private String resourcePath;

    private final AttachmentManager attachmentManager;
    private final VelocityHelperService velocityHelperService;
    private final ContentResolver contentResolver;
    private final UrlManager urlManager;
    private final AttachmentUtil attachmentUtil;
    private final ConfigService configSevice;
    private final DocumentManager documentManager;

    public OnlyOfficePreviewMacro(final AttachmentManager attachmentManager,
                                  final VelocityHelperService velocityHelperService,
                                  final ContentResolver contentResolver, final UrlManager urlManager,
                                  final AttachmentUtil attachmentUtil, final ConfigService configSevice,
                                  final DocumentManager documentManager) {
        this.attachmentManager = attachmentManager;
        this.velocityHelperService = velocityHelperService;
        this.contentResolver = contentResolver;
        this.urlManager = urlManager;
        this.attachmentUtil = attachmentUtil;
        this.configSevice = configSevice;
        this.documentManager = documentManager;
    }

    @Override
    public String execute(final Map<String, String> args, final String s, final ConversionContext conversionContext)
            throws MacroExecutionException {
        final String file = this.getFileName(args);
        final String pageName = args.get("page");
        final String space = args.get("space");
        final String date = args.get("date");
        String width = args.get("width");
        String height = args.get("height");

        final ContentEntityObject page = this.contentResolver.getContent(pageName, space, date,
                conversionContext.getPageContext().getEntity());
        final Attachment attachment = this.attachmentManager.getAttachment(page, file);

        if (attachment == null) {
            throw new MacroExecutionException("The viewfile macro is unable to locate the attachment \""
                    + file + "\" on " + ((pageName == null)
                    ? "this page" : ("the page \"" + pageName + "\" in space \"" + space + "\"")));
        }

        width = normalizeSize((width == null) ? DEFAULT_WIDTH : width);
        height = normalizeSize((height == null) ? DEFAULT_HEIGHT : height);

        try {
            Config config = configSevice.createConfig(String.valueOf(attachment.getId()), Mode.EDIT, Type.EMBEDDED);

            config.setWidth(width);
            config.setHeight(height);

            String fileName = attachment.getFileName();
            String action = "";

            final boolean isPreview = conversionContext.getOutputType().equals("preview");

            if (attachmentUtil.checkAccess(attachment.getId(),  AuthenticatedUserThreadLocal.get(), true)
                    && !isPreview) {
                if (documentManager.isEditable(fileName)) {
                    action = "edit";
                } else if (documentManager.isFillable(fileName)) {
                    action = "fill";
                }
            }

            ObjectMapper mapper = new ObjectMapper();

            final Map<String, Object> context = this.velocityHelperService.createDefaultVelocityContext();
            context.put("id", System.currentTimeMillis());
            context.put("attachmentId", attachment.getId());
            context.put("action", action);
            context.put("docServiceApiUrl", urlManager.getDocumentServerApiUrl());
            context.put("configAsHtml", mapper.writeValueAsString(config));

            return this.velocityHelperService.getRenderedTemplate("templates/preview.vm", context);
        } catch (Exception e) {
            throw new MacroExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public BodyType getBodyType() {
        return BodyType.NONE;
    }

    @Override
    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }

    @Override
    public String getResourcePath() {
        return this.resourcePath;
    }

    @Override
    public void setResourcePath(final String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public ImagePlaceholder getImagePlaceholder(final Map<String, String> args,
                                                final ConversionContext conversionContext) {
        String name = (String) args.get("name");
        if (name == null) {
            name = (String) args.get("0");
        }

        String documentType = DocumentType.WORD.name().toLowerCase();

        if (!StringUtils.isBlank(name)) {
            int dotIdx = name.lastIndexOf(DOT_INDEX);
            if (dotIdx != -1) {
                String fileExt = name.substring(dotIdx + 1).toLowerCase();
                if (documentManager.getDocumentType(name) != null) {
                    documentType = documentManager.getDocumentType(name).name().toLowerCase();
                    if (fileExt.equals("oform")) {
                        documentType = "form";
                    }
                }
            }
        }

        return new DefaultImagePlaceholder(this.resourcePath + "/images/preview-placeholder-"
                + documentType + ".svg",
                true, new ImageDimensions(DEFAULT_PLACEHOLDER_WIDTH, DEFAULT_PLACEHOLDER_HEIGHT));
    }

    private String getFileName(final Map<String, String> args) throws MacroExecutionException {
        String file = args.get("0");
        if (file == null) {
            file = args.get("filename");
            if (file == null) {
                file = args.get("name");
                if (file == null || file.trim().length() == 0) {
                    throw new MacroExecutionException("No attachment name specified");
                }
            }
        }
        return file;
    }

    private String normalizeSize(final String attr) {
        String size = attr;
        if (!attr.endsWith("px") && !attr.endsWith("%")) {
            size += "px";
        }

        return HtmlUtil.htmlEncode(size);
    }
}
