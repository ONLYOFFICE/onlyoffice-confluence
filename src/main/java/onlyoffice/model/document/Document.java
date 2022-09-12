/**
 *
 * (c) Copyright Ascensio System SIA 2022
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

package onlyoffice.model.document;

import onlyoffice.managers.document.DocumentManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.model.Type;
import onlyoffice.utils.attachment.AttachmentUtil;

public class Document {
    String key;
    String title;
    String url;
    String fileType;
    Permissions permissions;

    public Document(DocumentManager documentManager, AttachmentUtil attachmentUtil, UrlManager urlManager, Long attachmentId, Type type) {
        key = documentManager.getKeyOfFile(attachmentId, type.equals(Type.EMBEDDED));
        title = attachmentUtil.getFileName(attachmentId);
        fileType = attachmentUtil.getFileExt(attachmentId);
        url = urlManager.getFileUri(attachmentId);
        permissions = new Permissions(attachmentUtil, attachmentId);
    }
}
