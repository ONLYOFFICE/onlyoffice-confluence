package onlyoffice.utils.attachment;

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.user.User;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

public interface AttachmentUtil extends Serializable {
    Attachment getAttachment(Long attachmentId);

    Attachment getAttachmentByName(String fileName, Long pageId);

    boolean checkAccess(Long attachmentId, User user, boolean forEdit);

    boolean checkAccess(Attachment attachment, User user, boolean forEdit);

    boolean checkAccessCreate(User user, Long pageId);

    void saveAttachmentAsNewVersion(Long attachmentId, InputStream attachmentData, int size, ConfluenceUser user)
            throws IOException, IllegalArgumentException;

    void updateAttachment(Long attachmentId, InputStream attachmentData, int size, ConfluenceUser user);

    void saveAttachmentChanges(Long attachmentId, String history, String changesUrl) throws Exception;

    void removeAttachmentChanges(Long attachmentId);

    InputStream getAttachmentData(Long attachmentId);

    String getMediaType(Long attachmentId);

    String getFileName(Long attachmentId);

    String getFileExt(Long attachmentId);

    String getHashCode(Long attachmentId);

    String getCollaborativeEditingKey(Long attachmentId);

    void setCollaborativeEditingKey(Long attachmentId, String key);

    String getProperty(Long attachmentId, String name);

    boolean getPropertyAsBoolean(Long attachmentId, String name);

    void setProperty(Long attachmentId, String name, String value);

    void removeProperty(Long attachmentId, String name);

    List<Attachment> getAllVersions(Long attachmentId);

    int getVersion(Long attachmentId);

    Attachment getAttachmentChanges(Long attachmentId);

    Attachment getAttachmentDiff(Long attachmentId);

    String getAttachmentPageTitle(Long attachmentId);

    Long getAttachmentPageId(Long attachmentId);

    String getAttachmentSpaceName(Long attachmentId);

    String getAttachmentSpaceKey(Long attachmentId);

    Attachment createNewAttachment(String title, String mimeType, InputStream file, int size, Long pageId,
                                   ConfluenceUser user) throws IOException;

    File getConvertedFile(Long attachmentId);

    ContentEntityObject getContainer(Long containerId);
    public Attachment getAttachment(Long attachmentId);
}
