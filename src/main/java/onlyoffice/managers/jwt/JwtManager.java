package onlyoffice.managers.jwt;

import org.json.JSONObject;

import java.io.Serializable;

public interface JwtManager extends Serializable {
    Boolean jwtEnabled();
    String createToken(JSONObject payload) throws Exception;
    Boolean verify(String token);
    String getJwtHeader();
}
