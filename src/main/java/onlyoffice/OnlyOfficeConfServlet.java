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

package onlyoffice;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.jwt.JwtManager;
import onlyoffice.utils.parsing.ParsingUtil;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.spring.container.ContainerManager;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.inject.Inject;

public class OnlyOfficeConfServlet extends HttpServlet {
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeConfServlet");
    private final long serialVersionUID = 1L;

    @ComponentImport
    private final UserManager userManager;
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private final JwtManager jwtManager;
    private final ConfigurationManager configurationManager;

    private final ParsingUtil parsingUtil;


    @Inject
    public OnlyOfficeConfServlet(UserManager userManager, PluginSettingsFactory pluginSettingsFactory,
                                 JwtManager jwtManager, ConfigurationManager configurationManager,
                                 ParsingUtil parsingUtil) {
        this.userManager = userManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.jwtManager = jwtManager;
        this.configurationManager = configurationManager;
        this.parsingUtil = parsingUtil;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = userManager.getRemoteUsername(request);
        if (username == null || !userManager.isSystemAdmin(username)) {
            SettingsManager settingsManager = (SettingsManager) ContainerManager.getComponent("settingsManager");
            String baseUrl = settingsManager.getGlobalSettings().getBaseUrl();
            response.sendRedirect(baseUrl);
            return;
        }

        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        String apiUrl = (String) pluginSettings.get("onlyoffice.apiUrl");
        String jwtSecret = (String) pluginSettings.get("onlyoffice.jwtSecret");
        String docInnerUrl = (String) pluginSettings.get("onlyoffice.docInnerUrl");
		String confUrl = (String) pluginSettings.get("onlyoffice.confUrl");
        Boolean demo = configurationManager.demoEnabled();
        Boolean demoAvailable = configurationManager.demoAvailable(true);

        if (apiUrl == null || apiUrl.isEmpty()) { apiUrl = ""; }
        if (jwtSecret == null || jwtSecret.isEmpty()) { jwtSecret = ""; }
		if (docInnerUrl == null || docInnerUrl.isEmpty()) { docInnerUrl = ""; }
		if (confUrl == null || confUrl.isEmpty()) { confUrl = ""; }

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter writer = response.getWriter();

        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();

        contextMap.put("docserviceApiUrl", apiUrl);
        contextMap.put("docserviceInnerUrl", docInnerUrl);
		contextMap.put("docserviceConfUrl", confUrl);
        contextMap.put("docserviceJwtSecret", jwtSecret);
        contextMap.put("docserviceDemo", demo);
        contextMap.put("docserviceDemoAvailable", demoAvailable);

        writer.write(getTemplate(contextMap));
    }

    private String getTemplate(Map<String, Object> map) throws UnsupportedEncodingException {
        return VelocityUtils.getRenderedTemplate("templates/configure.vm", map);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = userManager.getRemoteUsername(request);
        if (username == null || !userManager.isSystemAdmin(username)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String body = parsingUtil.getBody(request.getInputStream());
        if (body.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String apiUrl;
        String docInnerUrl;
		String confUrl;
        String jwtSecret;
        Boolean demo;
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        try {
            JSONObject jsonObj = new JSONObject(body);

            demo = jsonObj.getBoolean("demo");
            configurationManager.selectDemo(demo);

            if (configurationManager.demoActive()) {
                apiUrl = configurationManager.getDemo("url");
                docInnerUrl = configurationManager.getDemo("url");
            }else {
                apiUrl = AppendSlash(jsonObj.getString("apiUrl"));
                jwtSecret = jsonObj.getString("jwtSecret");
                docInnerUrl = AppendSlash(jsonObj.getString("docInnerUrl"));
                pluginSettings.put("onlyoffice.apiUrl", apiUrl);
                pluginSettings.put("onlyoffice.jwtSecret", jwtSecret);
                pluginSettings.put("onlyoffice.docInnerUrl", docInnerUrl);
            }

            confUrl = AppendSlash(jsonObj.getString("confUrl"));
            pluginSettings.put("onlyoffice.confUrl", confUrl);

        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\": false, \"message\": \"jsonparse\"}");
            return;
        }

        log.debug("Checking docserv url");
        if (!CheckDocServUrl(apiUrl)) {
            response.getWriter().write("{\"success\": false, \"message\": \"docservunreachable\"}");
            return;
        }

        try {
            log.debug("Checking docserv commandservice");
            if (!CheckDocServCommandService((docInnerUrl == null || docInnerUrl.isEmpty()) ? apiUrl : docInnerUrl)) {
                response.getWriter().write("{\"success\": false, \"message\": \"docservcommand\"}");
                return;
            }
        } catch (SecurityException ex) {
            response.getWriter().write("{\"success\": false, \"message\": \"jwterror\"}");
            return;
        }

        response.getWriter().write("{\"success\": true}");
    }

    private String AppendSlash(String str) {
        if (str == null || str.isEmpty() || str.endsWith("/"))
            return str;
        return str + "/";
    }

    private Boolean CheckDocServUrl(String url) {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet request = new HttpGet(url + "healthcheck");
            CloseableHttpResponse response = httpClient.execute(request);

            String content = IOUtils.toString(response.getEntity().getContent(), "utf-8").trim();
            if (content.equalsIgnoreCase("true"))
                return true;
        } catch (Exception e) {
            log.debug("/healthcheck error: " + e.getMessage());
        }

        return false;
    }

    private Boolean CheckDocServCommandService(String url) throws SecurityException {
        Integer errorCode = -1;
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            JSONObject body = new JSONObject();
            body.put("c", "version");

            HttpPost request = new HttpPost(url + "coauthoring/CommandService.ashx");

            if (jwtManager.jwtEnabled()) {
                String token = jwtManager.createToken(body);
                JSONObject payloadBody = new JSONObject();
                payloadBody.put("payload", body);
                String headerToken = jwtManager.createToken(body);
                body.put("token", token);
                String header = jwtManager.getJwtHeader();
                request.setHeader(header, "Bearer " + headerToken);
            }

            StringEntity requestEntity = new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
            request.setEntity(requestEntity);
            request.setHeader("Accept", "application/json");

            log.debug("Sending POST to Docserver: " + body.toString());
            CloseableHttpResponse response = httpClient.execute(request);
            int status = response.getStatusLine().getStatusCode();

            if (status != HttpStatus.SC_OK) {
                return false;
            } else {
                String content = IOUtils.toString(response.getEntity().getContent(), "utf-8");
                log.debug("/CommandService content: " + content);
                JSONObject callBackJson = null;
                callBackJson = new JSONObject(content);

                if (callBackJson.isNull("error")) {
                    return false;
                }

                errorCode = callBackJson.getInt("error");
            }
        } catch (Exception e) {
            log.debug("/CommandService error: " + e.getMessage());
            return false;
        }

        if (errorCode == 6) {
            throw new SecurityException();
        } else if (errorCode != 0) {
            return false;
        } else {
            return true;
        }
    }
}
