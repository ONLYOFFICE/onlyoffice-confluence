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

package onlyoffice.managers.configuration;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import onlyoffice.model.Format;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class ConfigurationManagerImpl implements ConfigurationManager {
    private final Logger log = LogManager.getLogger("onlyoffice.managers.configuration.ConfigurationManager");
    private final PluginSettings pluginSettings;

    private final String configurationPath = "onlyoffice-config.properties";
    private final String formatsPath = "app_data/document-formats/onlyoffice-docs-formats.json";
    private final String pluginDemoName = "onlyoffice.demo";
    private final String pluginDemoNameStart = "onlyoffice.demoStart";

    private Map<String, String> demoData;
    private List<Format> supportedFormats;

    public ConfigurationManagerImpl(final PluginSettingsFactory pluginSettingsFactory) {
        pluginSettings = pluginSettingsFactory.createGlobalSettings();

        demoData = new HashMap<String, String>();
        demoData.put("url", "https://onlinedocs.onlyoffice.com/");
        demoData.put("header", "AuthorizationJWT");
        demoData.put("secret", "sn2puSUF7muF5Jas");
        demoData.put("trial", "30");

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(formatsPath);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            supportedFormats = objectMapper.readValue(inputStream, new TypeReference<List<Format>>() { });
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public Properties getProperties() throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configurationPath);
        if (inputStream != null) {
            properties.load(inputStream);
        }
        return properties;
    }

    public String getProperty(final String propertyName) {
        try {
            Properties properties = getProperties();
            String property = properties.getProperty(propertyName);
            return property;
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.error(e.toString() + "\n" + sw.toString());
            return null;
        }
    }

    public boolean forceSaveEnabled() {
        String forceSave = (String) pluginSettings.get("onlyoffice.forceSave");
        if (forceSave == null || forceSave.isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(forceSave);
    }

    public boolean selectDemo(final Boolean demo) {
        pluginSettings.put(pluginDemoName, demo.toString());
        if (demo) {
            String demoStart = (String) pluginSettings.get(pluginDemoNameStart);
            if (demoStart == null || demoStart.isEmpty()) {
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                pluginSettings.put(pluginDemoNameStart, dateFormat.format(date));
            }
            return true;
        }
        return false;
    }

    public Boolean demoEnabled() {
        String demo = (String) pluginSettings.get(pluginDemoName);
        if (demo == null || demo.isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(demo);
    }

    public Boolean demoAvailable(final Boolean forActivate) {
        String demoStart = (String) pluginSettings.get(pluginDemoNameStart);
        if (demoStart != null && !demoStart.isEmpty()) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            try {
                Calendar date = Calendar.getInstance();
                date.setTime(dateFormat.parse(demoStart));
                date.add(Calendar.DATE, Integer.parseInt(demoData.get("trial")));

                return date.after(Calendar.getInstance());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return forActivate;
    }

    public Boolean demoActive() {
        return demoEnabled() && demoAvailable(false);
    }

    public String getDemo(final String key) {
        return demoData.get(key);
    }

    public Boolean getBooleanPluginSetting(final String key, final Boolean defaultValue) {
        String setting = (String) pluginSettings.get("onlyoffice." + key);
        if (setting == null || setting.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(setting);
    }

    public String getStringPluginSetting(final String key, final String defaultValue) {
        String setting = (String) pluginSettings.get("onlyoffice." + key);
        if (setting == null || setting.isEmpty()) {
            return defaultValue;
        }
        return setting;
    }

    public Map<String, Boolean> getCustomizableEditingTypes() {
        Map<String, Boolean> customizableEditingTypes = new HashMap<>();
        List<String> editingTypes;

        String editingTypesString = (String) pluginSettings.get("onlyoffice.editingTypes");

        if (editingTypesString != null && !editingTypesString.isEmpty()) {
            editingTypes = Arrays.asList(
                    editingTypesString.substring(1, editingTypesString.length() - 1).replace("\"", "").split(","));
        } else {
            editingTypes = Arrays.asList("csv", "txt");
        }

        List<Format> formats = this.getSupportedFormats();

        for (Format format : formats) {
            if (format.getActions().contains("lossy-edit")) {
                customizableEditingTypes.put(format.getName(), editingTypes.contains(format.getName()));
            }
        }

        return customizableEditingTypes;
    }

    public CloseableHttpClient getHttpClient() throws Exception {
        Integer timeout = (int) TimeUnit.SECONDS.toMillis(Long.parseLong(getProperty("timeout")));
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout).build();

        CloseableHttpClient httpClient;

        if (getBooleanPluginSetting("verifyCertificate", false) && !demoActive()) {
            SSLContextBuilder builder = new SSLContextBuilder();

            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(final X509Certificate[] chain, final String authType)
                        throws CertificateException {
                    return true;
                }
            });

            SSLConnectionSocketFactory sslConnectionSocketFactory =
                    new SSLConnectionSocketFactory(builder.build(), new HostnameVerifier() {
                        @Override
                        public boolean verify(final String hostname, final SSLSession session) {
                            return true;
                        }
                    });

            httpClient =
                    HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).setDefaultRequestConfig(config)
                            .build();
        } else {
            httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        }

        return httpClient;
    }

    public List<Format> getSupportedFormats() {
        return supportedFormats;
    }
}

