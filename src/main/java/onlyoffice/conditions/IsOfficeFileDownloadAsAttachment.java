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

package onlyoffice.conditions;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import onlyoffice.managers.convert.ConvertManager;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.utils.attachment.AttachmentUtil;

import javax.inject.Inject;
import java.util.Map;

public class IsOfficeFileDownloadAsAttachment implements Condition {

    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;
    private final ConvertManager convertManager;

    @Inject
    public IsOfficeFileDownloadAsAttachment(DocumentManager documentManager, AttachmentUtil attachmentUtil,
                                            ConvertManager convertManager) {
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
        this.convertManager = convertManager;
    }

    @Override
    public void init(Map<String, String> map) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> map) {
        Attachment attachment = (Attachment) map.get("attachment");

        if (attachment == null) {
            return false;
        }

        String ext = attachment.getFileExtension();

        if (attachment.getFileSize() > documentManager.getConvertationFileSizeMax()) {
            return false;
        }

        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        boolean access = attachmentUtil.checkAccess(attachment, user, false);

        return access &&  convertManager.getTargetExtList(ext).size() != 0;
    }
}
