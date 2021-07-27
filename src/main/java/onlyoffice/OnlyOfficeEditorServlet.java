/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.inject.Inject;

import com.atlassian.confluence.core.DateFormatter;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.user.ConfluenceUserPreferences;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.spring.container.ContainerManager;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.convert.ConvertManager;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.managers.jwt.JwtManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

public class OnlyOfficeEditorServlet extends HttpServlet {
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeEditorServlet");
    private final long serialVersionUID = 1L;

    private Properties properties;

    @ComponentImport
    private final LocaleManager localeManager;
    @ComponentImport
    private final FormatSettingsManager formatSettingsManager;

    private final JwtManager jwtManager;
    private final UrlManager urlManager;
    private final ConfigurationManager configurationManager;
    private final AuthContext authContext;
    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;
    private final ConvertManager convertManager;


    @Inject
    public OnlyOfficeEditorServlet(LocaleManager localeManager, FormatSettingsManager formatSettingsManager,
            UrlManager urlManager, JwtManager jwtManager, ConfigurationManager configurationManager,
            AuthContext authContext, DocumentManager documentManager, AttachmentUtil attachmentUtil,
            ConvertManager convertManager) {
        this.localeManager = localeManager;
        this.formatSettingsManager = formatSettingsManager;
        this.urlManager = urlManager;
        this.jwtManager = jwtManager;
        this.configurationManager = configurationManager;
        this.authContext = authContext;
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
        this.convertManager = convertManager;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!authContext.checkUserAuthorisation(request, response)) {
            return;
        }

