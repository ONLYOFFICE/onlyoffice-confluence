/**
 *
 * (c) Copyright Ascensio System SIA 2026
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
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.common.User;
import com.onlyoffice.model.documenteditor.config.document.Permissions;
import com.onlyoffice.model.documenteditor.config.document.ReferenceData;
import com.onlyoffice.model.documenteditor.config.editorconfig.Customization;
import com.onlyoffice.service.documenteditor.config.DefaultConfigService;
import com.onlyoffice.manager.document.DocumentManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ConfigServiceImpl extends DefaultConfigService {

    private final Logger log = LogManager.getLogger("onlyoffice.ConfigServiceImpl");
    private final SystemInformationService sysInfoService;

    private AttachmentUtil attachmentUtil;

    public ConfigServiceImpl(final DocumentManager documentManager, final UrlManager urlManager,
                             final JwtManager jwtManager, final SystemInformationService sysInfoService,
                             final AttachmentUtil attachmentUtil, final SettingsManager settingsManager) {
        super(documentManager, urlManager, jwtManager, settingsManager);
        this.sysInfoService = sysInfoService;
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
        Boolean isFillable = super.getDocumentManager().isFillable(fileName);

        return Permissions.builder()
                .edit(editPermission && isEditable)
                .fillForms(editPermission && isFillable)
                .build();
    }

    @Override
    public User getUser() {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        if (user != null) {
            return User.builder()
                    .id(user.getKey().getStringValue())
                    .name(user.getFullName())
                    .build();
        } else {
            return super.getUser();
        }
    }

    @Override
    public Customization getCustomization(final String fileId) {
        Customization customization = super.getCustomization(fileId);

        customization.setMacros(
                getSettingsManager().getSettingBoolean("customization.macros", true)
        );

        return customization;
    }
}
