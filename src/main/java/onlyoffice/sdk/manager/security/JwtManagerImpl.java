/**
 *
 * (c) Copyright Ascensio System SIA 2026
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

package onlyoffice.sdk.manager.security;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.onlyoffice.manager.security.DefaultJwtManager;
import org.apache.hc.client5.http.utils.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

public class JwtManagerImpl extends DefaultJwtManager implements JwtManager {
    private final Logger log = LogManager.getLogger("onlyoffice.sdk.manager.security.JwtManagerImpl");
    private static final int PLUGIN_SECRET_LENGTH = 32;
    private static final String FILES_CONFLUENCE_SECRET = "Vskoproizvolny Salt par Chivreski";
    private final PluginSettings pluginSettings;

    public JwtManagerImpl(final PluginSettingsFactory pluginSettingsFactory,
                          final com.onlyoffice.manager.settings.SettingsManager settingsManagerSdk) {
        super(settingsManagerSdk);
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
    }


    public String createInternalToken(final Map<String, ?> payloadMap) {
        Algorithm algorithm = Algorithm.HMAC256(getPluginSecret());

        return JWT.create()
                .withPayload(payloadMap)
                .sign(algorithm);
    }

    public String verifyInternalToken(final String token) {
        return verifyToken(token, getPluginSecret());
    }

    public String createHash(final String str) {
        try {
            String payload = getHashHex(str + FILES_CONFLUENCE_SECRET) + "?" + str;

            String base64 = Base64.getEncoder().encodeToString(payload.getBytes("UTF-8"));
            return base64;
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

    public String readHash(final String base64) {
        try {
            String str = new String(Base64.getDecoder().decode(base64), "UTF-8");

            String[] payloadParts = str.split("\\?");

            String payload = getHashHex(payloadParts[1] + FILES_CONFLUENCE_SECRET);
            if (payload.equals(payloadParts[0])) {
                return payloadParts[1];
            }
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

    private String getHashHex(final String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(str.getBytes());
            String hex = Hex.encodeHexString(digest);

            return hex;
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

    private String getPluginSecret() {
        if (pluginSettings.get("onlyoffice.plugin-secret") == null
                || pluginSettings.get("onlyoffice.plugin-secret").equals("")) {
            Random random = new Random();
            char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();

            char[] randBuffer = new char[PLUGIN_SECRET_LENGTH];
            for (int i = 0; i < randBuffer.length; i++) {
                randBuffer[i] = numbersAndLetters[random.nextInt(numbersAndLetters.length)];
            }

            String secret = new String(randBuffer);

            pluginSettings.put("onlyoffice.plugin-secret", secret);

            return secret;
        } else {
            return (String) pluginSettings.get("onlyoffice.plugin-secret");
        }
    }
}
