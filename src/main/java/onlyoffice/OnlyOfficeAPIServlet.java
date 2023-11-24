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

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.status.service.SystemInformationService;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.google.gson.Gson;
import com.onlyoffice.manager.request.RequestManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.url.UrlManager;
import onlyoffice.sdk.manager.document.DocumentManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import onlyoffice.utils.parsing.ParsingUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnlyOfficeAPIServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeAPIServlet");

    private final SystemInformationService sysInfoService;
    private final SettingsManager settingsManager;
    private final JwtManager jwtManager;
    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;
    private final ParsingUtil parsingUtil;
    private final UrlManager urlManager;
    private final RequestManager requestManager;

    public OnlyOfficeAPIServlet(final SystemInformationService sysInfoService, final SettingsManager settingsManager,
                                final JwtManager jwtManager, final DocumentManager documentManager,
                                final AttachmentUtil attachmentUtil, final ParsingUtil parsingUtil,
                                final UrlManager urlManager, final RequestManager requestManager) {
        this.sysInfoService = sysInfoService;
        this.settingsManager = settingsManager;
        this.jwtManager = jwtManager;
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
        this.parsingUtil = parsingUtil;
        this.urlManager = urlManager;
        this.requestManager = requestManager;
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        String type = request.getParameter("type");
        if (type != null) {
            switch (type.toLowerCase()) {
                case "save-as":
                    saveAs(request, response);
                    break;
                case "attachment-data":
                    attachmentData(request, response);
                    break;
                case "reference-data":
                    referenceData(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }

    private void saveAs(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        InputStream requestStream = request.getInputStream();
        String body = parsingUtil.getBody(requestStream);

        try {
            JSONObject bodyJson = new JSONObject(body);
            String downloadUrl = bodyJson.getString("url");
            String title = bodyJson.getString("title");
            String ext = bodyJson.getString("ext");
            String pageIdString = bodyJson.getString("pageId");

            if (downloadUrl.isEmpty() || title.isEmpty() || ext.isEmpty() || pageIdString.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Long pageId = Long.parseLong(pageIdString);

            if (!attachmentUtil.checkAccessCreate(user, pageId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            downloadUrl = urlManager.replaceToInnerDocumentServerUrl(downloadUrl);

            requestManager.executeGetRequest(downloadUrl, new RequestManager.Callback<Void>() {
                @Override
                public Void doWork(final Object response) throws Exception {
                    byte[] bytes = IOUtils.toByteArray(((HttpEntity) response).getContent());
                    InputStream inputStream = new ByteArrayInputStream(bytes);

                    log.info("size = " + bytes.length);

                    String fileName = attachmentUtil.getCorrectName(title, ext, pageId);
                    String mimeType = documentManager.getMimeType(fileName);

                    attachmentUtil.createNewAttachment(fileName, mimeType, inputStream, bytes.length, pageId, user);
                    return null;
                }
            });
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    private void attachmentData(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        InputStream requestStream = request.getInputStream();
        String body = parsingUtil.getBody(requestStream);

        try {
            JSONObject bodyJson = new JSONObject(body);
            JSONArray attachments = bodyJson.getJSONArray("attachments");

            List<Object> responseJson = new ArrayList<>();
            Gson gson = new Gson();

            for (int i = 0; i < attachments.length(); i++) {
                Long attachmentId = attachments.getLong(i);

                if (attachmentUtil.checkAccess(attachmentId, user, false)) {
                    Map<String, String> data = new HashMap<>();

                    String documentName = documentManager.getDocumentName(String.valueOf(attachmentId));
                    String fileType = documentManager.getExtension(documentName);

                    if (bodyJson.has("command")) {
                        data.put("command", bodyJson.getString("command"));
                    }
                    data.put("fileType", fileType);
                    data.put("url", urlManager.getFileUrl(String.valueOf(attachmentId)));
                    if (settingsManager.isSecurityEnabled()) {
                        JSONObject dataJSON = new JSONObject(gson.toJson(data));
                        data.put("token", jwtManager.createToken(dataJSON));
                    }

                    responseJson.add(data);
                }
            }

            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write(gson.toJson(responseJson));
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    private void referenceData(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        InputStream requestStream = request.getInputStream();
        String body = parsingUtil.getBody(requestStream);

        try {
            JSONObject bodyJson = new JSONObject(body);
            JSONObject referenceData = new JSONObject();
            Long attachmentId = null;

            if (bodyJson.has("referenceData")) {
                referenceData = bodyJson.getJSONObject("referenceData");
                if (referenceData.getString("instanceId").equals(sysInfoService.getConfluenceInfo().getBaseUrl())) {
                    attachmentId = referenceData.getLong("fileKey");
                }
            }

            Attachment attachment = attachmentUtil.getAttachment(attachmentId);

            if (attachment == null) {
                String pageIdString = request.getParameter("pageId");

                if (pageIdString != null && !pageIdString.isEmpty()) {
                    Long pageId = Long.parseLong(pageIdString);
                    attachment = attachmentUtil.getAttachmentByName(bodyJson.getString("path"), pageId);
                    if (attachment != null) {
                        attachmentId = attachment.getId();
                        referenceData.put("fileKey", attachment.getId());
                        referenceData.put("instanceId", sysInfoService.getConfluenceInfo().getBaseUrl());
                    }
                }
            }

            if (attachment == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (!attachmentUtil.checkAccess(attachmentId, user, false)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            JSONObject responseJson = new JSONObject();

            String documentName = documentManager.getDocumentName(String.valueOf(attachmentId));
            String extension = documentManager.getExtension(documentName);

            responseJson.put("fileType", extension);
            responseJson.put("path", documentName);
            responseJson.put("referenceData", referenceData);
            responseJson.put("url", urlManager.getFileUrl(String.valueOf(attachmentId)));

            if (settingsManager.isSecurityEnabled()) {
                responseJson.put("token", jwtManager.createToken(responseJson));
            }

            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write(responseJson.toString());
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
