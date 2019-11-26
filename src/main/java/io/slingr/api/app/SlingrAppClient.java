package io.slingr.api.app;

import io.slingr.api.common.Json;
import io.slingr.api.common.RestException;
import io.slingr.api.common.RestMethod;
import io.slingr.api.common.SimpleRestClient;

import javax.ws.rs.client.WebTarget;
import java.util.Map;

public class SlingrAppClient extends SimpleRestClient {
    private static final String DOMAIN = ".slingrs.io";

    private String email;
    private String password;
    private String token;

    private SlingrAppClient(String apiUri) throws RestException {
        super(apiUri);
    }

    public static SlingrAppClient getInstance(String appName, Environment env, String email, String password) {
        String uri = "https://"+appName+DOMAIN+"/"+env.getPath()+"/runtime/api";
        SlingrAppClient client = new SlingrAppClient(uri);
        client.email = email;
        client.password = password;
        client.login();
        client.setupDefaultHeader("Content-Type", "application/json");
        return client;
    }

    public void login() throws RestException {
        Json credentials = Json.map()
                .set("email", email)
                .set("password", password);
        Json res = post("/auth/login", credentials);
        this.token = res.string("token");
        setupDefaultHeader("token", this.token);
    }

    @Override
    protected Json execute(RestMethod method, WebTarget target, Object content, Map<String, Object> additionalHeaders) throws RestException {
        try {
            return super.execute(method, target, content, additionalHeaders);
        } catch (RestException re) {
            if (re.getStatusCode() == 401) {
                login();
                return super.execute(method, target, content, additionalHeaders);
            } else {
                throw re;
            }
        }
    }
}
