package net.leaderos.auth.bungee.helpers;

import net.leaderos.auth.bungee.Bungee;
import net.leaderos.auth.shared.helpers.DebugAPI;
import net.leaderos.auth.shared.enums.DebugMode;

/**
 * Sends debug to console
 */
public class DebugBungee implements DebugAPI {

    /**
     * Constructor of debug
     */
    public DebugBungee() {
    }

    /**
     * Sends debug to console
     *
     * @param message to debug
     * @param strict  if true, it will send debug even if debug mode is disabled
     */
    @Override
    public void send(String message, boolean strict) {
        if (Bungee.getInstance().getConfigFile().getSettings().getDebugMode() == DebugMode.ENABLED ||
                (Bungee.getInstance().getConfigFile().getSettings().getDebugMode() == DebugMode.ONLY_ERRORS && strict)) {
            Bungee.getInstance().getLogger().warning("[DEBUG] " + message);
        }
    }
}
