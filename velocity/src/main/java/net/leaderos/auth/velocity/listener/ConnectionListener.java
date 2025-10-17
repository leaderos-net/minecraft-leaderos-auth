package net.leaderos.auth.velocity.listener;

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
import net.leaderos.auth.shared.Shared;
import net.leaderos.auth.shared.enums.SessionStatus;
import net.leaderos.auth.shared.helpers.AuthUtil;
import net.leaderos.auth.shared.helpers.Placeholder;
import net.leaderos.auth.shared.helpers.UserAgentUtil;
import net.leaderos.auth.shared.model.response.GameSessionResponse;

import java.util.List;

@RequiredArgsConstructor
public class ConnectionListener {
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
                // Make API request for game session
                Shared.getDebugAPI().send("Making API request for player " + playerName, false);
                String userAgent = UserAgentUtil.generateUserAgent(!plugin.getConfigFile().getSettings().isSession());
                GameSessionResponse session = AuthUtil.checkGameSession(playerName, ip, userAgent).join();

                // Kick the player if they have an invalid username
                if (session.getStatus() == SessionStatus.INVALID_USERNAME) {
                    kickPlayer(player, plugin.getLangFile().getMessages().getKickInvalidUsername());
                    return;
                }

                // Check email verification status
                if (session.getStatus() == SessionStatus.EMAIL_NOT_VERIFIED) {
                    // Kick the player if their email is not verified and kicking is enabled
                    if (plugin.getConfigFile().getSettings().getEmailVerification().isKickNonVerified()) {
                        kickPlayer(player, plugin.getLangFile().getMessages().getKickEmailNotVerified());
                        return;
                    } else {
                        // If email verification is disabled, set status to LOGIN_REQUIRED
                        session.setStatus(SessionStatus.LOGIN_REQUIRED);
                    }
                }

                // Kick the player if they are not registered and kicking is enabled
                if (plugin.getConfigFile().getSettings().isKickNonRegistered() && session.getStatus() == SessionStatus.ACCOUNT_NOT_FOUND) {
                    kickPlayer(player, plugin.getLangFile().getMessages().getKickNotRegistered());
                    return;
                }

                // If the player is already authenticated, allow them to join directly
                if (session.getStatus() == SessionStatus.HAS_SESSION && plugin.getConfigFile().getSettings().isSession()) {
                    // Change session status to authenticated
                    session.setStatus(SessionStatus.AUTHENTICATED);

                    Shared.getDebugAPI().send("Player " + playerName + " has active session, allowing direct login.", false);
                    ChatUtil.sendConsoleInfo(playerName + " has logged in with an active session.");
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getLogin().getSuccess());
                    plugin.getLimboServer().spawnPlayer(player, new ValidSessionHandler());
                    return;
                }

                // Spawn player in auth limbo
                Shared.getDebugAPI().send("Spawning player " + playerName + " in limbo for authentication.", false);
                plugin.getLimboServer().spawnPlayer(player, new AuthSessionHandler(plugin, player, ip, session));
            } catch (Exception e) {
                Shared.getDebugAPI().send("ErrorCode processing player " + playerName + ": " + e.getMessage(), true);

                // Kick the player with an error message
                kickPlayer(player, plugin.getLangFile().getMessages().getKickAnError());
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