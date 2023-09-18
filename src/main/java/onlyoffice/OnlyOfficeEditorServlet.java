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

package onlyoffice;

import com.atlassian.confluence.pages.BlogPost;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.sal.api.message.I18nResolver;
import onlyoffice.managers.auth.AuthContext;
import com.atlassian.confluence.pages.Attachment;
import onlyoffice.managers.config.ConfigManager;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.model.config.DocumentType;
import onlyoffice.model.config.Type;
import onlyoffice.model.config.editor.Mode;
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
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Map;

public class OnlyOfficeEditorServlet extends HttpServlet {
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeEditorServlet");
    private final long serialVersionUID = 1L;

    private final I18nResolver i18n;
    private final UrlManager urlManager;
    private final ConfigurationManager configurationManager;
    private final AuthContext authContext;
    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;
    private final ConfigManager configManager;

    public OnlyOfficeEditorServlet(final I18nResolver i18n, final UrlManager urlManager,
                                   final ConfigurationManager configurationManager, final AuthContext authContext,
                                   final DocumentManager documentManager, final AttachmentUtil attachmentUtil,
                                   final ConfigManager configManager) {
        this.i18n = i18n;
        this.urlManager = urlManager;
        this.configurationManager = configurationManager;
        this.authContext = authContext;
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
        this.configManager = configManager;
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        String attachmentIdString = request.getParameter("attachmentId");
        String actionDataString = request.getParameter("actionData");
        String referer = request.getHeader("referer");

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

                Long attachmentId = documentManager.createDemo(fileName, fileExt, Long.parseLong(pageId), user);

                response.sendRedirect(request.getContextPath() + "?attachmentId="
                        + URLEncoder.encode(attachmentId.toString(), "UTF-8"));
                return;
            }
        }

        try {
            Long attachmentId = Long.parseLong(attachmentIdString);
            Long pageId = attachmentUtil.getAttachmentPageId(attachmentId);
            Attachment attachment = attachmentUtil.getAttachment(attachmentId);
            String extension = attachmentUtil.getFileExt(attachmentId);
            DocumentType documentType = documentManager.getDocType(extension);

            if (attachment == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (!attachmentUtil.checkAccess(attachmentId, user, false)) {
                response.sendRedirect(authContext.getLoginUrl(request));
                return;
            }

            Map<String, Object> context = MacroUtils.defaultVelocityContext();
            context.put("docserviceApiUrl", urlManager.getDocServiceApiUrl());
            context.put("docTitle", attachmentUtil.getFileName(attachmentId));
            context.put("favicon", urlManager.getFaviconUrl(documentType));
            context.put("pageId", pageId);
            context.put("pageTitle", attachmentUtil.getAttachmentPageTitle(attachmentId));
            context.put("spaceKey", attachmentUtil.getAttachmentSpaceKey(attachmentId));
            context.put("spaceName", attachmentUtil.getAttachmentSpaceName(attachmentId));
            context.put("isBlogPost", String.valueOf(attachmentUtil.getContainer(pageId) instanceof BlogPost));

            if (documentType != null) {
                Type type = documentManager.getEditorType(request.getHeader("USER-AGENT"));

                JSONObject actionData = null;

                if (actionDataString != null && !actionDataString.isEmpty()) {
                    actionData = new JSONObject(actionDataString);
                }

                String config = configManager.createConfig(attachmentId, Mode.EDIT, type, actionData, referer);
                context.put("configAsHtml", config);
                context.put("historyInfoUriAsHtml", urlManager.getHistoryInfoUri(attachmentId));
                context.put("historyDataUriAsHtml", urlManager.getHistoryDataUri(attachmentId));
                context.put("attachmentDataAsHtml", urlManager.getAttachmentDataUri());
                context.put("saveAsUriAsHtml", urlManager.getSaveAsUri());
                context.put("referenceDataUriAsHtml", urlManager.getReferenceDataUri(pageId));
                context.put("insertImageTypesAsHtml", new JSONArray(documentManager.getInsertImageTypes()).toString());
                context.put("compareFileTypesAsHtml", new JSONArray(documentManager.getCompareFileTypes()).toString());
                context.put("mailMergeTypesAsHtml", new JSONArray(documentManager.getMailMergeTypes()).toString());
                context.put("demo", configurationManager.demoActive());
            } else {
                context.put("errorMessage", i18n.getText("onlyoffice.editor.message.error.unsupported") + "(."
                        + attachmentUtil.getFileExt(attachmentId) + ")");
            }

            response.setContentType("text/html;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            writer.write(VelocityUtils.getRenderedTemplate("templates/editor.vm", context));

        } catch (Exception e) {
            throw new ServletException(e.getMessage(), e);
        }
    }
}
