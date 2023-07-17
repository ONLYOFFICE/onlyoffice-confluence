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

import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.spring.container.ContainerManager;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.convert.ConvertManager;
import onlyoffice.managers.jwt.JwtManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import onlyoffice.utils.parsing.ParsingUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class OnlyOfficeSaveFileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeSaveFileServlet");

    private static final int STATUS_EDITING = 1;
    private static final int STATUS_MUST_SAVE = 2;
    private static final int STATUS_CORRUPTED = 3;
    private static final int STATUS_CLOSED = 4;
    private static final int STATUS_FORCE_SAVE = 6;
    private static final int STATUS_CORRUPTED_FORCE_SAVE = 7;

    private final JwtManager jwtManager;
    private final AttachmentUtil attachmentUtil;
    private final ParsingUtil parsingUtil;
    private final UrlManager urlManager;
    private final ConfigurationManager configurationManager;
    private final ConvertManager convertManager;

    public OnlyOfficeSaveFileServlet(final JwtManager jwtManager, final AttachmentUtil attachmentUtil,
                                     final ParsingUtil parsingUtil, final UrlManager urlManager,
                                     final ConfigurationManager configurationManager,
                                     final ConvertManager convertManager) {
        this.jwtManager = jwtManager;
        this.attachmentUtil = attachmentUtil;
        this.parsingUtil = parsingUtil;
        this.urlManager = urlManager;
        this.configurationManager = configurationManager;
        this.convertManager = convertManager;
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain; charset=utf-8");

        String token = request.getParameter("token");
        String payload;
        JSONObject bodyFromToken;

        try {
            payload = jwtManager.verifyInternalToken(token);
            bodyFromToken = new JSONObject(payload);

            if (!bodyFromToken.getString("action").equals("callback")) {
                throw new SecurityException();
            }
        } catch (Exception e) {
            throw new SecurityException("Invalid link token!");
        }

        String userKeyString = bodyFromToken.getString("userKey");
        String attachmentIdString = bodyFromToken.getString("attachmentId");

        UserAccessor userAccessor = (UserAccessor) ContainerManager.getComponent("userAccessor");

        UserKey userKey = new UserKey(userKeyString);
        ConfluenceUser user = userAccessor.getUserByKey(userKey);
        Long attachmentId = Long.parseLong(attachmentIdString);

        if (attachmentUtil.getAttachment(attachmentId) == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String error = "";
        try {
            processData(attachmentId, user, request);
        } catch (Exception e) {
            error = e.getMessage();
        }

        PrintWriter writer = response.getWriter();
        if (error.isEmpty()) {
            writer.write("{\"error\":0}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writer.write("{\"error\":1,\"message\":\"" + error + "\"}");
        }

        log.info("error = " + error);
    }

    private void processData(final Long attachmentId, final ConfluenceUser user, final HttpServletRequest request)
            throws Exception {
        log.info("attachmentId = " + attachmentId.toString());
        InputStream requestStream = request.getInputStream();

        try {
            String body = parsingUtil.getBody(requestStream);
            log.info("body = " + body);
            if (body.isEmpty()) {
                throw new IllegalArgumentException("requestBody is empty");
            }

            JSONObject jsonObj = new JSONObject(body);

            if (jwtManager.jwtEnabled()) {
                String token = jsonObj.optString("token");
                String payload = null;
                Boolean inBody = true;

                if (token == null || token == "") {
                    String jwth = jwtManager.getJwtHeader();
                    String header = (String) request.getHeader(jwth);
                    String authorizationPrefix = "Bearer ";
                    token = (header != null && header.startsWith(authorizationPrefix))
                            ? header.substring(authorizationPrefix.length()) : header;
                    inBody = false;
                }

                if (token == null || token == "") {
                    throw new SecurityException("Try save without JWT");
                }

                try {
                    payload = jwtManager.verify(token);
                } catch (Exception e) {
                    throw new SecurityException("Try save with wrong JWT");
                }

                JSONObject bodyFromToken = new JSONObject(payload);

                if (inBody) {
                    jsonObj = bodyFromToken;
                } else {
                    jsonObj = bodyFromToken.getJSONObject("payload");
                }
            }

            long status = jsonObj.getLong("status");
            log.info("status = " + status);
            log.info("user = " + user);

            if (status == STATUS_EDITING) {
                if (jsonObj.has("actions")) {
                    JSONArray actions = jsonObj.getJSONArray("actions");
                    if (actions.length() > 0) {
                        JSONObject action = (JSONObject) actions.get(0);
                        if (action.getLong("type") == 1) {
                            if (user == null || !attachmentUtil.checkAccess(attachmentId, user, true)) {
                                throw new SecurityException("Access denied. User " + user
                                        + " don't have the appropriate permissions to edit this document.");
                            }

                            if (attachmentUtil.getCollaborativeEditingKey(attachmentId) == null) {
                                String key = jsonObj.getString("key");
                                attachmentUtil.setCollaborativeEditingKey(attachmentId, key);
                            }
                        }
                    }
                }
            }

            if (status == STATUS_MUST_SAVE || status == STATUS_CORRUPTED) {
                if (user != null && attachmentUtil.checkAccess(attachmentId, user, true)) {
                    String downloadUrl = jsonObj.getString("url");
                    downloadUrl = urlManager.replaceDocEditorURLToInternal(downloadUrl);
                    log.info("downloadUri = " + downloadUrl);

                    JSONObject history = jsonObj.getJSONObject("history");
                    String changesUrl = !jsonObj.isNull("changesurl")
                            ? urlManager.replaceDocEditorURLToInternal(jsonObj.getString("changesurl"))
                            : null;
                    log.info("changesUri = " + changesUrl);

                    Boolean forceSaveVersion =
                            attachmentUtil.getPropertyAsBoolean(attachmentId, "onlyoffice-force-save");

                    attachmentUtil.setCollaborativeEditingKey(attachmentId, null);

                    if (forceSaveVersion) {
                        saveAttachmentFromUrl(attachmentId, downloadUrl, user, false);
                        attachmentUtil.removeProperty(attachmentId, "onlyoffice-force-save");
                        attachmentUtil.removeAttachmentChanges(attachmentId);

                        File convertedFile = attachmentUtil.getConvertedFile(attachmentId);
                        if (convertedFile.exists()) {
                            convertedFile.delete();
                        }
                    } else {
                        saveAttachmentFromUrl(attachmentId, downloadUrl, user, true);
                    }

                    attachmentUtil.saveAttachmentChanges(attachmentId, history.toString(), changesUrl);
                } else {
                    throw new SecurityException("Try save without access: " + user);
                }
            }

            if (status == STATUS_CLOSED) {
                attachmentUtil.setCollaborativeEditingKey(attachmentId, null);
            }

            if (status == STATUS_FORCE_SAVE || status == STATUS_CORRUPTED_FORCE_SAVE) {
                if (user != null && attachmentUtil.checkAccess(attachmentId, user, true)) {
                    if (configurationManager.forceSaveEnabled()) {
                        String downloadUrl = jsonObj.getString("url");
                        downloadUrl = urlManager.replaceDocEditorURLToInternal(downloadUrl);
                        log.info("downloadUri = " + downloadUrl);

                        JSONObject history = jsonObj.getJSONObject("history");
                        String changesUrl = !jsonObj.isNull("changesurl")
                                ? urlManager.replaceDocEditorURLToInternal(jsonObj.getString("changesurl"))
                                : null;
                        log.info("changesUri = " + downloadUrl);

                        Boolean forceSaveVersion =
                                attachmentUtil.getPropertyAsBoolean(attachmentId, "onlyoffice-force-save");

                        if (forceSaveVersion) {
                            saveAttachmentFromUrl(attachmentId, downloadUrl, user, false);
                            attachmentUtil.removeAttachmentChanges(attachmentId);
                        } else {
                            String key = attachmentUtil.getCollaborativeEditingKey(attachmentId);
                            attachmentUtil.setCollaborativeEditingKey(attachmentId, null);

                            saveAttachmentFromUrl(attachmentId, downloadUrl, user, true);
                            attachmentUtil.setCollaborativeEditingKey(attachmentId, key);
                            attachmentUtil.setProperty(attachmentId, "onlyoffice-force-save", "true");
                        }

                        attachmentUtil.saveAttachmentChanges(attachmentId, history.toString(), changesUrl);

                        File convertedFile = attachmentUtil.getConvertedFile(attachmentId);
                        if (convertedFile.exists()) {
                            convertedFile.delete();
                        }
                    } else {
                        log.info("Forcesave is disabled, ignoring forcesave request");
                    }
                } else {
                    throw new SecurityException("Try save without access: " + user);
                }
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);

            throw ex;
        }
    }

    private void saveAttachmentFromUrl(final Long attachmentId, final String downloadUrl, final ConfluenceUser user,
                                       final boolean newVersion) throws Exception {
        String attachmentExt = attachmentUtil.getFileExt(attachmentId);
        String extDownloadUrl = downloadUrl.substring(downloadUrl.lastIndexOf(".") + 1);
        String url = downloadUrl;

        if (!attachmentExt.equals(extDownloadUrl)) {
            JSONObject response =
                    convertManager.convert(attachmentId, extDownloadUrl, attachmentExt, downloadUrl, null, false, null);
            url = response.getString("fileUrl");
        }

        try (CloseableHttpClient httpClient = configurationManager.getHttpClient()) {
            HttpGet request = new HttpGet(url);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();

                if (status == HttpStatus.SC_OK) {
                    byte[] bytes = IOUtils.toByteArray(entity.getContent());
                    InputStream inputStream = new ByteArrayInputStream(bytes);

                    if (newVersion) {
                        attachmentUtil.saveAttachmentAsNewVersion(attachmentId, inputStream, bytes.length, user);
                    } else {
                        attachmentUtil.updateAttachment(attachmentId, inputStream, bytes.length, user);
                    }
                } else {
                    throw new HttpException("Document Server returned code " + status);
                }
            }
        }
    }
}
