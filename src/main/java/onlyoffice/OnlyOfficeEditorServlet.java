package onlyoffice;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.velocity.VelocityUtils;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.inject.Inject;

@Scanned
public class OnlyOfficeEditorServlet extends HttpServlet
{
	@ComponentImport
	private final PluginSettingsFactory pluginSettingsFactory;


	@Inject
	public OnlyOfficeEditorServlet(PluginSettingsFactory pluginSettingsFactory)
	{
		this.pluginSettingsFactory = pluginSettingsFactory;
	}

	private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeEditorServlet");
	private static final long serialVersionUID = 1L;

	private Properties properties;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		if (!AuthContext.checkUserAuthorisation(request, response))
		{
			return;
		}

		PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
		String apiUrl = (String) pluginSettings.get("onlyoffice.apiUrl");
		if (apiUrl == null || apiUrl.isEmpty())
		{
			apiUrl = "";
		}

		ConfigurationManager configurationManager = new ConfigurationManager();
		properties = configurationManager.GetProperties();

		String callbackUrl = "";
		String externalUrl = "";
		String key = "";
		String fileName = "";
		String errorMessage = "";
		ConfluenceUser user = null;

		String attachmentIdString = request.getParameter("attachmentId");
		Long attachmentId;

		try
		{
			attachmentId = Long.parseLong(attachmentIdString);
			log.info("attachmentId " + attachmentId);

			user = AuthenticatedUserThreadLocal.get();
			log.info("user " + user);
			if (AttachmentUtil.checkAccess(attachmentId, user, false))
			{
				key = DocumentManager.getKeyOfFile(attachmentId);

				fileName = AttachmentUtil.getFileName(attachmentId);

				externalUrl = DocumentManager.GetExternalUri(attachmentId, apiUrl + properties.getProperty("files.docservice.url.storage"));

				if (AttachmentUtil.checkAccess(attachmentId, user, true))
				{
					callbackUrl = DocumentManager.getCallbackUrl(attachmentId);
				}
			}
			else
			{
				log.error("access deny");
				errorMessage = "You don not have enough permission to view the file";
			}
		}
		catch (Exception ex)
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			String error = ex.toString() + "\n" + sw.toString();
			log.error(error);
			errorMessage = ex.toString();
		}

		response.setContentType("text/html;charset=UTF-8");
		PrintWriter writer = response.getWriter();

		writer.write(getTemplate(apiUrl, callbackUrl, externalUrl, key, fileName, user, errorMessage));
	}

	private String getTemplate(String apiUrl, String callbackUrl, String fileUrl, String key, String fileName, ConfluenceUser user, String errorMessage)
			throws UnsupportedEncodingException
	{
		Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();

		contextMap.put("docserviceApiUrl", apiUrl + properties.getProperty("files.docservice.url.api"));
		contextMap.put("callbackUrl", callbackUrl);
		contextMap.put("fileUrl", fileUrl);
		contextMap.put("key", key);
		contextMap.put("fileName", fileName);
		contextMap.put("errorMessage", errorMessage);
		if (user != null)
		{
			contextMap.put("userId", user.getName());
			contextMap.put("userName", user.getFullName());
		}

		return VelocityUtils.getRenderedTemplate("templates/editor.vm", contextMap);
	}
}
