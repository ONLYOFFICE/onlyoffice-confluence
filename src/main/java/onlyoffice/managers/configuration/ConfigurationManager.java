package onlyoffice.managers.configuration;

import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public interface ConfigurationManager extends Serializable {
    public Properties getProperties() throws IOException;
    public String getProperty(String propertyName);
    public boolean forceSaveEnabled();
    public boolean selectDemo(Boolean demo);
    public Boolean demoEnabled();
    public Boolean demoAvailable(Boolean forActivate);
    public Boolean demoActive();
    public String getDemo(String key);
    public Boolean getBooleanPluginSetting(String key, Boolean defaultValue);
    public String getStringPluginSetting(String key, String defaultValue);
    public List<String> getDefaultEditingTypes();
    public Map<String, Boolean> getCustomizableEditingTypes();
    public CloseableHttpClient getHttpClient() throws Exception;
}
