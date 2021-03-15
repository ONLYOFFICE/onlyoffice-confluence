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

package onlyoffice;

import java.util.List;
import java.util.Map;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

public class IsOfficeFileAttachment implements Condition {
    private boolean forEdit;

    public void init(Map<String, String> params) throws PluginParseException {
        forEdit = false;
        if (params != null && !params.isEmpty() && params.get("forEdit") != null) {
            forEdit = !params.get("forEdit").isEmpty();
        }
    }

    public boolean shouldDisplay(Map<String, Object> context) {
        Attachment attachment = (Attachment) context.get("attachment");
        if (attachment == null) {
            return false;
        }
        if (!isXExtension(attachment.getFileExtension())) {
            return false;
        }
        if (attachment.getFileSize() > DocumentManager.GetMaxFileSize()) {
            return false;
        }

        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        boolean accessEdit = AttachmentUtil.checkAccess(attachment, user, true);
        boolean accessView = AttachmentUtil.checkAccess(attachment, user, false);
        if (!forEdit && (!accessView || accessEdit)) {
            return false;
        }
        if (forEdit && !accessEdit) {
            return false;
        }

        return true;
    }

    private boolean isXExtension(String fileExtension) {
        List<String> exts = DocumentManager.GetEditedExts();
        return exts.contains(fileExtension);
    }
}
