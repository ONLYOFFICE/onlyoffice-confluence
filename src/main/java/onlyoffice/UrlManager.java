/**
 *
 * (c) Copyright Ascensio System SIA 2020
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
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.spring.container.ContainerManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.setup.settings.SettingsManager;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class UrlManager {
    private static final Logger log = LogManager.getLogger("onlyoffice.UrlManager");

    private static final String callbackServler = "plugins/servlet/onlyoffice/save";

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;
    @ComponentImport
    private final SettingsManager settingsManager;

    private final PluginSettings pluginSettings;

    @Inject
    public UrlManager(PluginSettingsFactory pluginSettingsFactory, SettingsManager settingsManager) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.settingsManager = settingsManager;
        pluginSettings = pluginSettingsFactory.createGlobalSettings();
    }

    public String getPublicDocEditorUrl() {
        String url = (String) pluginSettings.get("onlyoffice.apiUrl");
        return (url == null || url.isEmpty()) ? "" : url;
    }


    public String getInnerDocEditorUrl() {
        String url = (String) pluginSettings.get("onlyoffice.docInnerUrl");
        if (url == null || url.isEmpty()) {
            return getPublicDocEditorUrl();
        } else {
            return url;
        }
    }

    public String GetFileUri(Long attachmentId) throws Exception {
        String hash = DocumentManager.CreateHash(Long.toString(attachmentId));

        String callbackUrl = getConfluenceBaseUrl() + callbackServler + "?vkey=" + GeneralUtil.urlEncode(hash);
        log.info("fileUrl " + callbackUrl);

        return callbackUrl;
    }

    public String getCallbackUrl(Long attachmentId) {
        String hash = DocumentManager.CreateHash(Long.toString(attachmentId));

        String callbackUrl = getConfluenceBaseUrl() + callbackServler + "?vkey=" + GeneralUtil.urlEncode(hash);
        log.info("callbackUrl " + callbackUrl);

        return callbackUrl;
    }

    public String getGobackUrl(Long attachmentId) {
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        String gobackUrl = getConfluenceBaseUrl().substring(0, getConfluenceBaseUrl().length() - 1) + attachment.getUrlPath().split("\\?")[0];

        log.info("gobackUrl = " + gobackUrl);

        return gobackUrl;
    }

    private String getConfluenceBaseUrl() {
        String url = (String) pluginSettings.get("onlyoffice.confUrl");
        if (url == null || url.isEmpty()) {
            return settingsManager.getGlobalSettings().getBaseUrl() + "/";
        } else {
            return url;
        }
    }
}