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

package onlyoffice.managers.config;

import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.gson.Gson;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.managers.jwt.JwtManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.model.Config;
import onlyoffice.model.Type;
import onlyoffice.model.editor.Mode;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@Default
public class ConfigManagerImpl implements ConfigManager {
    private final Logger log = LogManager.getLogger("onlyoffice.ConfigManagerImpl");

    @ComponentImport
    private final LocaleManager localeManager;

    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;
    private final UrlManager urlManager;
    private final ConfigurationManager configurationManager;
    private final JwtManager jwtManager;

    @Inject
    public ConfigManagerImpl(LocaleManager localeManager, DocumentManager documentManager, AttachmentUtil attachmentUtil,
                             UrlManager urlManager, ConfigurationManager configurationManager, JwtManager jwtManager) {
        this.localeManager = localeManager;
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
        this.urlManager = urlManager;
        this.configurationManager = configurationManager;
        this.jwtManager = jwtManager;

    }

    public String createConfig(Long attachmentId, Mode mode, Type type, JSONObject actionLink, String referer) throws Exception {
        return this.createConfig(attachmentId, mode, type, actionLink, referer, null, null);
    }

    public String createConfig(Long attachmentId, Mode mode, Type type, JSONObject actionLink, String referer, String width, String height) throws Exception {
        Gson gson = new Gson();

        Config config = new Config(
                localeManager,
                documentManager,
                attachmentUtil,
                urlManager,
                configurationManager,
                attachmentId,
                mode,
                type,
                actionLink,
                referer
        );

        if (width != null) {
            config.setWidth(width);
        }

        if (height != null) {
            config.setHeight(height);
        }

        if (jwtManager.jwtEnabled()) {
            config.setToken(jwtManager.createToken(gson.toJson(config)));
        }

        return gson.toJson(config);
    }
}
