package onlyoffice;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONArray;

import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.spring.container.ContainerManager;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.inject.Inject;


@Scanned
public class OnlyOfficeConfServlet extends HttpServlet
{
	@ComponentImport
	private final UserManager userManager;
	@ComponentImport
	private final PluginSettingsFactory pluginSettingsFactory;


	@Inject
	public OnlyOfficeConfServlet(UserManager userManager, PluginSettingsFactory pluginSettingsFactory)
	{
		this.userManager = userManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
	}

	private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeConfServlet");
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		String username = userManager.getRemoteUsername(request);
		if (username == null || !userManager.isSystemAdmin(username))
		{
			SettingsManager settingsManager = (SettingsManager) ContainerManager.getComponent("settingsManager");
			String baseUrl = settingsManager.getGlobalSettings().getBaseUrl();
			response.sendRedirect(baseUrl);
			return;
		}

		PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
		String apiUrl = (String) pluginSettings.get("onlyoffice.apiUrl");
		if (apiUrl == null || apiUrl.isEmpty())
		{
			apiUrl = "";
		}

		response.setContentType("text/html;charset=UTF-8");
		PrintWriter writer = response.getWriter();

		writer.write(getTemplate(apiUrl));
	}
	
	private String getTemplate(String apiUrl)
			throws UnsupportedEncodingException
	{
		Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();

		contextMap.put("docserviceApiUrl", apiUrl);

		return VelocityUtils.getRenderedTemplate("templates/configure.vm", contextMap);
	}


	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		String username = userManager.getRemoteUsername(request);
		if (username == null || !userManager.isSystemAdmin(username))
		{
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
		String body = getBody(request.getInputStream());
		if (body.isEmpty())
		{
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String apiUrl;
		try
		{
			JSONObject jsonObj = new JSONObject(body);

			apiUrl = jsonObj.getString("apiUrl");
			if (!apiUrl.endsWith("/")) {
				apiUrl += "/";
			}
		}
		catch (Exception ex)
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			String error = ex.toString() + "\n" + sw.toString();
			log.error(error);

			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
		pluginSettings.put("onlyoffice.apiUrl", apiUrl);

		return;
	}

	private String getBody(InputStream stream)
	{
		Scanner scanner = null;
		Scanner scannerUseDelimiter = null;
		try
		{
			scanner = new Scanner(stream);
			scannerUseDelimiter = scanner.useDelimiter("\\A");
			return scanner.hasNext() ? scanner.next() : "";
		}
		finally
		{
			scannerUseDelimiter.close();
			scanner.close();
		}
	}
}
