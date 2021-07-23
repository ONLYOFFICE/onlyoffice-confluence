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
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.convert.ConvertManager;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.inject.Inject;

public class OnlyOfficeConvertServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeConvertServlet");

    @ComponentImport
    private final AttachmentManager attachmentManager;

    private final AttachmentUtil attachmentUtil;
    private final ConvertManager convertManager;
    private final AuthContext authContext;
    private final DocumentManager documentManager;
    private final ConfigurationManager configurationManager;

    @Inject
    public OnlyOfficeConvertServlet(AttachmentManager attachmentManager, AttachmentUtil attachmentUtil,
            ConvertManager convertManager, AuthContext authContext, DocumentManager documentManager,
            ConfigurationManager configurationManager) {
        this.attachmentManager = attachmentManager;
        this.attachmentUtil = attachmentUtil;
        this.convertManager = convertManager;
        this.authContext = authContext;
        this.documentManager = documentManager;
        this.configurationManager = configurationManager;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!authContext.checkUserAuthorisation(request, response)) {
            return;
        }

        String attachmentIdString = request.getParameter("attachmentId");
        Long attachmentId = Long.parseLong(attachmentIdString);
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter writer = response.getWriter();

        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        String ext = attachment.getFileExtension();
        String fn = attachment.getFileName();
        String newExt = convertManager.convertsTo(ext);

        contextMap.put("attachmentId", attachmentIdString);
        contextMap.put("oldName", fn);
        contextMap.put("oldExt", ext);
        contextMap.put("newName", fn.substring(0, fn.length() - ext.length()) + newExt);
        writer.write(getTemplate(contextMap));
    }

    private String getTemplate(Map<String, Object> map) throws UnsupportedEncodingException {
        return VelocityUtils.getRenderedTemplate("templates/convert.vm", map);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!authContext.checkUserAuthorisation(request, response)) {
            return;
        }

        String attachmentIdString = request.getParameter("attachmentId");
        ConfluenceUser user = null;
        String errorMessage = null;
        JSONObject json = null;

        try {
            Long attachmentId = Long.parseLong(attachmentIdString);
            log.info("attachmentId " + attachmentId);

            Attachment attachment = attachmentManager.getAttachment(attachmentId);

            user = AuthenticatedUserThreadLocal.get();
            log.info("user " + user);

            String ext = request.getParameter("oldExt");

            if (attachmentUtil.checkAccess(attachmentId, user, true)) {
                if (convertManager.isConvertable(ext)) {
                    json = convertManager.convert(attachmentId, ext);

                    if (json.getBoolean("endConvert")) {
                        Long newAttachmentId = savefile(attachment, json.getString("fileUrl"), request.getParameter("newName"));
                        json.put("attachmentId", newAttachmentId);
                    }
                } else {
                    errorMessage = "Files of " + ext + " format cannot be converted";
                }
            } else {
                log.error("access deny");
                errorMessage = "You don not have enough permission to convert the file";
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);
            errorMessage = ex.toString();
        }

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        if (errorMessage != null) {
            writer.write("{\"error\":\"" + errorMessage + "\"}");
        } else {
            writer.write(json.toString());
        }
    }

    private Long savefile(Attachment attachment, String fileUrl, String newName) throws Exception {
        log.info("downloadUri = " + fileUrl);

        URL url = new URL(fileUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        Integer timeout = Integer.parseInt(configurationManager.getProperty("timeout")) * 1000;
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        Integer size = connection.getContentLength();
        log.info("size = " + size);

        Attachment copy = attachment.copyLatestVersion();
        InputStream stream = connection.getInputStream();

        copy.setContainer(attachment.getContainer());
        copy.setFileName(newName);
        copy.setFileSize(size);
        copy.setMediaType(documentManager.getMimeType(newName));

        attachmentManager.saveAttachment(copy, null, stream);

        return copy.getLatestVersionId();
    }

}
