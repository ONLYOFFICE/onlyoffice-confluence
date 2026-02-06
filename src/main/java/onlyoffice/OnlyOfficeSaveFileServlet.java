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

package onlyoffice;

import com.atlassian.annotations.security.UnrestrictedAccess;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.spring.container.ContainerManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.Callback;
import onlyoffice.sdk.manager.security.JwtManager;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import onlyoffice.utils.attachment.AttachmentUtil;
import onlyoffice.utils.parsing.ParsingUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public class OnlyOfficeSaveFileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeSaveFileServlet");

    private final SettingsManager settingsManager;
    private final JwtManager jwtManager;
    private final AttachmentUtil attachmentUtil;
    private final ParsingUtil parsingUtil;
    private final CallbackService callbackService;

    public OnlyOfficeSaveFileServlet(final SettingsManager settingsManager, final JwtManager jwtManager,
                                     final AttachmentUtil attachmentUtil, final ParsingUtil parsingUtil,
                                     final CallbackService callbackService) {
        this.settingsManager = settingsManager;
        this.jwtManager = jwtManager;
        this.attachmentUtil = attachmentUtil;
        this.parsingUtil = parsingUtil;
        this.callbackService = callbackService;
    }

    @Override
    @UnrestrictedAccess
    public void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain; charset=utf-8");

        String token = request.getParameter("token");
        String payload;
        JSONObject bodyFromToken;

        try {
            payload = jwtManager.verifyInternalToken(token);
            bodyFromToken = new JSONObject(payload);

            if (!bodyFromToken.getString("action").equals("callback")) {
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
        AuthenticatedUserThreadLocal.set(user);

        Long attachmentId = Long.parseLong(attachmentIdString);

        if (attachmentUtil.getAttachment(attachmentId) == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String error = "";
        try {
            InputStream requestStream = request.getInputStream();

            String bodyString = parsingUtil.getBody(requestStream);

            if (bodyString.isEmpty()) {
                throw new IllegalArgumentException("requestBody is empty");
            }

            ObjectMapper mapper = new ObjectMapper();
            Callback callback = mapper.readValue(bodyString, Callback.class);

            String authorizationHeader = request.getHeader(settingsManager.getSecurityHeader());
            callback = callbackService.verifyCallback(callback, authorizationHeader);

            callbackService.processCallback(callback, attachmentIdString);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            error = e.getMessage();
        }

        PrintWriter writer = response.getWriter();
        if (error.isEmpty()) {
            writer.write("{\"error\":0}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writer.write("{\"error\":1,\"message\":\"" + error + "\"}");
        }
    }
}
