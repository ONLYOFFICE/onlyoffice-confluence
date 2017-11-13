package onlyoffice;


import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.commons.codec.binary.Hex;

import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.spring.container.ContainerManager;

public class DocumentManager
{
	private static final Logger log = LogManager.getLogger("onlyoffice.DocumentManager");
	private static final Map<String, String> CacheMap = new HashMap<String, String>();
	private static final String callbackServler = "/plugins/servlet/onlyoffice/save";
	
	public static long GetMaxFileSize()
	{
		long size;
		try
		{
			ConfigurationManager configurationManager = new ConfigurationManager();
			Properties properties = configurationManager.GetProperties();
			String filesizeMax = properties.getProperty("filesize-max");
			size = Long.parseLong(filesizeMax);
		}
		catch (Exception ex)
		{
			size = 0;
		}

		return size > 0 ? size : 5 * 1024 * 1024;
	}

	public static List<String> GetEditedExts()
	{
		try
		{
			ConfigurationManager configurationManager = new ConfigurationManager();
			Properties properties = configurationManager.GetProperties();
			String exts = properties.getProperty("files.docservice.edited-docs");

			return Arrays.asList(exts.split("\\|"));
		}
		catch (IOException e)
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			log.error(e.toString() + "\n" + sw.toString());
			return new ArrayList<String>();
		}
	}

	public static String GetExternalUri(Long attachmentId, String documentStorageUrl) throws Exception
	{
		String key =  getKeyOfFile(attachmentId);
		String externalUri;

		if (CacheMap.containsKey(key))
		{
			externalUri = CacheMap.get(key);
			log.info("externalUri from cache " + externalUri);
			return externalUri;
		}

		InputStream inputStream = AttachmentUtil.getAttachmentData(attachmentId);
		String contentType = AttachmentUtil.getMediaType(attachmentId);

		externalUri = ServiceConverter.GetExternalUri(inputStream, inputStream.available(), contentType, key, documentStorageUrl);

		CacheMap.put(key, externalUri);
		log.info("externalUri " + externalUri);

		return externalUri;
	}

	public static String getKeyOfFile(Long attachmentId)
	{
		String hashCode = AttachmentUtil.getHashCode(attachmentId);

		return GenerateRevisionId(hashCode);
	}

	private static String GenerateRevisionId(String expectedKey)
	{
		if (expectedKey.length() > 20)
		{
			expectedKey = Integer.toString(expectedKey.hashCode());
		}
		String key = expectedKey.replace("[^0-9-.a-zA-Z_=]", "_");
		key = key.substring(0, Math.min(key.length(), 20));
		log.info("key = " + key);
		return key;
	}

	public static String getCallbackUrl(Long attachmentId)
	{
		SettingsManager settingsManager = (SettingsManager) ContainerManager.getComponent("settingsManager");
		String baseUrl = settingsManager.getGlobalSettings().getBaseUrl();

		String hash = CreateHash(Long.toString(attachmentId));

		String callbackUrl = baseUrl + callbackServler + "?vkey=" + GeneralUtil.urlEncode(hash);
		log.info("callbackUrl " + callbackUrl);

		return callbackUrl;
	}

	public static String CreateHash(String str)
	{
		try
		{
			ConfigurationManager configurationManager = new ConfigurationManager();
			Properties properties = configurationManager.GetProperties();
			String secret = properties.getProperty("files.docservice.secret");
	
			String payload = GetHashHex(str + secret) + "?" + str;

			String base64 = Base64.getEncoder().encodeToString(payload.getBytes("UTF-8"));	
			return base64;
		}
		catch (Exception ex)
		{
			log.error(ex);
		}
		return "";
	}

	public static String ReadHash(String base64)
	{
		try
		{
			String str = new String(Base64.getDecoder().decode(base64), "UTF-8");

			ConfigurationManager configurationManager = new ConfigurationManager();
			Properties properties = configurationManager.GetProperties();
			String secret = properties.getProperty("files.docservice.secret");

			String[] payloadParts = str.split("\\?");

			String payload = GetHashHex(payloadParts[1] + secret);
			if (payload.equals(payloadParts[0]))
			{
				return payloadParts[1];
			}
		} catch (Exception ex)
		{
			log.error(ex);
		}
		return "";
	}

	private static String GetHashHex(String str)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(str.getBytes());
			String hex = Hex.encodeHexString(digest);

			return hex;
		} catch (Exception ex)
		{
			log.error(ex);
		}
		return "";
	}
}
