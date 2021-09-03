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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.atlassian.confluence.content.ContentProperties;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.pages.persistence.dao.AttachmentDao;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.document.DocumentManager;
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
    private final TransactionTemplate transactionTemplate;
    @ComponentImport
    private final PageManager pageManager;

    private final ConfigurationManager configurationManager;

    @Inject
    public AttachmentUtilImpl(AttachmentManager attachmentManager, TransactionTemplate transactionTemplate,
            ConfigurationManager configurationManager, PageManager pageManager) {
        this.attachmentManager = attachmentManager;
        this.transactionTemplate = transactionTemplate;
        this.configurationManager = configurationManager;
        this.pageManager = pageManager;
    }

    public boolean checkAccess(Long attachmentId, User user, boolean forEdit) {
        if (user == null) {
            return false;
        }

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

    public void saveAttachmentAsNewVersion(Long attachmentId, InputStream attachmentData, int size, ConfluenceUser user)
            throws IOException, IllegalArgumentException {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        Attachment oldAttachment = attachment.copy();
        attachment.setFileSize(size);

        AuthenticatedUserThreadLocal.set(user);

        attachmentManager.saveAttachment(attachment, oldAttachment, attachmentData);
    }

    public void updateAttachment(Long attachmentId, InputStream attachmentData, int size, ConfluenceUser user) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        Date date = Calendar.getInstance().getTime();

        attachment.setFileSize(size);
        attachment.setCreator(user);
        attachment.setCreationDate(date);

        AttachmentDao attDao = attachmentManager.getAttachmentDao();
        Object result = transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction() {
                attDao.replaceAttachmentData(attachment, attachmentData);
                attDao.updateAttachment(attachment);
                return null;
            }
        });
    }

    public void saveAttachmentChanges (Long attachmentId, String history, String changesUrl) throws IOException {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        if (history != null && !history.isEmpty() && changesUrl != null && !changesUrl.isEmpty()) {
            HttpURLConnection connection = null;
            try {
                InputStream changesStream = new ByteArrayInputStream(history.getBytes(StandardCharsets.UTF_8));
                Attachment changes = new Attachment("onlyoffice-changes.json", "application/json", changesStream.available(), "");
                changes.setContainer(attachment.getContainer());

                URL url = new URL(changesUrl);
                connection = (HttpURLConnection) url.openConnection();
                Integer timeout = Integer.parseInt(configurationManager.getProperty("timeout")) * 1000;
                connection.setConnectTimeout(timeout);
                connection.setReadTimeout(timeout);
                int size = connection.getContentLength();
                InputStream streamDiff = connection.getInputStream();

                Attachment diff = new Attachment("onlyoffice-diff.zip", "application/zip", size, "");
                diff.setContainer(attachment.getContainer());

                attachment.addAttachment(changes);
                attachment.addAttachment(diff);

                AttachmentDao attDao = attachmentManager.getAttachmentDao();
                Object result = transactionTemplate.execute(new TransactionCallback() {
                    @Override
                    public Object doInTransaction() {
                        attDao.saveNewAttachment(changes, changesStream);
                        attDao.saveNewAttachment(diff, streamDiff);
                        attDao.updateAttachment(attachment);
                        return null;
                    }
                });
            } catch (Exception e) {
                    throw e;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    public void removeAttachmentChanges (Long attachmentId) {
        Attachment changes = getAttachmentChanges(attachmentId);
        Attachment diff = getAttachmentDiff(attachmentId);

        AttachmentDao attDao = attachmentManager.getAttachmentDao();
        Object result = transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction() {
                if (changes != null) attDao.removeAttachmentFromServer(changes);
                if (diff != null) attDao.removeAttachmentFromServer(diff);
                return null;
            }
        });
    }

    public InputStream getAttachmentData(Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachmentManager.getAttachmentData(attachment);
    }

    public String getMediaType(Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getMediaType();
    }

    public String getFileName(Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getFileName();
    }

    public String getHashCode(Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        int hashCode = attachment.hashCode();
        log.info("hashCode = " + hashCode);

        int version = attachment.getVersion();
        return attachmentId + "_" + version + "_" + hashCode;
    }

    public String getCollaborativeEditingKey (Long attachmentId) {
        return getProperty(attachmentId, "onlyoffice-collaborative-editor-key");
    }

    public void setCollaborativeEditingKey (Long attachmentId, String key) {
        if (key == null || key.isEmpty()) {
            removeProperty(attachmentId, "onlyoffice-collaborative-editor-key");
        } else {
            setProperty(attachmentId, "onlyoffice-collaborative-editor-key", key);
        }
    }

    public String getProperty (Long attachmentId, String name) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            ContentProperties contentProperties = attachment.getProperties();
            return contentProperties.getStringProperty(name);
        }
        return null;
    }

    public boolean getPropertyAsBoolean (Long attachmentId, String name) {
        String property = getProperty(attachmentId, name);
        return Boolean.parseBoolean(property);
    }

    public void setProperty (Long attachmentId, String name, String value) {
        AttachmentDao attDao = attachmentManager.getAttachmentDao();
        Attachment attachment = attDao.getById(attachmentId);

        attachment.getProperties().setStringProperty(name, value);

        Object result = transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction() {
                attDao.updateAttachment(attachment);
                return null;
            }
        });
    }

    public void removeProperty (Long attachmentId, String name) {
        AttachmentDao attDao = attachmentManager.getAttachmentDao();
        Attachment attachment = attDao.getById(attachmentId);

        attachment.getProperties().removeProperty(name);

        Object result = transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction() {
                attDao.updateAttachment(attachment);
                return null;
            }
        });
    }

    public List<Attachment> getAllVersions (Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachmentManager.getAllVersions(attachment);
        }
        return null;
    }

    public int getVersion (Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getVersion();
    }

    public Attachment getAttachmentChanges (Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachment.getAttachmentNamed("onlyoffice-changes.json");
        }
        return null;
    }

    public Attachment getAttachmentDiff (Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachment.getAttachmentNamed("onlyoffice-diff.zip");
        }
        return null;
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

}