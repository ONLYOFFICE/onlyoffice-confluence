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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.commons.codec.binary.Hex;

public class DocumentManager {
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
}
