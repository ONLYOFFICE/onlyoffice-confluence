# ONLYOFFICE app for Confluence

This app allows users to create, view, and edit office documents directly within Confluence using [ONLYOFFICE Docs](https://www.onlyoffice.com/docs).

<p align="center">
  <a href="https://www.onlyoffice.com/office-for-confluence">
    <img width="800" src="https://static-site.onlyoffice.com/public/images/templates/office-for-confluence/hero/hero@2x.png" alt="ONLYOFFICE Docs for Confluence">
  </a>
</p>

## Features ✨ 

The app allows to:

* Create and edit text documents, spreadsheets, presentations, and PDFs.
* Share documents with other users.
* Co-edit documents in real time using two co-editing modes (Fast and Strict), Track Changes, comments, and built-in chat.

### Supported formats 📁

**For viewing:**
* **WORD**: DOC, DOCM, DOCX, DOT, DOTM, DOTX, EPUB, FB2, FODT, HTM, HTML, HWP, HWPX, MD, MHT, MHTML, ODT, OTT, PAGES, RTF, STW, SXW, TXT, WPS, WPT, XML
* **CELL**: CSV, ET, ETT, FODS, NUMBERS, ODS, OTS, SXC, XLS, XLSM, XLSX, XLT, XLTM, XLTX
* **SLIDE**: DPS, DPT, FODP, KEY, ODG, ODP, OTP, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX, SXI
* **PDF**: DJVU, DOCXF, OFORM, OXPS, PDF, XPS
* **DIAGRAM**: VSDM, VSDX, VSSM, VSSX, VSTM, VSTX

**For editing:**
* **WORD**: DOCM, DOCX, DOTM, DOTX
* **CELL**: XLSB, XLSM, XLSX, XLTM, XLTX
* **SLIDE**: POTM, POTX, PPSM, PPSX, PPTM, PPTX
* **PDF**: PDF

**For editing with possible loss of information:**
* **WORD**: EPUB, FB2, HTML, ODT, OTT, RTF, TXT
* **CELL**: CSV, ODS, OTS
* **SLIDE**: ODP, OTP

**For converting to Office Open XML formats:**
* **WORD**: DOC, DOCM, DOCX, DOT, DOTM, DOTX, EPUB, FB2, FODT, HTM, HTML, HWP, HWPX, MD, MHT, MHT, MHTML, ODT, OTT, PAGES, RTF, STW, SXW, TXT, WPS, WPT, XML
* **CELL**: CSV, ET, ETT, FODS, NUMBERS, ODS, OTS, SXC, XLS, XLSB, XLSM, XLSX, XLT, XLTM, XLTX
* **SLIDE**: DPS, DPT, FODP, KEY, ODG, ODP, OTP, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX, SXI
* **PDF**: DOCXF, OXPS, PDF, XPS

## Installing ONLYOFFICE Docs

Ensure ONLYOFFICE Docs is running and accessible to both Confluence and users’ browsers. The server must also be able to POST updates back to Confluence.

### 🖥️ Self-Hosted Version

- **Community Edition (Free):** [Docker guide](https://github.com/ONLYOFFICE/Docker-DocumentServer) or [manual installation](https://helpcenter.onlyoffice.com/installation/docs-community-install-ubuntu.aspx)
- **Enterprise Edition:** [Installation guide](https://helpcenter.onlyoffice.com/docs/installation/enterprise)

Community Edition vs Enterprise Edition comparison can be found [here](#onlyoffice-docs-editions).

### ☁️ Cloud

Use **ONLYOFFICE Docs Cloud** if you prefer not to maintain your own server — no installation or configuration required.  

👉 [Get started here](https://www.onlyoffice.com/docs-registration)

## Installing ONLYOFFICE app for Confluence

Upload the compiled ***target/onlyoffice-confluence-plugin.jar*** to Confluence on the `Manage add-ons` page.

The latest compiled package files are available [here](https://github.com/onlyoffice/onlyoffice-confluence/releases) and on [Atlassian Marketplace](https://marketplace.atlassian.com/apps/1218214/onlyoffice-connector-for-confluence).

You could also install the app from Confluence administration panel:

1. Navigate to `Manage add-ons` page.
2. Click **Find new apps** or **Find new add-ons** on the left panel.
3. Locate **ONLYOFFICE Connector for Confluence** using search.
4. Click **Install** to download and install the app.

## Configuring ONLYOFFICE app for Confluence ⚙️

Find the uploaded **ONLYOFFICE Connector** on the `Manage add-ons` page. Click `Configure` and enter the name of the server with ONLYOFFICE Docs installed:

```
http://documentserver/
```

Configuration settings include JWT, enabled by default to protect the editors from unauthorized access. If setting a custom **secret key**, ensure it matches the one in the ONLYOFFICE Docs [config file](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) for proper validation.

## Compiling ONLYOFFICE app for Confluence 🛠️

You will need:

* 1.8.X of the Oracle Java SE Development Kit 8
* Atlassian Plugin SDK

Get a submodule:

  ```bash
  git submodule update --init --recursive
  ```

Compile package:

  ```bash
  atlas-package
  ```

## How it works

The ONLYOFFICE app follows the API documented [here](https://api.onlyoffice.com/docs/docs-api/get-started/basic-concepts/):

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

* Community Edition 🆓 (`onlyoffice-documentserver` package)
* Enterprise Edition 🏢 (`onlyoffice-documentserver-ee` package)

The table below will help you to make the right choice.

| Pricing and licensing | Community Edition | Enterprise Edition |
| ------------- | ------------- | ------------- |
| | [Get it now](https://www.onlyoffice.com/download-community?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence#docs-community)  | [Start Free Trial](https://www.onlyoffice.com/download?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence#docs-enterprise)  |
| Cost  | FREE  | [Go to the pricing page](https://www.onlyoffice.com/docs-enterprise-prices?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence)  |
| Simultaneous connections | up to 20 maximum  | As in chosen pricing plan |
| Number of users | up to 20 recommended | As in chosen pricing plan |
| License | GNU AGPL v.3 | Proprietary |
| **Support** | **Community Edition** | **Enterprise Edition** |
| Documentation | [Help Center](https://helpcenter.onlyoffice.com/docs/installation/community) | [Help Center](https://helpcenter.onlyoffice.com/docs/installation/enterprise) |
| Standard support | [GitHub](https://github.com/ONLYOFFICE/DocumentServer/issues) or paid | 1 or 3 years support included |
| Premium support | [Contact us](mailto:sales@onlyoffice.com) | [Contact us](mailto:sales@onlyoffice.com) |
| **Services** | **Community Edition** | **Enterprise Edition** |
| Conversion Service                | + | + |
| Document Builder Service          | + | + |
| **Interface** | **Community Edition** | **Enterprise Edition** |
| Tabbed interface                  | + | + |
| Dark theme                        | + | + |
| 125%, 150%, 175%, 200% scaling    | + | + |
| White Label                       | - | - |
| Integrated test example (node.js) | + | + |
| Mobile web editors                | - | +* |
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
| Data validation                 | + | + |
| Conditional formatting          | + | + |
| Sparklines                      | + | + |
| Sheet Views                     | + | + |
| **Presentation Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Transitions                     | + | + |
| Animations                      | + | + |
| Presenter mode                  | + | + |
| Notes                           | + | + |
| **Form creator features** | **Community Edition** | **Enterprise Edition** |
| Adding form fields              | + | + |
| Form preview                    | + | + |
| Saving as PDF                   | + | + |
| **PDF Editor features**      | **Community Edition** | **Enterprise Edition** |
| Text editing and co-editing                                | + | + |
| Work with pages (adding, deleting, rotating)               | + | + |
| Inserting objects (shapes, images, hyperlinks, etc.)       | + | + |
| Text annotations (highlight, underline, cross out, stamps) | + | + |
| Comments                        | + | + |
| Freehand drawings               | + | + |
| Form filling                    | + | + |
| | [Get it now](https://www.onlyoffice.com/download-community?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence#docs-community)  | [Start Free Trial](https://www.onlyoffice.com/download?utm_source=github&utm_medium=cpc&utm_campaign=GitHubConfluence#docs-enterprise)  |

\* If supported by DMS.

## Need help? User Feedback and Support 💡

* **🐞 Found a bug?** Please report it by creating an [issue](https://github.com/ONLYOFFICE/onlyoffice-confluence/issues).
* **❓ Have a question?** Ask our community and developers on the [ONLYOFFICE Forum](https://community.onlyoffice.com).
* **👨‍💻 Need help for developers?** Check our [API documentation](https://api.onlyoffice.com).
* **💡 Want to suggest a feature?** Share your ideas on our [feedback platform](https://feedback.onlyoffice.com/forums/966080-your-voice-matters).