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

package onlyoffice;

import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.spring.container.ContainerManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.model.settings.Settings;
import com.onlyoffice.model.settings.SettingsConstants;
import com.onlyoffice.model.settings.security.Security;
import com.onlyoffice.model.settings.validation.ValidationResult;
import onlyoffice.sdk.manager.document.DocumentManager;
import onlyoffice.sdk.manager.url.UrlManager;
import onlyoffice.sdk.service.SettingsValidationService;
import onlyoffice.utils.parsing.ParsingUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class OnlyOfficeConfServlet extends HttpServlet {
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeConfServlet");
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final long serialVersionUID = 1L;

    private final UserManager userManager;
    private final com.onlyoffice.manager.settings.SettingsManager settingsManager;
    private final DocumentManager documentManager;
    private final ParsingUtil parsingUtil;
    private final SettingsValidationService settingsValidationService;

    public OnlyOfficeConfServlet(final UserManager userManager,
                                 final com.onlyoffice.manager.settings.SettingsManager settingsManager,
                                 final DocumentManager documentManager, final UrlManager urlManager,
                                 final ParsingUtil parsingUtil,
                                 final SettingsValidationService settingsValidationService) {
        this.userManager = userManager;
        this.settingsManager = settingsManager;
        this.documentManager = documentManager;
        this.parsingUtil = parsingUtil;
        this.settingsValidationService = settingsValidationService;
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        String username = userManager.getRemoteUsername(request);
        if (username == null || !userManager.isSystemAdmin(username)) {
            SettingsManager settingsManager = (SettingsManager) ContainerManager.getComponent("settingsManager");
            String baseUrl = settingsManager.getGlobalSettings().getBaseUrl();
            response.sendRedirect(baseUrl);
            return;
        }

        Boolean demoAvailable = settingsManager.isDemoAvailable();
        Map<String, Boolean> defaultCustomizableEditingTypes = documentManager.getLossyEditableMap();

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter writer = response.getWriter();

        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();

        contextMap.put("demoAvailable", demoAvailable);
        contextMap.put("pathApiUrl", settingsManager.getDocsIntegrationSdkProperties().getDocumentServer().getApiUrl());

        if (settingsManager.getSetting(SettingsConstants.LOSSY_EDIT) == null
                || settingsManager.getSetting(SettingsConstants.LOSSY_EDIT).isEmpty()) {
            defaultCustomizableEditingTypes.put("txt", true);
            defaultCustomizableEditingTypes.put("csv", true);
        }

        contextMap.put("defaultCustomizableEditingTypes", defaultCustomizableEditingTypes);

        try {
            Map<String, String> settings = settingsManager.getSettings();

            if (settings.get("customization.review.reviewDisplay") == null
                    || settings.get("customization.review.reviewDisplay").isEmpty()) {
                settings.put("customization.review.reviewDisplay", "ORIGINAL");
            }

            if (settings.get("customization.help") == null || settings.get("customization.help").isEmpty()) {
                settings.put("customization.help", "true");
            }

            if (settings.get("customization.chat") == null || settings.get("customization.chat").isEmpty()) {
                settings.put("customization.chat", "true");
            }

            contextMap.put("settings", settings);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        writer.write(getTemplate(contextMap));
    }

    private String getTemplate(final Map<String, Object> map) throws UnsupportedEncodingException {
        return VelocityUtils.getRenderedTemplate("templates/configure.vm", map);
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
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

        Settings settings = objectMapper.readValue(body, Settings.class);

        if (settings.getDemo() != null && settings.getDemo()) {
            settingsManager.enableDemo();
        } else {
            settingsManager.disableDemo();
        }

        if (settingsManager.isDemoActive()) {
            Security security = settings.getSecurity();
            security.setKey(null);
            security.setHeader(null);

            settings.setUrl(null);
            settings.setInnerUrl(null);
            settings.setSecurity(security);
        }

        try {
            settingsManager.setSettings(settings);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        Map<String, ValidationResult> validationResults = settingsValidationService.validateSettings();

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("validationResults", validationResults);

        response.getWriter().write(objectMapper.writeValueAsString(responseMap));
    }
}
