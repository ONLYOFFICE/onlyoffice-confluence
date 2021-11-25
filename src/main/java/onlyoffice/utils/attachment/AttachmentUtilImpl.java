/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import onlyoffice.managers.configuration.ConfigurationManager;
import org.apache.commons.codec.binary.Hex;
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

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@Default
public class AttachmentUtilImpl implements AttachmentUtil {
    private final Logger log = LogManager.getLogger("onlyoffice.utils.attachment.AttachmentUtil");

    @ComponentImport
    private final AttachmentManager attachmentManager;
    @ComponentImport
    private final PageManager pageManager;
    private final ConfigurationManager configurationManager;
    @Inject
    public AttachmentUtilImpl(AttachmentManager attachmentManager, PageManager pageManager,
                              ConfigurationManager configurationManager) {
        this.attachmentManager = attachmentManager;
        this.pageManager = pageManager;
        this.configurationManager = configurationManager;
    }

    public boolean checkAccess(Long attachmentId, User user, boolean forEdit) {
        if (user == null) {
            return false;
        }

        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        return checkAccess(attachment, user, forEdit);
    }

    public boolean checkAccess(Attachment attachment, User user, boolean forEdit) {
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

    public boolean checkAccessCreate(User user, Long pageId) {
        if (user == null) {
            return false;
        }

        PermissionManager permissionManager = (PermissionManager) ContainerManager.getComponent("permissionManager");

        Page page = pageManager.getPage(pageId);
        boolean access = permissionManager.hasCreatePermission(user, page, Attachment.class);

        return access;
    }

    public void saveAttachment(Long attachmentId, InputStream attachmentData, int size, ConfluenceUser user)
            throws IOException, IllegalArgumentException {
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        Attachment oldAttachment = attachment.copy();
        attachment.setFileSize(size);

        AuthenticatedUserThreadLocal.set(user);

        attachmentManager.saveAttachment(attachment, oldAttachment, attachmentData);
    }

    public InputStream getAttachmentData(Long attachmentId) {
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachmentManager.getAttachmentData(attachment);
    }

    public String getMediaType(Long attachmentId) {
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getMediaType();
    }

    public String getFileName(Long attachmentId) {
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getFileName();
    }

    public String getHashCode(Long attachmentId) {
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        int hashCode = attachment.hashCode();
        log.info("hashCode = " + hashCode);

        int version = attachment.getVersion();
        return attachmentId + "_" + version + "_" + hashCode;
    }

    public String getAttachmentPageTitle (Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachment.getContainer().getTitle();
        }
        return null;
    }

    public Long getAttachmentPageId (Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachment.getContainer().getId();
        }
        return null;
    }

    public String getAttachmentSpaceName (Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachment.getSpace().getName();
        }
        return null;
    }

    public String getAttachmentSpaceKey (Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachment.getSpace().getKey();
        }
        return null;
    }

    public Attachment createNewAttachment (String fileName, String mimeType, InputStream file, int size, Long pageId, ConfluenceUser user) throws IOException {
        Date date = Calendar.getInstance().getTime();

        Attachment attachment = new Attachment(fileName, mimeType, size, "");

        attachment.setCreator(user);
        attachment.setCreationDate(date);
        attachment.setLastModificationDate(date);
        attachment.setContainer(pageManager.getPage(pageId));

        attachmentManager.saveAttachment(attachment, null, file);

        Page page = pageManager.getPage(pageId);
        page.addAttachment(attachment);

        return attachment;
    }

    public String getAttachmentExt (Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getFileExtension();
    }

    public String createHash(String str) {
        try {
            String secret = configurationManager.getProperty("files.docservice.secret");

            String payload = getHashHex(str + secret) + "?" + str;

            String base64 = Base64.getEncoder().encodeToString(payload.getBytes("UTF-8"));
            return base64;
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

    public String readHash(String base64) {
        try {
            String str = new String(Base64.getDecoder().decode(base64), "UTF-8");

            String secret = configurationManager.getProperty("files.docservice.secret");

            String[] payloadParts = str.split("\\?");

            String payload = getHashHex(payloadParts[1] + secret);
            if (payload.equals(payloadParts[0])) {
                return payloadParts[1];
            }
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

    private String getHashHex(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(str.getBytes());
            String hex = Hex.encodeHexString(digest);

            return hex;
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

}