package onlyoffice.managers.url;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

public interface UrlManager extends Serializable {
    public String getPublicDocEditorUrl();
    public String getInnerDocEditorUrl();
    public String getFileUri(Long attachmentId) throws Exception;
    public String getCallbackUrl(Long attachmentId);
    public String getGobackUrl(Long attachmentId, HttpServletRequest request);
    public String replaceDocEditorURLToInternal(String url);
}
