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

package onlyoffice.model.editor;

import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.json.JSONObject;

public class EditorConfig {
    Mode mode;
    String createUrl;
    String callbackUrl;
    String lang;
    JSONObject actionLink;
    Customization customization;
    User user;

    public EditorConfig (LocaleManager localeManager, AttachmentUtil attachmentUtil, UrlManager urlManager,
                         ConfigurationManager configurationManager, Long attachmentId, Mode mode, JSONObject actionLink,
                         String referer) {
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
}
