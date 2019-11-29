package onlyoffice;

import org.json.JSONObject;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.util.Base64;
import java.util.Base64.Encoder;

import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Mac;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.inject.Inject;
import javax.inject.Named;

/*
    Copyright (c) Ascensio System SIA 2019. All rights reserved.
    http://www.onlyoffice.com
*/

@Named
public class JwtManager {

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private final PluginSettings settings;

    @Inject
    public JwtManager(PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        settings = pluginSettingsFactory.createGlobalSettings();
    }

    public Boolean jwtEnabled() {
        return settings.get("onlyoffice.jwtSecret") != null
                && !((String) settings.get("onlyoffice.jwtSecret")).isEmpty();
    }

    public String createToken(JSONObject payload) throws Exception {
        JSONObject header = new JSONObject();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Encoder enc = Base64.getUrlEncoder();

        String encHeader = enc.encodeToString(header.toString().getBytes("UTF-8")).replace("=", "");
        String encPayload = enc.encodeToString(payload.toString().getBytes("UTF-8")).replace("=", "");
        String hash = calculateHash(encHeader, encPayload);

        return encHeader + "." + encPayload + "." + hash;
    }

    public Boolean verify(String token) {
        if (!jwtEnabled())
            return false;

        String[] jwt = token.split("\\.");
        if (jwt.length != 3) {
            return false;
        }

        try {
            String hash = calculateHash(jwt[0], jwt[1]);
            if (!hash.equals(jwt[2]))
                return false;
        } catch (Exception ex) {
            return false;
        }

        return true;
    }

    private String calculateHash(String header, String payload) throws Exception {
        Mac hasher;
        hasher = getHasher();
        return Base64.getUrlEncoder().encodeToString(hasher.doFinal((header + "." + payload).getBytes("UTF-8")))
                .replace("=", "");
    }

    private Mac getHasher() throws Exception {
        String jwts = (String) settings.get("onlyoffice.jwtSecret");

        Mac sha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(jwts.getBytes("UTF-8"), "HmacSHA256");
        sha256.init(secret_key);

        return sha256;
    }
}
