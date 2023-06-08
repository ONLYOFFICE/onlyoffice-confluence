package onlyoffice.managers.configuration;

import onlyoffice.model.Format;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public interface ConfigurationManager extends Serializable {
    Properties getProperties() throws IOException;

    String getProperty(String propertyName);

    boolean forceSaveEnabled();

    boolean selectDemo(Boolean demo);

    Boolean demoEnabled();

    Boolean demoAvailable(Boolean forActivate);

    Boolean demoActive();

    String getDemo(String key);

    Boolean getBooleanPluginSetting(String key, Boolean defaultValue);

    String getStringPluginSetting(String key, String defaultValue);

    Map<String, Boolean> getCustomizableEditingTypes();

    CloseableHttpClient getHttpClient() throws Exception;

    List<Format> getSupportedFormats();
}
