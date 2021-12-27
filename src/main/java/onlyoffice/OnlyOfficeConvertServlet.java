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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import onlyoffice.managers.configuration.ConfigurationManager;
import com.atlassian.confluence.pages.PageManager;
import onlyoffice.managers.convert.ConvertManager;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
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
    private final PageManager pageManager;

    @Inject
    public OnlyOfficeConvertServlet(AttachmentManager attachmentManager, AttachmentUtil attachmentUtil,
            ConvertManager convertManager, AuthContext authContext, DocumentManager documentManager,
            ConfigurationManager configurationManager, PageManager pageManager) {
        this.attachmentManager = attachmentManager;
        this.attachmentUtil = attachmentUtil;
        this.convertManager = convertManager;
        this.authContext = authContext;
        this.documentManager = documentManager;
        this.configurationManager = configurationManager;
        this.pageManager = pageManager;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!authContext.checkUserAuthorisation(request, response)) {
            return;
        }
        String pageIdString = request.getParameter("pageId");
        String newName = request.getParameter("newName");

        String attachmentIdString = request.getParameter("attachmentId");
        Long attachmentId = Long.parseLong(attachmentIdString);
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter writer = response.getWriter();

        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        Long pageId = attachment.getContainer().getId();
        String fileName = attachment.getFileName();
        String ext = attachment.getFileExtension();
        String newExt = convertManager.convertsTo(ext);

        if (pageIdString != null && !pageIdString.isEmpty()) {
            pageId = Long.parseLong(pageIdString);
            contextMap.put("pageId", pageId);
        }

        if (newName != null && !newName.isEmpty()) {
            newName = documentManager.getCorrectName(newName, newExt, pageId);
        } else {
            newName = documentManager.getCorrectName(fileName.substring(0, fileName.lastIndexOf(".")), newExt, pageId);
        }

        contextMap.put("attachmentId", attachmentIdString);
        contextMap.put("oldName", fileName);
        contextMap.put("newName", newName);
        contextMap.put("oldExt", ext);
        contextMap.put("newExt", newExt);
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

            String fileName = attachment.getFileName();
            String ext = attachment.getFileExtension();
            String title = fileName.substring(0, fileName.lastIndexOf("."));
            String pageIdAsString = request.getParameter("pageId");

            Long pageId = null;
            if (pageIdAsString != null && !pageIdAsString.isEmpty()) {
                pageId = Long.parseLong(pageIdAsString);
            } else {
                pageId = attachment.getContainer().getId();
            }

            if (attachmentUtil.checkAccess(attachmentId, user, false) && attachmentUtil.checkAccessCreate(user, pageId)) {
                if (convertManager.isConvertable(ext)) {
                    String convertToExt = convertManager.convertsTo(ext);
                    json = convertManager.convert(attachmentId, ext, convertToExt);

                    if (json.has("endConvert") && json.getBoolean("endConvert")) {
                        String newFileName = documentManager.getCorrectName(title, convertToExt, pageId);
                        Long newAttachmentId = savefile(attachment, json.getString("fileUrl"), newFileName, pageId);
                        json.put("attachmentId", newAttachmentId);
                    } else if (json.has("error")) {
                        errorMessage = "Unknown conversion error";
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

    private Long savefile(Attachment attachment, String fileUrl, String newName, Long pageId) throws Exception {
        log.info("downloadUri = " + fileUrl);

        try (CloseableHttpClient httpClient = configurationManager.getHttpClient()) {
            HttpGet request = new HttpGet(fileUrl);

            try (CloseableHttpResponse response = httpClient.execute(request)) {

                int status = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();

                if (status == HttpStatus.SC_OK) {
                    InputStream stream = entity.getContent();
                    Long size = entity.getContentLength();

                    Attachment copy = attachment.copyLatestVersion();

                    copy.setContainer(pageManager.getPage(pageId));
                    copy.setFileName(newName);
                    copy.setFileSize(size);
                    copy.setMediaType(documentManager.getMimeType(newName));

                    attachmentManager.saveAttachment(copy, null, stream);

                    return copy.getLatestVersionId();
                } else {
                    throw new HttpException("Document Server returned code " + status);
                }
            }
        }
    }

}
