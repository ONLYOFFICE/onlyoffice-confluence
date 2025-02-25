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

import com.atlassian.annotations.security.AnonymousSiteAccess;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.status.service.SystemInformationService;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.user.actions.ProfilePictureInfo;
import com.atlassian.sal.api.user.UserKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.manager.request.RequestManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.model.common.User;
import com.onlyoffice.model.documenteditor.config.document.ReferenceData;
import onlyoffice.model.dto.UsersInfoRequest;
import onlyoffice.model.dto.UsersInfoResponse;
import onlyoffice.sdk.manager.document.DocumentManager;
import onlyoffice.sdk.manager.url.UrlManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import onlyoffice.utils.parsing.ParsingUtil;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AnonymousSiteAccess
public class OnlyOfficeAPIServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeAPIServlet");

    private final SystemInformationService sysInfoService;
    private final UserAccessor userAccessor;
    private final SettingsManager settingsManager;
    private final JwtManager jwtManager;
    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;
    private final ParsingUtil parsingUtil;
    private final UrlManager urlManager;
    private final RequestManager requestManager;
    private final PermissionManager permissionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OnlyOfficeAPIServlet(final SystemInformationService sysInfoService, final UserAccessor userAccessor,
                                final SettingsManager settingsManager, final JwtManager jwtManager,
                                final DocumentManager documentManager, final AttachmentUtil attachmentUtil,
                                final ParsingUtil parsingUtil, final UrlManager urlManager,
                                final RequestManager requestManager, final PermissionManager permissionManager) {
        this.sysInfoService = sysInfoService;
        this.userAccessor = userAccessor;
        this.settingsManager = settingsManager;
        this.jwtManager = jwtManager;
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
        this.parsingUtil = parsingUtil;
        this.urlManager = urlManager;
        this.requestManager = requestManager;
        this.permissionManager = permissionManager;
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
                case "users-info":
                    usersInfo(request, response);
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

            for (int i = 0; i < attachments.length(); i++) {
                Long attachmentId = attachments.getLong(i);

                if (attachmentUtil.checkAccess(attachmentId, user, false)) {
                    Map<String, String> data = new HashMap<>();

                    String documentName = documentManager.getDocumentName(String.valueOf(attachmentId));
                    String fileType = documentManager.getExtension(documentName);

                    if (!bodyJson.get("command").equals(null)) {
                        data.put("command", bodyJson.getString("command"));
                    }
                    data.put("fileType", fileType);
                    data.put("url", urlManager.getFileUrl(String.valueOf(attachmentId)));
                    if (settingsManager.isSecurityEnabled()) {
                        data.put("token", jwtManager.createToken(data));
                    }

                    responseJson.add(data);
                }
            }

            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write(objectMapper.writeValueAsString(responseJson));
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
            ReferenceData referenceData = new ReferenceData();
            Long attachmentId = null;

            if (bodyJson.has("referenceData")) {
                String referenceDataString = bodyJson.getJSONObject("referenceData").toString();
                referenceData = objectMapper.readValue(referenceDataString, ReferenceData.class);
                if (referenceData.getInstanceId().equals(sysInfoService.getConfluenceInfo().getBaseUrl())) {
                    attachmentId = Long.valueOf(referenceData.getFileKey());
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
                        referenceData.setFileKey(String.valueOf(attachment.getId()));
                        referenceData.setInstanceId(sysInfoService.getConfluenceInfo().getBaseUrl());
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

            Map<String, Object> responseMap = new HashMap<>();

            String documentName = documentManager.getDocumentName(String.valueOf(attachmentId));
            String extension = documentManager.getExtension(documentName);

            responseMap.put("fileType", extension);
            responseMap.put("path", documentName);
            responseMap.put("referenceData", referenceData);
            responseMap.put("url", urlManager.getFileUrl(String.valueOf(attachmentId)));

            if (settingsManager.isSecurityEnabled()) {
                responseMap.put("token", jwtManager.createToken(responseMap));
            }

            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write(objectMapper.writeValueAsString(responseMap));
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void usersInfo(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        ConfluenceUser currentUser = AuthenticatedUserThreadLocal.get();

        if (!permissionManager.hasPermission(currentUser, Permission.VIEW, PermissionManager.TARGET_PEOPLE_DIRECTORY)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        UsersInfoRequest usersInfoRequest = new UsersInfoRequest();

        try (InputStream requestStream = request.getInputStream()) {
            String bodyString = parsingUtil.getBody(requestStream);

            if (bodyString.isEmpty()) {
                throw new IllegalArgumentException("requestBody is empty");
            }

            usersInfoRequest = objectMapper.readValue(bodyString, UsersInfoRequest.class);
        } catch (IOException e) {
            throw e;
        }

        List<User> users = new ArrayList<>();

        for (String userKeyString : usersInfoRequest.getIds()) {
            if (userKeyString == null || userKeyString.isEmpty()) {
                continue;
            }

            UserKey userKey = new UserKey(userKeyString);
            ConfluenceUser confluenceUser = userAccessor.getUserByKey(userKey);

            if (confluenceUser != null) {
                User user = User.builder()
                        .id(confluenceUser.getKey().getStringValue())
                        .name(confluenceUser.getFullName())
                        .build();

                ProfilePictureInfo profilePictureInfo = userAccessor.getUserProfilePicture(confluenceUser);

                if (profilePictureInfo != null && !profilePictureInfo.isDefault()) {
                    try (InputStream pictureInputStream = profilePictureInfo.getBytes()) {
                        byte[] pictureByteArray = IOUtils.toByteArray(pictureInputStream);
                        String pictureBase64 = Base64.getEncoder().encodeToString(pictureByteArray);
                        String contentType = profilePictureInfo.getContentType();

                        user.setImage("data:" + contentType + ";base64," + pictureBase64);
                    }
                }

                users.add(user);
            }
        }

        UsersInfoResponse usersInfoResponse = new UsersInfoResponse();
        usersInfoResponse.setUsers(users);

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(usersInfoResponse));
    }
}
