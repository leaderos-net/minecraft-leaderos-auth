package net.leaderos.auth.velocity.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.leaderos.auth.velocity.Velocity;
import net.leaderos.auth.velocity.handler.AuthSessionHandler;
import net.leaderos.auth.velocity.helpers.ChatUtil;
import net.leaderos.shared.Shared;
import net.leaderos.shared.helpers.AuthResponse;
import net.leaderos.shared.helpers.AuthUtil;
import net.leaderos.shared.helpers.Placeholder;

import java.time.Duration;

@RequiredArgsConstructor
public class ConnectionListener {

    // Cache to store responses for 60 seconds
    // This is to avoid hitting the API too frequently for the same player
    // May be useful for bot attacks or repeated login attempts
    public static final Cache<String, AuthResponse> RESPONSE_CACHE = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofSeconds(60)).build();
    private final Velocity plugin;

    @Subscribe
    public void onLogin(PreLoginEvent event) {
        String ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress();

        try {
            AuthResponse cachedResponse = RESPONSE_CACHE.getIfPresent(event.getUsername());
            AuthResponse response;
            if (cachedResponse != null) {
                response = cachedResponse;
            } else {
                response = AuthUtil.checkGameSession(event.getUsername(), ip).join();
                RESPONSE_CACHE.put(event.getUsername(), response);
            }

            if (plugin.getConfigFile().getSettings().isKickNonRegistered() && response == AuthResponse.ACCOUNT_NOT_FOUND) {
                Shared.getDebugAPI().send("Player " + event.getUsername() + " is not registered, blocking connection.", false);
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                        Component.join(JoinConfiguration.newlines(),
                                ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickNotRegistered(),
                                        new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix())))
                ));
            }
        } catch (Exception e) {
            Shared.getDebugAPI().send("Error checking game session for player " + event.getUsername() + ": " + e.getMessage(), true);
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                    Component.join(JoinConfiguration.newlines(),
                            ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickAnError(),
                                    new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix())))
            ));
            RESPONSE_CACHE.invalidate(event.getUsername());
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onJoin(LoginLimboRegisterEvent event) {
        Player player = event.getPlayer();
        String ip = player.getRemoteAddress().getAddress().getHostAddress();

        AuthResponse cachedResponse = RESPONSE_CACHE.getIfPresent(player.getUsername());

        // If the response is cached and indicates the player has a session, we skip spawning them in limbo
        if (cachedResponse == AuthResponse.HAS_SESSION && plugin.getConfigFile().getSettings().isSession()) {
            Shared.getDebugAPI().send("Skipping limbo for player " + player.getUsername() + " with cached session response.", false);
            return;
        }

        event.addOnJoinCallback(() -> plugin.getLimboServer().spawnPlayer(player, new AuthSessionHandler(player, ip, cachedResponse, plugin)));
    }

}
