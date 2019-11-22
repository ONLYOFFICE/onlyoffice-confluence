package onlyoffice;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.setup.settings.SettingsManager;

public class UrlManager {
    private static final Logger log = LogManager.getLogger("onlyoffice.UrlManager");

    private static final String callbackServler = "plugins/servlet/onlyoffice/save";

    private final SettingsManager glSettings;

    public UrlManager(SettingsManager settings) {
        glSettings = settings;
    }

    private String getBaseUrl() {
        return glSettings.getGlobalSettings().getBaseUrl() + "/";
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