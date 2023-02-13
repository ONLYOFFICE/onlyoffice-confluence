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

package onlyoffice.managers.jwt;

import com.atlassian.config.ApplicationConfiguration;
import onlyoffice.managers.configuration.ConfigurationManager;
import org.json.JSONObject;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.util.Base64;
import java.util.Base64.Encoder;

import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Mac;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@Default
public class JwtManagerImpl implements JwtManager {

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;
    @ComponentImport
    private final ApplicationConfiguration applicationConfiguration;

    private final ConfigurationManager configurationManager;
    private final PluginSettings settings;

    @Inject
    public JwtManagerImpl(final PluginSettingsFactory pluginSettingsFactory, final ApplicationConfiguration applicationConfiguration,
                          final ConfigurationManager configurationManager) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        settings = pluginSettingsFactory.createGlobalSettings();
        this.applicationConfiguration = applicationConfiguration;
        this.configurationManager = configurationManager;
    }

    public Boolean jwtEnabled() {
        return configurationManager.demoActive() || settings.get("onlyoffice.jwtSecret") != null
                && !((String) settings.get("onlyoffice.jwtSecret")).isEmpty();
    }

    public String createToken(final JSONObject payload) throws Exception {
        JSONObject header = new JSONObject();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Encoder enc = Base64.getUrlEncoder();

        String encHeader = enc.encodeToString(header.toString().getBytes("UTF-8"))
                .replace("=", "");
        String encPayload = enc.encodeToString(payload.toString().getBytes("UTF-8"))
                .replace("=", "");

        String hash = calculateHash(encHeader, encPayload);

        return encHeader + "." + encPayload + "." + hash;
    }

    public Boolean verify(final String token) {
        if (!jwtEnabled()) {
            return false;
        }

        String[] jwt = token.split("\\.");
        if (jwt.length != 3) {
            return false;
        }

        try {
            String hash = calculateHash(jwt[0], jwt[1]);
            if (!hash.equals(jwt[2])) {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }

        return true;
    }

    public String getJwtHeader() {
        String header = configurationManager.demoActive()
                ? configurationManager
                        .getDemo("header") : (String) applicationConfiguration.getProperty("onlyoffice.jwt.header");
        return header == null || header.isEmpty() ? "Authorization" : header;
    }

    private String calculateHash(final String header, final String payload) throws Exception {
        Mac hasher = getHasher();
        return Base64.getUrlEncoder().encodeToString(hasher.doFinal((header + "." + payload).getBytes("UTF-8")))
                .replace("=", "");
    }

    private Mac getHasher() throws Exception {
        String jwts = configurationManager.demoActive()
                ? configurationManager.getDemo("secret") : (String) settings.get("onlyoffice.jwtSecret");

        Mac sha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(jwts.getBytes("UTF-8"), "HmacSHA256");
        sha256.init(secret_key);

        return sha256;
    }
}
