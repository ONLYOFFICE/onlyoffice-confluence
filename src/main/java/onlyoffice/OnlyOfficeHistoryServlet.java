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
import com.atlassian.confluence.core.DateFormatter;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.ConfluenceUserPreferences;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.sal.api.message.I18nResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.model.common.User;
import com.onlyoffice.model.documenteditor.HistoryData;
import com.onlyoffice.model.documenteditor.history.Version;
import onlyoffice.sdk.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import onlyoffice.sdk.manager.url.UrlManager;
import onlyoffice.sdk.manager.document.DocumentManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AnonymousSiteAccess
public class OnlyOfficeHistoryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeHistoryServlet");

    private final I18nResolver i18n;
    private final LocaleManager localeManager;
    private final FormatSettingsManager formatSettingsManager;
    private final UserAccessor userAccessor;
    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;
    private final UrlManager urlManager;
    private final SettingsManager settingsManager;
    private final JwtManager jwtManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OnlyOfficeHistoryServlet(final I18nResolver i18n, final LocaleManager localeManager,
                                    final FormatSettingsManager formatSettingsManager, final UserAccessor userAccessor,
                                    final DocumentManager documentManager, final AttachmentUtil attachmentUtil,
                                    final UrlManager urlManager, final SettingsManager settingsManager,
                                    final JwtManager jwtManager) {
        this.i18n = i18n;
        this.localeManager = localeManager;
        this.formatSettingsManager = formatSettingsManager;
        this.userAccessor = userAccessor;
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
        this.urlManager = urlManager;
        this.settingsManager = settingsManager;
        this.jwtManager = jwtManager;
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        String type = request.getParameter("type");
        if (type != null) {
            switch (type.toLowerCase()) {
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

    private void getAttachmentHistoryInfo(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
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
            ConfluenceUserPreferences preferences = userAccessor.getConfluenceUserPreferences(user);
            DateFormatter dateFormatter = preferences.getDateFormatter(formatSettingsManager, localeManager);

            Collections.reverse(attachments);
            List<Version> history = new ArrayList<>();
            for (Attachment attachment : attachments) {
                ConfluenceUser creator = attachment.getCreator();
                String creatorId = null;
                String creatorName = i18n.getText("anonymous.name");

                if (creator != null) {
                    creatorId = creator.getKey().getStringValue();
                    creatorName = creator.getFullName();
                }

                Version version = Version.builder()
                        .version(String.valueOf(attachment.getVersion()))
                        .key(documentManager.getDocumentKey(String.valueOf(attachment.getId()), false))
                        .created(dateFormatter.formatDateTime(attachment.getCreationDate()))
                        .user(User.builder()
                                .id(creatorId)
                                .name(creatorName)
                                .build()
                        )
                        .build();

                history.add(version);
            }

            Map<String, Object> historyInfo = new HashMap<>();

            historyInfo.put("currentVersion", attachmentUtil.getVersion(attachmentId));
            historyInfo.put("history", history);

            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write(objectMapper.writeValueAsString(historyInfo));
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }

    private void getAttachmentHistoryData(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
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
            HistoryData historyData = null;

            Collections.reverse(attachments);
            for (Attachment attachment : attachments) {
                if (attachment.getVersion() == version) {
                    historyData = HistoryData.builder()
                            .version(String.valueOf(attachment.getVersion()))
                            .key(documentManager.getDocumentKey(String.valueOf(attachment.getId()), false))
                            .url(urlManager.getFileUrl(String.valueOf(attachment.getId())))
                            .fileType(attachment.getFileExtension())
                            .build();
                    break;
                }
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
                writer.write(objectMapper.writeValueAsString(historyData));
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
