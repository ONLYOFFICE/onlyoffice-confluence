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

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Named
public class ConfigurationManager {
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private final PluginSettings pluginSettings;
    private Map<String, String> demoData;

    @Inject
    public ConfigurationManager(PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        pluginSettings = pluginSettingsFactory.createGlobalSettings();

        demoData = new HashMap<String, String>();
        demoData.put("url", "https://onlinedocs.onlyoffice.com/");
        demoData.put("header", "AuthorizationJWT");
        demoData.put("secret", "sn2puSUF7muF5Jas");
        demoData.put("trial", "30");
    }

    public Properties GetProperties() throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("onlyoffice-config.properties");
        if (inputStream != null) {
            properties.load(inputStream);
        }
        return properties;
    }

    public boolean selectDemo(Boolean demo) {
        pluginSettings.put("onlyoffice.demo", demo.toString());
        if (demo) {
            String demoStart = (String) pluginSettings.get("onlyoffice.demoStart");
            if (demoStart == null || demoStart.isEmpty()) {
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                pluginSettings.put("onlyoffice.demoStart", dateFormat.format(date));
            }
            return true;
        }
        return false;
    }

    public Boolean demoEnabled() {
        String demo = (String) pluginSettings.get("onlyoffice.demo");
        if (demo == null || demo.isEmpty()) {
            demo = "false";
        }
        return Boolean.parseBoolean(demo);
    }

    public Boolean demoAvailable(Boolean forActivate) {
        String demoStart = (String) pluginSettings.get("onlyoffice.demoStart");
        if (demoStart != null && !demoStart.isEmpty()) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            try {
                Calendar date = Calendar.getInstance();
                date.setTime(dateFormat.parse(demoStart));
                date.add(Calendar.DATE, Integer.parseInt(demoData.get("trial")));
                if (date.after(Calendar.getInstance())) {
                    return true;
                }else {
                    return false;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return forActivate;
    }

    public Boolean demoActive() {
        return demoEnabled() && demoAvailable(false);
    }

    public String getDemo(String key) {
        return demoData.get(key);
    }
}
