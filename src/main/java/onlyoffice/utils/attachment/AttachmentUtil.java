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

    void saveAttachmentAsNewVersion(Long attachmentId, File file, ConfluenceUser user) throws IOException;

    InputStream getAttachmentData(Long attachmentId);

    String getMediaType(Long attachmentId);

    String getHashCode(Long attachmentId);

    List<Attachment> getAllVersions(Long attachmentId);

    int getVersion(Long attachmentId);

    String getAttachmentPageTitle(Long attachmentId);

    Long getAttachmentPageId(Long attachmentId);

    String getAttachmentSpaceName(Long attachmentId);

    String getAttachmentSpaceKey(Long attachmentId);

    Attachment createNewAttachment(String title, String mimeType, InputStream file, int size, Long pageId,
                                   ConfluenceUser user) throws IOException;

    Attachment createNewAttachment(String fileName, String mimeType, File file, Long pageId, ConfluenceUser user)
            throws IOException;

    ContentEntityObject getContainer(Long containerId);

    String getCorrectName(String fileName, String fileExt, Long pageID);
}
