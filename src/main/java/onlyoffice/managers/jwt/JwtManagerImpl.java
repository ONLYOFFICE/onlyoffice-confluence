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
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import onlyoffice.managers.configuration.ConfigurationManager;
import org.json.JSONObject;

import java.util.Base64;
import java.util.Map;
import java.util.Random;

public class JwtManagerImpl implements JwtManager {

    private static final long ACCEPT_LEEWAY = 3;
    private static final int PLUGIN_SECRET_LENGTH = 32;

    private final ApplicationConfiguration applicationConfiguration;
    private final ConfigurationManager configurationManager;
    private final PluginSettings settings;

    public JwtManagerImpl(final PluginSettingsFactory pluginSettingsFactory,
                          final ApplicationConfiguration applicationConfiguration,
                          final ConfigurationManager configurationManager) {
        settings = pluginSettingsFactory.createGlobalSettings();
        this.applicationConfiguration = applicationConfiguration;
        this.configurationManager = configurationManager;
    }

    public String createToken(final Object payload) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, ?> payloadMap = objectMapper.convertValue(payload, Map.class);

        return createToken(payloadMap, getJwtSecret());
    }

    public String createToken(final JSONObject payload) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, ?> payloadMap = objectMapper.readValue(payload.toString(), Map.class);

        return createToken(payloadMap, getJwtSecret());
    }

    public String verify(final String token) {
        return verifyToken(token, getJwtSecret());
    }

    public String createInternalToken(final Map<String, ?> payloadMap) {
        return createToken(payloadMap, getPluginSecret());
    }

    public String verifyInternalToken(final String token) {
        return verifyToken(token, getPluginSecret());
    }

    public Boolean jwtEnabled() {
        return configurationManager.demoActive() || settings.get("onlyoffice.jwtSecret") != null
                && !((String) settings.get("onlyoffice.jwtSecret")).isEmpty();
    }

    public String getJwtHeader() {
        String header = configurationManager.demoActive()
                ? configurationManager
                .getDemo("header") : (String) applicationConfiguration.getProperty("onlyoffice.jwt.header");
        return header == null || header.isEmpty() ? "Authorization" : header;
    }

    private String getJwtSecret() {
        return configurationManager.demoActive()
                ? configurationManager.getDemo("secret") : (String) settings.get("onlyoffice.jwtSecret");
    }

    private String createToken(final Map<String, ?> payloadMap, final String key) {
        Algorithm algorithm = Algorithm.HMAC256(key);

        String token = JWT.create()
                .withPayload(payloadMap)
                .sign(algorithm);

        return token;
    }

    private String verifyToken(final String token, final String key) {
        Algorithm algorithm = Algorithm.HMAC256(key);
        Base64.Decoder decoder = Base64.getUrlDecoder();

        DecodedJWT jwt = JWT.require(algorithm)
                .acceptLeeway(ACCEPT_LEEWAY)
                .build()
                .verify(token);

        return new String(decoder.decode(jwt.getPayload()));
    }

    private String getPluginSecret() {
        if (settings.get("onlyoffice.plugin-secret") == null || settings.get("onlyoffice.plugin-secret").equals("")) {
            Random random = new Random();
            char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();

            char[] randBuffer = new char[PLUGIN_SECRET_LENGTH];
            for (int i = 0; i < randBuffer.length; i++) {
                randBuffer[i] = numbersAndLetters[random.nextInt(numbersAndLetters.length)];
            }

            String secret = new String(randBuffer);

            settings.put("onlyoffice.plugin-secret", secret);

            return secret;
        } else {
            return (String) settings.get("onlyoffice.plugin-secret");
        }
    }
}
