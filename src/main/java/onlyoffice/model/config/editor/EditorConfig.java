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

package onlyoffice.model.config.editor;

import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.json.JSONObject;

public class EditorConfig {
    private Mode mode;
    private String createUrl;
    private String callbackUrl;
    private String lang;
    private JSONObject actionLink;
    private Customization customization;
    private User user;

    public EditorConfig(final LocaleManager localeManager, final AttachmentUtil attachmentUtil,
                         final UrlManager urlManager, final ConfigurationManager configurationManager,
                         final Long attachmentId, final Mode mode, final JSONObject actionLink, final String referer) {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        Long pageId = attachmentUtil.getAttachmentPageId(attachmentId);
        String fileExt = attachmentUtil.getFileExt(attachmentId);

        this.mode = mode;
        this.actionLink = actionLink;
        this.lang = localeManager.getLocale(user).toLanguageTag();
        this.customization = new Customization(urlManager, configurationManager, attachmentId, referer);

        if (user != null) {
            this.user = new User(user);
        }

        if (attachmentUtil.checkAccessCreate(user, pageId)) {
            this.createUrl = urlManager.getCreateUri(pageId, fileExt);
        }

        if (attachmentUtil.checkAccess(attachmentId, user, true)) {
            this.callbackUrl = urlManager.getCallbackUrl(attachmentId);
        }
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(final Mode mode) {
        this.mode = mode;
    }

    public String getCreateUrl() {
        return createUrl;
    }

    public void setCreateUrl(final String createUrl) {
        this.createUrl = createUrl;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(final String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(final String lang) {
        this.lang = lang;
    }

    public JSONObject getActionLink() {
        return actionLink;
    }

    public void setActionLink(final JSONObject actionLink) {
        this.actionLink = actionLink;
    }

    public Customization getCustomization() {
        return customization;
    }

    public void setCustomization(final Customization customization) {
        this.customization = customization;
    }

    public User getUser() {
        return user;
    }

    public void setUser(final User user) {
        this.user = user;
    }
}
