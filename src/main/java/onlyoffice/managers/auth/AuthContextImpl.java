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

package onlyoffice.managers.auth;

import com.atlassian.confluence.util.HtmlUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.security.Principal;

public class AuthContextImpl implements AuthContext {
    private final Logger log = LoggerFactory.getLogger("onlyoffice.managers.auth.AuthContext");

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
        String destination = request.getRequestURI().substring(request.getContextPath().length());
        if (destination.startsWith("/")) {
            destination = destination.substring(1);
        }

        String queryString = request.getQueryString();

        if (queryString != null && !queryString.isEmpty()) {
            destination += "?" + queryString;
        }

        return request.getContextPath() + "/login.action?permissionViolation=true&os_destination="
                + HtmlUtil.urlEncode(destination);
    }
}
