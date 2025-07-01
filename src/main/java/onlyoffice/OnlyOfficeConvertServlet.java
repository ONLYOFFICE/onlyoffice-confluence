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
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.model.common.CommonResponse;
import com.onlyoffice.model.common.Format;
import com.onlyoffice.model.convertservice.ConvertRequest;
import com.onlyoffice.model.convertservice.ConvertResponse;
import com.onlyoffice.model.convertservice.convertrequest.PDF;
import com.onlyoffice.service.convert.ConvertService;
import onlyoffice.managers.auth.AuthContext;
import onlyoffice.sdk.manager.document.DocumentManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class OnlyOfficeConvertServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeConvertServlet");

    private final LocaleManager localeManager;
    private final AttachmentManager attachmentManager;
    private final AttachmentUtil attachmentUtil;
    private final ConvertService convertService;
    private final AuthContext authContext;
    private final DocumentManager documentManager;
    private final DocumentServerClient documentServerClient;

    public OnlyOfficeConvertServlet(final LocaleManager localeManager, final AttachmentManager attachmentManager,
                                    final AttachmentUtil attachmentUtil, final ConvertService convertService,
                                    final AuthContext authContext, final DocumentManager documentManager,
                                    final DocumentServerClient documentServerClient) {
        this.localeManager = localeManager;
        this.attachmentManager = attachmentManager;
        this.attachmentUtil = attachmentUtil;
        this.convertService = convertService;
        this.authContext = authContext;
        this.documentManager = documentManager;
        this.documentServerClient = documentServerClient;
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        if (!authContext.checkUserAuthorization(request, response)) {
            return;
        }
        String pageIdString = request.getParameter("pageId");
        String newTitle = request.getParameter("newTitle");
        Boolean createForm = Boolean.valueOf(request.getParameter("createForm"));

        String attachmentIdString = request.getParameter("attachmentId");
        Long attachmentId = Long.parseLong(attachmentIdString);
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter writer = response.getWriter();

        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        Long pageId = attachment.getContainer().getId();
        String fileName = attachment.getFileName();
        String newFileExtension = documentManager.getDefaultConvertExtension(fileName);

        String extension = documentManager.getExtension(fileName);
        Format docx = documentManager.getFormats().stream()
                .filter(format -> format.getName().equals("docx"))
                .findFirst()
                .get();

        if (docx != null
                && extension.equals(docx.getName())
                && docx.getConvert() != null
                && docx.getConvert().contains("pdf")) {
            newFileExtension = "pdf";
        }

        String title = fileName.substring(0, fileName.lastIndexOf("."));

        if (pageIdString != null && !pageIdString.isEmpty()) {
            pageId = Long.parseLong(pageIdString);
            contextMap.put("pageId", pageId);
        }

        if (newTitle != null && !newTitle.isEmpty()) {
            contextMap.put("newTitle", newTitle);
            title = newTitle;
        }

        String newName = documentManager.getCorrectNewFileName(title, newFileExtension, pageId);

        contextMap.put("attachmentId", attachmentIdString);
        contextMap.put("oldName", fileName);
        contextMap.put("newName", newName);
        contextMap.put("createForm", createForm);
        writer.write(getTemplate(contextMap));
    }

    private String getTemplate(final Map<String, Object> map) throws UnsupportedEncodingException {
        return VelocityUtils.getRenderedTemplate("templates/convert.vm", map);
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        if (!authContext.checkUserAuthorization(request, response)) {
            return;
        }

        String attachmentIdString = request.getParameter("attachmentId");
        Boolean createForm = Boolean.valueOf(request.getParameter("createForm"));

        ConfluenceUser user = null;
        JSONObject json = null;

        Long attachmentId = Long.parseLong(attachmentIdString);
        log.info("attachmentId " + attachmentId);

        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        user = AuthenticatedUserThreadLocal.get();
        log.info("user " + user);

        String fileName = attachment.getFileName();
        String title = fileName.substring(0, fileName.lastIndexOf("."));

        String pageIdAsString = request.getParameter("pageId");
        String newTitle = request.getParameter("newTitle");

        if (newTitle != null && !newTitle.isEmpty()) {
            title = newTitle;
        }

        Long pageId = null;
        if (pageIdAsString != null && !pageIdAsString.isEmpty()) {
            pageId = Long.parseLong(pageIdAsString);
        } else {
            pageId = attachment.getContainer().getId();
        }

        String convertToExt = documentManager.getDefaultConvertExtension(fileName);

        String extension = documentManager.getExtension(fileName);
        Format docx = documentManager.getFormats().stream()
                .filter(format -> format.getName().equals("docx"))
                .findFirst()
                .get();

        if (docx != null
                && extension.equals(docx.getName())
                && docx.getConvert() != null
                && docx.getConvert().contains("pdf")) {
            convertToExt = "pdf";
        }

        if (!attachmentUtil.checkAccess(attachmentId, user, false)
                || !attachmentUtil.checkAccessCreate(user, pageId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (convertToExt == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();

        try {
            String region = localeManager.getLocale(user).toLanguageTag();

            ConvertRequest convertRequest = ConvertRequest.builder()
                    .async(true)
                    .outputtype(convertToExt)
                    .region(region)
                    .build();

            if (convertToExt.equals("pdf") && createForm) {
                convertRequest.setPdf(new PDF(true));
            }

            ConvertResponse convertResponse = convertService.processConvert(convertRequest,
                    String.valueOf(attachmentId));

            json = new JSONObject(convertResponse);

            if (convertResponse.getEndConvert() != null && convertResponse.getEndConvert()) {
                String newFileName = documentManager.getCorrectNewFileName(title, convertToExt, pageId);
                Long newAttachmentId = savefile(attachment, convertResponse.getFileUrl(), newFileName, pageId);
                json.put("attachmentId", newAttachmentId);
            }

            writer.write(json.toString());
        } catch (IOException e) {
            CommonResponse commonResponse = new CommonResponse();
            commonResponse.setError(CommonResponse.Error.CONNECTION);
            json = new JSONObject(commonResponse);
            writer.write(json.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Long savefile(final Attachment attachment, final String fileUrl, final String newName, final Long pageId)
            throws Exception {
        log.info("downloadUri = " + fileUrl);

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(null, null);

            documentServerClient.getFile(
                    fileUrl,
                    Files.newOutputStream(tempFile)
            );

            Attachment copy = attachment.copyLatestVersion();

            copy.setContainer(attachmentUtil.getContainer(pageId));
            copy.setFileName(newName);
            copy.setFileSize(tempFile.toFile().length());
            copy.setMediaType(documentManager.getMimeType(newName));

            attachmentManager.saveAttachment(copy, null, Files.newInputStream(tempFile));
            attachmentUtil.setCollaborativeEditingKey(copy.getLatestVersionId(), null);

            return copy.getLatestVersionId();
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

}
