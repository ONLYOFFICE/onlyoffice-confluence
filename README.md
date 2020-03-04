# Confluence ONLYOFFICE integration plugin

This plugin enables users to edit office documents from [Confluence](https://www.atlassian.com/software/confluence/) using ONLYOFFICE Document Server - [Community or Integration Edition](#onlyoffice-document-server-editions).

## Features

The plugin allows to:

* Create and edit text documents, spreadsheets, and presentations.
* Share documents with other users.
* Co-edit documents in real-time: use two co-editing modes (Fast and Strict), Track Changes, comments, and built-in chat.

Supported formats:

* For viewing and editing: DOCX, XLSX, PPTX. 
* For conversion to Office Open XML: ODT, DOC, ODP, PPT, ODS, XLS. 

## Installing ONLYOFFICE Document Server

You will need an instance of ONLYOFFICE Document Server that is resolvable and connectable both from Confluence and any end clients (version 3.0 and later are supported for use with the plugin). ONLYOFFICE Document Server must also be able to POST to Confluence directly.

You can install free Community version of ONLYOFFICE Document Server or scalable enterprise-level Integration Edition.

To install free Community version, use [Docker](https://github.com/onlyoffice/Docker-DocumentServer) (recommended) or follow [these instructions](https://helpcenter.onlyoffice.com/server/linux/document/linux-installation.aspx) for Debian, Ubuntu, or derivatives.  

To install Integration Edition, follow instructions [here](https://helpcenter.onlyoffice.com/server/integration-edition/index.aspx).

Community Edition vs Integration Edition comparison can be found [here](#onlyoffice-document-server-editions).

## Installing Confluence ONLYOFFICE integration plugin

Upload the compiled ***target/onlyoffice-confluence-plugin.jar*** to Confluence on the `Manage add-ons` page.
The latest compiled package files are available [here](https://github.com/onlyoffice/onlyoffice-confluence/releases).

## Configuring Confluence ONLYOFFICE integration plugin

Find the uploaded ***ONLYOFFICE Confluence plugin*** on the `Manage add-ons` page. Click `Configure` and enter the name of the server with the ONLYOFFICE Document Server installed:
```
http://documentserver/
```
## Compiling Confluence ONLYOFFICE integration plugin

You will need:

* 1.8.X of the Oracle Java SE Development Kit 8,

* Atlassian Plugin SDK,

* Compile package:
```bash
atlas-package
```

## How it works

The ONLYOFFICE integration follows the API documented here https://api.onlyoffice.com/editors/basic:

* User navigates to a Confluence attachments and selects the `Edit in ONLYOFFICE` action.
* Confluence makes a request to OnlyOfficeEditorServlet (URL of the form: `/plugins/servlet/onlyoffice/doceditor?attachmentId=$attachment.id`).
* Confluence sends document to ONLYOFFICE Document storage service and receive a temporary link.
* Confluence prepares a JSON object with the following properties:
  * **url**: the temporary link that ONLYOFFICE Document Server uses to download the document,
  * **callbackUrl**: the URL that ONLYOFFICE Document Server informs about status of the document editing,
  * **docserviceApiUrl**: the URL that the client needs to reply to ONLYOFFICE Document Server (provided by the files.docservice.url.api property),
  * **key**: the UUID to instruct ONLYOFFICE Document Server whether to download the document again or not,
  * **title**: the document Title (name).
* Confluence takes this object and constructs a page from a freemarker template, filling in all of those values so that the client browser can load up the editor.
* The client browser makes a request for the javascript library from ONLYOFFICE Document Server and sends ONLYOFFICE Document Server the docEditor configuration with the above properties.
* Then ONLYOFFICE Document Server downloads the document from Document storage and the user begins editing.
* When all users and client browsers are done with editing, they close the editing window.
* After 10 seconds of inactivity, ONLYOFFICE Document Server sends a POST to the `callback` URL letting Confluence know that the clients have finished editing the document and closed it.
* Confluence downloads the new version of the document, replacing the old one.

## ONLYOFFICE Document Server editions 

ONLYOFFICE offers different versions of its online document editors that can be deployed on your own servers.

**ONLYOFFICE Document Server:**

* Community Edition (`onlyoffice-documentserver` package)
* Integration Edition (`onlyoffice-documentserver-ie` package)

The table below will help you make the right choice.

| Pricing and licensing | Community Edition | Integration Edition |
| ------------- | ------------- | ------------- |
| | [Get it now](https://www.onlyoffice.com/download.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence)  | [Start Free Trial](https://www.onlyoffice.com/connectors-request.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence)  |
| Cost  | FREE  | [Go to the pricing page](https://www.onlyoffice.com/integration-edition-prices.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence)  |
| Simultaneous connections | up to 20 maximum  | As in chosen pricing plan |
| Number of users | up to 20 recommended | As in chosen pricing plan |
| License | GNU AGPL v.3 | Proprietary |
| **Support** | **Community Edition** | **Integration Edition** | 
| Documentation | [Help Center](https://helpcenter.onlyoffice.com/server/docker/opensource/index.aspx) | [Help Center](https://helpcenter.onlyoffice.com/server/integration-edition/index.aspx) |
| Standard support | [GitHub](https://github.com/ONLYOFFICE/DocumentServer/issues) or paid | One year support included |
| Premium support | [Buy Now](https://www.onlyoffice.com/support.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence) | [Buy Now](https://www.onlyoffice.com/support.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence) |
| **Services** | **Community Edition** | **Integration Edition** | 
| Conversion Service                | + | + | 
| Document Builder Service          | + | + | 
| **Interface** | **Community Edition** | **Integration Edition** |
| Tabbed interface                       | + | + |
| White Label                            | - | - |
| Integrated test example (node.js)     | - | + |
| **Plugins & Macros** | **Community Edition** | **Integration Edition** |
| Plugins                           | + | + |
| Macros                            | + | + |
| **Collaborative capabilities** | **Community Edition** | **Integration Edition** |
| Two co-editing modes              | + | + |
| Comments                          | + | + |
| Built-in chat                     | + | + |
| Review and tracking changes       | + | + |
| Display modes of tracking changes | + | + |
| Version history                   | + | + |
| **Document Editor features** | **Community Edition** | **Integration Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Adding Content control          | - | + | 
| Editing Content control         | + | + | 
| Layout tools                    | + | + |
| Table of contents               | + | + |
| Navigation panel                | + | + |
| Mail Merge                      | + | + |
| Comparing Documents             | - | +* |
| **Spreadsheet Editor features** | **Community Edition** | **Integration Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Functions, formulas, equations  | + | + |
| Table templates                 | + | + |
| Pivot tables                    | +** | +** |
| **Presentation Editor features** | **Community Edition** | **Integration Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Animations                      | + | + |
| Presenter mode                  | + | + |
| Notes                           | + | + |
| | [Get it now](https://www.onlyoffice.com/download.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence)  | [Start Free Trial](https://www.onlyoffice.com/connectors-request.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence)  |

\* It's possible to add documents for comparison from your local drive or from URL. Adding files for comparison from storage is not available yet. 

\** Changing style and deleting (Full support coming soon)
