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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;

public class JwtManagerImpl implements JwtManager {

    private static final long ACCEPT_LEEWAY = 3;

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

    public Boolean jwtEnabled() {
        return configurationManager.demoActive() || settings.get("onlyoffice.jwtSecret") != null
                && !((String) settings.get("onlyoffice.jwtSecret")).isEmpty();
    }

    public String createToken(final JSONObject payload) throws Exception {
        Algorithm algorithm = Algorithm.HMAC256(getJwtSecret());

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, ?> payloadMap = objectMapper.readValue(payload.toString(), Map.class);

        String token = JWT.create()
                .withPayload(payloadMap)
                .sign(algorithm);

        return token;
    }

    public String verify(final String token) {
        Algorithm algorithm = Algorithm.HMAC256(getJwtSecret());
        Base64.Decoder decoder = Base64.getUrlDecoder();

        DecodedJWT jwt = JWT.require(algorithm)
                .acceptLeeway(ACCEPT_LEEWAY)
                .build()
                .verify(token);

        return new String(decoder.decode(jwt.getPayload()));
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
        SecretKeySpec secretKey = new SecretKeySpec(jwts.getBytes("UTF-8"), "HmacSHA256");
        sha256.init(secretKey);

        return sha256;
    }

    private String getJwtSecret() {
        return configurationManager.demoActive()
                ? configurationManager.getDemo("secret") : (String) settings.get("onlyoffice.jwtSecret");
    }
}
