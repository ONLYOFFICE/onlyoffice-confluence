package onlyoffice.managers.document;

import java.io.Serializable;
import java.util.List;

public interface DocumentManager extends Serializable {
    public long getMaxFileSize();
    public List<String> getEditedExts();
    public String getKeyOfFile(Long attachmentId);
    public String createHash(String str);
    public String readHash(String base64);
    public Long createDemo(String fileName, String fileExt, Long pageID);
    public String getDocType(String ext);
    public String getMimeType(String name);
}
