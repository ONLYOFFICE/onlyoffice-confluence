package onlyoffice.managers.jwt;

import org.json.JSONObject;

import java.io.Serializable;

public interface JwtManager extends Serializable {
    public Boolean jwtEnabled();
    public String createToken(JSONObject payload) throws Exception;
    public Boolean verify(String token);
    public String getJwtHeader();
}
