/**
 *
 * (c) Copyright Ascensio System SIA 2020
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
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.velocity.VelocityUtils;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.inject.Inject;

public class OnlyOfficeEditorServlet extends HttpServlet {
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;
    @ComponentImport
    private final SettingsManager settingsManager;
    @ComponentImport
    private final LocaleManager localeManager;

    private final JwtManager jwtManager;
    private final UrlManager urlManager;

    @Inject
    public OnlyOfficeEditorServlet(PluginSettingsFactory pluginSettingsFactory, LocaleManager localeManager,
            SettingsManager settingsManager, UrlManager urlManager, JwtManager jwtManager) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.settingsManager = settingsManager;
        this.jwtManager = jwtManager;
        this.urlManager = urlManager;
        this.localeManager = localeManager;
    }

    private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeEditorServlet");
    private static final long serialVersionUID = 1L;

    private Properties properties;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!AuthContext.checkUserAuthorisation(request, response)) {
            return;
        }

        String apiUrl = urlManager.getPublicDocEditorUrl();
        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = "";
        }

        ConfigurationManager configurationManager = new ConfigurationManager();
        properties = configurationManager.GetProperties();

        String callbackUrl = "";
        String fileUrl = "";
        String key = "";
        String fileName = "";
        String ext = "";
        String errorMessage = "";
        ConfluenceUser user = null;

        String attachmentIdString = request.getParameter("attachmentId");
        if (attachmentIdString == null){
            fileName = request.getParameter("fileName");
            ext = request.getParameter("ext");
            if (fileName != null && !fileName.equals("") && ext != null && !ext.equals("")) {
                attachmentIdString = DocumentManager.createDemo(fileName, ext, request);
            }
        }

        Long attachmentId;

        try {
            attachmentId = Long.parseLong(attachmentIdString);
            log.info("attachmentId " + attachmentId);

            user = AuthenticatedUserThreadLocal.get();
            log.info("user " + user);
            if (AttachmentUtil.checkAccess(attachmentId, user, false)) {
                key = DocumentManager.getKeyOfFile(attachmentId);

                fileName = AttachmentUtil.getFileName(attachmentId);

                fileUrl = urlManager.GetFileUri(attachmentId);

                if (AttachmentUtil.checkAccess(attachmentId, user, true)) {
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

        writer.write(getTemplate(apiUrl, callbackUrl, fileUrl, key, fileName, user, errorMessage));
    }

    private String getTemplate(String apiUrl, String callbackUrl, String fileUrl, String key, String fileName,
            ConfluenceUser user, String errorMessage) throws UnsupportedEncodingException {
        Map<String, Object> defaults = MacroUtils.defaultVelocityContext();
        Map<String, String> config = new HashMap<String, String>();

        String docTitle = fileName.trim();
        String docExt = docTitle.substring(docTitle.lastIndexOf(".") + 1).trim().toLowerCase();

        config.put("docserviceApiUrl", apiUrl + properties.getProperty("files.docservice.url.api"));
        config.put("errorMessage", errorMessage);
        config.put("docTitle", docTitle);

        JSONObject responseJson = new JSONObject();
        JSONObject documentObject = new JSONObject();
        JSONObject editorConfigObject = new JSONObject();
        JSONObject userObject = new JSONObject();
        JSONObject permObject = new JSONObject();

        try {
            responseJson.put("type", "desktop");
            responseJson.put("width", "100%");
            responseJson.put("height", "100%");
            responseJson.put("documentType", getDocType(docExt));

            responseJson.put("document", documentObject);
            documentObject.put("title", docTitle);
            documentObject.put("url", fileUrl);
            documentObject.put("fileType", docExt);
            documentObject.put("key", key);
            documentObject.put("permissions", permObject);
            permObject.put("edit", callbackUrl != null && !callbackUrl.isEmpty());

            responseJson.put("editorConfig", editorConfigObject);
            editorConfigObject.put("lang", localeManager.getLocale(user).toLanguageTag());
            editorConfigObject.put("mode", "edit");
            editorConfigObject.put("callbackUrl", callbackUrl);

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
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);
        }

        defaults.putAll(config);
        return VelocityUtils.getRenderedTemplate("templates/editor.vm", defaults);
    }

    private String getDocType(String ext) {
        if (".doc.docx.docm.dot.dotx.dotm.odt.fodt.ott.rtf.txt.html.htm.mht.pdf.djvu.fb2.epub.xps".indexOf(ext) != -1)
            return "text";
        if (".xls.xlsx.xlsm.xlt.xltx.xltm.ods.fods.ots.csv".indexOf(ext) != -1)
            return "spreadsheet";
        if (".pps.ppsx.ppsm.ppt.pptx.pptm.pot.potx.potm.odp.fodp.otp".indexOf(ext) != -1)
            return "presentation";
        return null;
    }
}
