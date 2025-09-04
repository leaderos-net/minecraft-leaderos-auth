package net.leaderos.auth.shared.helpers;

/**
 * DebugAPI for requests
 */
public interface DebugAPI {

    /**
     * Sends debug to console
     *
     * @param message to debug
     * @param strict  if true, it will send debug message even if debug mode is
     *                disabled
     */
    void send(String message, boolean strict);

}
