package net.leaderos.shared.model.request.impl.auth;

import net.leaderos.shared.model.request.PostRequest;

import java.io.IOException;
import java.util.HashMap;

public class VerifyTfaRequest extends PostRequest {
    public VerifyTfaRequest(String code, String token) throws IOException {
        super("auth/tfa/verify", new HashMap<String, String>() {{
            put("code", code);
            put("token", token);
        }});
    }
}
