package net.leaderos.auth.shared.model.request;

import net.leaderos.auth.shared.model.Request;

import java.io.IOException;
import java.util.Map;

/**
 * PatchRequest class extended with Request
 */
public class PatchRequest extends Request {

    /**
     * Request constructor
     *
     * @param api  of request
     * @param body of request
     * @throws IOException for HttpUrlConnection
     */
    public PatchRequest(String api, Map<String, String> body) throws IOException {
        super(api, body, RequestType.PATCH);
    }

    /**
     * Request constructor
     *
     * @param api of request
     * @throws IOException for HttpUrlConnection
     */
    public PatchRequest(String api) throws IOException {
        super(api, null, RequestType.PATCH);
    }
}
