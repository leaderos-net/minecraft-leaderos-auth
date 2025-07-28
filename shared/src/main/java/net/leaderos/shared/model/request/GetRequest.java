package net.leaderos.shared.model.request;

import net.leaderos.shared.model.Request;

import java.io.IOException;

/**
 * GetRequest class extended with Request
 */
public class GetRequest extends Request {

    /**
     * Request constructor
     *
     * @param api of request
     * @throws IOException for HttpUrlConnection
     */
    public GetRequest(String api) throws IOException {
        super(api, null, RequestType.GET);
    }
}
