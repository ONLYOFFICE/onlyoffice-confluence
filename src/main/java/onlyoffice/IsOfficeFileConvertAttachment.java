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

import java.util.Map;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

public class IsOfficeFileConvertAttachment implements Condition {
    public void init(Map<String, String> params) throws PluginParseException {
    }

    public boolean shouldDisplay(Map<String, Object> context) {
        Attachment attachment = (Attachment) context.get("attachment");
        if (attachment == null) {
            return false;
        }
        String ext = attachment.getFileExtension();
        if (attachment.getFileSize() > DocumentManager.GetMaxFileSize()) {
            return false;
        }

        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        boolean accessEdit = AttachmentUtil.checkAccess(attachment, user, true);

        if (!accessEdit || !ConvertManager.isConvertable(ext)) {
            return false;
        }

        return true;
    }
}
