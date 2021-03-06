AJS.toInit(function ($) {
    var buttonCreate = $("#onlyoffice-filecreate");
    var docx = "docx";

    var paramsPage = {
        pageId: AJS.params.pageId,
        extensions: {
            "docx" : AJS.I18n.getText ("onlyoffice.connector.dialog-filecreate.docx"),
            "xlsx" : AJS.I18n.getText ("onlyoffice.connector.dialog-filecreate.xlsx"),
            "pptx" : AJS.I18n.getText ("onlyoffice.connector.dialog-filecreate.pptx")
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
    });

    $(document).on("submit", ".filenameform", function(event) {
        setTimeout(function () { document.location.reload(); }, 1000);
        return true;
    });
});