package onlyoffice.managers.jwt;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Map;

public interface JwtManager extends Serializable {

    String createToken(Object payload) throws Exception;

    String createToken(JSONObject payload) throws Exception;

    String verify(String token);

    String createInternalToken(Map<String, ?> payloadMap);

    String verifyInternalToken(String token);

    Boolean jwtEnabled();

    String getJwtHeader();
}
