package net.leaderos.auth.bukkit.listener;

import lombok.RequiredArgsConstructor;
import net.leaderos.auth.bukkit.Bukkit;
import net.leaderos.auth.bukkit.helpers.BossBarUtil;
import net.leaderos.auth.bukkit.helpers.ChatUtil;
import net.leaderos.auth.bukkit.helpers.TitleUtil;
import net.leaderos.auth.shared.Shared;
import net.leaderos.auth.shared.enums.ErrorCode;
import net.leaderos.auth.shared.enums.SessionState;
import net.leaderos.auth.shared.helpers.AuthUtil;
import net.leaderos.auth.shared.helpers.Placeholder;
import net.leaderos.auth.shared.helpers.UserAgentUtil;
import net.leaderos.auth.shared.model.response.GameSessionResponse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class ConnectionListener implements Listener {

    private final Bukkit plugin;

    @EventHandler(ignoreCancelled = true)
    public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        String playerName = event.getName();

        if (plugin.getServer().getPlayerExact(playerName) != null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "You are already connected to this server!");
            return;
        }

        try {
            String ip = event.getAddress().getHostAddress();

            // Make API request to get user game session
            Shared.getDebugAPI().send("Making API request for player " + playerName, false);
            String userAgent = UserAgentUtil.generateUserAgent(!plugin.getConfigFile().getSettings().isSession());

            GameSessionResponse session = AuthUtil.checkGameSession(playerName, ip, userAgent).join();
            plugin.getSessions().put(playerName, session);

            // If the session response status is false, handle errors
            if (!session.isStatus()) {
                // Kick the player if they have an invalid username
                if (session.getError() == ErrorCode.INVALID_USERNAME) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, String.join("\n",
                            ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickInvalidUsername(),
                                    new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))));

                    plugin.getSessions().remove(playerName);
                    return;
                }

                // Kick the player with a generic error message for other errors
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, String.join("\n",
                        ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickAnError(),
                                new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))));

                plugin.getSessions().remove(playerName);
                return;
            }

            // Kick the player if their username case does not match
            if (session.getUsername() != null && !session.getUsername().equals(playerName)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, String.join("\n",
                        ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickUsernameCaseMismatch(),
                                new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()),
                                new Placeholder("{valid}", session.getUsername()),
                                new Placeholder("{invalid}", playerName)
                        )));

                plugin.getSessions().remove(playerName);
                return;
            }

            // Check email verification status
            if (session.getState() == SessionState.EMAIL_NOT_VERIFIED) {
                // Kick the player if their email is not verified and kicking is enabled
                if (plugin.getConfigFile().getSettings().getEmailVerification().isKickNonVerified()) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, String.join("\n",
                            ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickEmailNotVerified(),
                                    new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))));

                    plugin.getSessions().remove(playerName);
                    return;
                } else {
                    // If email verification is disabled, set status to LOGIN_REQUIRED
                    session.setState(SessionState.LOGIN_REQUIRED);
                }
            }

            // Kick the player if they are not registered and kicking is enabled
            if (plugin.getConfigFile().getSettings().isKickNonRegistered() && session.getState() == SessionState.REGISTER_REQUIRED) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, String.join("\n",
                        ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickNotRegistered(),
                                new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))));

                plugin.getSessions().remove(playerName);
                return;
            }

            // If the player is already authenticated, allow them to join directly
            if (session.getState() == SessionState.HAS_SESSION && plugin.getConfigFile().getSettings().isSession()) {
                session.setState(SessionState.AUTHENTICATED);
                session.setToken(session.getToken());
                Shared.getDebugAPI().send("Player " + playerName + " has active session, allowing direct login.", false);
                ChatUtil.sendConsoleInfo(playerName + " has active session, allowing direct login.");
            }
        } catch (Exception e) {
            Shared.getDebugAPI().send("ErrorCode processing player " + playerName + ": " + e.getMessage(), true);

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, String.join("\n",
                    ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickAnError(),
                            new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))));

            plugin.getSessions().remove(playerName);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getSessions().remove(player.getName());

        // Clear title
        if (plugin.getConfigFile().getSettings().isShowTitle()) {
            TitleUtil.clearTitle(player);
        }

        // Clear boss bar
        if (plugin.getConfigFile().getSettings().getBossBar().isEnabled()) {
            BossBarUtil.hideBossBar(player);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event) {
        // Especially for offline CraftBukkit, we need to catch players being kicked because of
        // "logged in from another location" and to cancel their kick
        if (event.getReason().contains("You logged in from another location")) {
            event.setCancelled(true);
        }
    }

}
