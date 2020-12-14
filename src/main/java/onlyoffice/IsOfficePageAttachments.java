/**
 *
 * (c) Copyright Ascensio System SIA 2020
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

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.opensymphony.webwork.ServletActionContext;
import java.util.Map;

public class IsOfficePageAttachments implements Condition {
    private String pageAttachments = "viewpageattachments.action";

    @Override
    public void init(Map<String, String> map) throws PluginParseException {}

    @Override
    public boolean shouldDisplay(Map<String, Object> map) {
        String uri = ServletActionContext.getRequest().getServletPath();
        if (uri.split("/")[uri.split("/").length-1].equals(pageAttachments)){
            return true;
        }
        return false;
    }
}
