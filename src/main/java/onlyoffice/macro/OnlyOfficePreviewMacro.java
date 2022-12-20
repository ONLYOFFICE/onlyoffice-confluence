/**
 *
 * (c) Copyright Ascensio System SIA 2022
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
import com.atlassian.confluence.macro.*;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.plugin.services.VelocityHelperService;
import com.atlassian.confluence.util.HtmlUtil;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import onlyoffice.macro.components.ContentResolver;
import onlyoffice.managers.config.ConfigManager;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.model.DocumentType;
import onlyoffice.model.Type;
import onlyoffice.model.editor.Mode;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class OnlyOfficePreviewMacro implements Macro, EditorImagePlaceholder, ResourceAware {

    private static final Logger log = LogManager.getLogger("onlyoffice.macro.OnlyOfficePreviewMacro");

    public static final String DEFAULT_WIDTH = "632";
    public static final String DEFAULT_HEIGHT = "507";
    private String resourcePath;

    private final AttachmentManager attachmentManager;
    @ComponentImport
    private final VelocityHelperService velocityHelperService;
    private final ContentResolver contentResolver;
    private final ConfigManager configManager;
    private final UrlManager urlManager;
    private final DocumentManager documentManager;

    public OnlyOfficePreviewMacro(AttachmentManager attachmentManager,
                                  VelocityHelperService velocityHelperService, ContentResolver contentResolver,
                                  ConfigManager configManager, UrlManager urlManager, DocumentManager documentManager) {
        this.attachmentManager = attachmentManager;
        this.velocityHelperService = velocityHelperService;
        this.contentResolver = contentResolver;
        this.configManager = configManager;
        this.urlManager = urlManager;
        this.documentManager = documentManager;
    }

    @Override
    public String execute(Map<String, String> args, String s, ConversionContext conversionContext) throws MacroExecutionException {
        final String file = this.getFileName(args);
        final String pageName = args.get("page");
        final String space = args.get("space");
        final String date = args.get("date");
        String width = args.get("width");
        String height = args.get("height");

        final ContentEntityObject page = this.contentResolver.getContent(pageName, space, date, conversionContext.getPageContext().getEntity());
        final Attachment attachment = this.attachmentManager.getAttachment(page, file);

        if (attachment == null) {
            throw new MacroExecutionException("The viewfile macro is unable to locate the attachment \"" + file + "\" on " + ((pageName == null) ? "this page" : ("the page \"" + pageName + "\" in space \"" + space + "\"")));
        }

        width = normalizeSize((width == null) ? DEFAULT_WIDTH : width);
        height = normalizeSize((height == null) ? DEFAULT_HEIGHT : height);

        try {
            String config = configManager.createConfig(
                    attachment.getId(),
                    Mode.VIEW,
                    Type.EMBEDDED,
                    null,
                    null,
                    width,
                    height
            );

            final Map<String, Object> context = this.velocityHelperService.createDefaultVelocityContext();
            context.put("id", documentManager.getKeyOfFile(attachment.getId(), true));
            context.put("docServiceApiUrl", urlManager.getDocServiceApiUrl());
            context.put("configAsHtml", config);

            return this.velocityHelperService.getRenderedTemplate("templates/preview.vm", context);
        } catch (Exception e) {
            throw new MacroExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public BodyType getBodyType() { return BodyType.NONE; }

    @Override
    public OutputType getOutputType() { return OutputType.BLOCK; }

    @Override
    public String getResourcePath() {
        return this.resourcePath;
    }

    @Override
    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public ImagePlaceholder getImagePlaceholder(Map<String, String> args, ConversionContext conversionContext) {
        String name = (String)args.get("name");
        if (name == null) {
            name = (String)args.get("0");
        }

        DocumentType documentType = DocumentType.WORD;

        if (!StringUtils.isBlank(name)) {
            int dotIdx = name.lastIndexOf(46);
            if (dotIdx != -1) {
                String fileExt = name.substring(dotIdx + 1).toLowerCase();
                if (documentManager.getDocType(fileExt) != null) {
                    documentType = documentManager.getDocType(fileExt);
                    if (fileExt.equals("oform")) documentType = DocumentType.FORM;
                }
            }
        }

        return new DefaultImagePlaceholder(this.resourcePath + "/images/preview-placeholder-" + documentType.name().toLowerCase() + ".svg", true, new ImageDimensions(380, 300));
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

    private String normalizeSize(String attr) {
        if (!attr.endsWith("px") && !attr.endsWith("%")) {
            attr += "px";
        }

        return HtmlUtil.htmlEncode(attr);
    }
}