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

    @Inject
    public OnlyOfficeFileProviderServlet(ParsingUtil parsingUtil, AttachmentUtil attachmentUtil, JwtManager jwtManager,
            UrlManager urlManager) {
        this.parsingUtil = parsingUtil;
        this.attachmentUtil = attachmentUtil;
        this.jwtManager = jwtManager;
        this.urlManager = urlManager;
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
            String command = bodyJson.getString("command");
            JSONArray attachments = bodyJson.getJSONArray("attachments");

            List<Object> responseJson = new ArrayList<>();
            Gson gson = new Gson();

            for (int i = 0; i < attachments.length(); i++) {
                Long attachmentId = attachments.getLong(i);

                if (attachmentUtil.checkAccess(attachmentId, user, false)) {
                    Map<String, String> data = new HashMap<>();

                    String fileName = attachmentUtil.getFileName(attachmentId);
                    String fileType = fileName.substring(fileName.lastIndexOf(".") + 1).trim().toLowerCase();

                    data.put("command", command);
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
