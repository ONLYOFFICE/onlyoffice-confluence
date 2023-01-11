(function ($) {
    $(function () {
        AJS.$('#download-as').unbind('click');
        AJS.$('#download-as').bind("click", function (e) {
            e.preventDefault();
            var link = AJS.$(this);
            AJS.$.get(link.attr('href'), function (response) {
                AJS.$('.aui-page-panel').after(response);
                AJS.dialog2("#download-as-popup").show();
                Confluence.Binder.autocompletePage(AJS.$("#download-as-popup-binder"));
            });
            return false;
        });
    });
})(AJS.$);