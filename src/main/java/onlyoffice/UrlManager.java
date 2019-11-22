package onlyoffice;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class UrlManager {
    private static final Logger log = LogManager.getLogger("onlyoffice.UrlManager");

    private static final String callbackServler = "plugins/servlet/onlyoffice/save";

    private final PluginSettings plSettings;
    private final SettingsManager glSettings;
    
    public UrlManager(PluginSettingsFactory pluginSettingsFactory, SettingsManager settings) {
        plSettings = pluginSettingsFactory.createGlobalSettings();
        glSettings = settings;
    }

    private String getBaseUrl()
	{
		String baseUrl = (String) plSettings.get("onlyoffice.confUrl");

		if (baseUrl == null || baseUrl.isEmpty()) {
			baseUrl = glSettings.getGlobalSettings().getBaseUrl() + "/";
		}

		return baseUrl;
    }
    
    public String GetUri(Long attachmentId) throws Exception
	{
		String hash = DocumentManager.CreateHash(Long.toString(attachmentId));

		String callbackUrl = getBaseUrl() + callbackServler + "?vkey=" + GeneralUtil.urlEncode(hash);
		log.info("callbackUrl " + callbackUrl);

		return callbackUrl;
	}

	public String getCallbackUrl(Long attachmentId)
	{
		String hash = DocumentManager.CreateHash(Long.toString(attachmentId));

		String callbackUrl = getBaseUrl() + callbackServler + "?vkey=" + GeneralUtil.urlEncode(hash);
		log.info("callbackUrl " + callbackUrl);

		return callbackUrl;
	}
}