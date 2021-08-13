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
    public void saveAttachmentChanges (Long attachmentId, String history, String changesUrl) throws IOException;
    public void removeAttachmentChanges (Long attachmentId);
    public InputStream getAttachmentData(Long attachmentId);
    public String getMediaType(Long attachmentId);
    public String getFileName(Long attachmentId);
    public String getHashCode(Long attachmentId);
    public String getCollaborativeEditingKey (Long attachmentId);
    public void setCollaborativeEditingKey (Long attachmentId, String key);
    public List<Attachment> getAllVersions (Long attachmentId);
    public int getVersion (Long attachmentId);
    public Attachment getAttachmentChanges (Long attachmentId);
    public Attachment getAttachmentDiff (Long attachmentId);
    public String getAttachmentPageTitle (Long attachmentId);
    public Long getAttachmentPageId (Long attachmentId);
    public String getAttachmentSpaceName (Long attachmentId);
    public String getAttachmentSpaceKey (Long attachmentId);
}
