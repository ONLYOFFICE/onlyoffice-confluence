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

package onlyoffice.model.config;

import com.atlassian.confluence.languages.LocaleManager;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.model.config.document.Document;
import onlyoffice.model.config.editor.EditorConfig;
import onlyoffice.model.config.editor.Mode;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.json.JSONObject;

public class Config {
    private Type type;
    private DocumentType documentType;
    private EditorConfig editorConfig;
    private Document document;
    private String token;
    private String height = "100%";
    private String width = "100%";

    public Config(final LocaleManager localeManager, final DocumentManager documentManager,
                  final AttachmentUtil attachmentUtil, final UrlManager urlManager,
                  final ConfigurationManager configurationManager, final Long attachmentId, final Mode mode,
                  final Type type, final JSONObject actionLink, final String referer, final String instanceId) {
        this.type = type;
        this.documentType = documentManager.getDocType(attachmentUtil.getFileExt(attachmentId));
        this.document = new Document(documentManager, attachmentUtil, urlManager, attachmentId, type, instanceId);
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

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(final String height) {
        this.height = height;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(final String width) {
        this.width = width;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(final DocumentType documentType) {
        this.documentType = documentType;
    }

    public EditorConfig getEditorConfig() {
        return editorConfig;
    }

    public void setEditorConfig(final EditorConfig editorConfig) {
        this.editorConfig = editorConfig;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(final Document document) {
        this.document = document;
    }
}
