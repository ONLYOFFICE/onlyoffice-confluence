/**
 *
 * (c) Copyright Ascensio System SIA 2024
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

package onlyoffice.sdk.manager.document;

import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.spring.container.ContainerManager;
import com.onlyoffice.manager.document.DefaultDocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DocumentManagerImpl extends DefaultDocumentManager implements DocumentManager {
    private final Logger log = LogManager.getLogger("onlyoffice.sdk.manager.document.DocumentManagerImpl");
    private static final int MAX_KEY_LENGTH = 20;

    private final AttachmentUtil attachmentUtil;
    private final AttachmentManager attachmentManager;

    public DocumentManagerImpl(final SettingsManager settingsManager, final AttachmentUtil attachmentUtil,
                               final AttachmentManager attachmentManager) {
        super(settingsManager);
        this.attachmentUtil = attachmentUtil;
        this.attachmentManager = attachmentManager;
    }

    @Override
    public String getDocumentKey(final String fileId, final boolean embedded) {
        Long attachmentId = Long.parseLong(fileId);
        String key = attachmentUtil.getCollaborativeEditingKey(attachmentId);
        if (key == null) {
            String hashCode = attachmentUtil.getHashCode(attachmentId);
            key = generateRevisionId(hashCode);
        }

        return embedded ? key + "_embedded" : key;
    }

    @Override
    public String getDocumentName(final String fileId) {
        Long attachmentId = Long.parseLong(fileId);

        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        if (attachment != null) {
            return attachment.getFileName();
        }

        return null;
    }

    @Override
    public String getCorrectNewFileName(final String fileName, final String fileExtension, final Long pageID) {
        ContentEntityManager contentEntityManager =
                (ContentEntityManager) ContainerManager.getComponent("contentEntityManager");
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        ContentEntityObject contentEntityObject = contentEntityManager.getById(pageID);

        List<Attachment> attachments = attachmentManager.getLatestVersionsOfAttachments(contentEntityObject);
        String name = (fileName + "." + fileExtension).replaceAll("[*?:\"<>/|\\\\]", "_");
        int count = 0;
        Boolean flag = true;

        while (flag) {
            flag = false;
            for (Attachment attachment : attachments) {
                if (attachment.getFileName().equals(name)) {
                    count++;
                    name = fileName + " (" + count + ")." + fileExtension;
                    flag = true;
                    break;
                }
            }
        }

        return name;
    }

    @Override
    public String getMimeType(final String name) {
        Path path = new File(name).toPath();
        String mimeType = null;
        try {
            mimeType = Files.probeContentType(path);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    private String generateRevisionId(final String expectedKey) {
        String result = expectedKey;

        if (result.length() > MAX_KEY_LENGTH) {
            result = Integer.toString(result.hashCode());
        }
        String key = result.replace("[^0-9-.a-zA-Z_=]", "_");
        key = key.substring(0, Math.min(key.length(), MAX_KEY_LENGTH));
        log.info("key = " + key);
        return key;
    }

}
