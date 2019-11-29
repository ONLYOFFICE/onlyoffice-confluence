package onlyoffice;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.atlassian.confluence.util.GeneralUtil;

/*
    Copyright (c) Ascensio System SIA 2019. All rights reserved.
    http://www.onlyoffice.com
*/

public class AuthContext {
    private static final Logger log = LogManager.getLogger("onlyoffice.AuthContext");

    public static boolean checkUserAuthorisation(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            log.error("User is not authenticated");
            String fullUrl = getLoginUrl(request);
            response.sendRedirect(fullUrl);

            return false;
        }
        log.info("principal name = " + principal.getName());
        return true;
    }

    private static String getLoginUrl(HttpServletRequest request) throws IOException {
        StringBuilder stringBuilder = new StringBuilder(request.getContextPath());
        String fullUrl = stringBuilder.append("/login.action?permissionViolation=true&os_destination=")
                .append("plugins%2Fservlet%2Fonlyoffice%2Fdoceditor").append("?")
                .append(GeneralUtil.urlEncode(request.getQueryString())).toString();
        return fullUrl;
    }
}
