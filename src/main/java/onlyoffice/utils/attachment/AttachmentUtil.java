package onlyoffice.utils.attachment;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.user.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

public interface AttachmentUtil extends Serializable {
    public boolean checkAccess(Long attachmentId, User user, boolean forEdit);
    public boolean checkAccess(Attachment attachment, User user, boolean forEdit);
    public void saveAttachment(Long attachmentId, InputStream attachmentData, int size, ConfluenceUser user)
            throws IOException, IllegalArgumentException;
    public InputStream getAttachmentData(Long attachmentId);
    public String getMediaType(Long attachmentId);
    public String getFileName(Long attachmentId);
    public String getHashCode(Long attachmentId);
    public List<Attachment> getAllVersions (Long attachmentId);
    public int getVersion (Long attachmentId);
}
