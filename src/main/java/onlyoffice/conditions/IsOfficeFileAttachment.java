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

package onlyoffice.conditions;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.onlyoffice.manager.document.DocumentManager;
import onlyoffice.utils.attachment.AttachmentUtil;

import java.util.Map;

public class IsOfficeFileAttachment implements Condition {
    private boolean forEdit;
    private DocumentManager documentManager;
    private AttachmentUtil attachmentUtil;

    public IsOfficeFileAttachment(final DocumentManager documentManager, final AttachmentUtil attachmentUtil) {
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
    }

    public void init(final Map<String, String> params) throws PluginParseException {
        forEdit = false;
        if (params != null && !params.isEmpty() && params.get("forEdit") != null) {
            forEdit = !params.get("forEdit").isEmpty();
        }
    }

    public boolean shouldDisplay(final Map<String, Object> context) {
        Attachment attachment = (Attachment) context.get("attachment");
        if (attachment == null) {
            return false;
        }
        if (attachment.getFileSize() > documentManager.getMaxFileSize()) {
            return false;
        }

        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        boolean accessEdit = attachmentUtil.checkAccess(attachment, user, true);
        boolean accessView = attachmentUtil.checkAccess(attachment, user, false);
        String fileName = attachment.getFileName();

        if (forEdit) {
            if (accessEdit && documentManager.isEditable(fileName)) {
                return true;
            }
        } else {
            if (accessView
                    && documentManager.isViewable(fileName) && !(accessEdit && documentManager.isEditable(fileName))
            ) {
                return true;
            }
        }

        return false;
    }
}
