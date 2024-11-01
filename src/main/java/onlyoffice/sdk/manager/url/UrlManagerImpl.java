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

package onlyoffice.sdk.manager.url;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.HtmlUtil;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.webresource.api.UrlMode;
import com.atlassian.webresource.api.WebResourceUrlProvider;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.url.DefaultUrlManager;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import com.onlyoffice.model.settings.SettingsConstants;
import onlyoffice.sdk.manager.security.JwtManager;
import onlyoffice.utils.attachment.AttachmentUtil;

import java.util.HashMap;
import java.util.Map;

public class UrlManagerImpl extends DefaultUrlManager implements UrlManager {

    public static final String API_SERVLET = "/plugins/servlet/onlyoffice/api";
    public static final String DOC_EDITOR_SERVLET = "/plugins/servlet/onlyoffice/doceditor";
    public static final String FILE_PROVIDER_SERVLET = "/plugins/servlet/onlyoffice/file-provider";
    public static final String CALLBACK_SERVLET = "/plugins/servlet/onlyoffice/save";
    public static final String HISTORY_SERVLET = "/plugins/servlet/onlyoffice/history";

    private final WebResourceUrlProvider webResourceUrlProvider;
    private final SettingsManager settingsManager;

    private final JwtManager jwtManager;
    private final AttachmentUtil attachmentUtil;
    private final DocumentManager documentManager;

    public UrlManagerImpl(final WebResourceUrlProvider webResourceUrlProvider, final SettingsManager settingsManager,
                          final JwtManager jwtManager, final AttachmentUtil attachmentUtil,
                          final DocumentManager documentManager,
                          final com.onlyoffice.manager.settings.SettingsManager settingsManagerSdk) {
        super(settingsManagerSdk);
        this.webResourceUrlProvider = webResourceUrlProvider;
        this.settingsManager = settingsManager;
        this.jwtManager = jwtManager;
        this.attachmentUtil = attachmentUtil;
        this.documentManager = documentManager;
    }

    @Override
    public String getFileUrl(final String fileId) {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        Map<String, String> params = new HashMap<>();

        if (user != null) {
            params.put("userKey", user.getKey().getStringValue());
        }
        params.put("attachmentId", fileId);
        params.put("action", "download");

        String fileUri =
                getConfluenceBaseUrl(true) + FILE_PROVIDER_SERVLET + "?token=" + jwtManager.createInternalToken(params);

        return fileUri;
    }

    @Override
    public String getCallbackUrl(final String fileId) {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        Map<String, String> params = new HashMap<>();
        params.put("userKey", user.getKey().getStringValue());
        params.put("attachmentId", fileId);
        params.put("action", "callback");

        String callbackUrl = getConfluenceBaseUrl(true)
                + CALLBACK_SERVLET
                + "?token="
                + jwtManager.createInternalToken(params);

        return callbackUrl;
    }

    @Override
    public String getGobackUrl(final String fileId) {
        String viewPageAttachments = "/pages/viewpageattachments.action?pageId=";
        AttachmentManager attachmentManager =
                (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(Long.parseLong(fileId));
        return settingsManager.getGlobalSettings().getBaseUrl()
                + viewPageAttachments
                + attachment.getContainer().getContentId().asLong();
    }

    @Override
    public String getCreateUrl(final String fileId) {
        Long pageId = attachmentUtil.getAttachmentPageId(Long.parseLong(fileId));
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        if (attachmentUtil.checkAccessCreate(user, pageId)) {
            String fileName = documentManager.getDocumentName(fileId);
            String extension = documentManager.getExtension(fileName);

            return getConfluenceBaseUrl(false) + DOC_EDITOR_SERVLET + "?pageId=" + pageId + "&fileExt=" + extension;
        } else {
            return null;
        }
    }

    @Override
    public String getTestConvertUrl(final String url) {
        return getConfluenceBaseUrl(true) + "/plugins/servlet/onlyoffice/test";
    }

    public String getHistoryInfoUri(final Long attachmentId) {
        String hash = jwtManager.createHash(Long.toString(attachmentId));
        String historyInfoUri =
                getConfluenceBaseUrl(false) + HISTORY_SERVLET + "?type=info&vkey=" + HtmlUtil.urlEncode(hash);

        return historyInfoUri;
    }

    public String getHistoryDataUri(final Long attachmentId) {
        String hash = jwtManager.createHash(Long.toString(attachmentId));
        String historyDataUri =
                getConfluenceBaseUrl(false) + HISTORY_SERVLET + "?type=data&vkey=" + HtmlUtil.urlEncode(hash);

        return historyDataUri;
    }
    public String getAttachmentDataUri() {
        String attachmentDataUri = getConfluenceBaseUrl(false) + API_SERVLET + "?type=attachment-data";

        return attachmentDataUri;
    }

    public String getSaveAsUri() {
        String saveAsUri = getConfluenceBaseUrl(false) + API_SERVLET + "?type=save-as";

        return saveAsUri;
    }

    public String getReferenceDataUri(final Long pageId) {
        String referenceDataUri = getConfluenceBaseUrl(false) + API_SERVLET + "?type=reference-data&pageId=" + pageId;

        return referenceDataUri;
    }

    public String getFaviconUrl(final DocumentType documentType) {
        String nameIcon = "word";

        if (documentType != null) {
            nameIcon = documentType.name().toLowerCase();
        }

        return webResourceUrlProvider.getStaticPluginResourceUrl(
                "onlyoffice.onlyoffice-confluence-plugin:onlyoffice-confluence-plugin-resources-editor",
                nameIcon + ".ico",
                UrlMode.ABSOLUTE
        );
    }

    public String getUsersInfoUrl() {
        String usersInfoUrl = getConfluenceBaseUrl(false) + API_SERVLET + "?type=users-info";

        return usersInfoUrl;
    }

    private String getConfluenceBaseUrl(final Boolean inner) {
        String productInnerUrl = getSettingsManager().getSetting(SettingsConstants.PRODUCT_INNER_URL);

        if (inner && productInnerUrl != null && !productInnerUrl.isEmpty()) {
            return sanitizeUrl(productInnerUrl);
        } else {
            return sanitizeUrl(settingsManager.getGlobalSettings().getBaseUrl());
        }
    }
}
