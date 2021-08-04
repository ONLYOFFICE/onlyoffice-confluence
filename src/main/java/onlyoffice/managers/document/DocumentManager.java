package onlyoffice.managers.document;

import java.io.Serializable;

public interface DocumentManager extends Serializable {
    public long getMaxFileSize();
    public String getKeyOfFile(Long attachmentId);
    public String createHash(String str);
    public String readHash(String base64);
    public Long createDemo(String fileName, String fileExt, Long pageID);
    public String getDocType(String ext);
    public String getEditorType (String userAgent);
    public String getMimeType(String name);
    public boolean isEditable(String fileExtension);
    public boolean isViewable(String fileExtension);
}
