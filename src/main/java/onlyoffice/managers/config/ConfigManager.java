package onlyoffice.managers.config;

import onlyoffice.model.config.Type;
import onlyoffice.model.config.editor.Mode;
import org.json.JSONObject;

import java.io.Serializable;

public interface ConfigManager extends Serializable {
    String createConfig(Long attachmentId, Mode mode, Type type, JSONObject actionLink, String referer)
            throws Exception;
    String createConfig(Long attachmentId, Mode mode, Type type, JSONObject actionLink, String referer,
                               String width, String height) throws Exception;
}
