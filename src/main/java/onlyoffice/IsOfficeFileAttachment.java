package onlyoffice;

import java.util.List;
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
