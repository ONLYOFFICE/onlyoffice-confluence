package onlyoffice;

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

    private String getBaseUrl() {
        return settingsManager.getGlobalSettings().getBaseUrl() + "/";
    }

    public String getDocEditorUrl() {
        String url = (String) pluginSettings.get("onlyoffice.apiUrl");
        return url == null ? "" : url;
    }

    public String GetUri(Long attachmentId) throws Exception {
        String hash = DocumentManager.CreateHash(Long.toString(attachmentId));

        String callbackUrl = getBaseUrl() + callbackServler + "?vkey=" + GeneralUtil.urlEncode(hash);
        log.info("callbackUrl " + callbackUrl);

        return callbackUrl;
    }

    public String getCallbackUrl(Long attachmentId) {
        String hash = DocumentManager.CreateHash(Long.toString(attachmentId));

        String callbackUrl = getBaseUrl() + callbackServler + "?vkey=" + GeneralUtil.urlEncode(hash);
        log.info("callbackUrl " + callbackUrl);

        return callbackUrl;
    }
}