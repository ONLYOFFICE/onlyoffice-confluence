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
package onlyoffice.model.config.editor;

import onlyoffice.managers.configuration.ConfigurationManager;
import onlyoffice.managers.url.UrlManager;

public class Customization {
    private boolean forcesave;
    private boolean chat;
    private boolean compactHeader;
    private boolean feedback;
    private boolean help;
    private boolean toolbarNoTabs;
    private ReviewDisplay reviewDisplay;
    private Goback goback;

    public Customization(final UrlManager urlManager, final ConfigurationManager configurationManager,
                         final Long attachmentId, final String referer) {
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
                    break;
                case "final":
                    this.reviewDisplay = ReviewDisplay.FINAL;
                    break;
                case "original":
                default:
                    this.reviewDisplay = ReviewDisplay.ORIGINAL;
            }
        }
        this.goback = new Goback(urlManager, attachmentId, referer);
    }

    public boolean isForcesave() {
        return forcesave;
    }

    public void setForcesave(final boolean forcesave) {
        this.forcesave = forcesave;
    }

    public boolean isChat() {
        return chat;
    }

    public void setChat(final boolean chat) {
        this.chat = chat;
    }

    public boolean isCompactHeader() {
        return compactHeader;
    }

    public void setCompactHeader(final boolean compactHeader) {
        this.compactHeader = compactHeader;
    }

    public boolean isFeedback() {
        return feedback;
    }

    public void setFeedback(final boolean feedback) {
        this.feedback = feedback;
    }

    public boolean isHelp() {
        return help;
    }

    public void setHelp(final boolean help) {
        this.help = help;
    }

    public boolean isToolbarNoTabs() {
        return toolbarNoTabs;
    }

    public void setToolbarNoTabs(final boolean toolbarNoTabs) {
        this.toolbarNoTabs = toolbarNoTabs;
    }

    public ReviewDisplay getReviewDisplay() {
        return reviewDisplay;
    }

    public void setReviewDisplay(final ReviewDisplay reviewDisplay) {
        this.reviewDisplay = reviewDisplay;
    }

    public Goback getGoback() {
        return goback;
    }

    public void setGoback(final Goback goback) {
        this.goback = goback;
    }
}
