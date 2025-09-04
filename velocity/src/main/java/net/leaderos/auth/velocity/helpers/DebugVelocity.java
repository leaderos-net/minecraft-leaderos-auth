package net.leaderos.auth.velocity.helpers;

import net.leaderos.auth.velocity.Velocity;
import net.leaderos.auth.shared.helpers.DebugAPI;
import net.leaderos.auth.shared.enums.DebugMode;

/**
 * Sends debug to console
 */
public class DebugVelocity implements DebugAPI {

    /**
     * Constructor of debug
     */
    public DebugVelocity() {
    }

    /**
     * Sends debug to console
     *
     * @param message to debug
     * @param strict  if true, it will send debug even if debug mode is disabled
     */
    @Override
    public void send(String message, boolean strict) {
        if (Velocity.getInstance().getConfigFile().getSettings().getDebugMode() == DebugMode.ENABLED ||
                (Velocity.getInstance().getConfigFile().getSettings().getDebugMode() == DebugMode.ONLY_ERRORS
                        && strict)) {
            Velocity.getInstance().getLogger().warn(
                    "[DEBUG] " + message);
        }
    }
}
