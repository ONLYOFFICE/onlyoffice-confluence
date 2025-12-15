/**
 *
 * (c) Copyright Ascensio System SIA 2024
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

package onlyoffice;

import com.atlassian.annotations.security.UnrestrictedAccess;
import com.onlyoffice.manager.settings.SettingsManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import onlyoffice.sdk.manager.security.JwtManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class OnlyOfficeTestServlet extends HttpServlet {
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeTestServlet");
    private static final long serialVersionUID = 1L;

    private final JwtManager jwtManager;
    private final SettingsManager settingsManager;

    public OnlyOfficeTestServlet(final JwtManager jwtManager, final SettingsManager settingsManager) {
        this.jwtManager = jwtManager;
        this.settingsManager = settingsManager;
    }

    @Override
    @UnrestrictedAccess
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        if (settingsManager.isSecurityEnabled()) {
            String jwth = settingsManager.getSecurityHeader();
            String header = request.getHeader(jwth);
            String authorizationPrefix = settingsManager.getSecurityPrefix();
            String token = (header != null && header.startsWith(authorizationPrefix))
                    ? header.substring(authorizationPrefix.length()) : header;

            if (token == null || token == "") {
                throw new SecurityException("Expected JWT");
            }

            try {
                String payload = jwtManager.verify(token);
            } catch (Exception e) {
                throw new SecurityException("JWT verification failed!");
            }
        }

        String message = "Test file for conversion";

        response.setContentType("text/plain");
        response.setContentLength(message.getBytes("UTF-8").length);
        response.setHeader("Content-Disposition", "attachment; filename=test.txt");

        response.getWriter().write(message);
    }
}
