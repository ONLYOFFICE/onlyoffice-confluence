AJS.toInit(function ($) {
    var $webItem = $('#onlyoffice-doccreate');
    var dialogId = "doccreate";
    var selectedAjsParams = {
                url: AJS.contextPath() + "/plugins/servlet/onlyoffice/doceditor"
            };

    var dialog = AJS.InlineDialog($webItem, dialogId,
        function(content, trigger, showPopup) {
            content.html(Confluence.Templates.Onlyoffice.dialog(selectedAjsParams));
            showPopup();
            return false;
        },
        {
            calculatePositions: function getPosition(popup, targetPosition, mousePosition, opts) {
                return {
                    popupCss: {
                        top: $webItem.offset().top + $webItem.outerHeight() + 4,
                        right: $(document).width() - $webItem.offset().left - $webItem.outerWidth()
                    },
                    arrowCss:{}
                };
            }
        });

    $webItem.click(function() {
        if($('#inline-dialog-' + dialogId).is(':visible')) {
            dialog.hide();
        }
    });

    $(document).on("click", '.menuitem', function(event){
       $('.menuitem').removeClass('active');
       $('.displayname').removeClass('hidden');
       $('.filenameform').addClass('hidden');
       $(this).find('.displayname').addClass('hidden');
       $(this).find('.filenameform').removeClass('hidden');
       $(this).addClass('active');
    });

    $(document).on("submit", '.filenameform', function(event){
            setTimeout(function () { document.location.reload(); }, 1000);
            return true;
    });
});