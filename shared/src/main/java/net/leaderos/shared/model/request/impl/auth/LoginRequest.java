package net.leaderos.shared.model.request.impl.auth;

import net.leaderos.shared.model.request.PostRequest;

import java.io.IOException;
import java.util.HashMap;

public class LoginRequest extends PostRequest {
    public LoginRequest(String username, String password, String ip, String userAgent) throws IOException {
        super("auth/login", new HashMap<String, String>() {{
            put("username", username);
            put("password", password);
            put("ip", ip);
            put("useragent", userAgent);
        }});
    }
}