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

package onlyoffice.sdk.service;

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.onlyoffice.manager.request.RequestManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;

import com.onlyoffice.model.convertservice.ConvertRequest;
import com.onlyoffice.model.convertservice.ConvertResponse;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.service.documenteditor.callback.DefaultCallbackService;
import com.onlyoffice.service.convert.ConvertService;
import onlyoffice.sdk.manager.document.DocumentManager;
import onlyoffice.sdk.manager.url.UrlManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class CallbackServiceImpl extends DefaultCallbackService {
    private final AttachmentUtil attachmentUtil;
    private final ConvertService convertService;
    private final RequestManager requestManager;
    private final UrlManager urlManager;
    private final DocumentManager documentManager;

    public CallbackServiceImpl(final JwtManager jwtManager, final AttachmentUtil attachmentUtil,
                               final ConvertService convertService, final RequestManager requestManager,
                               final SettingsManager settingsManager, final UrlManager urlManager,
                               final DocumentManager documentManager) {
        super(jwtManager, settingsManager);
        this.attachmentUtil = attachmentUtil;
        this.convertService = convertService;
        this.requestManager = requestManager;
        this.urlManager = urlManager;
        this.documentManager = documentManager;
    }

    public void handlerSave(final Callback callback, final String fileId) throws Exception {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        if (user != null && attachmentUtil.checkAccess(Long.valueOf(fileId), user, true)) {
            String fileType = callback.getFiletype();
            String downloadUrl = callback.getUrl();

            saveAttachmentFromUrl(Long.valueOf(fileId), downloadUrl, fileType, user);
        } else {
            throw new SecurityException("Try save without access: " + user);
        }
    }

    private void saveAttachmentFromUrl(final Long attachmentId, final String downloadUrl, final String fileType,
                                       final ConfluenceUser user) throws Exception {
        String documentName = documentManager.getDocumentName(String.valueOf(attachmentId));
        String extension = documentManager.getExtension(documentName);
        String url = urlManager.replaceToInnerDocumentServerUrl(downloadUrl);

        ConvertRequest convertRequest = ConvertRequest.builder()
                .outputtype(extension)
                .url(url)
                .build();

        if (!extension.equals(fileType)) {
            ConvertResponse convertResponse = convertService.processConvert(convertRequest,
                    String.valueOf(attachmentId));
            url = convertResponse.getFileUrl();
        }

        requestManager.executeGetRequest(url, new RequestManager.Callback<Void>() {
            @Override
            public Void doWork(final Object response) throws Exception {
                byte[] bytes = IOUtils.toByteArray(((HttpEntity) response).getContent());
                InputStream inputStream = new ByteArrayInputStream(bytes);

                attachmentUtil.saveAttachmentAsNewVersion(attachmentId, inputStream, bytes.length, user);

                return null;
            }
        });
    }
}
