/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Named
@Default
public class ConfigurationManagerImpl implements ConfigurationManager {
    private final Logger log = LogManager.getLogger("onlyoffice.managers.configuration.ConfigurationManager");

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;
    private final PluginSettings pluginSettings;

    private final String configurationPath = "onlyoffice-config.properties";
    private final String pluginDemoName = "onlyoffice.demo";
    private final String pluginDemoNameStart = "onlyoffice.demoStart";
    private Map<String, String> demoData;

    @Inject
    public ConfigurationManagerImpl(PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        pluginSettings = pluginSettingsFactory.createGlobalSettings();

        demoData = new HashMap<String, String>();
        demoData.put("url", "https://onlinedocs.onlyoffice.com/");
        demoData.put("header", "AuthorizationJWT");
        demoData.put("secret", "sn2puSUF7muF5Jas");
        demoData.put("trial", "30");
    }

    public Properties getProperties() throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configurationPath);
        if (inputStream != null) {
            properties.load(inputStream);
        }
        return properties;
    }

    public String getProperty(String propertyName){
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

    public boolean selectDemo(Boolean demo) {
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

    public Boolean demoAvailable(Boolean forActivate) {
        String demoStart = (String) pluginSettings.get(pluginDemoNameStart);
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

    public Boolean getBooleanPluginSetting(String key, Boolean defaultValue) {
        String setting = (String) pluginSettings.get("onlyoffice." + key);
        if (setting == null || setting.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(setting);
    }

    public Map<String, Boolean> getCustomizableEditingTypes () {
        Map<String, Boolean> customizableEditingTypes = new HashMap<>();
        List<String> editingTypes = null;

        String editingTypesString = (String) pluginSettings.get("onlyoffice.editingTypes");

        if (editingTypesString != null && !editingTypesString.isEmpty()) {
            editingTypes = Arrays.asList(editingTypesString.substring(1, editingTypesString.length() - 1).replace("\"", "").split(","));
        } else {
            editingTypes = Arrays.asList("csv", "txt");
        }

        List<String> availableTypes = Arrays.asList(getProperty("docservice.type.edit.customizable").split("\\|"));

        for (String type : availableTypes) {
            customizableEditingTypes.put(type, editingTypes.contains(type));
        }

        return customizableEditingTypes;
    }
}

