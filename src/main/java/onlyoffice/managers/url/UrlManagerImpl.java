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

package onlyoffice.managers.url;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.spring.container.ContainerManager;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.model.config.DocumentType;
import onlyoffice.managers.jwt.JwtManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class UrlManagerImpl implements UrlManager {
    private final Logger log = LogManager.getLogger("onlyoffice.managers.url.UrlManager");
    private final String docEditorServlet = "plugins/servlet/onlyoffice/doceditor";
    private final String callbackServlet = "plugins/servlet/onlyoffice/save";
    private final String historyServlet = "plugins/servlet/onlyoffice/history";
    private final String fileProviderServlet = "plugins/servlet/onlyoffice/file-provider";
    private final String apiServlet = "plugins/servlet/onlyoffice/api";

    private final WebResourceUrlProvider webResourceUrlProvider;    private final SettingsManager settingsManager;
    private final PluginSettings pluginSettings;
    private final ConfigurationManager configurationManager;
    private final DocumentManager documentManager;
    private final JwtManager jwtManager;

    public UrlManagerImpl(final WebResourceUrlProvider webResourceUrlProvider, final PluginSettingsFactory pluginSettingsFactory, final SettingsManager settingsManager,
                          final ConfigurationManager configurationManager, final DocumentManager documentManager,
                          final JwtManager jwtManager) {
        this.webResourceUrlProvider = webResourceUrlProvider;        this.settingsManager = settingsManager;
        this.configurationManager = configurationManager;
        this.documentManager = documentManager;
        this.jwtManager = jwtManager;
        pluginSettings = pluginSettingsFactory.createGlobalSettings();
    }

    public String getPublicDocEditorUrl() {
        String url = "";
        if (configurationManager.demoActive()) {
            url = configurationManager.getDemo("url");
        } else {
            url = (String) pluginSettings.get("onlyoffice.apiUrl");
        }
        return (url == null || url.isEmpty()) ? "" : url;
    }


    public String getInnerDocEditorUrl() {
        String url = (String) pluginSettings.get("onlyoffice.docInnerUrl");
        if (url == null || url.isEmpty() || configurationManager.demoActive()) {
            return getPublicDocEditorUrl();
        } else {
            return url;
        }
    }

    public String getFileUri(final Long attachmentId) {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        Map<String, String> params = new HashMap<>();
        params.put("userKey", user.getKey().getStringValue());
        params.put("attachmentId", attachmentId.toString());
        params.put("action", "download");

        String fileUri =
                getConfluenceBaseUrl() + fileProviderServlet + "?token=" + jwtManager.createInternalToken(params);

        return fileUri;
    }

    public String getAttachmentDiffUri(final Long attachmentId) {
        String hash = documentManager.createHash(Long.toString(attachmentId));
        String diffAttachmentUrl =
                getConfluenceBaseUrl() + historyServlet + "?type=diff&vkey=" + GeneralUtil.urlEncode(hash);

        return diffAttachmentUrl;
    }

    public String getHistoryInfoUri(final Long attachmentId) {
        String hash = documentManager.createHash(Long.toString(attachmentId));
        String historyInfoUri =
                getConfluenceBaseUrl() + historyServlet + "?type=info&vkey=" + GeneralUtil.urlEncode(hash);

        return historyInfoUri;
    }

    public String getHistoryDataUri(final Long attachmentId) {
        String hash = documentManager.createHash(Long.toString(attachmentId));
        String historyDataUri =
                getConfluenceBaseUrl() + historyServlet + "?type=data&vkey=" + GeneralUtil.urlEncode(hash);

        return historyDataUri;
    }

    public String getAttachmentDataUri() {
        String attachmentDataUri = getConfluenceBaseUrl() + apiServlet + "?type=attachment-data";

        return attachmentDataUri;
    }

    public String getSaveAsUri() {
        String saveAsUri = getConfluenceBaseUrl() + apiServlet + "?type=save-as";

        return saveAsUri;
    }

    public String getReferenceDataUri(final Long pageId) {
        String referenceDataUri = getConfluenceBaseUrl() + apiServlet + "?type=reference-data&pageId=" + pageId;

        return referenceDataUri;
    }

    public String getCallbackUrl(final Long attachmentId) {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        Map<String, String> params = new HashMap<>();
        params.put("userKey", user.getKey().getStringValue());
        params.put("attachmentId", attachmentId.toString());
        params.put("action", "callback");

        String callbackUrl =
                getConfluenceBaseUrl() + callbackServlet + "?token=" + jwtManager.createInternalToken(params);
        log.info("callbackUrl " + callbackUrl);

        return callbackUrl;
    }

    public String getGobackUrl(final Long attachmentId, final String referer) {
        String gobackUrl = "";

        if (referer != null && referer.contains("/display/")) {
            gobackUrl = referer;
        } else {
            String viewPageAttachments = "/pages/viewpageattachments.action?pageId=";
            AttachmentManager attachmentManager =
                    (AttachmentManager) ContainerManager.getComponent("attachmentManager");
            Attachment attachment = attachmentManager.getAttachment(attachmentId);
            gobackUrl = settingsManager.getGlobalSettings().getBaseUrl() + viewPageAttachments
                    + attachment.getContainer().getContentId().asLong();
        }

        log.info("gobackUrl = " + gobackUrl);

        return gobackUrl;
    }

    public String getCreateUri(final Long pageId, final String ext) {

        String targetExt = "docx";

        switch (documentManager.getDocType(ext)) {
            case WORD:
                targetExt = ext.equals("docxf") ? "docxf" : "docx";
                break;
            case CELL:
                targetExt = "xlsx";
                break;
            case SLIDE:
                targetExt = "pptx";
                break;
            default:
        }

        return getConfluenceBaseUrl() + docEditorServlet + "?pageId=" + pageId + "&fileExt=" + targetExt;
    }

    private String getConfluenceBaseUrl() {
        String url = (String) pluginSettings.get("onlyoffice.confUrl");
        if (url == null || url.isEmpty()) {
            return settingsManager.getGlobalSettings().getBaseUrl() + "/";
        } else {
            return url;
        }
    }

    public String replaceDocEditorURLToInternal(final String url) {
        String innerDocEditorUrl = getInnerDocEditorUrl();
        String publicDocEditorUrl = getPublicDocEditorUrl();
        String result = url;

        if (!publicDocEditorUrl.equals(innerDocEditorUrl) && !configurationManager.demoActive()) {
            result = result.replace(publicDocEditorUrl, innerDocEditorUrl);
        }
        return result;
    }

    public String getDocServiceApiUrl() {
        return getPublicDocEditorUrl() + configurationManager.getProperty("files.docservice.url.api");
    }

    public String getFaviconUrl(Long attachmentId) {
        String nameIcon = "word";

        DocumentType documentType = documentManager.getDocType(attachmentId);
        if (documentType != null) {
            nameIcon = documentType.name().toLowerCase();
        }

        return webResourceUrlProvider.getStaticPluginResourceUrl(
                "onlyoffice.onlyoffice-confluence-plugin:onlyoffice-confluence-plugin-resources-editor",
                nameIcon +".ico",
                UrlMode.ABSOLUTE
        );
    }
}