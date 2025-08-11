package net.leaderos.shared.model.request.impl.auth;

import net.leaderos.shared.model.request.PostRequest;

import java.io.IOException;
import java.util.HashMap;

public class SessionRequest extends PostRequest {
    public SessionRequest(String username, String ip) throws IOException {
        super("auth/game-sessions", new HashMap<String, String>() {{
            put("username", username);
            put("ip", ip);
        }});
    }
}