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
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.core.filters.ServletContextThreadLocal;
import com.atlassian.xwork.HttpMethod;
import com.atlassian.xwork.PermittedMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.model.common.CommonResponse;
import com.onlyoffice.model.convertservice.ConvertRequest;
import com.onlyoffice.model.convertservice.ConvertResponse;
import com.onlyoffice.service.convert.ConvertService;
import onlyoffice.sdk.manager.document.DocumentManager;
import onlyoffice.sdk.manager.url.UrlManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import com.atlassian.confluence.user.ConfluenceUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class DownloadAsAction extends ConfluenceActionSupport {

    private final Logger log = LogManager.getLogger("onlyoffice.action.DownloadAsAction");

    private AttachmentUtil attachmentUtil;
    private ConvertService convertService;
    private DocumentManager documentManager;
    private final LocaleManager localeManager;
    private final UrlManager urlManager;

    private String attachmentId;
    private String fileName;
    private String targetFileType;
    private static final char[] INVALID_CHARS;

    public DownloadAsAction(final AttachmentUtil attachmentUtil, final ConvertService convertService,
                            final LocaleManager localeManager, final DocumentManager documentManager,
                            final UrlManager urlManager) {
        this.attachmentUtil = attachmentUtil;
        this.convertService = convertService;
        this.documentManager = documentManager;
        this.localeManager = localeManager;
        this.urlManager = urlManager;
    }

    @PermittedMethods({ HttpMethod.GET })
    public String doDefault() {
        return ConfluenceActionSupport.INPUT;
    }

    @Override
    public void validate() {
        super.validate();

        Long attachmentId = Long.parseLong(this.attachmentId);

        if (!attachmentUtil.checkAccess(attachmentId, getAuthenticatedUser(), false)) {
            addActionError(getText("onlyoffice.connector.dialog.conversion.message.error.permission"));
            ServletContextThreadLocal.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (fileName == null || fileName.isEmpty()) {
            addActionError(getText("onlyoffice.connector.error.Unknown"));
            ServletContextThreadLocal.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        if (StringUtils.containsAny((CharSequence) this.fileName, DownloadAsAction.INVALID_CHARS)) {
            addActionError(getText("filename.contain.invalid.character"));
            ServletContextThreadLocal.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        if (targetFileType == null || targetFileType.isEmpty()) {
            addActionError(getText("onlyoffice.connector.error.Unknown"));
            ServletContextThreadLocal.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String documentName = documentManager.getDocumentName(String.valueOf(attachmentId));

        if (documentManager.getConvertExtensionList(documentName) == null
                || documentManager.getConvertExtensionList(documentName).isEmpty()
                || !documentManager.getConvertExtensionList(documentName).contains(targetFileType)
        ) {
            addActionError(getText("onlyoffice.connector.error.Unknown"));
            ServletContextThreadLocal.getResponse().setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        }
    }

    @PermittedMethods({ HttpMethod.POST })
    public String execute() throws Exception {
        Long attachmentId = Long.parseLong(this.attachmentId);
        String fileName = documentManager.getDocumentName(String.valueOf(attachmentId));
        String targetExt = documentManager.getDefaultConvertExtension(fileName);

        if (!this.targetFileType.isEmpty()) {
            targetExt = this.targetFileType;
        }

        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        String region = localeManager.getLocale(user).toLanguageTag();

        ConvertRequest convertRequest = ConvertRequest.builder()
                .async(true)
                .outputtype(targetExt)
                .region(region)
                .build();

        HttpServletResponse response = ServletContextThreadLocal.getResponse();
        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        ObjectMapper mapper = new ObjectMapper();

        try {
            ConvertResponse convertResponse = convertService.processConvert(convertRequest, this.attachmentId);

            if (convertResponse.getEndConvert() != null && convertResponse.getEndConvert()) {
                String fileUrl = convertResponse.getFileUrl();

                String documentServerUrl = urlManager.getDocumentServerUrl();
                String innerDocumentServerUrl = urlManager.getInnerDocumentServerUrl();

                if (!documentServerUrl.equals(innerDocumentServerUrl)) {
                    return fileUrl.replace(innerDocumentServerUrl, documentServerUrl);
                }

                convertResponse.setFileUrl(fileUrl);
            }

            writer.write(mapper.writeValueAsString(convertResponse));
        } catch (IOException e) {
            log.error(e.getMessage(), e);

            CommonResponse commonResponse = new CommonResponse();
            commonResponse.setError(CommonResponse.Error.CONNECTION);
            writer.write(mapper.writeValueAsString(commonResponse));
        }

        response.setStatus(HttpServletResponse.SC_OK);
        return "none";
    }

    public void setAttachmentId(final String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public void setTargetFileType(final String targetFileType) {
        this.targetFileType = targetFileType;
    }

    public String getAttachmentId() {
        return attachmentId;
    }

    public String getFileName() {
        String fileName = documentManager.getDocumentName(this.attachmentId);

        return documentManager.getBaseName(fileName);
    }

    public String getFileType() {
        String fileName = documentManager.getDocumentName(this.attachmentId);

        return documentManager.getExtension(fileName);
    }

    public String getTargetFileType() {
        return documentManager.getDefaultConvertExtension(getFileName());
    }

    public List<String> getTargetFileTypeList() {
        Long attachmentId = Long.parseLong(this.attachmentId);
        String fileName = documentManager.getDocumentName(String.valueOf(attachmentId));

        return documentManager.getConvertExtensionList(fileName);
    }

    static {
        INVALID_CHARS = new char[] {'\\', '/', '\"', ':', '?', '*', '<', '|', '>'};
    }
}
