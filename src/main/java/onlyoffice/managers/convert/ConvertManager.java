package onlyoffice.managers.convert;

import com.atlassian.confluence.user.ConfluenceUser;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

public interface ConvertManager extends Serializable {
    public boolean isConvertable(String ext);
    public String convertsTo(String ext);
    public JSONObject convert(Long attachmentId, String ext, String convertToExt, ConfluenceUser user) throws Exception;
    public JSONObject convert(Long attachmentId, String currentExt, String convertToExt, String url, String region, boolean async) throws Exception;
    String getTargetExt(String ext);
    List<String> getTargetExtList(String ext);
}
