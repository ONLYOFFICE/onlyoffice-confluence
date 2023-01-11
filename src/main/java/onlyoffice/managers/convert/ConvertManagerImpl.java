/**
 *
 * (c) Copyright Ascensio System SIA 2022
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

package onlyoffice.managers.convert;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import onlyoffice.constants.Format;
import onlyoffice.constants.Formats;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.managers.jwt.JwtManager;
import onlyoffice.managers.url.UrlManager;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@Default
public class ConvertManagerImpl implements ConvertManager {
    private final Logger log = LogManager.getLogger("onlyoffice.managers.convert.ConvertManager");

    @ComponentImport
    private final LocaleManager localeManager;

    private final UrlManager urlManager;
    private final JwtManager jwtManager;
    private final ConfigurationManager configurationManager;
    private final DocumentManager documentManager;

    @Inject
    public ConvertManagerImpl(UrlManager urlManager, JwtManager jwtManager,
                              ConfigurationManager configurationManager,
                              DocumentManager documentManager, LocaleManager localeManager) {
        this.urlManager = urlManager;
        this.jwtManager = jwtManager;
        this.configurationManager = configurationManager;
        this.documentManager = documentManager;
        this.localeManager = localeManager;
    }

    public boolean isConvertable(String ext) {
        String convertableTypes = configurationManager.getProperty("docservice.type.convert");
        if(convertableTypes == null) return false;
        List<String> exts = Arrays.asList(convertableTypes.split("\\|"));
        return exts.contains(ext);
    }

    public String convertsTo(String ext) {
        String docType = documentManager.getDocType(ext);
        if (docType != null) {
            if (ext.equals("docx")) return "docxf";
            if (ext.equals("docxf")) return "oform";

            if (docType.equals("word")) return "docx";
            if (docType.equals("cell")) return "xlsx";
            if (docType.equals("slide")) return "pptx";
        }
        return null;
    }

    public JSONObject convert(Long attachmentId, String ext, String convertToExt, ConfluenceUser user) throws Exception {
       String url = urlManager.getFileUri(attachmentId);
       String region = localeManager.getLocale(user).toLanguageTag();
       return convert(attachmentId, ext, convertToExt, url, region, true);
    }

    public JSONObject convert(Long attachmentId, String currentExt, String convertToExt, String url, String region, boolean async) throws Exception {
        try (CloseableHttpClient httpClient = configurationManager.getHttpClient()) {
            JSONObject body = new JSONObject();
            body.put("async", async);
            body.put("embeddedfonts", true);
            body.put("filetype", currentExt);
            body.put("outputtype", convertToExt);
            body.put("key", documentManager.getKeyOfFile(attachmentId));
            body.put("url", url);
            body.put("region", region);

            StringEntity requestEntity = new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
            HttpPost request = new HttpPost(urlManager.getInnerDocEditorUrl()
                    + configurationManager.getProperties().getProperty("files.docservice.url.convert"));
            request.setEntity(requestEntity);
            request.setHeader("Accept", "application/json");

            if (jwtManager.jwtEnabled()) {
                String token = jwtManager.createToken(body);
                JSONObject payloadBody = new JSONObject();
                payloadBody.put("payload", body);
                String headerToken = jwtManager.createToken(body);
                body.put("token", token);
                String header = jwtManager.getJwtHeader();
                request.setHeader(header, "Bearer " + headerToken);
            }

            log.debug("Sending POST to Docserver: " + body.toString());

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();

                if (status != HttpStatus.SC_OK) {
                    throw new HttpException("Docserver returned code " + status);
                } else {
                    InputStream is = response.getEntity().getContent();
                    String content = IOUtils.toString(is, StandardCharsets.UTF_8);

                    log.debug("Docserver returned: " + content);
                    JSONObject callBackJson = null;
                    try {
                        callBackJson = new JSONObject(content);
                    } catch (Exception e) {
                        throw new Exception("Couldn't convert JSON from docserver: " + e.getMessage());
                    }

                    return callBackJson;
                }
            }
        }
    }

    public String getTargetExt(String ext) {
        List<Format> supportedFormats = Formats.getSupportedFormats();

        for (Format format : supportedFormats) {
            if (format.getName().equals(ext)) {
                switch(format.getType()) {
                    case FORM:
                        if (format.getConvertTo().contains("oform")) return "oform";
                        break;
                    case WORD:
                        if (format.getConvertTo().contains("docx")) return "docx";
                        break;
                    case CELL:
                        if (format.getConvertTo().contains("xlsx")) return "xlsx";
                        break;
                    case SLIDE:
                        if (format.getConvertTo().contains("pptx")) return "pptx";
                        break;
                    default:
                        break;
                }
            }
        }

        return null;
    }

    public List<String> getTargetExtList(String ext) {
        List<Format> supportedFormats = Formats.getSupportedFormats();

        for (Format format : supportedFormats) {
            if (format.getName().equals(ext)) {
                return format.getConvertTo();
            }
        }

        return null;
    }

    private String trimDot(String input) {
        return input.startsWith(".") ? input.substring(1) : input;
    }
}