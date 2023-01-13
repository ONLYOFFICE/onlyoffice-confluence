package onlyoffice.conditions;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import onlyoffice.constants.Formats;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.utils.attachment.AttachmentUtil;

import javax.inject.Inject;
import java.util.Map;

public class IsOfficeFileDownloadAsAttachment implements Condition {

    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;

    @Inject
    public IsOfficeFileDownloadAsAttachment(DocumentManager documentManager, AttachmentUtil attachmentUtil) {
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
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

        if (attachment.getFileSize() > documentManager.getConvertationFilesizeMax()) {
            return false;
        }

        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        boolean access = attachmentUtil.checkAccess(attachment, user, false);

        return access && Formats.getConvertFormatsByExt(ext).size() != 0;
    }
}
