(function ($) {
    $(function () {
        AJS.$('.download-as-class').unbind('click');
        AJS.$('.download-as-class').bind("click", function (e) {
            e.preventDefault();
            var link = AJS.$(this);
            AJS.$.get(link.attr('href'), function (response) {
                AJS.$('.aui-page-panel').after(response);
                AJS.dialog2("#download-as-popup").show();
                $(".aui-message-context").html("");
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
            error: function (data) { onError(data) }
        });
    }
    function getErrorByResponse(response) {
        if (response.errorData && response.errorData.error) {
            var errorMessage;
            var servicePrefix = false;

            switch (response.errorData.error) {
                case -1:
                    errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.unknown");
                    servicePrefix = true;
                    break;
                case -2:
                    errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.timeout");
                    servicePrefix = true;
                    break;
                case -3:
                    errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.conversion");
                    servicePrefix = true;
                    break;
                case -4:
                    errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.download");
                    servicePrefix = true;
                    break;
                case -5:
                    errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.password");
                    servicePrefix = true;
                    break;
                case -6:
                    errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.database");
                    servicePrefix = true;
                    break;
                case -7:
                    errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.input");
                    servicePrefix = true;
                    break;
                case -8:
                    errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.token");
                    servicePrefix = true;
                    break;
                case -10:
                    errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.not-reached");
                    break;
                default:
                    errorMessage = AJS.I18n.getText("onlyoffice.connector.error.Unknown");
            }

            if (servicePrefix) {
                errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.service-prefix").replace("$", errorMessage);
            }

            return errorMessage;
        }
    }

    function onError(response) {
        var errorMessage = getErrorByResponse(response);
        $(".aui-message-context").html("<div class='aui-message aui-message-error'>" + errorMessage + "</div>");
    }

    function onResponse(data) {
        if (data.errorData) {
            onError(data);
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