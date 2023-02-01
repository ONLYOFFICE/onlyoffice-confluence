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

package onlyoffice.action;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.core.filters.ServletContextThreadLocal;
import com.atlassian.xwork.HttpMethod;
import com.atlassian.xwork.PermittedMethods;
import onlyoffice.managers.convert.ConvertManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import com.atlassian.confluence.user.ConfluenceUser;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;

public class DownloadAsAction extends ConfluenceActionSupport {

    private AttachmentUtil attachmentUtil;
    private ConvertManager convertManager;

    private String attachmentId;
    private String fileName;
    private String targetFileType;
    private static final char[] INVALID_CHARS;

    @Inject
    public DownloadAsAction(final AttachmentUtil attachmentUtil, final ConvertManager convertManager) {
        this.attachmentUtil = attachmentUtil;
        this.convertManager = convertManager;
    }

    @PermittedMethods({ HttpMethod.GET })
    public String doDefault() { return ConfluenceActionSupport.INPUT; }

    @Override
    public void validate() {
        super.validate();

        Long attachmentId = Long.parseLong(this.attachmentId);
        String ext = attachmentUtil.getFileExt(attachmentId);

        if (!attachmentUtil.checkAccess(attachmentId, getAuthenticatedUser(), false)) {
            addActionError(getText("onlyoffice.connector.dialog.conversion.message.error.permission"));
            ServletContextThreadLocal.getResponse().setStatus(403);
            return;
        }

        if (fileName == null || fileName.isEmpty()) {
            addActionError(getText("onlyoffice.connector.error.Unknown"));
            ServletContextThreadLocal.getResponse().setStatus(400);
        }

        if (StringUtils.containsAny((CharSequence)this.fileName, DownloadAsAction.INVALID_CHARS)) {
            addActionError(getText("filename.contain.invalid.character"));
            ServletContextThreadLocal.getResponse().setStatus(400);
        }

        if (targetFileType == null || targetFileType.isEmpty()) {
            addActionError(getText("onlyoffice.connector.error.Unknown"));
            ServletContextThreadLocal.getResponse().setStatus(400);
            return;
        }

        if (convertManager.getTargetExtList(ext) == null ||
                convertManager.getTargetExtList(ext).isEmpty() ||
                !convertManager.getTargetExtList(ext).contains(targetFileType)
        ) {
            addActionError(getText("onlyoffice.connector.error.Unknown"));
            ServletContextThreadLocal.getResponse().setStatus(415);
        }
    }

    @PermittedMethods({ HttpMethod.POST })
    public String execute() throws Exception {
        Long attachmentId = Long.parseLong(this.attachmentId);
        String ext = attachmentUtil.getFileExt(attachmentId);
        String targetExt = convertManager.getTargetExt(ext);

        if (!this.targetFileType.isEmpty()) {
            targetExt = this.targetFileType;
        }

        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        JSONObject convertResult = convertManager.convert(attachmentId, ext, targetExt, user, this.fileName + "." + targetExt);
        HttpServletResponse response = ServletContextThreadLocal.getResponse();
        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.write(convertResult.toString());
        response.setStatus(200);
        return "none";
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setTargetFileType(String targetFileType) {
        this.targetFileType = targetFileType;
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

    static {
        INVALID_CHARS = new char[] { '\\', '/', '\"', ':', '?', '*', '<', '|', '>' };
    }
}
