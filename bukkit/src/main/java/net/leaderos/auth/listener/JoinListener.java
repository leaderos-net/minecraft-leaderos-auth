package net.leaderos.auth.listener;

import lombok.RequiredArgsConstructor;
import net.leaderos.auth.Bukkit;
import net.leaderos.auth.helpers.ChatUtil;
import net.leaderos.auth.helpers.LocationUtil;
import net.leaderos.auth.helpers.TitleUtil;
import net.leaderos.shared.Shared;
import net.leaderos.shared.enums.SessionStatus;
import net.leaderos.shared.helpers.Placeholder;
import net.leaderos.shared.model.response.GameSessionResponse;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class JoinListener implements Listener {

    private final Bukkit plugin;

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        try {
            if (plugin.getConfigFile().getSettings().isForceSurvivalMode()) {
                player.setGameMode(GameMode.SURVIVAL);
            }

            // Teleport to spawn if set
            if (plugin.getConfigFile().getSettings().getSpawn().getLocation() != null && !plugin.getConfigFile().getSettings().getSpawn().getLocation().isEmpty()) {
                Location location = LocationUtil.stringToLocation(plugin.getConfigFile().getSettings().getSpawn().getLocation());
                if (location != null && location.getWorld() != null) {
                    // Only teleport if forceTeleportOnJoin is true or if the player is joining for the first time
                    if (plugin.getConfigFile().getSettings().getSpawn().isForceTeleportOnJoin() || !player.hasPlayedBefore()) {
                        plugin.getFoliaLib().getScheduler().teleportAsync(player, location);
                    }

                }
            }

            GameSessionResponse session = plugin.getSessions().get(player.getName());

            // No need for a isSession check here, as we handle it in the ConnectionListener
            if (session.isAuthenticated()) {
                ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getLogin().getSuccess());
                plugin.getFoliaLib().getScheduler().runLater(() -> {
                    plugin.sendStatus(player, true);
                }, 5);

                if (plugin.getConfigFile().getSettings().getSendAfterAuth().isEnabled()) {
                    plugin.getFoliaLib().getScheduler().runLater(() -> {
                        plugin.sendPlayerToServer(player, plugin.getConfigFile().getSettings().getSendAfterAuth().getServer());
                    }, 20L);
                }
                return;
            }

            if (session.getStatus() == SessionStatus.LOGIN_REQUIRED) {
                String title = ChatUtil.color(plugin.getLangFile().getMessages().getLogin().getTitle());
                String subtitle = ChatUtil.color(plugin.getLangFile().getMessages().getLogin().getSubtitle());
                TitleUtil.sendTitle(player, title, subtitle, 10, plugin.getLangFile().getMessages().getLogin().getTitleDuration() * 20, 10);
            }
            if (session.getStatus() == SessionStatus.ACCOUNT_NOT_FOUND) {
                String title = ChatUtil.color(plugin.getLangFile().getMessages().getRegister().getTitle());
                String subtitle = ChatUtil.color(plugin.getLangFile().getMessages().getRegister().getSubtitle());
                TitleUtil.sendTitle(player, title, subtitle, 10, plugin.getLangFile().getMessages().getRegister().getTitleDuration() * 20, 10);
            }
            if (session.getStatus() == SessionStatus.TFA_REQUIRED) {
                String title = ChatUtil.color(plugin.getLangFile().getMessages().getTfa().getTitle());
                String subtitle = ChatUtil.color(plugin.getLangFile().getMessages().getTfa().getSubtitle());
                TitleUtil.sendTitle(player, title, subtitle, 10, plugin.getLangFile().getMessages().getTfa().getTitleDuration() * 20, 10);
            }

            long joinTime = System.currentTimeMillis();
            AtomicInteger i = new AtomicInteger();

            plugin.getFoliaLib().getScheduler().runTimer((task) -> {
                if (plugin.isAuthenticated(player)) {
                    task.cancel();
                    return;
                }

                if (System.currentTimeMillis() - joinTime > plugin.getConfigFile().getSettings().getAuthTimeout() * 1000L) {
                    player.kickPlayer(String.join("\n",
                            ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickTimeout(),
                                    new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))));
                    task.cancel();
                    return;
                }

                // We'll send a message every 5 seconds to remind the player to login or register
                if (i.incrementAndGet() % 5 == 0) {
                    if (session.getStatus() == SessionStatus.LOGIN_REQUIRED) {
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getLogin().getMessage());
                    }
                    if (session.getStatus() == SessionStatus.ACCOUNT_NOT_FOUND) {
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getMessage());
                    }
                    if (session.getStatus() == SessionStatus.TFA_REQUIRED) {
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getTfa().getRequired());
                    }
                }
            }, 10L, 20L);

            plugin.getFoliaLib().getScheduler().runLater(() -> {
                plugin.sendStatus(player, session.isAuthenticated());
            }, 5);
        } catch (Exception e) {
            Shared.getDebugAPI().send("ErrorCode PlayerJoinEvent for " + player.getName() + ": " + e.getMessage(), true);

            player.kickPlayer(String.join("\n",
                    ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickAnError(),
                            new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))));
        }
    }

}
