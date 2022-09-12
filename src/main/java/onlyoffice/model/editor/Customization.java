/**
 *
 * (c) Copyright Ascensio System SIA 2022
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

package onlyoffice.model.editor;

import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.url.UrlManager;

public class Customization {
    boolean forcesave;
    boolean chat;
    boolean compactHeader;
    boolean feedback;
    boolean help;
    boolean toolbarNoTabs;
    ReviewDisplay reviewDisplay;
    Goback goback;

    public Customization(UrlManager urlManager, ConfigurationManager configurationManager, Long attachmentId, String referer) {
        this.forcesave = configurationManager.forceSaveEnabled();
        this.chat = configurationManager.getBooleanPluginSetting("chat", true);
        this.compactHeader = configurationManager.getBooleanPluginSetting("compactHeader", false);
        this.feedback = configurationManager.getBooleanPluginSetting("feedback", false);
        this.help = configurationManager.getBooleanPluginSetting("helpMenu", true);
        this.toolbarNoTabs = configurationManager.getBooleanPluginSetting("toolbarNoTabs", false);
        if (!configurationManager.getStringPluginSetting("reviewDisplay", "original").equals("original")) {
            switch (configurationManager.getStringPluginSetting("reviewDisplay", "original")) {
                case "markup":
                    this.reviewDisplay = ReviewDisplay.MARKUP;
                case "final":
                    this.reviewDisplay = ReviewDisplay.FINAL;
                case "original":
                    this.reviewDisplay = ReviewDisplay.ORIGINAL;
            }
        }
        this.goback = new Goback(urlManager, attachmentId, referer);
    }
}
