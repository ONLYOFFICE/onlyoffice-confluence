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

package onlyoffice.conditions;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import onlyoffice.sdk.manager.document.DocumentManager;
import onlyoffice.utils.attachment.AttachmentUtil;

import java.util.Map;

public class IsOfficeFileConvertAttachment implements Condition {

    private boolean form;
    private final AttachmentUtil attachmentUtil;
    private final DocumentManager documentManager;

    public IsOfficeFileConvertAttachment(final AttachmentUtil attachmentUtil, final DocumentManager documentManager) {
        this.attachmentUtil = attachmentUtil;
        this.documentManager = documentManager;
    }

    public void init(final Map<String, String> params) throws PluginParseException {
        form = false;

        if (params != null && !params.isEmpty() && params.get("form") != null) {
            form = !params.get("form").isEmpty();
        }
    }

    public boolean shouldDisplay(final Map<String, Object> context) {
        Attachment attachment = (Attachment) context.get("attachment");
        if (attachment == null) {
            return false;
        }
        String ext = attachment.getFileExtension();
        if (attachment.getFileSize() > documentManager.getMaxFileSize()) {
            return false;
        }

        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        boolean accessEdit = attachmentUtil.checkAccess(attachment, user, true);

        if (!accessEdit || documentManager.getDefaultConvertExtension(attachment.getFileName()) == null) {
            return false;
        }

        if ((form && !ext.equals("docxf")) || (!form && ext.equals("docxf"))) {
            return false;
        }

        return true;
    }
}
