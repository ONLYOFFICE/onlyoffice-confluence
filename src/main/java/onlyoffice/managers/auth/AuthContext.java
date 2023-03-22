package onlyoffice.managers.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface AuthContext {
    boolean checkUserAuthorisation(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
