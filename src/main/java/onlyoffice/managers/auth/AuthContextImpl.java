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

package onlyoffice.managers.auth;

import com.atlassian.confluence.util.GeneralUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

public class AuthContextImpl implements AuthContext {
    private final Logger log = LogManager.getLogger("onlyoffice.managers.auth.AuthContext");

    public boolean checkUserAuthorization(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            log.error("User is not authenticated");
            String fullUrl = getLoginUrl(request);
            response.sendRedirect(fullUrl);

            return false;
        }
        log.info("principal name = " + principal.getName());
        return true;
    }

    public String getLoginUrl(final HttpServletRequest request) throws IOException {
        StringBuilder stringBuilder = new StringBuilder(request.getContextPath());
        String fullUrl = stringBuilder.append("/login.action?permissionViolation=true&os_destination=")
                .append("plugins%2Fservlet%2Fonlyoffice%2Fdoceditor").append("?")
                .append(GeneralUtil.urlEncode(request.getQueryString())).toString();
        return fullUrl;
    }
}
