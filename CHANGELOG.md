# Change Log

## 6.3.0
## Added
- setting Run document macros

## Changed
- compatible with Confluence 9.5.2
- updated list supported formats, see [ONLYOFFICE/document-formats v3.0.0](https://github.com/ONLYOFFICE/document-formats/releases/tag/v3.0.0)
- fix CVE-2025-27820

## 6.2.0
## Changed
- com.onlyoffice:documentserver-sdk-java:1.4.0
    - support hwp, hwpx, pages, numbers, key formats
    - fi, he, no, sl, sq-AL empty file templates
    - shardkey parameter
    - the list of image formats available for insertion into the editor has been expanded (tiff)
    - demo server address changed
    - address of the convert service, /converter instead /ConvertService.ashx
    - apache httpclient 5
    - default token lifetime is 5 minutes
- fixed issue with opening editor, opening a history, the download as action and the button to open the editor in
preview for an anonymous user
- fixed ui for dark theme
- compatible with Confluence 9.3.2

## 6.1.0
## Added
- compatible with Confluence 9.*

## Changed
- forcesave is no longer supported
- history highlighting is no longer supported

## 5.1.0
## Added
- compatible with Confluence 8.9.7
- creating pdf form
- button go to edit in editor
- pdf favicon

## 5.0.2
## Changed
- redefine dependencies com.fasterxml.jackson

## 5.0.1
## Added
- compatible with Confluence 8.9

## Changed
- delete dependency com.fasterxml.jackson.core (CVE-2023-35116)

## 5.0.0
## Added
- compatible with Confluence 8.8
- core of the plugin has been moved to com.onlyoffice.docs-integration-sdk (https://github.com/ONLYOFFICE/docs-integration-sdk-java)
- improved connection settings validation
- improved history servlet security
- setting authorization header on settings page
- user image in editor
- filling pdf
- translations (zh_CN)

## Changed
- improved date formatting in history changes
- default conversion format (from docxf to pdf instead oform)
- remove filling for oform

## 4.4.0
## Added
- compatible with Confluence 8.6
- opening documents for viewing by an anonymous user
- edit button in ONLYOFFICE preview macro
- link to docs cloud

## 4.3.1
## Changed
- vulnerability dependency update

## 4.3.0
## Added
- compatible with Confluence 8.4.0
- extended list of supported formats
- document server v6.4 and earlier is no longer supported
- macros for view documents
- improvement of jwt signature
- download as
- Paste Special to add a link between files

## 4.2.0
## Changed
- compatible with Confluence 8.1.1
- Confluence 6 is no longer supported
- fix editing in blog

## 4.1.0
## Added
- button ONLYOFFICE in confluence preview 

## Changed
- compatible with Confluence 7.19.0

## 4.0.0
## Added
- review display settings
- disable certificate verification
- opening for editing not OOXML
- editor interface customization
- ability to get links to bookmarks in document
- creating documents from editor
- open non-editable formats on views
- formats for conversion docm, dot, dotx, epub, htm, html, otp, ots, ott, pot, potm, potx, pps, ppsm, ppsx, pptm, rtf, xlsm, xlt, xltm, xltx
- keep intermediate versions when editing (forcesave)
- version history with highlighting changes
- detecting mobile browser
- change favicon in editor by document type
- insert image from storage
- compare file from storage
- mail merge from storage

## Changed
- document server v6.0 and earlier is no longer supported
- redesign settings page

## 3.1.0
## Added
- support docxf and oform formats
- create blank docxf from creation menu
- create docxf from docx from creation menu
- create oform from docxf from document manager
- "save as" in editor

## Changed
- compatible with Confluence 7.15.0

## 3.0.1
## Changed
- compatible with Confluence 7.13

## 3.0.0
## Added
- compatible with Data Center

## Changed
- compatible with Confluence 7.12
- improving JWT validation

## 2.4.2
## Changed
- Updated marketplace version

## 2.4.1
## Changed
- fixed page refresh on creation

# 2.4.0
## Added
- Ability to create documents
- Added connection to a demo document server
- Button to hide/show Secret key
- Button to open file location

## Changed
- Moved jwt-header parameter to confluence.cfg.xml file

## 2.3.0
## Changed
- Apache license
- Document Editing Service address is now splitted in two settings: inner address (address that confluence will use to access service) and public address (address that user will use to access editors)

## 2.2.1
## Changed
- Updated marketplace version

## 2.2.0
## Fixed
- ANSI encoding issue

## Added
- Option to convert `odt`, `doc`, `odp`, `ppt`, `ods`, `xls` files to Office OpenXML
- DE, IT, ES, RU, FR translations for settings page

## 2.1.0
## Added
- DE, IT, ES, RU, FR translations

## 2.0.0
## Added
- jwt support
- saving settings will now run a set of test to identify potential problems

## Changed
- document editors will now use user locale
- fixed an issue with trailing slash when configuring document server url
