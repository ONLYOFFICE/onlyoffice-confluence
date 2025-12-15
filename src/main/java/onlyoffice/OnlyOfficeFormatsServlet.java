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

import com.atlassian.annotations.security.AnonymousSiteAccess;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.model.common.Format;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import onlyoffice.sdk.manager.document.DocumentManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@AnonymousSiteAccess
public class OnlyOfficeFormatsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeFormatsServlet");

    private final DocumentManager documentManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OnlyOfficeFormatsServlet(final DocumentManager documentManager) {
        this.documentManager = documentManager;
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        List<Format> supportedFormats = documentManager.getFormats();
        List<String> result = new ArrayList<>();

        for (Format format : supportedFormats) {
            if (format.getActions().contains("view")) {
                result.add(format.getName());
            }
        }

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(result));
    }
}
