package onlyoffice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
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
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.spring.container.ContainerManager;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.inject.Inject;

/*
    Copyright (c) Ascensio System SIA 2020. All rights reserved.
    http://www.onlyoffice.com
*/

public class OnlyOfficeSaveFileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeSaveFileServlet");

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private final JwtManager jwtManager;
    private final PluginSettings settings;

    @Inject
    public OnlyOfficeSaveFileServlet(PluginSettingsFactory pluginSettingsFactory, JwtManager jwtManager) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        settings = pluginSettingsFactory.createGlobalSettings();
        this.jwtManager = jwtManager;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String vkey = request.getParameter("vkey");
        log.info("vkey = " + vkey);
        String attachmentIdString = DocumentManager.ReadHash(vkey);

        Long attachmentId = Long.parseLong(attachmentIdString);
        log.info("attachmentId " + attachmentId);

        String contentType = AttachmentUtil.getMediaType(attachmentId);
        response.setContentType(contentType);

        InputStream inputStream = AttachmentUtil.getAttachmentData(attachmentId);
        response.setContentLength(inputStream.available());

        byte[] buffer = new byte[10240];

        OutputStream output = response.getOutputStream();
        for (int length = 0; (length = inputStream.read(buffer)) > 0;) {
            output.write(buffer, 0, length);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain; charset=utf-8");

        String vkey = request.getParameter("vkey");
        log.info("vkey = " + vkey);
        String attachmentIdString = DocumentManager.ReadHash(vkey);

        String error = "";
        try {
            processData(attachmentIdString, request);
        } catch (Exception e) {
            error = e.getMessage();
        }

        PrintWriter writer = response.getWriter();
        if (error.isEmpty()) {
            writer.write("{\"error\":0}");
        } else {
            response.setStatus(500);
            writer.write("{\"error\":1,\"message\":\"" + error + "\"}");
        }

        log.info("error = " + error);
    }

    private void processData(String attachmentIdString, HttpServletRequest request) throws Exception {
        log.info("attachmentId = " + attachmentIdString);
        InputStream requestStream = request.getInputStream();
        if (attachmentIdString.isEmpty()) {
            throw new IllegalArgumentException("attachmentId is empty");
        }

        HttpURLConnection connection = null;
        try {
            Long attachmentId = Long.parseLong(attachmentIdString);

            String body = getBody(requestStream);
            log.info("body = " + body);
            if (body.isEmpty()) {
                throw new IllegalArgumentException("requestBody is empty");
            }

            JSONObject jsonObj = new JSONObject(body);

            if (jwtManager.jwtEnabled()) {
                String token = jsonObj.optString("token");
                Boolean inBody = true;

                if (token == null || token == "") {
                    String jwth = (String) settings.get("onlyoffice.jwtHeader");
                    String header = (String) request.getHeader(jwth == null || jwth.isEmpty() ? "Authorization" : jwth);
                    token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : header;
                    inBody = false;
                }

                if (token == null || token == "") {
                    throw new SecurityException("Try save without JWT");
                }

                if (!jwtManager.verify(token)) {
                    throw new SecurityException("Try save with wrong JWT");
                }

                JSONObject bodyFromToken = new JSONObject(
                        new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"));

                if (inBody) {
                    jsonObj = bodyFromToken;
                } else {
                    jsonObj = bodyFromToken.getJSONObject("payload");
                }
            }

            long status = jsonObj.getLong("status");
            log.info("status = " + status);

            // MustSave, Corrupted
            if (status == 2 || status == 3) {
                ConfluenceUser user = null;
                JSONArray users = jsonObj.getJSONArray("users");
                if (users.length() > 0) {
                    String userName = users.getString(0);

                    UserAccessor userAccessor = (UserAccessor) ContainerManager.getComponent("userAccessor");
                    user = userAccessor.getUserByName(userName);
                    log.info("user = " + user);
                }

                if (user == null || !AttachmentUtil.checkAccess(attachmentId, user, true)) {
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
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);

            throw ex;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String getBody(InputStream stream) {
        Scanner scanner = null;
        Scanner scannerUseDelimiter = null;
        try {
            scanner = new Scanner(stream);
            scannerUseDelimiter = scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } finally {
            scannerUseDelimiter.close();
            scanner.close();
        }
    }
}
