package onlyoffice.action;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.actions.PageAware;
import onlyoffice.managers.convert.ConvertManager;
import onlyoffice.utils.attachment.AttachmentUtil;

import javax.inject.Inject;
import java.util.List;

public class DownloadAsPopupAction extends ConfluenceActionSupport implements PageAware {

    private AttachmentUtil attachmentUtil;
    private ConvertManager convertManager;
    private AbstractPage page;
    private String attachmentId;
    private String fileName;
    private String targetFileType;
    private Long pageId;

    @Inject
    public DownloadAsPopupAction(final AttachmentUtil attachmentUtil,
                                 final ConvertManager convertManager) {
        this.attachmentUtil = attachmentUtil;
        this.convertManager = convertManager;
    }

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setTargetFileType(String targetFileType) {
        this.targetFileType = targetFileType;
    }

    @Override
    public AbstractPage getPage() {
        return this.page;
    }

    @Override
    public void setPage(AbstractPage abstractPage) {
        this.page = abstractPage;
    }

    @Override
    public boolean isPageRequired() {
        return false;
    }

    @Override
    public boolean isLatestVersionRequired() {
        return false;
    }

    @Override
    public boolean isViewPermissionRequired() {
        return false;
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getAttachmentId() {
        return attachmentId;
    }

    public String getFileName() {
        Long attachmentId = Long.parseLong(this.attachmentId);
        String fileName = attachmentUtil.getFileName(attachmentId);

        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    public String getFileType() {
        Long attachmentId = Long.parseLong(this.attachmentId);
        return attachmentUtil.getFileExt(attachmentId);
    }

    public String getTargetFileType() {
        return convertManager.getTargetExt(getFileType());
    }

    public List<String> getTargetFileTypeList() {
        return convertManager.getTargetExtList(getFileType());
    }
}
