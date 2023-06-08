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

package onlyoffice.managers.convert;

import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.user.ConfluenceUser;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.managers.jwt.JwtManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.model.Format;
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ConvertManagerImpl implements ConvertManager {
    private final Logger log = LogManager.getLogger("onlyoffice.managers.convert.ConvertManager");

    private final LocaleManager localeManager;
    private final UrlManager urlManager;
    private final JwtManager jwtManager;
    private final ConfigurationManager configurationManager;
    private final DocumentManager documentManager;

    public ConvertManagerImpl(final UrlManager urlManager, final JwtManager jwtManager,
                              final ConfigurationManager configurationManager,
                              final DocumentManager documentManager, final LocaleManager localeManager) {
        this.urlManager = urlManager;
        this.jwtManager = jwtManager;
        this.configurationManager = configurationManager;
        this.documentManager = documentManager;
        this.localeManager = localeManager;
    }

    public JSONObject convert(final Long attachmentId, final String ext, final String convertToExt,
                              final ConfluenceUser user) throws Exception {
        String url = urlManager.getFileUri(attachmentId);
        String region = localeManager.getLocale(user).toLanguageTag();
        return convert(attachmentId, ext, convertToExt, url, region, true);
    }

    public JSONObject convert(final Long attachmentId, final String currentExt, final String convertToExt,
                              final String url, final String region, final boolean async) throws Exception {
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

    public String getTargetExt(final String ext) {
        List<Format> supportedFormats = configurationManager.getSupportedFormats();

        for (Format format : supportedFormats) {
            if (format.getName().equals(ext)) {
                switch (format.getType()) {
                    case WORD:
                        if (format.getName().equals("docxf") && format.getConvert().contains("oform")) {
                            return "oform";
                        }
                        if (format.getConvert().contains("docx")) {
                            return "docx";
                        }
                        break;
                    case CELL:
                        if (format.getConvert().contains("xlsx")) {
                            return "xlsx";
                        }
                        break;
                    case SLIDE:
                        if (format.getConvert().contains("pptx")) {
                            return "pptx";
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        return null;
    }

    public List<String> getTargetExtList(final String ext) {
        List<Format> supportedFormats = configurationManager.getSupportedFormats();

        for (Format format : supportedFormats) {
            if (format.getName().equals(ext)) {
                return format.getConvert();
            }
        }

        return null;
    }

}
