(function ($) {
    $(function () {
        AJS.$('.download-as-class').unbind('click');
        AJS.$('.download-as-class').bind("click", function (e) {
            e.preventDefault();
            var link = AJS.$(this);
            AJS.$.get(link.attr('href'), function (response) {
                AJS.$('.aui-page-panel').after(response);
                AJS.dialog2("#download-as-popup").show();
                $("#download-as-popup-binder").css("padding-bottom", "0");
            });
            return false;
        });
    });
})(AJS.$);