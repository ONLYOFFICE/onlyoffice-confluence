AJS.toInit(function ($) {
    var $webItem = $('#onlyoffice-doccreate');
    var dialogId = "doccreate";

    var width = $webItem.width() <= 98 ? 120 : $webItem.width() + 22;

    var dialog = AJS.InlineDialog($webItem, dialogId,
        function(content, trigger, showPopup) {
            content.html(Confluence.Templates.Onlyoffice.dialog());
            showPopup();
            return false;
        },
       {
       width: width
       }
    );

    $webItem.click(function() {
     $('#inline-dialog-doccreate .aui-inline-dialog-contents').width(width);
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
       if($('#inline-dialog-doccreate .aui-inline-dialog-contents').width() == width){
            $('#inline-dialog-doccreate .aui-inline-dialog-contents').width(width + 36);
       }
    });
});