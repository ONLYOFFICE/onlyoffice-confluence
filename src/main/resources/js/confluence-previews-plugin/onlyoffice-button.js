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

define('cp/component/onlyoffice-button', [
    'jquery',
    'ajs',
    'backbone',
    'core/template-store-singleton'
], function ($,
             AJS,
             Backbone,
             templateStore) {
    'use strict';

    var OnlyofficeButtonView = Backbone.View.extend({
        tagName: 'span',

        initialize: function (options) {
            this._mediaViewer = options.mediaViewer;
        },

        render: function () {
            var attachmentId = this._mediaViewer.getCurrentFile().get('id');

            var xhr = new XMLHttpRequest();

            xhr.open("POST", AJS.contextPath() + "/plugins/servlet/onlyoffice/confluence/previews/plugin/access", false);
            xhr.send(JSON.stringify({
                 attachmentId: attachmentId
            }));

            var title;
            var mode;

            if (xhr.status == 200) {
                var response = JSON.parse(xhr.responseText);

                if (response.access == "edit") {
                    title = AJS.I18n.getText('onlyoffice.editor.editlink');
                } else if (response.access == "view") {
                    title = AJS.I18n.getText('onlyoffice.editor.viewlink');
                } else if (response.access == "fillForms") {
                    title = AJS.I18n.getText('onlyoffice.editor.fillFormlink');
                    mode = "fillForms";
                }
            }

            if (title) {
                this.$el.html(templateStore.get('controlOnlyofficeButton')({
                    contextPath: AJS.contextPath(),
                    attachmentId: attachmentId,
                    title: title,
                    mode: mode
                }));
                if ($.fn.tooltip) {
                    this.$('a').tooltip({gravity: 'n'});
                }

                this.$('a').on("click", function() {
                    this.blur();
                });
            }

            return this;
        }
    });

    var OnlyofficeButton = function (mediaViewer) {
        mediaViewer.getView().fileControlsView.addLayerView('onlyofficeButton', OnlyofficeButtonView, {
            weight: 1,
            predicate: function (mediaViewer) {
                return true;
            }
        });
    };

    return OnlyofficeButton;
});

(function () {
    var OnlyofficeButton = require('cp/component/onlyoffice-button');
    var MediaViewer = require('MediaViewer');
    MediaViewer.registerPlugin('onlyofficebutton', OnlyofficeButton);
})();
