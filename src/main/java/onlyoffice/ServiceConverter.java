package onlyoffice;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class ServiceConverter
{
	private static final MessageFormat ConvertParams = new MessageFormat("?url={0}&outputtype={1}&filetype={2}&title={3}&key={4}");
	
	public static String GetExternalUri(InputStream fileStream, long contentLength, String contentType, String documentRevisionId, String documentStorageUrl)
			throws IOException, Exception
	{
        Object[] args = {"", "", "", "", documentRevisionId};
        
        String urlTostorage = documentStorageUrl + ConvertParams.format(args);

        URL url = new URL(urlTostorage);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false); 
        connection.setRequestMethod("POST"); 
        connection.setRequestProperty("Content-Type", contentType == null ? "application/octet-stream" : contentType);
        connection.setRequestProperty("charset", StandardCharsets.UTF_8.name());
        connection.setRequestProperty("Content-Length", Long.toString(contentLength));
        connection.setUseCaches(false);

        DataOutputStream dataOutputStream = null;
        try {
        	dataOutputStream = new DataOutputStream(connection.getOutputStream());
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = fileStream.read(bytes)) != -1) {
                dataOutputStream.write(bytes, 0, read);
            }
            
            dataOutputStream.flush();
        } finally {
        	if(dataOutputStream != null) {
        	    dataOutputStream.close();
        	}
        }
        
        InputStream stream = connection.getInputStream();

        if (stream == null) {
            throw new Exception("Could not get an answer");
        }
        
        String xml = ConvertStreamToString(stream);

        connection.disconnect();

        Pair<Integer, String> res = GetResponseUri(xml);
        
        return res.getValue();
    }
	
	private static Pair<Integer, String> GetResponseUri(String xml)
			throws Exception
	{
        Document document = ConvertStringToXmlDocument(xml);
        
        Element responceFromConvertService = document.getDocumentElement();
        if (responceFromConvertService == null)
            throw new Exception("Invalid answer format");

        NodeList errorElement = responceFromConvertService.getElementsByTagName("Error");
        if (errorElement != null && errorElement.getLength() > 0)
        	ProcessConvertServiceResponseError(Integer.parseInt(errorElement.item(0).getTextContent()));

        NodeList endConvert = responceFromConvertService.getElementsByTagName("EndConvert");
        if (endConvert == null || endConvert.getLength() == 0)
            throw new Exception("Invalid answer format");
        
        Boolean isEndConvert = Boolean.parseBoolean(endConvert.item(0).getTextContent());

        int resultPercent = 0;
        String responseUri = null;
        
        if (isEndConvert) {
            NodeList fileUrl = responceFromConvertService.getElementsByTagName("FileUrl");
            if (fileUrl == null || endConvert.getLength() == 0)
                throw new Exception("Invalid answer format");

            resultPercent = 100;
            responseUri = fileUrl.item(0).getTextContent();
        } else {
            NodeList percent = responceFromConvertService.getElementsByTagName("Percent");
            if (percent != null && percent.getLength() > 0)
                resultPercent = Integer.parseInt(percent.item(0).getTextContent());
            
            resultPercent = resultPercent >= 100 ? 99 : resultPercent;
        }
        
        return Pair.of(resultPercent, responseUri);
    }
	
    private static String ConvertStreamToString(InputStream stream)
    		throws IOException
    {
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line = bufferedReader.readLine();

        while(line != null) {
            stringBuilder.append(line);
            line =bufferedReader.readLine();
        }

        String result = stringBuilder.toString();
        
        return result;
    }
    
    private static void ProcessConvertServiceResponseError(int errorCode)
    		throws Exception
    {
        String errorMessage = "";
        String errorMessageTemplate = "Error occurred in the ConvertService: ";

        switch (errorCode) {
            case -8:
                errorMessage = errorMessageTemplate + "Error document VKey";
                break;
            case -7:
                errorMessage = errorMessageTemplate + "Error document request";
                break;
            case -6:
                errorMessage = errorMessageTemplate + "Error database";
                break;
            case -5:
                errorMessage = errorMessageTemplate + "Error unexpected guid";
                break;
            case -4:
                errorMessage = errorMessageTemplate + "Error download error";
                break;
            case -3:
                errorMessage = errorMessageTemplate + "Error convertation error";
                break;
            case -2:
                errorMessage = errorMessageTemplate + "Error convertation timeout";
                break;
            case -1:
                errorMessage = errorMessageTemplate + "Error convertation unknown";
                break;
            case 0:
                break;
            default:
                errorMessage = "ErrorCode = " + errorCode;
                break;
        }

        throw new Exception(errorMessage);
    }
    
    private static Document ConvertStringToXmlDocument(String xml)
    		throws IOException, ParserConfigurationException, SAXException
    {
        DocumentBuilderFactory documentBuildFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder doccumentBuilder = documentBuildFactory.newDocumentBuilder();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        InputSource inputSource = new InputSource(inputStream);
        Document document = doccumentBuilder.parse(inputSource);
        
        return document;
    }
}
