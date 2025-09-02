package net.leaderos.shared.model.request.impl.auth;

import net.leaderos.shared.model.request.PostRequest;

import java.io.IOException;
import java.util.HashMap;

public class RegisterRequest extends PostRequest {
    public RegisterRequest(String username, String password, String email, String ip, String userAgent) throws IOException {
        super("auth/register", new HashMap<String, String>() {{
            put("username", username);
            put("password", password);
            put("ip", ip);
            put("useragent", userAgent);

            if (email != null && !email.isEmpty()) {
                put("email", email);
            }
        }});
    }
}