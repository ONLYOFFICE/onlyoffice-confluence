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

package onlyoffice.sdk.service;

import com.atlassian.confluence.status.service.SystemInformationService;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.confluence.user.actions.ProfilePictureInfo;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceIntegration;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.common.User;
import com.onlyoffice.model.documenteditor.config.document.Permissions;
import com.onlyoffice.model.documenteditor.config.document.ReferenceData;
import com.onlyoffice.service.documenteditor.config.DefaultConfigService;
import com.onlyoffice.manager.document.DocumentManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ConfigServiceImpl extends DefaultConfigService {

    private final Logger log = LogManager.getLogger("onlyoffice.ConfigServiceImpl");
    private final SystemInformationService sysInfoService;
    private final WebResourceIntegration webResourceIntegration;
    private final UserAccessor userAccessor;

    private AttachmentUtil attachmentUtil;

    public ConfigServiceImpl(final DocumentManager documentManager, final UrlManager urlManager,
                             final JwtManager jwtManager, final SystemInformationService sysInfoService,
                             final WebResourceIntegration webResourceIntegration, final UserAccessor userAccessor,
                             final AttachmentUtil attachmentUtil, final SettingsManager settingsManager) {
        super(documentManager, urlManager, jwtManager, settingsManager);
        this.sysInfoService = sysInfoService;
        this.webResourceIntegration = webResourceIntegration;
        this.userAccessor = userAccessor;
        this.attachmentUtil = attachmentUtil;
    }

    @Override
    public ReferenceData getReferenceData(final String fileId) {
        return ReferenceData.builder()
                .fileKey(fileId)
                .instanceId(sysInfoService.getConfluenceInfo().getBaseUrl())
                .build();
    }

    @Override
    public Permissions getPermissions(final String fileId) {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        String fileName = getDocumentManager().getDocumentName(fileId);

        Boolean editPermission = attachmentUtil.checkAccess(Long.parseLong(fileId), user, true);
        Boolean isEditable = super.getDocumentManager().isEditable(fileName);

        return Permissions.builder()
                .edit(editPermission && isEditable)
                .build();
    }

    @Override
    public User getUser() {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        ProfilePictureInfo profilePictureInfo = userAccessor.getUserProfilePicture(user);
        String userImage = null;

        if (!profilePictureInfo.isDefault()) {
            userImage = webResourceIntegration.getBaseUrl(UrlMode.ABSOLUTE) + profilePictureInfo.getUriReference()
                    .substring(webResourceIntegration.getBaseUrl(UrlMode.RELATIVE).length());
        }

        if (user != null) {
            return User.builder()
                    .id(user.getKey().getStringValue())
                    .name(user.getFullName())
                    .image(userImage)
                    .build();
        } else {
            return super.getUser();
        }
    }
}
