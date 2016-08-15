# Confluence ONLYOFFICE™ integration plugin

This plugin enables users to edit office documents from [Confluence](https://www.atlassian.com/software/confluence/) using ONLYOFFICE™ Document Server. Currently the following document formats can be opened and edited with this plugin: DOCX, XLSX, PPTX.

This will create a new **Edit in ONLYOFFICE** action within the document library for Office documents. This allows multiple users to collaborate in real time and to save back those changes to Confluence.


## Installing ONLYOFFICE™ Document Server

You will need an instance of ONLYOFFICE™ Document Server that is resolvable and connectable both from Confluence and any end clients (version 3.0 and later are supported for use with the plugin). If that is not the case, use the official ONLYOFFICE™ Document Server documetnations page: [Document Server for Linux](http://helpcenter.onlyoffice.com/server/linux/document/linux-installation.aspx). ONLYOFFICE™ Document Server must also be able to POST to Confluence directly.

The easiest way to start an instance of ONLYOFFICE™ Document Server is to use [Docker](https://github.com/ONLYOFFICE/Docker-DocumentServer).



## Configuring Confluence CONLYOFFICE™ integration plugin

Change the **files.docservice.url.domain** properties in `src/main/resources/onlyoffice-config.properties` to the name of the server with the ONLYOFFICE™ Document Server installed: 
```
files.docservice.url.domain=http://documentserver/
```


## Installing Confluence ONLYOFFICE™ integration plugin

You will need:

* 1.8.X of the Oracle Java SE Development Kit 8,

* Atlassian Plugin SDK,

* Compile package
```bash
atlas-package
```
* Upload ***target/onlyoffice-confluence-plugin.jar*** to Confluence on page `Manage add-ons`.


## How it works

The ONLYOFFICE™ integration follows the API documented here https://api.onlyoffice.com/editors/basic:

* User navigates to a Confluence attachments and selects the `Edit in ONLYOFFICE` action.
* Confluence makes a request to OnlyOfficeEditorServlet (URL of the form: `/plugins/servlet/onlyoffice/doceditor?attachmentId=$attachment.id`).
* Confluence sends document to ONLYOFFICE™ Document storage service and receive a temporary link.
* Confluence prepares a JSON object with the following properties:
  * **fileUrl**: the temporary link that ONLYOFFICE™ Document Server uses to download the document,
  * **callbackUrl**: the URL that ONLYOFFICE™ Document Server informs about status of the document editing,
  * **docserviceApiUrl**: the URL that the client needs to reply to ONLYOFFICE™ Document Server (provided by the files.docservice.url.api property),
  * **key**: the UUID to instruct ONLYOFFICE™ Document Server whether to download the document again or not,
  * **fileName**: the document Title (name).
* Confluence takes this object and constructs a page from a freemarker template, filling in all of those values so that the client browser can load up the editor.
* The client browser makes a request for the javascript library from ONLYOFFICE™ Document Server and sends ONLYOFFICE™ Document Server the docEditor configuration with the above properties.
* Then ONLYOFFICE™ Document Server downloads the document from Document storage and the user begins editing.
* When all users and client browsers are done with editing, they close the editing window.
* After 10 seconds of inactivity, ONLYOFFICE™ Document Server sends a POST to the `callback` URL letting Confluence know that the clients have finished editing the document and closed it.
* Confluence downloads the new version of the document, replacing the old one.
