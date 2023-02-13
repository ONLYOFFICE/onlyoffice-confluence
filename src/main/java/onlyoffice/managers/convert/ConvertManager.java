package onlyoffice.managers.convert;

import com.atlassian.confluence.user.ConfluenceUser;
import org.json.JSONObject;

import java.io.Serializable;

public interface ConvertManager extends Serializable {
    boolean isConvertable(String ext);
    String convertsTo(String ext);
    JSONObject convert(Long attachmentId, String ext, String convertToExt, ConfluenceUser user) throws Exception;
    JSONObject convert(Long attachmentId, String currentExt, String convertToExt, String url, String region, boolean async) throws Exception;
}
