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

package onlyoffice.conditions;

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.core.filters.ServletContextThreadLocal;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import onlyoffice.utils.attachment.AttachmentUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IsOfficePageAttachments implements Condition {
    private String pageAttachments = "viewpageattachments.action";
    private final AttachmentUtil attachmentUtil;

    public IsOfficePageAttachments(final AttachmentUtil attachmentUtil) {
        this.attachmentUtil = attachmentUtil;
    }

    public void init(final Map<String, String> map) throws PluginParseException {

    }

    public boolean shouldDisplay(final Map<String, Object> context) {
        HttpServletRequest request = ServletContextThreadLocal.getRequest();

        if (request != null) {
            String uri = request.getServletPath();
            Pattern pattern = Pattern.compile(".*/" + pageAttachments + ".*");
            Matcher matcher = pattern.matcher(uri);

            String pageId = request.getParameter("pageId");
            boolean access = false;
            if (pageId != null) {
                ConfluenceUser user = AuthenticatedUserThreadLocal.get();

                access = attachmentUtil.checkAccessCreate(user, Long.parseLong(pageId));
            }
            return matcher.matches() && access;
        } else {
            return false;
        }
    }
}
