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

package onlyoffice.model;

import com.atlassian.confluence.languages.LocaleManager;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.model.document.Document;
import onlyoffice.model.editor.EditorConfig;
import onlyoffice.model.editor.Mode;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.json.JSONObject;

public class Config {
    Type type;
    DocumentType documentType;
    EditorConfig editorConfig;
    Document document;
    String token;
    String height = "100%";
    String width = "100%";

    public Config (LocaleManager localeManager, DocumentManager documentManager, AttachmentUtil attachmentUtil,
                   UrlManager urlManager, ConfigurationManager configurationManager, Long attachmentId, Mode mode,
                   Type type, JSONObject actionLink, String referer) {
        this.type = type;
        this.documentType = documentManager.getDocType(attachmentId);
        this.document = new Document(documentManager, attachmentUtil, urlManager, attachmentId, type);
        this.editorConfig = new EditorConfig(
                localeManager,
                attachmentUtil,
                urlManager,
                configurationManager,
                attachmentId,
                mode,
                actionLink,
                referer
        );
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setHeight(String height) { this.height = height; }

    public void setWidth(String width) { this.width = width; }

}
