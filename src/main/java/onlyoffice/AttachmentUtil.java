/**
 *
 * (c) Copyright Ascensio System SIA 2020
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

package onlyoffice;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.user.User;

public class AttachmentUtil {
    private static final Logger log = LogManager.getLogger("onlyoffice.AttachmentUtil");

    public static boolean checkAccess(Long attachmentId, User user, boolean forEdit) {
        if (user == null) {
            return false;
        }

        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        return checkAccess(attachment, user, forEdit);
    }

    public static boolean checkAccess(Attachment attachment, User user, boolean forEdit) {
        if (user == null) {
            return false;
        }

        PermissionManager permissionManager = (PermissionManager) ContainerManager.getComponent("permissionManager");

        Permission permission = Permission.VIEW;
        if (forEdit) {
            permission = Permission.EDIT;
        }

        boolean access = permissionManager.hasPermission(user, permission, attachment);
        return access;
    }

    public static void saveAttachment(Long attachmentId, InputStream attachmentData, int size, ConfluenceUser user)
            throws IOException, IllegalArgumentException {
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        Attachment oldAttachment = attachment.copy();
        attachment.setFileSize(size);

        AuthenticatedUserThreadLocal.set(user);

        attachmentManager.saveAttachment(attachment, oldAttachment, attachmentData);
    }

    public static InputStream getAttachmentData(Long attachmentId) {
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachmentManager.getAttachmentData(attachment);
    }

    public static String getMediaType(Long attachmentId) {
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getMediaType();
    }

    public static String getFileName(Long attachmentId) {
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getFileName();
    }

    public static String getHashCode(Long attachmentId) {
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        int hashCode = attachment.hashCode();
        log.info("hashCode = " + hashCode);

        int version = attachment.getVersion();
        return attachmentId + "_" + version + "_" + hashCode;
    }
}