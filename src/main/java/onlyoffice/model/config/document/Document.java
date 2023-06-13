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

package onlyoffice.model.config.document;

import onlyoffice.managers.document.DocumentManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.model.config.Type;
import onlyoffice.utils.attachment.AttachmentUtil;

public class Document {
    private String key;
    private String title;
    private String url;
    private String fileType;
    private Permissions permissions;

    public Document(final DocumentManager documentManager, final AttachmentUtil attachmentUtil,
                    final UrlManager urlManager, final Long attachmentId, final Type type) {
        key = documentManager.getKeyOfFile(attachmentId, type.equals(Type.EMBEDDED));
        title = attachmentUtil.getFileName(attachmentId);
        fileType = attachmentUtil.getFileExt(attachmentId);
        url = urlManager.getFileUri(attachmentId);
        permissions = new Permissions(documentManager, attachmentUtil, attachmentId);
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(final String fileType) {
        this.fileType = fileType;
    }

    public Permissions getPermissions() {
        return permissions;
    }

    public void setPermissions(final Permissions permissions) {
        this.permissions = permissions;
    }
}
