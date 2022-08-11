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

    var currentDialog = null;

    var OnlyofficeButtonView = Backbone.View.extend({
        tagName: 'span',

        initialize: function (options) {
            this._mediaViewer = options.mediaViewer;
        },

        render: function () {
            var that = this;
            this.$el.html(templateStore.get('controlOnlyofficeButton')({
                attachmentId: that._mediaViewer.getCurrentFile().get('id')
            }));
            if ($.fn.tooltip) {
                this.$('a').tooltip({gravity: 'n'});
            }

            return this;
        }
    });

    var OnlyofficeButton = function (mediaViewer) {
        if (!mediaViewer.getConfig().enableShareButton) {
            return;
        }
        mediaViewer.getView().fileControlsView.addLayerView('onlyofficeButton', OnlyofficeButtonView, {
            weight: 9,
            predicate: function (mediaViewer) {
                return !mediaViewer.getCurrentFile().get('isRemoteLink');
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
