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

package onlyoffice.managers.document;


import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.spring.container.ContainerManager;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Named
@Default
public class DocumentManagerImpl implements DocumentManager {
    private final Logger log = LogManager.getLogger("onlyoffice.managers.document.DocumentManager");
    private static final String USER_AGENT_MOBILE = "android|avantgo|playbook|blackberry|blazer|compal|elaine|fennec"
            + "|hiptop|iemobile|ip(hone|od|ad)|iris|kindle|lge |maemo|midp|mmp|opera m(ob|in)i|palm( os)?|phone"
            + "|p(ixi|re)\\/|plucker|pocket|psp|symbian|treo|up\\.(browser|link)|vodafone|wap"
            + "|windows (ce|phone)|xda|xiino";
    private static final int DEFAULT_MAX_FILE_SIZE = 5242880;
    private static final int MAX_KEY_LENGTH = 20;

    @ComponentImport
    private final I18nResolver i18n;
    private final ConfigurationManager configurationManager;
    private final AttachmentUtil attachmentUtil;

    @Inject
    public DocumentManagerImpl(final I18nResolver i18n, final ConfigurationManager configurationManager,
                               final AttachmentUtil attachmentUtil) {
        this.i18n = i18n;
        this.configurationManager = configurationManager;
        this.attachmentUtil = attachmentUtil;
    }

    public long getMaxFileSize() {
        long size;
        try {
            String filesizeMax = configurationManager.getProperty("filesize-max");
            size = Long.parseLong(filesizeMax);
        } catch (Exception ex) {
            size = 0;
        }

        return size > 0 ? size : DEFAULT_MAX_FILE_SIZE;
    }

    public String getKeyOfFile(final Long attachmentId) {
        String key = attachmentUtil.getCollaborativeEditingKey(attachmentId);
        if (key == null) {
            String hashCode = attachmentUtil.getHashCode(attachmentId);
            key = generateRevisionId(hashCode);
        }

        return key;
    }

    private String generateRevisionId(final String expectedKey) {
        String result = expectedKey;

        if (result.length() > MAX_KEY_LENGTH) {
            result = Integer.toString(result.hashCode());
        }
        String key = result.replace("[^0-9-.a-zA-Z_=]", "_");
        key = key.substring(0, Math.min(key.length(), MAX_KEY_LENGTH));
        log.info("key = " + key);
        return key;
    }

