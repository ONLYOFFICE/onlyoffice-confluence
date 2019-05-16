package onlyoffice;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.languages.LocaleManager;
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
	@ComponentImport
	private final LocaleManager localeManager;

	private final JwtManager jwtManager;

	@Inject
	public OnlyOfficeEditorServlet(PluginSettingsFactory pluginSettingsFactory, LocaleManager localeManager)
	{
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.jwtManager = new JwtManager(pluginSettingsFactory);
		this.localeManager = localeManager;
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

				externalUrl = DocumentManager.GetUri(attachmentId);

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
		Map<String, Object> defaults = MacroUtils.defaultVelocityContext();
		Map<String, String> config = new HashMap<String, String>();

		config.put("docserviceApiUrl", apiUrl + properties.getProperty("files.docservice.url.api"));
		config.put("callbackUrl", callbackUrl);
		config.put("fileUrl", fileUrl);
		config.put("key", key);
		config.put("fileName", fileName);
		config.put("errorMessage", errorMessage);
		config.put("lang", localeManager.getLocale(user).toLanguageTag());

		if (user != null)
		{
			config.put("userId", user.getName());
			config.put("userName", user.getFullName());
		}

		if (jwtManager.jwtEnabled()) {
			JSONObject responseJson = new JSONObject(config);
			try {
				config.put("token", jwtManager.createToken(responseJson));
			} catch (Exception ex) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				ex.printStackTrace(pw);
				String error = ex.toString() + "\n" + sw.toString();
				log.error(error);
			}
		}

		defaults.putAll(config);
		return VelocityUtils.getRenderedTemplate("templates/editor.vm", defaults);
	}
}
