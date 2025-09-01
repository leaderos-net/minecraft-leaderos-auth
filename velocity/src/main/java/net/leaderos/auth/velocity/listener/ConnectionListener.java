package net.leaderos.auth.velocity.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.leaderos.auth.velocity.Velocity;
import net.leaderos.auth.velocity.handler.AuthSessionHandler;
import net.leaderos.auth.velocity.handler.ValidSessionHandler;
import net.leaderos.auth.velocity.helpers.ChatUtil;
import net.leaderos.shared.Shared;
import net.leaderos.shared.enums.AuthResponse;
import net.leaderos.shared.helpers.AuthUtil;
import net.leaderos.shared.helpers.Placeholder;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
public class ConnectionListener {

    // Cache to store responses for 60 seconds
    // This is to avoid hitting the API too frequently for the same player
    // May be useful for bot attacks or repeated login attempts
    private static final int CACHE_EXPIRATION_SECONDS = 60;
    public static final Cache<String, AuthResponse> RESPONSE_CACHE = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofSeconds(CACHE_EXPIRATION_SECONDS)).build();
    private final Velocity plugin;

    @Subscribe(order = PostOrder.LAST)
    public void onJoin(LoginLimboRegisterEvent event) {
        // Get the player who is joining
        Player player = event.getPlayer();
        String playerName = player.getUsername();
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        // Add the authentication logic inside event.addOnJoinCallback to ensure that,
        // if there is a system like LimboFilter,
        // it processes the player first before our logic runs.
        event.addOnJoinCallback(() -> {
            try {
                // Check cache first
                AuthResponse response = RESPONSE_CACHE.getIfPresent(playerName);

                if (response == null) {
                    // Make API request if not cached
                    Shared.getDebugAPI().send("Making API request for player " + playerName, false);
                    response = AuthUtil.checkGameSession(playerName, ip).join();

                    // Cache the response for future use
                    RESPONSE_CACHE.put(playerName, response);
                } else {
                    Shared.getDebugAPI().send("Using cached response for player " + playerName + ": " + response, false);
                }

                // Kick the player if they have an invalid username
                if (response == AuthResponse.INVALID_USERNAME) {
                    kickPlayer(player, plugin.getLangFile().getMessages().getKickInvalidUsername());
                    return;
                }

                // Kick the player if they are not registered and kicking is enabled
                if (plugin.getConfigFile().getSettings().isKickNonRegistered() && response == AuthResponse.ACCOUNT_NOT_FOUND) {
                    kickPlayer(player, plugin.getLangFile().getMessages().getKickNotRegistered());
                    return;
                }

                // If the player is already authenticated, allow them to join directly
                if (response == AuthResponse.HAS_SESSION && plugin.getConfigFile().getSettings().isSession()) {
                    Shared.getDebugAPI().send("Player " + playerName + " has active session, allowing direct login.", false);
                    ChatUtil.sendConsoleInfo(playerName + " has logged in with an active session.");
                    plugin.getLimboServer().spawnPlayer(player, new ValidSessionHandler());
                    return;
                }

                // Spawn player in auth limbo
                Shared.getDebugAPI().send("Spawning player " + playerName + " in limbo for authentication.", false);
                plugin.getLimboServer().spawnPlayer(player, new AuthSessionHandler(player, ip, response, plugin));
            } catch (Exception e) {
                Shared.getDebugAPI().send("ErrorCode processing player " + playerName + ": " + e.getMessage(), true);

                // Kick the player with an error message
                kickPlayer(player, plugin.getLangFile().getMessages().getKickAnError());

                // Invalidate the cache for this player to avoid repeated errors
                RESPONSE_CACHE.invalidate(playerName);
            }
        });
    }

    private void kickPlayer(Player player, List<String> kickMessage) {
        String playerName = player.getUsername();
        Shared.getDebugAPI().send("Player " + playerName + " is being kicked.", false);

        Component message = Component.join(JoinConfiguration.newlines(),
                ChatUtil.replacePlaceholders(kickMessage,
                        new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix())));

        player.disconnect(message);
    }
}