        String apiUrl = urlManager.getPublicDocEditorUrl();
        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = "";
        }

        properties = configurationManager.getProperties();

        String callbackUrl = "";
        String fileUrl = "";
        String gobackUrl = "";
        String key = "";
        String fileName = "";
        String errorMessage = "";
        ConfluenceUser user = null;

        String attachmentIdString = request.getParameter("attachmentId");

        if (attachmentIdString == null) {
            fileName = request.getParameter("fileName");
            String fileExt = request.getParameter("fileExt");
            String pageID = request.getParameter("pageId");
            if (pageID != null && !pageID.equals("")) {
                try {
                    Long attachmentId = documentManager.createDemo(fileName, fileExt, Long.parseLong(pageID));

                    response.sendRedirect( request.getContextPath() +  "?attachmentId=" + URLEncoder.encode(attachmentId.toString(), "UTF-8"));
                    return;
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    ex.printStackTrace(pw);
                    String error = ex.toString() + "\n" + sw.toString();
                    log.error(error);
                    errorMessage = ex.toString();
                }
            }
        }

        Long attachmentId = null;

        try {
            attachmentId = Long.parseLong(attachmentIdString);
            log.info("attachmentId " + attachmentId);

            user = AuthenticatedUserThreadLocal.get();
            log.info("user " + user);
            if (attachmentUtil.checkAccess(attachmentId, user, false)) {
                key = documentManager.getKeyOfFile(attachmentId);

                fileName = attachmentUtil.getFileName(attachmentId);

                fileUrl = urlManager.getFileUri(attachmentId);

                gobackUrl = urlManager.getGobackUrl(attachmentId, request);

                if (attachmentUtil.checkAccess(attachmentId, user, true)) {
                    callbackUrl = urlManager.getCallbackUrl(attachmentId);
                }
            } else {
                log.error("access deny");
                errorMessage = "You don not have enough permission to view the file";
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);
            errorMessage = ex.toString();
        }

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter writer = response.getWriter();

        writer.write(getTemplate(attachmentId, apiUrl, callbackUrl, fileUrl, key, fileName, user, gobackUrl, errorMessage));
    }

    private String getTemplate(Long attachmentId, String apiUrl, String callbackUrl, String fileUrl, String key, String fileName,
            ConfluenceUser user, String gobackUrl, String errorMessage) throws UnsupportedEncodingException {
        Map<String, Object> defaults = MacroUtils.defaultVelocityContext();
        Map<String, String> config = new HashMap<String, String>();

        String docTitle = fileName.trim();
        String docExt = docTitle.substring(docTitle.lastIndexOf(".") + 1).trim().toLowerCase();
        boolean canEdit = documentManager.isEditable(docExt);

        config.put("docserviceApiUrl", apiUrl + properties.getProperty("files.docservice.url.api"));
        config.put("errorMessage", errorMessage);
        config.put("docTitle", docTitle);

        JSONObject responseJson = new JSONObject();
        JSONObject documentObject = new JSONObject();
        JSONObject editorConfigObject = new JSONObject();
        JSONObject userObject = new JSONObject();
        JSONObject permObject = new JSONObject();
        JSONObject customizationObject = new JSONObject();
        JSONObject gobackObject = new JSONObject();

        try {
            responseJson.put("type", "desktop");
            responseJson.put("width", "100%");
            responseJson.put("height", "100%");
            responseJson.put("documentType", documentManager.getDocType(docExt));

            responseJson.put("document", documentObject);
            documentObject.put("title", docTitle);
            documentObject.put("url", fileUrl);
            documentObject.put("fileType", docExt);
            documentObject.put("key", key);
            documentObject.put("permissions", permObject);
            responseJson.put("editorConfig", editorConfigObject);

            if (canEdit && callbackUrl != null) {
                permObject.put("edit", true);
                editorConfigObject.put("mode", "edit");
                editorConfigObject.put("callbackUrl", callbackUrl);
            } else {
                permObject.put("edit", false);
                editorConfigObject.put("mode", "view");
            }

            editorConfigObject.put("lang", localeManager.getLocale(user).toLanguageTag());
            editorConfigObject.put("customization", customizationObject);

            customizationObject.put("goback", gobackObject);
            gobackObject.put("url", gobackUrl);

            if (user != null) {
                editorConfigObject.put("user", userObject);
                userObject.put("id", user.getName());
                userObject.put("name", user.getFullName());
            }

            if (jwtManager.jwtEnabled()) {
                responseJson.put("token", jwtManager.createToken(responseJson));
            }

            // AsHtml at the end disables automatic html encoding
            config.put("jsonAsHtml", responseJson.toString());
            config.put("historyAsHtml", getHistory(attachmentId, user).toString());
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);
        }

        defaults.putAll(config);
        defaults.put("demo", configurationManager.demoActive());
        return VelocityUtils.getRenderedTemplate("templates/editor.vm", defaults);
    }

    private JSONObject getHistory (Long attachmentId, ConfluenceUser confluenceUser) throws Exception {
        UserAccessor userAccessor = (UserAccessor) ContainerManager.getComponent("userAccessor");
        ConfluenceUserPreferences preferences = userAccessor.getConfluenceUserPreferences(confluenceUser);
        DateFormatter dateFormatter = preferences.getDateFormatter(formatSettingsManager, localeManager);

        JSONObject history = new JSONObject();
        JSONObject info = new JSONObject();
        Map<String, Object> data = new HashMap<>();
        List<JSONObject> versions = new ArrayList<>();

        List<Attachment> attachments = attachmentUtil.getAllVersions(attachmentId);
        Collections.reverse(attachments);
        for (Attachment attachment : attachments) {
            JSONObject versionInfo = new JSONObject();
            JSONObject versionData = new JSONObject();
            JSONObject user = new JSONObject();
            versionInfo.put("key", documentManager.getKeyOfFile(attachment.getId()));
            versionInfo.put("version", attachment.getVersion());
            versionInfo.put("created",  dateFormatter.formatDateTime(attachment.getCreationDate()));
            versionInfo.put("user", user);

            user.put("id", attachment.getCreator().getName());
            user.put("name", attachment.getCreator().getFullName());

            versionData.put("key", documentManager.getKeyOfFile(attachment.getId()));
            versionData.put("version", attachment.getVersion());
            versionData.put("url", urlManager.getFileUri(attachment.getId()));

            if (jwtManager.jwtEnabled())
            {
                versionData.put("token", jwtManager.createToken(versionData));
            }

            versions.add(versionInfo);
            data.put(String.valueOf(attachment.getVersion()), versionData);
        }
        info.put("currentVersion", attachmentUtil.getVersion(attachmentId));
        info.put("history", versions);

        history.put("info", info);
        history.put("data", data);

        return history;
    }
}
