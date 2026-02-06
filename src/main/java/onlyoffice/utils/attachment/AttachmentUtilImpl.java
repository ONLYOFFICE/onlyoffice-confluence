/**
 *
 * (c) Copyright Ascensio System SIA 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package onlyoffice.utils.attachment;

import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.spring.container.ContainerManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AttachmentUtilImpl implements AttachmentUtil {
    private final Logger log = LogManager.getLogger("onlyoffice.utils.attachment.AttachmentUtil");

    private final AttachmentManager attachmentManager;

    public AttachmentUtilImpl(final AttachmentManager attachmentManager) {
        this.attachmentManager = attachmentManager;
    }

    public Attachment getAttachment(final Long attachmentId) {
        try {
            return attachmentManager.getAttachment(attachmentId);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public Attachment getAttachmentByName(final String fileName, final Long pageId) {
        ContentEntityManager contentEntityManager =
                (ContentEntityManager) ContainerManager.getComponent("contentEntityManager");
        ContentEntityObject contentEntityObject = contentEntityManager.getById(pageId);

        List<Attachment> attachments = attachmentManager.getLatestVersionsOfAttachments(contentEntityObject);

        for (Attachment attachment : attachments) {
            if (attachment.getFileName().equals(fileName)) {
                return attachment;
            }
        }

        return null;
    }

    public boolean checkAccess(final Long attachmentId, final ConfluenceUser user, final boolean forEdit) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        return checkAccess(attachment, user, forEdit);
    }

    public boolean checkAccess(final Attachment attachment, final ConfluenceUser user, final boolean forEdit) {
        PermissionManager permissionManager = (PermissionManager) ContainerManager.getComponent("permissionManager");

        if (forEdit) {
            boolean create = checkAccessCreate(user, attachment.getContainer().getId());
            boolean access = permissionManager.hasPermission(user, Permission.EDIT, attachment);
            return create && access && attachment.isLatestVersion();
        } else {
            boolean access = permissionManager.hasPermission(user, Permission.VIEW, attachment);
            return access;
        }
    }

    public boolean checkAccessCreate(final ConfluenceUser user, final Long pageId) {
        if (user == null) {
            return false;
        }

        PermissionManager permissionManager = (PermissionManager) ContainerManager.getComponent("permissionManager");

        ContentEntityObject container = getContainer(pageId);
        boolean access = permissionManager.hasCreatePermission(user, container, Attachment.class);

        return access;
    }

    public void saveAttachmentAsNewVersion(final Long attachmentId, final File file, final ConfluenceUser user)
            throws IOException {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        Attachment oldAttachment = attachment.copy();
        attachment.setFileSize(file.length());

        AuthenticatedUserThreadLocal.set(user);

        attachmentManager.saveAttachment(attachment, oldAttachment, Files.newInputStream(file.toPath()));
    }

    public InputStream getAttachmentData(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachmentManager.getAttachmentData(attachment);
    }

    public String getMediaType(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getMediaType();
    }

    public String getHashCode(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        int hashCode = attachment.hashCode();
        log.info("hashCode = " + hashCode);

        int version = attachment.getVersion();
        return attachmentId + "_" + version + "_" + hashCode;
    }

    public List<Attachment> getAllVersions(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachmentManager.getAllVersions(attachment);
        }
        return null;
    }

    public int getVersion(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getVersion();
    }

    public String getAttachmentPageTitle(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachment.getContainer().getTitle();
        }
        return null;
    }

    public Long getAttachmentPageId(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachment.getContainer().getId();
        }
        return null;
    }

    public String getAttachmentSpaceName(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachment.getSpace().getName();
        }
        return null;
    }

    public String getAttachmentSpaceKey(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachment.getSpace().getKey();
        }
        return null;
    }

    public Attachment createNewAttachment(final String fileName, final String mimeType, final InputStream file,
                                          final int size, final Long pageId, final ConfluenceUser user)
            throws IOException {
        Date date = Calendar.getInstance().getTime();
        ContentEntityObject container = getContainer(pageId);

        Attachment attachment = new Attachment(fileName, mimeType, size, "");

        attachment.setCreator(user);
        attachment.setCreationDate(date);
        attachment.setLastModificationDate(date);
        attachment.setContainer(container);

        attachmentManager.saveAttachment(attachment, null, file);

        container.addAttachment(attachment);

        return attachment;
    }

    public Attachment createNewAttachment(final String fileName, final String mimeType, final File file,
                                          final Long pageId, final ConfluenceUser user)
            throws IOException {
        Date date = Calendar.getInstance().getTime();
        ContentEntityObject container = getContainer(pageId);

        Attachment attachment = new Attachment(
                fileName,
                mimeType,
                file.length(),
                ""
        );

        attachment.setCreator(user);
        attachment.setCreationDate(date);
        attachment.setLastModificationDate(date);
        attachment.setContainer(container);

        attachmentManager.saveAttachment(attachment, null, Files.newInputStream(file.toPath()));

        container.addAttachment(attachment);

        return attachment;
    }

    public ContentEntityObject getContainer(final Long containerId) {
        ContentEntityManager contentEntityManager =
                (ContentEntityManager) ContainerManager.getComponent("contentEntityManager");
        return contentEntityManager.getById(containerId);
    }

    public String getCorrectName(final String fileName, final String fileExt, final Long pageID) {
        ContentEntityManager contentEntityManager =
                (ContentEntityManager) ContainerManager.getComponent("contentEntityManager");
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        ContentEntityObject contentEntityObject = contentEntityManager.getById(pageID);

        List<Attachment> attachments = attachmentManager.getLatestVersionsOfAttachments(contentEntityObject);
        String name = (fileName + "." + fileExt).replaceAll("[*?:\"<>/|\\\\]", "_");
        int count = 0;
        Boolean flag = true;

        while (flag) {
            flag = false;
            for (Attachment attachment : attachments) {
                if (attachment.getFileName().equals(name)) {
                    count++;
                    name = fileName + " (" + count + ")." + fileExt;
                    flag = true;
                    break;
                }
            }
        }

        return name;
    }
}
