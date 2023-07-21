# Confluence ONLYOFFICE integration app

This app enables users to edit office documents from [Confluence](https://www.atlassian.com/software/confluence/) using ONLYOFFICE Docs packaged as Document Server - [Community or Enterprise Edition](#onlyoffice-docs-editions).

## Features

The app allows to:

* Create and edit text documents, spreadsheets, and presentations.
* Share documents with other users.
* Co-edit documents in real-time: use two co-editing modes (Fast and Strict), Track Changes, comments, and built-in chat.

Supported formats:

**For viewing:**
* **WORD:** DJVU, DOC, DOCM, DOCX, DOCXF, DOT, DOTM, DOTX, EPUB, FB2, FODT, HTM, HTML, MHT, ODT, OFORM, OTT, OXPS, PDF, RTF, TXT, XML, XPS
* **CELL:** CSV, FODS, ODS, OTS, XLS, XLSM, XLSX, XLT, XLTM, XLTX
* **SLIDE:** FODP, ODP, OTP, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX

**For editing:**

* **WORD:** DOCM, DOCX, DOCXF, DOTM, DOTX, HTM, XML
* **CELL:** XLSM, XLSX, XLTM, XLTX
* **SLIDE:** POTM, POTX, PPSM, PPSX, PPTM, PPTX

**For editing with possible loss of information:**

* **WORD:** EPUB, FB2, HTML, ODT, OTT, RTF, TXT
* **CELL:** CSV, ODS, OTS
* **SLIDE:** ODP, OTP

**For filling:**

* **WORD:** OFORM

**For converting to Office Open XML formats:**

* **WORD:** DOC, DOCM, DOCXF, DOT, DOTM, DOTX, EPUB, FB2, FODT, HTM, HTML, MHT, ODT, OTT, OXPS, PDF, RTF, XML, XPS
* **CELL:** FODS, ODS, OTS, XLS, XLSM, XLT, XLTM, XLTX
* **SLIDE:** FODP, ODP, OTP, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM

## Installing ONLYOFFICE Docs

You will need an instance of ONLYOFFICE Docs (Document Server) that is resolvable and connectable both from Confluence and any end clients. ONLYOFFICE Document Server must also be able to POST to Confluence directly.

You can install free Community version of ONLYOFFICE Docs or scalable Enterprise Edition with pro features.

To install free Community version, use [Docker](https://github.com/onlyoffice/Docker-DocumentServer) (recommended) or follow [these instructions](https://helpcenter.onlyoffice.com/installation/docs-community-install-ubuntu.aspx) for Debian, Ubuntu, or derivatives.  

To install Enterprise Edition, follow instructions [here](https://helpcenter.onlyoffice.com/installation/docs-enterprise-index.aspx).

Community Edition vs Enterprise Edition comparison can be found [here](#onlyoffice-docs-editions).

## Installing Confluence ONLYOFFICE integration app

Upload the compiled ***target/onlyoffice-confluence-plugin.jar*** to Confluence on the `Manage add-ons` page.

The latest compiled package files are available [here](https://github.com/onlyoffice/onlyoffice-confluence/releases) and on [Atlassian Marketplace](https://marketplace.atlassian.com/apps/1218214/onlyoffice-connector-for-confluence).

You could also install the app from Confluence administration panel:

1. Navigate to `Manage add-ons` page.
2. Click **Find new apps** or **Find new add-ons** on the left panel.
3. Locate **ONLYOFFICE Connector for Confluence** using search.
4. Click **Install** to download and install the app.

## Configuring Confluence ONLYOFFICE integration app

Find the uploaded ***ONLYOFFICE Confluence connector*** on the `Manage add-ons` page. Click `Configure` and enter the name of the server with the ONLYOFFICE Document Server installed:
```
http://documentserver/
```
Starting from version 7.2, JWT is enabled by default and the secret key is generated automatically to restrict the access to ONLYOFFICE Docs and for security reasons and data integrity. 
Specify your own **Secret key** on the Confluence administration page. 
In the ONLYOFFICE Docs [config file](https://api.onlyoffice.com/editors/signature/), specify the same secret key and enable the validation.

## Compiling Confluence ONLYOFFICE integration app

You will need:

* 1.8.X of the Oracle Java SE Development Kit 8,

* Atlassian Plugin SDK,

* Get a submodule:
  ```bash
  git submodule update --init --recursive
  ```

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

## ONLYOFFICE Docs editions 

ONLYOFFICE offers different versions of its online document editors that can be deployed on your own servers.

**ONLYOFFICE Docs** packaged as Document Server: 

* Community Edition (`onlyoffice-documentserver` package)
* Enterprise Edition (`onlyoffice-documentserver-ee` package)

The table below will help you make the right choice.

| Pricing and licensing | Community Edition | Enterprise Edition |
| ------------- | ------------- | ------------- |
| | [Get it now](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence#docs-community)  | [Start Free Trial](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence#docs-enterprise)  |
| Cost  | FREE  | [Go to the pricing page](https://www.onlyoffice.com/docs-enterprise-prices.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence)  |
| Simultaneous connections | up to 20 maximum  | As in chosen pricing plan |
| Number of users | up to 20 recommended | As in chosen pricing plan |
| License | GNU AGPL v.3 | Proprietary |
| **Support** | **Community Edition** | **Enterprise Edition** |
| Documentation | [Help Center](https://helpcenter.onlyoffice.com/installation/docs-community-index.aspx) | [Help Center](https://helpcenter.onlyoffice.com/installation/docs-enterprise-index.aspx) |
| Standard support | [GitHub](https://github.com/ONLYOFFICE/DocumentServer/issues) or paid | One year support included |
| Premium support | [Contact us](mailto:sales@onlyoffice.com) | [Contact us](mailto:sales@onlyoffice.com) |
| **Services** | **Community Edition** | **Enterprise Edition** |
| Conversion Service                | + | + |
| Document Builder Service          | + | + |
| **Interface** | **Community Edition** | **Enterprise Edition** |
| Tabbed interface                       | + | + |
| Dark theme                             | + | + |
| 125%, 150%, 175%, 200% scaling         | + | + |
| White Label                            | - | - |
| Integrated test example (node.js)      | + | + |
| Mobile web editors                     | - | +* |
| **Plugins & Macros** | **Community Edition** | **Enterprise Edition** |
| Plugins                           | + | + |
| Macros                            | + | + |
| **Collaborative capabilities** | **Community Edition** | **Enterprise Edition** |
| Two co-editing modes              | + | + |
| Comments                          | + | + |
| Built-in chat                     | + | + |
| Review and tracking changes       | + | + |
| Display modes of tracking changes | + | + |
| Version history                   | + | + |
| **Document Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Adding Content control          | + | + | 
| Editing Content control         | + | + | 
| Layout tools                    | + | + |
| Table of contents               | + | + |
| Navigation panel                | + | + |
| Mail Merge                      | + | + |
| Comparing Documents             | + | + |
| **Spreadsheet Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Functions, formulas, equations  | + | + |
| Table templates                 | + | + |
| Pivot tables                    | + | + |
| Data validation           | + | + |
| Conditional formatting          | + | + |
| Sparklines                   | + | + |
| Sheet Views                     | + | + |
| **Presentation Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Transitions                     | + | + |
| Presenter mode                  | + | + |
| Notes                           | + | + |
| **Form creator features** | **Community Edition** | **Enterprise Edition** |
| Adding form fields           | + | + |
| Form preview                    | + | + |
| Saving as PDF                   | + | + |
| | [Get it now](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence#docs-community)  | [Start Free Trial](https://www.onlyoffice.com/download-docs.aspx?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence#docs-enterprise) |

\* If supported by DMS.