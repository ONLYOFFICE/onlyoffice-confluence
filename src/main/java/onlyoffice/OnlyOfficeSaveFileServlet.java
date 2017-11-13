package onlyoffice;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONArray;

import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.spring.container.ContainerManager;

public class OnlyOfficeSaveFileServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeSaveFileServlet");

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		response.setContentType("text/plain; charset=utf-8");

		String vkey = request.getParameter("vkey");
		log.info("vkey = " + vkey);
		String attachmentIdString = DocumentManager.ReadHash(vkey);

		boolean result = processData(attachmentIdString, request.getInputStream());
		String error = "1";
		if (result)
		{
			error = "0";
		}

		PrintWriter writer = response.getWriter();
		writer.write("{\"error\":" + error + "}");
		log.info("error = " + error);
	}

	private boolean processData(String attachmentIdString, InputStream requestStream)
			throws IOException
	{
		log.info("attachmentId = " + attachmentIdString);
		if (attachmentIdString.isEmpty())
		{
			return false;
		}

		HttpURLConnection connection = null;
		try
		{
			Long attachmentId = Long.parseLong(attachmentIdString);

			String body = getBody(requestStream);
			log.info("body = " + body);
			if (body.isEmpty())
			{
				return false;
			}

			JSONObject jsonObj = new JSONObject(body);

			long status = jsonObj.getLong("status");
			log.info("status = " + status);

			// MustSave, Corrupted
			if (status == 2 || status == 3)
			{
				ConfluenceUser user = null;
				JSONArray users = jsonObj.getJSONArray("users");
				if (users.length() > 0)
				{ 
					String userName = users.getString(0);

					UserAccessor userAccessor = (UserAccessor) ContainerManager.getComponent("userAccessor");
					user = userAccessor.getUserByName(userName);
					log.info("user = " + user);
				}

				if (user == null || !AttachmentUtil.checkAccess(attachmentId, user, true))
				{
					throw new SecurityException("Try save without access: " + user);
				}

				String downloadUrl = jsonObj.getString("url");
				log.info("downloadUri = " + downloadUrl);

				URL url = new URL(downloadUrl);

				connection = (HttpURLConnection) url.openConnection();
				int size = connection.getContentLength();
				log.info("size = " + size);

				InputStream stream = connection.getInputStream();

				AttachmentUtil.saveAttachment(attachmentId, stream, size, user);
			}

			return true;
		}
		catch (Exception ex)
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			String error = ex.toString() + "\n" + sw.toString();
			log.error(error);

			return false;
		}
		finally
		{
			if (connection != null)
			{
				connection.disconnect();
			}
		}
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
