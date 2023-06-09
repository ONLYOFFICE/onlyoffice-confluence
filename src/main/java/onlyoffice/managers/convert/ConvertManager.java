package onlyoffice.managers.convert;

import com.atlassian.confluence.user.ConfluenceUser;
import org.json.JSONObject;
import java.util.List;
import java.io.Serializable;

public interface ConvertManager extends Serializable {
    JSONObject convert(Long attachmentId, String ext, String convertToExt, ConfluenceUser user,
                       String title) throws Exception;

    JSONObject convert(Long attachmentId, String currentExt, String convertToExt, String url, String region,
                       boolean async, String title) throws Exception;

    String getTargetExt(String ext);

    List<String> getTargetExtList(String ext);
}
