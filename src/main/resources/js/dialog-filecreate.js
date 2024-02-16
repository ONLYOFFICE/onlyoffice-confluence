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

AJS.toInit(function ($) {
    var buttonCreate = $("#onlyoffice-filecreate");
    var docx = "docx";

    var paramsPage = {
        pageId: AJS.params.pageId,
        extensions: {
            "docx" : AJS.I18n.getText ("onlyoffice.editor.dialog.filecreate.docx"),
            "xlsx" : AJS.I18n.getText ("onlyoffice.editor.dialog.filecreate.xlsx"),
            "pptx" : AJS.I18n.getText ("onlyoffice.editor.dialog.filecreate.pptx"),
            "docxf" : AJS.I18n.getText ("onlyoffice.editor.dialog.filecreate.docxf")
        }
    };

    var dialog = AJS.InlineDialog(buttonCreate, "filecreate",
        function(content, trigger, showPopup) {
            content.html(Confluence.Templates.Onlyoffice.filecreate(paramsPage));
            showPopup();
            return false;
        },
        {
            calculatePositions: function getPosition(popup, targetPosition, mousePosition, opts) {
                return {
                    popupCss: {
                        top: buttonCreate.offset().top + buttonCreate.outerHeight() + 4,
                        right: $(document).width() - buttonCreate.offset().left - buttonCreate.outerWidth()
                    },
                    arrowCss:{}
                };
            },
            closeOthers: true,
            persistent: false

        }
    );

    buttonCreate.click(function() {
        $('#inline-dialog-filecreate .aui-inline-dialog-contents').css({minWidth: buttonCreate.outerWidth()});
        if($("#inline-dialog-filecreate").is(":visible")) {
            dialog.hide();
        }
    });

    $(document).on("click", ".menuitem", function(event) {
       $(".menuitem").removeClass("active");
       $(".filename label").removeClass("hidden");
       $(".filename input").addClass("hidden");
       $(this).find(".filename label").addClass("hidden");
       $(this).find(".filename input").removeClass("hidden");
       $(this).addClass("active");
       $(this).find(".filename input.input-name").focus();
    });

    $(document).on("submit", ".filenameform", function(event) {
        if (event.currentTarget.id == "form-docxf") {
            if (AJS.Editor.ImageDialog.panelComponent.length == 0) {
                var attachmentsPanelView = new AJS.Editor.FileDialog.AttachmentsPanelView({
                    eventListener: AJS.Editor.FileDialog.eventListener
                });

                AJS.Editor.ImageDialog.panelComponent.push(attachmentsPanelView);
                AJS.Editor.ImageDialog.panelComponent.push(new AJS.Editor.FileDialog.SearchPanelView());
            }

            if (AJS.Editor.ImageDialog.panelComponent.length > 2) AJS.Editor.ImageDialog.panelComponent.splice(1, 1);

            var insertImageDialog = AJS.Editor.ImageDialog.insertImageDialog(function(a) {
                var fileExt = a.selectItems[0].attributes.fileName.split(".").pop();
                if (fileExt == "docx") {
                    window.open(AJS.contextPath() + "/plugins/servlet/onlyoffice/convert?attachmentId=" + a.selectItems[0].attributes.id +
                            "&pageId=" + AJS.params.pageId + "&newTitle=" +  $("#view-input-docxf").attr("value")
                    )
                    setTimeout(function () { document.location.reload(); }, 1000);
                } else {
                    AJS.flag({
                        type: "error",
                        body: AJS.I18n.getText("onlyoffice.editor.dialog.create.form.message.error")
                    });
                }
            });

            var dialog = $("#insert-image-dialog");

            dialog.find(".file-list").on("DOMNodeInserted", function(event) {
                var fileExt = event.srcElement.dataset.fileName.split(".").pop();
                if (fileExt != "docx") $(event.srcElement).remove();
            });

            var buttonPanel = dialog.find(".dialog-button-panel");
            var insertButton = dialog.find(".button-panel-button.insert")[0];

            $(insertButton).text(AJS.I18n.getText("onlyoffice.editor.dialog.create.form.button.create"));

            $(insertButton).clone()
                .text(AJS.I18n.getText("onlyoffice.editor.dialog.create.form.button.create-blank"))
                .removeAttr("disabled")
                .removeAttr("aria-disabled")
                .click(function() {
                    insertImageDialog.dialog.remove();
                    insertImageDialog.clearSelection();
                    event.currentTarget.submit()
                    setTimeout(function () { document.location.reload(); }, 1000);
                 })
                .prependTo(buttonPanel);

            dialog.find(".dialog-title").text(AJS.I18n.getText("onlyoffice.editor.dialog.create.form.title"));
            dialog.find("#upload-attachment").remove();
            dialog.find(".divider").remove();
            dialog.find(".dialog-tip").text("");

            return false;
        }

        setTimeout(function () { document.location.reload(); }, 1000);
        return true;
    });
});