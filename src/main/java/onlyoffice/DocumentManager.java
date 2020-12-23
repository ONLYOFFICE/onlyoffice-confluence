/**
 *
 * (c) Copyright Ascensio System SIA 2020
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

package onlyoffice;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.spring.container.ContainerManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.commons.codec.binary.Hex;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DocumentManager {
    @ComponentImport
    private static I18nResolver i18n;

    @Inject
    public DocumentManager(I18nResolver i18n) {
        this.i18n = i18n;
    }

    private static final Logger log = LogManager.getLogger("onlyoffice.DocumentManager");

    public static long GetMaxFileSize() {
        long size;
        try {
            ConfigurationManager configurationManager = new ConfigurationManager();
            Properties properties = configurationManager.GetProperties();
            String filesizeMax = properties.getProperty("filesize-max");
            size = Long.parseLong(filesizeMax);
        } catch (Exception ex) {
            size = 0;
        }

        return size > 0 ? size : 5 * 1024 * 1024;
    }

    public static List<String> GetEditedExts() {
        try {
            ConfigurationManager configurationManager = new ConfigurationManager();
            Properties properties = configurationManager.GetProperties();
            String exts = properties.getProperty("files.docservice.edited-docs");

            return Arrays.asList(exts.split("\\|"));
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.error(e.toString() + "\n" + sw.toString());
            return new ArrayList<String>();
        }
    }

    public static String getKeyOfFile(Long attachmentId) {
        String hashCode = AttachmentUtil.getHashCode(attachmentId);

        return GenerateRevisionId(hashCode);
    }

    private static String GenerateRevisionId(String expectedKey) {
        if (expectedKey.length() > 20) {
            expectedKey = Integer.toString(expectedKey.hashCode());
        }
        String key = expectedKey.replace("[^0-9-.a-zA-Z_=]", "_");
        key = key.substring(0, Math.min(key.length(), 20));
        log.info("key = " + key);
        return key;
    }

    public static String CreateHash(String str) {
        try {
            ConfigurationManager configurationManager = new ConfigurationManager();
            Properties properties = configurationManager.GetProperties();
            String secret = properties.getProperty("files.docservice.secret");

            String payload = GetHashHex(str + secret) + "?" + str;

            String base64 = Base64.getEncoder().encodeToString(payload.getBytes("UTF-8"));
            return base64;
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

    public static String ReadHash(String base64) {
        try {
            String str = new String(Base64.getDecoder().decode(base64), "UTF-8");

            ConfigurationManager configurationManager = new ConfigurationManager();
            Properties properties = configurationManager.GetProperties();
            String secret = properties.getProperty("files.docservice.secret");

            String[] payloadParts = str.split("\\?");

            String payload = GetHashHex(payloadParts[1] + secret);
            if (payload.equals(payloadParts[0])) {
                return payloadParts[1];
            }
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

    private static String GetHashHex(String str) {
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

    private static String GetCorrectName(String fileName, String fileExt, Long pageID) {
        ContentEntityManager contentEntityManager = (ContentEntityManager) ContainerManager.getComponent("contentEntityManager");
        AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");
        ContentEntityObject contentEntityObject = contentEntityManager.getById(pageID);

        List<Attachment> Attachments  =  attachmentManager.getLatestVersionsOfAttachments(contentEntityObject);
        String name = (fileName + "." + fileExt).replaceAll("[*?:\"<>/|\\\\]","_");
        int count = 0;
        Boolean flag = true;

        while(flag) {
            flag = false;
            for (Attachment attachment : Attachments) {
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

    private static InputStream GetDemoFile(ConfluenceUser user, String fileExt) {
        LocaleManager localeManager = (LocaleManager) ContainerManager.getComponent("localeManager");
        PluginAccessor pluginAccessor = (PluginAccessor) ContainerManager.getComponent("pluginAccessor");

        String pathToDemoFile = "app_data/" + localeManager.getLocale(user).toString().replace("_", "-");

        if (pluginAccessor.getDynamicResourceAsStream(pathToDemoFile) == null) {
            pathToDemoFile = "app_data/en-US";
        }

        return pluginAccessor.getDynamicResourceAsStream(pathToDemoFile + "/new." + fileExt);
    }

    public static String createDemo(String fileName, String fileExt, String pageId) {
        Attachment attachment = null;
        try {
            ConfluenceUser confluenceUser = AuthenticatedUserThreadLocal.get();
            PageManager pageManager = (PageManager) ContainerManager.getComponent("pageManager");
            AttachmentManager attachmentManager = (AttachmentManager) ContainerManager.getComponent("attachmentManager");

            Long pageID = Long.parseLong(pageId.trim());
            fileExt = fileExt == null || !fileExt.equals("xlsx") && !fileExt.equals("pptx") ? "docx" : fileExt.trim();
            fileName = fileName == null ? i18n.getText("onlyoffice.connector.dialog-filecreate." + fileExt) : fileName;

            Date date = Calendar.getInstance().getTime();

            InputStream demoFile = GetDemoFile(confluenceUser, fileExt);

            fileName = GetCorrectName(fileName, fileExt, pageID);

            Page page = pageManager.getPage(pageID);
            attachment = new Attachment(fileName, ConvertManager.getMimeType(fileExt),  demoFile.available(), "");
                attachment.setCreator(confluenceUser);
                attachment.setCreationDate(date);
                attachment.setLastModificationDate(date);
                attachment.setContainer(pageManager.getPage(pageID));

            attachmentManager.saveAttachment(attachment, null, demoFile);
            page.addAttachment(attachment);
        } catch (Exception ex) {
            log.error(ex);
        }

        return String.valueOf(attachment.getContentId().asLong());
    }
}
