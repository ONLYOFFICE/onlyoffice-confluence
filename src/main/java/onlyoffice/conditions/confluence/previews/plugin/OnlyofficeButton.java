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

package onlyoffice.conditions.confluence.previews.plugin;

import com.atlassian.annotations.security.AnonymousSiteAccess;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.onlyoffice.manager.document.DocumentManager;
import onlyoffice.utils.attachment.AttachmentUtil;
import onlyoffice.utils.parsing.ParsingUtil;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

@AnonymousSiteAccess
public class OnlyofficeButton extends HttpServlet {
    private final AttachmentManager attachmentManager;
    private final ParsingUtil parsingUtil;
    private final AttachmentUtil attachmentUtil;
    private final DocumentManager documentManager;

    public OnlyofficeButton(final AttachmentManager attachmentManager, final ParsingUtil parsingUtil,
                            final AttachmentUtil attachmentUtil, final DocumentManager documentManager) {
        this.attachmentManager = attachmentManager;
        this.parsingUtil = parsingUtil;
        this.attachmentUtil = attachmentUtil;
        this.documentManager = documentManager;
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        InputStream requestStream = request.getInputStream();
        String body = parsingUtil.getBody(requestStream);

        try {
            JSONObject bodyJson = new JSONObject(body);
            String attachmentIdString = bodyJson.getString("attachmentId");
            Long attachmentId = Long.parseLong(attachmentIdString);
            Attachment attachment = attachmentManager.getAttachment(attachmentId);

            ConfluenceUser user = AuthenticatedUserThreadLocal.get();
            boolean accessEdit = attachmentUtil.checkAccess(attachment, user, true);
            boolean accessView = attachmentUtil.checkAccess(attachment, user, false);

            String fileName = attachment.getFileName();
            String access = null;

            if (accessEdit && documentManager.isEditable(fileName)) {
                access = "edit";
            } else if (accessView && documentManager.isViewable(fileName)
                    && !(accessEdit
                    && (documentManager.isEditable(fileName) || documentManager.isFillable(fileName)))) {
                access = "view";
            }

            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write("{\"access\":\"" + access + "\"}");

        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
}
