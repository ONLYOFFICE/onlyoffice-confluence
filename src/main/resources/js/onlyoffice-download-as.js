(function ($) {
    const dialogId = "#onlyoffice-download-as-dialog";
    const buttonCloseId = "#dialog-close-button";
    const buttonDownloadAsId = "#dialog-download-as-button";

    $(function () {
        AJS.$(".onlyoffice-download-as-action").unbind("click");
        AJS.$(".onlyoffice-download-as-action").bind("click", function (e) {
            e.preventDefault();

            var link = AJS.$(this);

            AJS.$.get(link.attr('href'), function (response) {
                AJS.$('.aui-page-panel').after(response);
                AJS.dialog2(dialogId).show();

                Confluence.Binder.autocompletePage(AJS.$("#onlyoffice-download-as-binder"));
                $(".aui-message-context").html("");

                AJS.$(buttonCloseId).bind("click", function (e) {
                    e.preventDefault();
                    AJS.dialog2(dialogId).hide();
                });

                AJS.$(buttonDownloadAsId).bind("click", function (e) {
                    e.preventDefault();
                    downloadAsAction();
                });
            });

            return false;
        });
    });

    function downloadAsAction(url) {
        var actionUrl = Confluence.getBaseUrl() + "/" + $(dialogId).find("form").attr("action");
        var data = {
            "fileName": $(dialogId).find("#file-name").val(),
            "targetFileType": $(dialogId).find("#target-file-type").val()
        };

        $(dialogId).find(buttonDownloadAsId)[0].busy();
        $(dialogId + " form").find("input,select").not(".disabled").attr("disabled","disabled");

        conversionRequest(
            actionUrl,
            data,
            function(response) {
                $("#onlyoffice-download-as-iframe").remove();
                $("body").append("<iframe id='onlyoffice-download-as-iframe' style='display:none;'></iframe>");
                $("#onlyoffice-download-as-iframe").attr("src", response.fileUrl);
                AJS.dialog2(dialogId).hide();
            },
            function(errorMessage) {
                $(dialogId).find(buttonDownloadAsId)[0].idle();
                $(".aui-message-context").html("<div class='aui-message aui-message-error'>" + errorMessage + "</div>");
                $(dialogId + " form").find("input,select").not(".disabled").attr("disabled", null);
            }
        )
    }

    function conversionRequest(url, data, onSuccess, onError) {
        if ($(dialogId).length <= 0) return;

        if (data.fileName == "") {
            onError(AJS.I18n.getText("fileName.required"));
            return;
        }

        if (/[\/:*?"<>|]/.test(data.fileName)) {
            onError(AJS.I18n.getText("filename.contain.invalid.character"));
            return;
        }

        $.ajax({
            type: "POST",
            url: url,
            data: data,
            success: function (response) {
                if (response.error) {
                    var errorMessage = getErrorMessage(response);
                    onError(errorMessage);
                    return;
                }

                if (response.endConvert) {
                    onSuccess(response);
                } else {
                    setTimeout(function() {
                        conversionRequest(url, data, onSuccess, onError);
                    }, 1000);
                }
            },
            error: function (xhr) {
                if (xhr.status == 403) {
                    onError(AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.permission"));
                } else {
                    onError(AJS.I18n.getText("onlyoffice.connector.error.Unknown"));
                }
            }
        });
    }

    function getErrorMessage(response) {
        var errorMessage;
        var servicePrefix = false;

        switch (response.error) {
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
})(AJS.$);