    public String createHash(final String str) {
        try {
            String secret = configurationManager.getProperty("files.docservice.secret");

            String payload = getHashHex(str + secret) + "?" + str;

            String base64 = Base64.getEncoder().encodeToString(payload.getBytes("UTF-8"));
            return base64;
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

    public String readHash(final String base64) {
        try {
            String str = new String(Base64.getDecoder().decode(base64), "UTF-8");

            String secret = configurationManager.getProperty("files.docservice.secret");

            String[] payloadParts = str.split("\\?");

            String payload = getHashHex(payloadParts[1] + secret);
            if (payload.equals(payloadParts[0])) {
                return payloadParts[1];
            }
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

    private String getHashHex(final String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(str.getBytes());
            String hex = Hex.encodeHexString(digest);

            return hex;
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

    public String getCorrectName(final String fileName, final String fileExt, final Long pageID) {
        ContentEntityManager contentEntityManager =
                (ContentEntityManager) ContainerManager.getComponent("contentEntityManager");
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        ContentEntityObject contentEntityObject = contentEntityManager.getById(pageID);

        List<Attachment> attachments = attachmentManager.getLatestVersionsOfAttachments(contentEntityObject);
        String name = (fileName + "." + fileExt).replaceAll("[*?:\"<>/|\\\\]", "_");
        int count = 0;
        Boolean flag = true;

        while (flag) {
            flag = false;
            for (Attachment attachment : attachments) {
                if (attachment.getFileName().equals(name)) {
                    count++;
                    name = fileName + " (" + count + ")." + fileExt;
                    flag = true;
                    break;
                }
            }
        }

        return name;
    }

    private InputStream getDemoFile(final ConfluenceUser user, final String fileExt) {
        LocaleManager localeManager = (LocaleManager) ContainerManager.getComponent("localeManager");
        PluginAccessor pluginAccessor = (PluginAccessor) ContainerManager.getComponent("pluginAccessor");

        String pathToDemoFile = "app_data/" + localeManager.getLocale(user).toString().replace("_", "-");

        if (pluginAccessor.getDynamicResourceAsStream(pathToDemoFile) == null) {
            pathToDemoFile = "app_data/en-US";
        }

        return pluginAccessor.getDynamicResourceAsStream(pathToDemoFile + "/new." + fileExt);
    }

    public Long createDemo(final String fileName, final String fileExt, final Long pageId, final ConfluenceUser user)
            throws
            IOException {
        String extension =
                fileExt == null || !fileExt.equals("xlsx") && !fileExt.equals("pptx") && !fileExt.equals("docxf")
                        ? "docx" : fileExt.trim();
        String name = fileName == null || fileName.equals("")
                ? i18n.getText("onlyoffice.editor.dialog.filecreate." + fileExt) : fileName;

        InputStream demoFile = getDemoFile(user, extension);

        name = getCorrectName(name, extension, pageId);
        String mimeType = getMimeType(name);

        Attachment attachment =
                attachmentUtil.createNewAttachment(name, mimeType, demoFile, demoFile.available(), pageId, user);

        return attachment.getContentId().asLong();
    }

    public String getDocType(final String ext) {
        List<String> wordFormats = Arrays.asList(configurationManager.getProperty("docservice.type.word").split("\\|"));
        List<String> cellFormats = Arrays.asList(configurationManager.getProperty("docservice.type.cell").split("\\|"));
        List<String> slideFormats =
                Arrays.asList(configurationManager.getProperty("docservice.type.slide").split("\\|"));

        if (wordFormats.contains(ext)) {
            return "word";
        }
        if (cellFormats.contains(ext)) {
            return "cell";
        }
        if (slideFormats.contains(ext)) {
            return "slide";
        }

        return null;
    }

    public String getMimeType(final String name) {
        Path path = new File(name).toPath();
        String mimeType = null;
        try {
            mimeType = Files.probeContentType(path);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    public String getEditorType(final String userAgent) {
        Pattern pattern = Pattern.compile(USER_AGENT_MOBILE, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        if (userAgent != null && pattern.matcher(userAgent).find()) {
            return "mobile";
        } else {
            return "desktop";
        }
    }

    public boolean isEditable(final String fileExtension) {
        List<String> editingTypes = configurationManager.getDefaultEditingTypes();

        Map<String, Boolean> customizableEditingTypes = configurationManager.getCustomizableEditingTypes();

        for (Map.Entry<String, Boolean> customizableEditingType : customizableEditingTypes.entrySet()) {
            if (customizableEditingType.getValue()) {
                editingTypes.add(customizableEditingType.getKey());
            }
        }

        return editingTypes.contains(fileExtension);
    }

    public boolean isFillForm(final String fileExtension) {
        List<String> fillFormTypes = configurationManager.getFillFormTypes();
        return configurationManager.getFillFormTypes().contains(fileExtension);
    }

    public boolean isViewable(final String fileExtension) {
        String docType = getDocType(fileExtension);
        return docType != null;
    }

    public List<String> getInsertImageTypes() {
        return Arrays.asList("bmp", "gif", "jpeg", "jpg", "png");
    }

    public List<String> getCompareFileTypes() {
        return Arrays.asList(configurationManager.getProperty("docservice.type.word").split("\\|"));
    }

    public List<String> getMailMergeTypes() {
        return Arrays.asList(configurationManager.getProperty("docservice.type.cell").split("\\|"));
    }
}
