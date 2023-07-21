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

package onlyoffice;

import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.spring.container.ContainerManager;
import onlyoffice.managers.jwt.JwtManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OnlyOfficeFileProviderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int BUFFER_SIZE = 10240;

    private final AttachmentUtil attachmentUtil;
    private final JwtManager jwtManager;

    public OnlyOfficeFileProviderServlet(final AttachmentUtil attachmentUtil, final JwtManager jwtManager) {
        this.attachmentUtil = attachmentUtil;
        this.jwtManager = jwtManager;
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        if (jwtManager.jwtEnabled()) {
            String jwth = jwtManager.getJwtHeader();
            String header = request.getHeader(jwth);
            String authorizationPrefix = "Bearer ";
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

        String token = request.getParameter("token");
        String payload;
        JSONObject bodyFromToken;

        try {
            payload = jwtManager.verifyInternalToken(token);
            bodyFromToken = new JSONObject(payload);

            if (!bodyFromToken.getString("action").equals("download")) {
                throw new SecurityException();
            }
        } catch (Exception e) {
            throw new SecurityException("Invalid link token!");
        }

        String userKeyString = bodyFromToken.getString("userKey");
        String attachmentIdString = bodyFromToken.getString("attachmentId");

        UserAccessor userAccessor = (UserAccessor) ContainerManager.getComponent("userAccessor");

        UserKey userKey = new UserKey(userKeyString);
        ConfluenceUser user = userAccessor.getUserByKey(userKey);
        Long attachmentId = Long.parseLong(attachmentIdString);

        if (attachmentUtil.getAttachment(attachmentId) == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!attachmentUtil.checkAccess(attachmentId, user, false)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String contentType = attachmentUtil.getMediaType(attachmentId);
        response.setContentType(contentType);

        InputStream inputStream = attachmentUtil.getAttachmentData(attachmentId);
        response.setContentLength(inputStream.available());

        byte[] buffer = new byte[BUFFER_SIZE];

        OutputStream output = response.getOutputStream();
        for (int length = 0; (length = inputStream.read(buffer)) > 0;) {
            output.write(buffer, 0, length);
        }
    }
}
