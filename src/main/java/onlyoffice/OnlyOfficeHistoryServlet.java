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
import com.atlassian.spring.container.ContainerManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.onlyoffice.model.common.User;
import com.onlyoffice.model.documenteditor.HistoryData;
import com.onlyoffice.model.documenteditor.callback.History;
import com.onlyoffice.model.documenteditor.history.Version;
import com.onlyoffice.model.documenteditor.historydata.Previous;
import onlyoffice.sdk.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import onlyoffice.sdk.manager.url.UrlManager;
import onlyoffice.managers.auth.AuthContext;
import onlyoffice.sdk.manager.document.DocumentManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import onlyoffice.utils.parsing.ParsingUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnlyOfficeHistoryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeHistoryServlet");
    private static final int BUFFER_SIZE = 10240;

    private final LocaleManager localeManager;
    private final FormatSettingsManager formatSettingsManager;
    private final AuthContext authContext;
    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;
    private final UrlManager urlManager;
    private final SettingsManager settingsManager;
    private final JwtManager jwtManager;
    private final ParsingUtil parsingUtil;

    public OnlyOfficeHistoryServlet(final LocaleManager localeManager,
                                    final FormatSettingsManager formatSettingsManager,
                                    final AuthContext authContext, final DocumentManager documentManager,
                                    final AttachmentUtil attachmentUtil, final UrlManager urlManager,
                                    final SettingsManager settingsManager, final JwtManager jwtManager,
                                    final ParsingUtil parsingUtil) {
        this.localeManager = localeManager;
        this.formatSettingsManager = formatSettingsManager;
        this.authContext = authContext;
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
        this.urlManager = urlManager;
        this.settingsManager = settingsManager;
        this.jwtManager = jwtManager;
        this.parsingUtil = parsingUtil;
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        String type = request.getParameter("type");
        if (type != null) {
            switch (type.toLowerCase()) {
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

    private void getAttachmentDiff(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        String vkey = request.getParameter("vkey");
        String attachmentIdString = jwtManager.readHash(vkey);

        if (attachmentIdString.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Long attachmentId = Long.parseLong(attachmentIdString);
        Attachment diff = attachmentUtil.getAttachmentDiff(attachmentId);

        if (diff != null) {
            InputStream inputStream = attachmentUtil.getAttachmentData(diff.getId());
            String publicDocEditorUrl = urlManager.getDocumentServerUrl();

            if (publicDocEditorUrl.endsWith("/")) {
                publicDocEditorUrl = publicDocEditorUrl.substring(0, publicDocEditorUrl.length() - 1);
            }

            response.setContentType(diff.getMediaType());
            response.setContentLength(inputStream.available());
            response.addHeader("Access-Control-Allow-Origin", publicDocEditorUrl);

            byte[] buffer = new byte[BUFFER_SIZE];

            OutputStream output = response.getOutputStream();
            for (int length = 0; (length = inputStream.read(buffer)) > 0;) {
                output.write(buffer, 0, length);
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }

    private void getAttachmentHistoryInfo(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        if (!authContext.checkUserAuthorization(request, response)) {
            return;
        }

        String vkey = request.getParameter("vkey");
        String attachmentIdString = jwtManager.readHash(vkey);

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
                Version version = Version.builder()
                        .version(String.valueOf(attachment.getVersion()))
                        .key(documentManager.getDocumentKey(String.valueOf(attachment.getId()), false))
                        .created(dateFormatter.formatDateTime(attachment.getCreationDate()))
                        .user(User.builder()
                                .id(attachment.getCreator().getName())
                                .name(attachment.getCreator().getFullName())
                                .build()
                        )
                        .build();

                Attachment changesAttachment = attachmentUtil.getAttachmentChanges(attachment.getId());
                if (changesAttachment != null) {
                    if (prevVersion != null && (attachment.getVersion() - prevVersion.getVersion()) == 1) {
                        InputStream changesSteam = attachmentUtil.getAttachmentData(changesAttachment.getId());
                        ObjectMapper mapper = new ObjectMapper();
                        History changes = mapper.readValue(changesSteam, History.class);
                        try {
                            version.setServerVersion(changes.getServerVersion());
                            version.setChanges(changes.getChanges());
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

    private void getAttachmentHistoryData(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        if (!authContext.checkUserAuthorization(request, response)) {
            return;
        }

        String vkey = request.getParameter("vkey");
        String attachmentIdString = jwtManager.readHash(vkey);
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
            HistoryData historyData = null;

            Attachment prevVersion = null;
            Collections.reverse(attachments);
            for (Attachment attachment : attachments) {
                if (attachment.getVersion() == version) {
                    historyData = HistoryData.builder()
                            .version(String.valueOf(attachment.getVersion()))
                            .key(documentManager.getDocumentKey(String.valueOf(attachment.getId()), false))
                            .url(urlManager.getFileUrl(String.valueOf(attachment.getId())))
                            .fileType(attachment.getFileExtension())
                            .build();

                    Attachment diff = attachmentUtil.getAttachmentDiff(attachment.getId());
                    if (prevVersion != null && diff != null) {
                        boolean adjacentVersions = (attachment.getVersion() - prevVersion.getVersion()) == 1;
                        if (adjacentVersions) {
                            historyData.setChangesUrl(urlManager.getAttachmentDiffUri(attachment.getId()));
                            historyData.setPrevious(Previous.builder()
                                            .key(
                                                    documentManager.getDocumentKey(
                                                            String.valueOf(prevVersion.getId()),
                                                            false
                                                    )
                                            )
                                            .url(urlManager.getFileUrl(String.valueOf(prevVersion.getId())))
                                            .fileType(prevVersion.getFileExtension())
                                            .build()
                            );
                        }
                    }
                    break;
                }
                prevVersion = attachment;
            }

            if (historyData != null) {
                if (settingsManager.isSecurityEnabled()) {
                    try {
                        historyData.setToken(jwtManager.createToken(historyData));
                    } catch (Exception e) {
                        throw new IOException(e.getMessage());
                    }
                }

                response.setContentType("application/json");
                PrintWriter writer = response.getWriter();
                writer.write(gson.toJson(historyData));
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }
}
