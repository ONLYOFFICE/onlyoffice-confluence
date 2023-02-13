package onlyoffice.managers.url;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

public interface UrlManager extends Serializable {
    String getPublicDocEditorUrl();
    String getInnerDocEditorUrl();
    String getFileUri(Long attachmentId);
    String getAttachmentDiffUri(Long attachmentId);
    String getHistoryInfoUri(Long attachmentId);
    String getHistoryDataUri(Long attachmentId);
    String getAttachmentDataUri();
    String getSaveAsUri();
    String getCallbackUrl(Long attachmentId);
    String getGobackUrl(Long attachmentId, HttpServletRequest request);
    String getCreateUri(Long pageId, String ext);
    String replaceDocEditorURLToInternal(String url);
}
