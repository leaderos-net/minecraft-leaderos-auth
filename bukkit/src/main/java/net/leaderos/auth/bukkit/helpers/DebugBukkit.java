package net.leaderos.auth.bukkit.helpers;

import net.leaderos.auth.bukkit.Bukkit;
import net.leaderos.auth.shared.helpers.DebugAPI;
import net.leaderos.auth.shared.enums.DebugMode;

/**
 * Sends debug to console
 */
public class DebugBukkit implements DebugAPI {

    /**
     * Constructor of debug
     */
    public DebugBukkit() {
    }

    /**
     * Sends debug to console
     *
     * @param message to debug
     * @param strict  if true, it will send debug even if debug mode is disabled
     */
    @Override
    public void send(String message, boolean strict) {
        if (Bukkit.getInstance().getConfigFile().getSettings().getDebugMode() == DebugMode.ENABLED ||
                (Bukkit.getInstance().getConfigFile().getSettings().getDebugMode() == DebugMode.ONLY_ERRORS
                        && strict)) {
            Bukkit.getInstance().getLogger().warning(
                    "[DEBUG] " + message);
        }
    }
}
