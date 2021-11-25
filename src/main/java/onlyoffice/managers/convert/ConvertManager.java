package onlyoffice.managers.convert;

import org.json.JSONObject;

import java.io.Serializable;

public interface ConvertManager extends Serializable {
    public boolean isConvertable(String ext);
    public String convertsTo(String ext);
    public String getMimeType(String ext);
    public JSONObject convert(Long attachmentId, String key,  String ext) throws Exception;
    public JSONObject convert(Long attachmentId, String key,  String currentExt, String convertToExt, boolean async) throws Exception;
    public JSONObject convert(String key, String currentExt, String convertToExt, String url, boolean async) throws Exception;
}
