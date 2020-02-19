package onlyoffice;

import java.util.Map;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

/*
    Copyright (c) Ascensio System SIA 2020. All rights reserved.
    http://www.onlyoffice.com
*/

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
