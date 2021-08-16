/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.google.gson.Gson;
import onlyoffice.managers.document.DocumentManager;
import onlyoffice.managers.jwt.JwtManager;
import onlyoffice.managers.url.UrlManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import onlyoffice.utils.parsing.ParsingUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnlyOfficeFileProviderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeFileProviderServlet");

    private final ParsingUtil parsingUtil;
    private final AttachmentUtil attachmentUtil;
    private final UrlManager urlManager;
    private final JwtManager jwtManager;
    private final DocumentManager documentManager;

    @Inject
    public OnlyOfficeFileProviderServlet(ParsingUtil parsingUtil, AttachmentUtil attachmentUtil, JwtManager jwtManager,
            UrlManager urlManager, DocumentManager documentManager) {
        this.parsingUtil = parsingUtil;
        this.attachmentUtil = attachmentUtil;
        this.jwtManager = jwtManager;
        this.urlManager = urlManager;
        this.documentManager = documentManager;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (jwtManager.jwtEnabled()) {
            String jwth = jwtManager.getJwtHeader();
            String header = request.getHeader(jwth);
            String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : header;

            if (token == null || token == "") {
                throw new SecurityException("Expected JWT");
            }

            if (!jwtManager.verify(token)) {
                throw new SecurityException("JWT verification failed");
            }
        }

        String vkey = request.getParameter("vkey");
        log.info("vkey = " + vkey);
        String attachmentIdString = documentManager.readHash(vkey);

        Long attachmentId = Long.parseLong(attachmentIdString);
        log.info("attachmentId " + attachmentId);

        String contentType = attachmentUtil.getMediaType(attachmentId);
        response.setContentType(contentType);

        InputStream inputStream = attachmentUtil.getAttachmentData(attachmentId);
        response.setContentLength(inputStream.available());

        byte[] buffer = new byte[10240];

        OutputStream output = response.getOutputStream();
        for (int length = 0; (length = inputStream.read(buffer)) > 0;) {
            output.write(buffer, 0, length);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        InputStream requestStream = request.getInputStream();
        String body = parsingUtil.getBody(requestStream);

        try {
            JSONObject bodyJson = new JSONObject(body);
            JSONArray attachments = bodyJson.getJSONArray("attachments");

            List<Object> responseJson = new ArrayList<>();
            Gson gson = new Gson();

            for (int i = 0; i < attachments.length(); i++) {
                Long attachmentId = attachments.getLong(i);

                if (attachmentUtil.checkAccess(attachmentId, user, false)) {
                    Map<String, String> data = new HashMap<>();

                    String fileName = attachmentUtil.getFileName(attachmentId);
                    String fileType = fileName.substring(fileName.lastIndexOf(".") + 1).trim().toLowerCase();

                    if (bodyJson.has("command")) {
                        data.put("command", bodyJson.getString("command"));
                    }
                    data.put("fileType", fileType);
                    data.put("url", urlManager.getFileUri(attachmentId));
                    if (jwtManager.jwtEnabled()) {
                        JSONObject dataJSON = new JSONObject(gson.toJson(data));
                        data.put("token", jwtManager.createToken(dataJSON));
                    }

                    responseJson.add(data);
                }
            }

            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write(gson.toJson(responseJson));
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
}