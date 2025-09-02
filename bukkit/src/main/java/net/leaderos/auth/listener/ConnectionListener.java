package net.leaderos.auth.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import net.leaderos.auth.Bukkit;
import net.leaderos.auth.helpers.ChatUtil;
import net.leaderos.shared.Shared;
import net.leaderos.shared.enums.AuthResponse;
import net.leaderos.shared.helpers.AuthUtil;
import net.leaderos.shared.helpers.Placeholder;
import net.leaderos.shared.helpers.UserAgentUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.Map;

@RequiredArgsConstructor
public class ConnectionListener implements Listener {

    public static final Map<String, AuthResponse> STATUS_MAP = Maps.newHashMap();
    private static final int CACHE_EXPIRATION_SECONDS = 60;
    public static final Cache<String, AuthResponse> RESPONSE_CACHE = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofSeconds(CACHE_EXPIRATION_SECONDS)).build();
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

            // Check cache first
            AuthResponse response = RESPONSE_CACHE.getIfPresent(playerName);

            if (response == null) {
                // Make API request if not cached
                Shared.getDebugAPI().send("Making API request for player " + playerName, false);
                String userAgent = UserAgentUtil.generateUserAgent(!plugin.getConfigFile().getSettings().isSession());
                response = AuthUtil.checkGameSession(playerName, ip, userAgent).join();

                // Cache the response for future use
                RESPONSE_CACHE.put(playerName, response);
            } else {
                Shared.getDebugAPI().send("Using cached response for player " + playerName + ": " + response, false);
            }

            // Kick the player if they have an invalid username
            if (response == AuthResponse.INVALID_USERNAME) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, String.join("\n",
                        ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickInvalidUsername(),
                                new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))));
                return;
            }

            // Kick the player if they are not registered and kicking is enabled
            if (plugin.getConfigFile().getSettings().isKickNonRegistered() && response == AuthResponse.ACCOUNT_NOT_FOUND) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, String.join("\n",
                        ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickNotRegistered(),
                                new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))));
                return;
            }

            // If the player is already authenticated, allow them to join directly
            if (response == AuthResponse.HAS_SESSION && plugin.getConfigFile().getSettings().isSession()) {
                STATUS_MAP.put(playerName, response);
                Shared.getDebugAPI().send("Player " + playerName + " has active session, allowing direct login.", false);
                ChatUtil.sendConsoleInfo(playerName + " has active session, allowing direct login.");
                return;
            }

            AuthResponse finalResponse = response == AuthResponse.HAS_SESSION ? AuthResponse.LOGIN_REQUIRED : response;
            STATUS_MAP.put(playerName, finalResponse);
        } catch (Exception e) {
            Shared.getDebugAPI().send("ErrorCode processing player " + playerName + ": " + e.getMessage(), true);

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, String.join("\n",
                    ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickAnError(),
                            new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))));

            // Invalidate the cache for this player to avoid repeated errors
            RESPONSE_CACHE.invalidate(playerName);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        STATUS_MAP.remove(player.getName());
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
