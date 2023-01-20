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

import com.atlassian.confluence.core.DateFormatter;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.ConfluenceUserPreferences;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.spring.container.ContainerManager;
import com.google.gson.Gson;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.managers.jwt.JwtManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import onlyoffice.utils.parsing.ParsingUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

public class OnlyOfficeHistoryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeHistoryServlet");

    @ComponentImport
    private final LocaleManager localeManager;
    @ComponentImport
    private final FormatSettingsManager formatSettingsManager;

    private final AuthContext authContext;
    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;
    private final UrlManager urlManager;
    private final JwtManager jwtManager;
    private final ParsingUtil parsingUtil;

    @Inject
    public OnlyOfficeHistoryServlet(LocaleManager localeManager, FormatSettingsManager formatSettingsManager,
            AuthContext authContext, DocumentManager documentManager, AttachmentUtil attachmentUtil,
            UrlManager urlManager, JwtManager jwtManager, ParsingUtil parsingUtil) {
        this.localeManager = localeManager;
        this.formatSettingsManager = formatSettingsManager;
        this.authContext = authContext;
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
        this.urlManager = urlManager;
        this.jwtManager = jwtManager;
        this.parsingUtil = parsingUtil;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String type = request.getParameter("type");
        if (type != null) {
            switch (type.toLowerCase())
            {
                case "diff":
                    getAttachmentDiff(request, response);
                    break;
                case "info":
                    getAttachmentHistoryInfo(request, response);
                    break;
                case "data":
                    getAttachmentHistoryData(request, response);
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

    private void getAttachmentDiff (HttpServletRequest request, HttpServletResponse response) throws IOException {
        String vkey = request.getParameter("vkey");
        String attachmentIdString = documentManager.readHash(vkey);

        if (attachmentIdString.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Long attachmentId = Long.parseLong(attachmentIdString);
        Attachment diff = attachmentUtil.getAttachmentDiff(attachmentId);

        if (diff != null) {
            InputStream inputStream = attachmentUtil.getAttachmentData(diff.getId());
            String publicDocEditorUrl = urlManager.getPublicDocEditorUrl();

            if (publicDocEditorUrl.endsWith("/")) {
                publicDocEditorUrl = publicDocEditorUrl.substring(0, publicDocEditorUrl.length() - 1);
            }

            response.setContentType(diff.getMediaType());
            response.setContentLength(inputStream.available());
            response.addHeader("Access-Control-Allow-Origin", publicDocEditorUrl);

            byte[] buffer = new byte[10240];

            OutputStream output = response.getOutputStream();
            for (int length = 0; (length = inputStream.read(buffer)) > 0; ) {
                output.write(buffer, 0, length);
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }

    private void getAttachmentHistoryInfo (HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!authContext.checkUserAuthorisation(request, response)) {
            return;
        }

        String vkey = request.getParameter("vkey");
        String attachmentIdString = documentManager.readHash(vkey);

        if (attachmentIdString.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Long attachmentId = Long.parseLong(attachmentIdString);
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        if (!attachmentUtil.checkAccess(attachmentId, user, false)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        List<Attachment> attachments = attachmentUtil.getAllVersions(attachmentId);
        if (attachments != null) {
            UserAccessor userAccessor = (UserAccessor) ContainerManager.getComponent("userAccessor");
            ConfluenceUserPreferences preferences = userAccessor.getConfluenceUserPreferences(user);
            DateFormatter dateFormatter = preferences.getDateFormatter(formatSettingsManager, localeManager);
            Gson gson = new Gson();

            Attachment prevVersion = null;
            Collections.reverse(attachments);
            List<Version> history = new ArrayList<>();
            for (Attachment attachment : attachments) {
                Version version = new Version();
                version.setVersion(attachment.getVersion());
                version.setKey(documentManager.getKeyOfFile(attachment.getId()));
                version.setCreated(dateFormatter.formatDateTime(attachment.getCreationDate()));
                version.setUser(attachment.getCreator().getName(), attachment.getCreator().getFullName());

                Attachment changes = attachmentUtil.getAttachmentChanges(attachment.getId());
                if (changes != null) {
                    if (prevVersion != null && (attachment.getVersion() - prevVersion.getVersion()) == 1) {
                        InputStream changesSteam = attachmentUtil.getAttachmentData(changes.getId());
                        String changesString = parsingUtil.getBody(changesSteam);
                        JSONObject changesJSON = null;
                        try {
                            changesJSON = new JSONObject(changesString);
                            version.setServerVersion(changesJSON.getString("serverVersion"));
                            version.setChanges(gson.fromJson(changesJSON.getString("changes"), Object.class));
                        } catch (JSONException e) {
                            throw new IOException(e.getMessage());
                        }
                    } else {
                        attachmentUtil.removeAttachmentChanges(attachment.getId());
                    }
                }
                prevVersion = attachment;
                history.add(version);
            }

            Map<String, Object> historyInfo = new HashMap<>();

            historyInfo.put("currentVersion", attachmentUtil.getVersion(attachmentId));
            historyInfo.put("history", history);

            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write(gson.toJson(historyInfo));
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }

    private void getAttachmentHistoryData (HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!authContext.checkUserAuthorisation(request, response)) {
            return;
        }

        String vkey = request.getParameter("vkey");
        String attachmentIdString = documentManager.readHash(vkey);
        String versionString = request.getParameter("version");

        if (attachmentIdString.isEmpty() || versionString == null || versionString.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Long attachmentId = Long.parseLong(attachmentIdString);
        int version = Integer.parseInt(versionString);

        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        if (!attachmentUtil.checkAccess(attachmentId, user, false)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        List<Attachment> attachments = attachmentUtil.getAllVersions(attachmentId);
        if (attachments != null) {
            Gson gson = new Gson();
            VersionData versionData = null;

            Attachment prevVersion = null;
            Collections.reverse(attachments);
            for (Attachment attachment : attachments) {
                if (attachment.getVersion() == version) {
                    versionData = new VersionData();
                    versionData.setVersion(attachment.getVersion());
                    versionData.setKey(documentManager.getKeyOfFile(attachment.getId()));
                    versionData.setUrl(urlManager.getFileUri(attachment.getId()));
                    versionData.setFileType(attachment.getFileExtension());

                    Attachment diff = attachmentUtil.getAttachmentDiff(attachment.getId());
                    if (prevVersion != null && diff != null) {
                        boolean adjacentVersions = (attachment.getVersion() - prevVersion.getVersion()) == 1;
                        if (adjacentVersions) {
                            versionData.setChangesUrl(urlManager.getAttachmentDiffUri(attachment.getId()));
                            versionData.setPrevious(documentManager.getKeyOfFile(prevVersion.getId()), urlManager.getFileUri(prevVersion.getId()), prevVersion.getFileExtension());
                        }
                    }
                    break;
                }
                prevVersion = attachment;
            }

            if (versionData != null) {
                if (jwtManager.jwtEnabled()) {
                    try {
                        JSONObject versionDataJSON = new JSONObject(gson.toJson(versionData));
                        versionData.setToken(jwtManager.createToken(versionDataJSON));
                    } catch (Exception e) {
                        throw new IOException(e.getMessage());
                    }
                }

                response.setContentType("application/json");
                PrintWriter writer = response.getWriter();
                writer.write(gson.toJson(versionData));
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }

    public class Version {
        public int version;
        public String key;
        public Object changes;
        public String created;
        public User user;
        public String serverVersion;

        public Version() { }

        public void setVersion(int version) {
            this.version = version;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void setChanges(Object changes) {
            this.changes = changes;
        }

        public void setCreated(String created) { this.created = created; }

        public void setUser(String id, String name) {
            this.user = new User(id, name);
        }

        public void setServerVersion(String serverVersion) {
            this.serverVersion = serverVersion;
        }

        public class User {
            public String id;
            public String name;

            public User(String id, String name) {
                this.id = id;
                this.name = name;
            }
        }
    }

    public class VersionData {
        public int version;
        public String key;
        public String url;
        public String fileType;
        public String changesUrl;
        public Previous previous;
        public String token;

        public VersionData() { }

        public void setVersion(int version) {
            this.version = version;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public void setChangesUrl(String changesUrl) {
            this.changesUrl = changesUrl;
        }

        public void setPrevious(String key, String url, String fileType) {
            this.previous = new Previous(key, url, fileType);
        }

        public void setToken(String token) { this.token = token; }

        public class Previous {
            public String key;
            public String url;
            public String fileType;

            public Previous(String key, String url, String fileType) {
                this.key = key;
                this.url = url;
                this.fileType = fileType;
            }
        }
    }
}
