/**
 *
 * (c) Copyright Ascensio System SIA 2022
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
    const wordFormats = ["djvu", "doc", "docm", "docx", "docxf", "oform", "dot", "dotm", "dotx", "epub", "fb2", "fodt", "html", "mht", "odt", "ott", "oxps", "pdf", "rtf", "txt"];
    const cellFormats = ["xps", "xml", "csv", "fods","ods", "ots","xls","xlsb","xlsm", "xlsx", "xlt", "xltm", "xltx"];
    const slideFormats = ["fodp", "odp", "otp", "pot", "potm", "potx", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx"];

    AJS.MacroBrowser.activateSmartFieldsAttachmentsOnPage("onlyoffice-preview", [].concat(wordFormats, cellFormats, slideFormats));
})(AJS.$);
