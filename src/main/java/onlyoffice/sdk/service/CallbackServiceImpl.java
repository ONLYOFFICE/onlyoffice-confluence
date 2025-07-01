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

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.pages.persistence.dao.AttachmentDao;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;

import com.onlyoffice.model.convertservice.ConvertRequest;
import com.onlyoffice.model.convertservice.ConvertResponse;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Action;
import com.onlyoffice.model.documenteditor.callback.History;
import com.onlyoffice.model.documenteditor.callback.action.Type;
import com.onlyoffice.service.documenteditor.callback.DefaultCallbackService;
import com.onlyoffice.service.convert.ConvertService;
import onlyoffice.sdk.manager.document.DocumentManager;
import onlyoffice.sdk.manager.url.UrlManager;
import onlyoffice.utils.attachment.AttachmentUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CallbackServiceImpl extends DefaultCallbackService {
    private final AttachmentUtil attachmentUtil;
    private final ConvertService convertService;
    private final DocumentServerClient documentServerClient;
    private final TransactionTemplate transactionTemplate;
    private final AttachmentManager attachmentManager;
    private final SettingsManager settingsManager;
    private final UrlManager urlManager;
    private final DocumentManager documentManager;

    public CallbackServiceImpl(final JwtManager jwtManager, final AttachmentUtil attachmentUtil,
                               final ConvertService convertService, final DocumentServerClient documentServerClient,
                               final SettingsManager settingsManager, final TransactionTemplate transactionTemplate,
                               final AttachmentManager attachmentManager, final UrlManager urlManager,
                               final DocumentManager documentManager) {
        super(jwtManager, settingsManager);
        this.settingsManager = settingsManager;
        this.attachmentUtil = attachmentUtil;
        this.convertService = convertService;
        this.documentServerClient = documentServerClient;
        this.urlManager = urlManager;
        this.transactionTemplate = transactionTemplate;
        this.attachmentManager = attachmentManager;
        this.documentManager = documentManager;
    }

    public void handlerEditing(final Callback callback, final String fileId) throws Exception {
        if (callback.getActions() != null) {
            List<Action> actions = callback.getActions();
            if (actions.size() > 0) {
                Action action = actions.get(0);
                if (action.getType().equals(Type.CONNECTED)) {
                    ConfluenceUser user = AuthenticatedUserThreadLocal.get();

                    if (user == null || !attachmentUtil.checkAccess(Long.valueOf(fileId), user, true)) {
                        throw new SecurityException("Access denied. User " + user
                                + " don't have the appropriate permissions to edit this document.");
                    }

                    if (attachmentUtil.getCollaborativeEditingKey(Long.valueOf(fileId)) == null) {
                        String key = callback.getKey();
                        attachmentUtil.setCollaborativeEditingKey(Long.valueOf(fileId), key);
                    }
                }
            }
        }
    }

    public void handlerSave(final Callback callback, final String fileId) throws Exception {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        if (user != null && attachmentUtil.checkAccess(Long.valueOf(fileId), user, true)) {
            String fileType = callback.getFiletype();
            String downloadUrl = callback.getUrl();
            History history = callback.getHistory();
            String changesUrl = callback.getChangesurl();

            Boolean forceSaveVersion =
                    attachmentUtil.getPropertyAsBoolean(Long.valueOf(fileId), "onlyoffice-force-save");

            attachmentUtil.setCollaborativeEditingKey(Long.valueOf(fileId), null);

            if (forceSaveVersion) {
                saveAttachmentFromUrl(Long.valueOf(fileId), downloadUrl, fileType, user, false);
                attachmentUtil.removeProperty(Long.valueOf(fileId), "onlyoffice-force-save");
                attachmentUtil.removeAttachmentChanges(Long.valueOf(fileId));

                File convertedFile = attachmentUtil.getConvertedFile(Long.valueOf(fileId));
                if (convertedFile.exists()) {
                    convertedFile.delete();
                }
            } else {
                saveAttachmentFromUrl(Long.valueOf(fileId), downloadUrl, fileType, user, true);
            }

            ObjectMapper mapper = new ObjectMapper();

            saveAttachmentChanges(Long.valueOf(fileId), mapper.writeValueAsString(history), changesUrl);
        } else {
            throw new SecurityException("Try save without access: " + user);
        }
    }

    public void handlerForcesave(final Callback callback, final String fileId) throws Exception {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        if (user != null && attachmentUtil.checkAccess(Long.valueOf(fileId), user, true)) {
            if (settingsManager.getSettingBoolean("customization.forcesave", false)) {
                String fileType = callback.getFiletype();
                String downloadUrl = callback.getUrl();
                History history = callback.getHistory();
                String changesUrl = callback.getChangesurl();

                Boolean forceSaveVersion =
                        attachmentUtil.getPropertyAsBoolean(Long.valueOf(fileId), "onlyoffice-force-save");

                if (forceSaveVersion) {
                    saveAttachmentFromUrl(Long.valueOf(fileId), downloadUrl, fileType, user, false);
                    attachmentUtil.removeAttachmentChanges(Long.valueOf(fileId));
                } else {
                    String key = attachmentUtil.getCollaborativeEditingKey(Long.valueOf(fileId));
                    attachmentUtil.setCollaborativeEditingKey(Long.valueOf(fileId), null);

                    saveAttachmentFromUrl(Long.valueOf(fileId), downloadUrl, fileType, user, true);
                    attachmentUtil.setCollaborativeEditingKey(Long.valueOf(fileId), key);
                    attachmentUtil.setProperty(Long.valueOf(fileId), "onlyoffice-force-save", "true");
                }

                ObjectMapper mapper = new ObjectMapper();

                saveAttachmentChanges(Long.valueOf(fileId), mapper.writeValueAsString(history),
                        changesUrl);

                File convertedFile = attachmentUtil.getConvertedFile(Long.valueOf(fileId));
                if (convertedFile.exists()) {
                    convertedFile.delete();
                }
            }
        } else {
            throw new SecurityException("Try save without access: " + user);
        }
    }

    private void saveAttachmentFromUrl(final Long attachmentId, final String downloadUrl, final String fileType,
                                       final ConfluenceUser user, final boolean newVersion) throws Exception {
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

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(null, null);

            documentServerClient.getFile(
                    url,
                    Files.newOutputStream(tempFile)
            );

            if (newVersion) {
                attachmentUtil.saveAttachmentAsNewVersion(attachmentId, tempFile.toFile(), user);
            } else {
                attachmentUtil.updateAttachment(attachmentId, tempFile.toFile(), user);
            }
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    private void saveAttachmentChanges(final Long attachmentId, final String history, final String changesUrl)
            throws Exception {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        if (history != null && !history.isEmpty() && changesUrl != null && !changesUrl.isEmpty()) {
            InputStream changesStream = new ByteArrayInputStream(history.getBytes(StandardCharsets.UTF_8));
            Attachment changes =
                    new Attachment("onlyoffice-changes.json", "application/json", changesStream.available(), "");
            changes.setContainer(attachment.getContainer());
            changes.setHidden(true);

            Path tempFile = null;
            try {
                tempFile = Files.createTempFile(null, null);

                int fileSize = documentServerClient.getFile(
                        changesUrl,
                        Files.newOutputStream(tempFile)
                );


                Attachment diff = new Attachment(
                        "onlyoffice-diff.zip",
                        "application/zip",
                        fileSize,
                        ""
                );
                diff.setContainer(attachment.getContainer());
                diff.setHidden(true);

                attachment.addAttachment(changes);
                attachment.addAttachment(diff);

                AttachmentDao attDao = attachmentManager.getAttachmentDao();
                Path finalTempFile = tempFile;
                Object result = transactionTemplate.execute(new TransactionCallback() {
                    @Override
                    public Object doInTransaction() {
                        attDao.saveNewAttachment(changes, changesStream);
                        try {
                            attDao.saveNewAttachment(diff, Files.newInputStream(finalTempFile));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        attDao.updateAttachment(attachment);
                        return null;
                    }
                });
            } finally {
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            }
        }
    }
}
