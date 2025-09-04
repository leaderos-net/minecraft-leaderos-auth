package net.leaderos.auth.shared.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.leaderos.auth.shared.enums.ErrorCode;
import org.json.JSONObject;

/**
 * Response class for request
 */
@Setter
@Getter
@AllArgsConstructor
public class Response {

    /**
     * Response code of request
     */
    private int responseCode;

    /**
     * status of response
     */
    private boolean status;

    /**
     * Response message of request
     */
    private JSONObject responseMessage;

    /**
     * ErrorCode
     */
    private ErrorCode error;

    /**
     * Getter of responseMessage
     *
     * @return JSONObject - message
     */
    public JSONObject getResponseMessage() {
        if (status)
            return this.responseMessage;
        else
            return null;
    }
}
