/**
 *
 * (c) Copyright Ascensio System SIA 2023
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.atlassian.confluence.content.ContentProperties;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.pages.persistence.dao.AttachmentDao;
import com.atlassian.confluence.pages.persistence.dao.filesystem.HierarchicalContentFileSystemHelper;
import com.atlassian.confluence.setup.BootstrapManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import onlyoffice.managers.configuration.ConfigurationManager;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
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
    private static final HierarchicalContentFileSystemHelper fileSystemHelper = new HierarchicalContentFileSystemHelper();

    @ComponentImport
    private final AttachmentManager attachmentManager;
    @ComponentImport
    private final TransactionTemplate transactionTemplate;
    @ComponentImport
    private final PageManager pageManager;
    @ComponentImport
    private final BootstrapManager bootstrapManager;

    private final ConfigurationManager configurationManager;

    @Inject
    public AttachmentUtilImpl(final AttachmentManager attachmentManager, final TransactionTemplate transactionTemplate,
                              final ConfigurationManager configurationManager, final PageManager pageManager,
                              final BootstrapManager bootstrapManager) {
        this.attachmentManager = attachmentManager;
        this.transactionTemplate = transactionTemplate;
        this.configurationManager = configurationManager;
        this.pageManager = pageManager;
        this.bootstrapManager = bootstrapManager;
    }

    public boolean checkAccess(final Long attachmentId, final User user, final boolean forEdit) {
        if (user == null) {
            return false;
        }

        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        return checkAccess(attachment, user, forEdit);
    }

    public boolean checkAccess(final Attachment attachment, final User user, final boolean forEdit) {
        if (user == null) {
            return false;
        }

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

    public boolean checkAccessCreate(final User user, final Long pageId) {
        if (user == null) {
            return false;
        }

        PermissionManager permissionManager = (PermissionManager) ContainerManager.getComponent("permissionManager");

        Page page = pageManager.getPage(pageId);
        boolean access = permissionManager.hasCreatePermission(user, page, Attachment.class);

        return access;
    }

    public void saveAttachmentAsNewVersion(final Long attachmentId, final InputStream attachmentData, final int size, final ConfluenceUser user)
            throws IOException, IllegalArgumentException {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        Attachment oldAttachment = attachment.copy();
        attachment.setFileSize(size);

        AuthenticatedUserThreadLocal.set(user);

        attachmentManager.saveAttachment(attachment, oldAttachment, attachmentData);
    }

    public void updateAttachment(final Long attachmentId, final InputStream attachmentData, final int size, final ConfluenceUser user) {
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

    public void saveAttachmentChanges(final Long attachmentId, final String history, final String changesUrl) throws Exception {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        if (history != null && !history.isEmpty() && changesUrl != null && !changesUrl.isEmpty()) {
            InputStream changesStream = new ByteArrayInputStream(history.getBytes(StandardCharsets.UTF_8));
            Attachment changes = new Attachment("onlyoffice-changes.json", "application/json", changesStream.available(), "");
            changes.setContainer(attachment.getContainer());
            changes.setHidden(true);

            try (CloseableHttpClient httpClient = configurationManager.getHttpClient()) {
                HttpGet request = new HttpGet(changesUrl);

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int status = response.getStatusLine().getStatusCode();
                    HttpEntity entity = response.getEntity();

                    if (status == HttpStatus.SC_OK) {
                        byte[] bytes = IOUtils.toByteArray(entity.getContent());
                        InputStream streamDiff = new ByteArrayInputStream(bytes);

                        Attachment diff = new Attachment("onlyoffice-diff.zip", "application/zip", bytes.length, "");
                        diff.setContainer(attachment.getContainer());
                        diff.setHidden(true);

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
                    } else {
                        throw new HttpException("Docserver returned code " + status);
                    }
                }
            }
        }
    }

    public void removeAttachmentChanges(final Long attachmentId) {
        Attachment changes = getAttachmentChanges(attachmentId);
        Attachment diff = getAttachmentDiff(attachmentId);

        AttachmentDao attDao = attachmentManager.getAttachmentDao();
        Object result = transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction() {
                if (changes != null) { attDao.removeAttachmentFromServer(changes);}
                if (diff != null) { attDao.removeAttachmentFromServer(diff); }
                return null;
            }
        });
    }

    public InputStream getAttachmentData(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachmentManager.getAttachmentData(attachment);
    }

    public String getMediaType(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getMediaType();
    }

    public String getFileName(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getFileName();
    }

    public String getFileExt(final Long attachmentId) {
        String fileName = getFileName(attachmentId);
        return fileName.substring(fileName.lastIndexOf(".") + 1).trim().toLowerCase();
    }

    public String getHashCode(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        int hashCode = attachment.hashCode();
        log.info("hashCode = " + hashCode);

        int version = attachment.getVersion();
        return attachmentId + "_" + version + "_" + hashCode;
    }

    public String getCollaborativeEditingKey(final Long attachmentId) {
        return getProperty(attachmentId, "onlyoffice-collaborative-editor-key");
    }

    public void setCollaborativeEditingKey(final Long attachmentId, final String key) {
        if (key == null || key.isEmpty()) {
            removeProperty(attachmentId, "onlyoffice-collaborative-editor-key");
        } else {
            setProperty(attachmentId, "onlyoffice-collaborative-editor-key", key);
        }
    }

    public String getProperty(final Long attachmentId, final String name) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            ContentProperties contentProperties = attachment.getProperties();
            return contentProperties.getStringProperty(name);
        }
        return null;
    }

    public boolean getPropertyAsBoolean(final Long attachmentId, final String name) {
        String property = getProperty(attachmentId, name);
        return Boolean.parseBoolean(property);
    }

    public void setProperty(final Long attachmentId, final String name, final String value) {
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

    public void removeProperty(final Long attachmentId, final String name) {
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

    public Attachment getAttachmentChanges(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachment.getAttachmentNamed("onlyoffice-changes.json");
        }
        return null;
    }

    public Attachment getAttachmentDiff(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        if (attachment != null) {
            return attachment.getAttachmentNamed("onlyoffice-diff.zip");
        }
        return null;
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

    public Attachment createNewAttachment(final String fileName, final String mimeType, final InputStream file, final int size, final Long pageId, final ConfluenceUser user) throws IOException {
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

    public File getConvertedFile(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        File rootStorageDirectory = new File(bootstrapManager.getSharedHome() + File.separator + "dcl-document" + File.separator);
        File convertStorageFolder = fileSystemHelper.createDirectoryHierarchy(rootStorageDirectory, attachment.getContainer().getId());

        return new File(
                convertStorageFolder,
                Long.toString(attachment.getId()) + "_" + Integer.toString(attachment.getVersion())
        );
    }

}