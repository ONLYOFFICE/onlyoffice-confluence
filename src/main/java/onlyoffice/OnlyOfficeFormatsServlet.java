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

import com.google.gson.Gson;
import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.model.Format;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class OnlyOfficeFormatsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeFormatsServlet");

    private final ConfigurationManager configurationManager;

    public OnlyOfficeFormatsServlet(final ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        List<Format> supportedFormats = configurationManager.getSupportedFormats();
        List<String> result = new ArrayList<>();

        for (Format format : supportedFormats) {
            if (format.getActions().contains("view")) {
                result.add(format.getName());
            }
        }

        Gson gson = new Gson();

        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.write(gson.toJson(result));
    }
}
