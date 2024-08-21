/**
 *
 * (c) Copyright Ascensio System SIA 2024
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

(function($) {
    $(document).ready(function() {
        var url = AJS.contextPath() + "/plugins/servlet/onlyoffice/configure";
        var msgBox = $("#onlyofficeMsg");
        var submitBtn = $("#onlyofficeSubmitBtn");

        function updateConfig() {
            var editingTypes = new Array();
            $(".editing-types").each(function () {
                if ($(this).is(":checked")) editingTypes.push($(this).attr("id"));
            });

            var data = {
                "url": $("#apiUrlField").val().trim(),
                "innerUrl": $("#docInnerUrlField").val().trim(),
                "productInnerUrl": $("#confUrlField").val().trim(),
                "security": {
                    "key": $("#jwtSecretField").val(),
                    "header": $("#securityHeader").val()
                },
                "ignoreSSLCertificate": $("#verifyCertificate").is(":checked"),
                "customization": {
                    "forcesave": $("#forceSave").is(":checked"),
                    "chat": $("#chat").is(":checked"),
                    "compactHeader": $("#compactHeader").is(":checked"),
                    "feedback": $("#feedback").is(":checked"),
                    "help": $("#helpMenu").is(":checked"),
                    "toolbarNoTabs": $("#toolbarNoTabs").is(":checked"),
                    "review": {
                        "reviewDisplay": $("input[name='reviewDisplay']:checked").attr("id").replace("reviewDisplay_", "")
                    },
                },
                "demo": $("#onlyofficeDemo").is(":checked"),
                "lossyEdit": editingTypes
            };

            $.ajax({
                url: url,
                type: "POST",
                contentType: "application/json",
                data: JSON.stringify(data),
                processData: false,
                success: function (response) { onResponse(response, data); },
                error: function (response) { onResponse(response, data); }
            });
        }

        function onResponse(response) {
            $("#onlyoffice-loader").hide();
            submitBtn.attr("disabled", false);

            if (response) {
                const responseJson = JSON.parse(response);
                const validationResults = responseJson.validationResults;

                if (validationResults.documentServer) {
                    if (validationResults.documentServer.status == "failed") {
                        AJS.messages.error(
                            msgBox,
                            {
                                body: validationResults.documentServer.message
                            }
                        );
                    }
                }

                if (validationResults.commandService) {
                    if (validationResults.commandService.status == "failed") {
                        AJS.messages.error(
                            msgBox,
                            {
                                body: AJS.I18n.getText("onlyoffice.service.command.check.error-prefix").replace(
                                    "$",
                                    validationResults.commandService.message
                                )
                            }
                        );
                    }
                }

                if (validationResults.convertService) {
                    if (validationResults.convertService.status == "failed") {
                        AJS.messages.error(
                            msgBox,
                            {
                                body: AJS.I18n.getText("onlyoffice.service.convert.check.error-prefix").replace(
                                    "$",
                                   validationResults.convertService.message
                                )
                            }
                        );
                    }
                }

                AJS.messages.success(
                    msgBox,
                    {
                        body: AJS.I18n.getText("onlyoffice.configuration.message.settings.saved")
                    }
                );
            } else {
                AJS.messages.error(
                    msgBox,
                    {
                        body: AJS.I18n.getText("onlyoffice.configuration.message.settings.saving-error")
                    }
                );
            }
        };

        var testDocServiceApi = function () {
            var testApiResult = function () {
                var result = typeof DocsAPI != "undefined";

                if (result) {
                    updateConfig();
                } else {
                    AJS.messages.error(
                        msgBox,
                        {
                            body: AJS.I18n.getText("onlyoffice.server.common.error.api-js")
                        }
                    );

                    updateConfig();
                }
            };

            if (window.location.protocol == "https:" && $("#apiUrlField").val().startsWith("http:")) {
               AJS.messages.error(
                   msgBox,
                   {
                       body: AJS.I18n.getText("onlyoffice.server.common.error.mixed-content")
                   }
               );
            }

            delete DocsAPI;

            $("#scriptDocServiceAddress").remove();

            var js = document.createElement("script");
            js.setAttribute("type", "text/javascript");
            js.setAttribute("id", "scriptDocServiceAddress");
            document.getElementsByTagName("head")[0].appendChild(js);

            var scriptAddress = $("#scriptDocServiceAddress");

            scriptAddress.on("load", testApiResult).on("error", testApiResult);

            var docServiceUrlApi = $("#apiUrlField").val().trim();

            if (docServiceUrlApi.endsWith("/")) {
                docServiceUrlApi = docServiceUrlApi.slice(0, -1);
            }
            docServiceUrlApi += $("#pathApiUrl").html();

            scriptAddress.attr("src", docServiceUrlApi);
        };

        $("#onlyofficeConf").submit(function(e) {
            e.preventDefault();

            $("#onlyoffice-loader").show();
            submitBtn.attr("disabled", true);
            $("#admin-body-content .aui-message").remove();

            if ($("#onlyofficeDemo").is(":checked") && !$("#onlyofficeDemo").prop("disabled")) {
                updateConfig();
            } else {
                testDocServiceApi();
            }
        });

        $("#onlyofficeConf").on("click", ".view-control", function() {
            if ($("#jwtSecretField").attr("type") == "password"){
                $(this).addClass("view");
                $("#jwtSecretField").attr("type", "text");
            } else {
                $(this).removeClass("view");
                $("#jwtSecretField").attr("type", "password");
            }
            return false;
        });

        var demoToggle = function () {
            if (!$("#onlyofficeDemo").prop("disabled")) {
                $("#apiUrlField, #jwtSecretField, #docInnerUrlField, #verifyCertificate, #securityHeader").prop("disabled", $("#onlyofficeDemo").prop("checked"));
                if ($("#onlyofficeDemo").prop("checked")) {
                    $(".view-control").css("pointer-events", "none");
                    $(".view-control").removeClass("view");
                    $("#jwtSecretField").attr("type", "password");
                } else {
                    $(".view-control").css("pointer-events", "");
                }
            }
        };

        $("#onlyofficeDemo").click(demoToggle);
        demoToggle();

        $(".onlyoffice-tooltip").tooltip();
    });
})(AJS.$);