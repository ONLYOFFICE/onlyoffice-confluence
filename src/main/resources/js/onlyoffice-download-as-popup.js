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

                AJS.$('#dialog-close-button').bind("click", function (e) {
                    e.preventDefault();
                    AJS.dialog2("#download-as-popup").hide();
                });

                AJS.$('#dialog-download-as-button').bind("click", function (e) {
                    e.preventDefault();
                    downloadAsAction();
                });
            });
            return false;
        });
    });

    function downloadAsAction() {
        var params = {
            fileName: AJS.Onlyoffice.fileName,
            currentFileType: AJS.Onlyoffice.currentFileType,
            targetFileType: AJS.Onlyoffice.targetFileType,
            pageId: AJS.Onlyoffice.pageId,
            attachmentId: AJS.Onlyoffice.attachmentId
        };
        var url = AJS.contextPath() + "/plugins/servlet/onlyoffice/convert?attachmentId="+ params.attachmentId
            + "&newTitle=" + params.fileName + "&pageId=" + params.pageId + "&isDownloadAs=true" + "&newExt=" + params.targetFileType;
        fetchConvertData(url);
    }

    function fetchConvertData(url) {
        $.ajax({
            url: url,
            type: "POST",
            cache: false,
            success: function (data) { onResponse(data) },
            error: function (data) { console.log(data) }
        });
    }

    function onError(error) {
        console.log(error);
    }

    function onResponse(data) {
        if (data.error) {
            onError(data.error);
            return;
        }

        if (!data.endConvert) {
            setTimeout(fetchConvertData, 1000);
        } else {
            $("#onlyoffice-download-as-iframe").remove();
            $("body").append("<iframe id='onlyoffice-download-as-iframe' style='display:none;'></iframe>");
            $("#onlyoffice-download-as-iframe").attr("src", data.fileUrl);
        }
    }
})(AJS.$);