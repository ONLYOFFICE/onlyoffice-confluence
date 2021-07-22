/**
 *
 * (c) Copyright Ascensio System SIA 2021
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.managers.jwt.JwtManager;
import onlyoffice.managers.url.UrlManager;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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

    private final UrlManager urlManager;
    private final JwtManager jwtManager;
    private final ConfigurationManager configurationManager;
    private final DocumentManager documentManager;

    @Inject
    public ConvertManagerImpl(UrlManager urlManager, JwtManager jwtManager,
                              ConfigurationManager configurationManager,
                              DocumentManager documentManager) {
        this.urlManager = urlManager;
        this.jwtManager = jwtManager;
        this.configurationManager = configurationManager;
        this.documentManager = documentManager;
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
            if (docType.equals("text")) return "docx";
            if (docType.equals("spreadsheet")) return "xlsx";
            if (docType.equals("presentation")) return "pptx";
        }
        return null;
    }

    public JSONObject convert(Long attachmentId, String ext) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        JSONObject body = new JSONObject();
        body.put("async", true);
        body.put("embeddedfonts", true);
        body.put("filetype", ext);
        body.put("outputtype", convertsTo(ext));
        body.put("key", documentManager.getKeyOfFile(attachmentId));
        body.put("url", urlManager.getFileUri(attachmentId));

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
        CloseableHttpResponse response = httpClient.execute(request);
        int status = response.getStatusLine().getStatusCode();

        if (status != HttpStatus.SC_OK) {
            throw new HttpException("Docserver returned code " + status);
        } else {
            InputStream is = response.getEntity().getContent();
            String content = "";

            byte[] buffer = new byte[10240];
            for (int length = 0; (length = is.read(buffer)) > 0;) {
                content += new String(buffer, 0, length);
            }

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

    private String trimDot(String input) {
        return input.startsWith(".") ? input.substring(1) : input;
    }
}