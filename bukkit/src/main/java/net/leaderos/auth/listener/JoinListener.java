package net.leaderos.auth.listener;

import lombok.RequiredArgsConstructor;
import net.leaderos.auth.Bukkit;
import net.leaderos.auth.helpers.ChatUtil;
import net.leaderos.auth.helpers.LocationUtil;
import net.leaderos.shared.helpers.AuthResponse;
import net.leaderos.shared.helpers.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.atomic.AtomicInteger;

import static net.leaderos.auth.listener.ConnectionListener.STATUS_MAP;

@RequiredArgsConstructor
public class JoinListener implements Listener {

    private final Bukkit plugin;

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Teleport to spawn if set
        if (plugin.getConfigFile().getSettings().getSpawn().getLocation() != null && !plugin.getConfigFile().getSettings().getSpawn().getLocation().isEmpty()) {
            Location location = LocationUtil.stringToLocation(plugin.getConfigFile().getSettings().getSpawn().getLocation());
            if (location != null && location.getWorld() != null) {
                // Only teleport if forceTeleportOnJoin is true or if the player is joining for the first time
                if (plugin.getConfigFile().getSettings().getSpawn().isForceTeleportOnJoin() || !player.hasPlayedBefore()) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.teleport(location);
                        }
                    }, 5L);
                }

            }
        }

        AuthResponse currentStatus = STATUS_MAP.get(player.getName());
        // No need for a isSession check here, as we handle it in the ConnectionListener
        if (currentStatus.isAuthenticated()) {
            plugin.forceLogin(player);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.sendStatus(player, currentStatus.isAuthenticated());
            }, 5);
            return;
        }

        if (plugin.getConfigFile().getSettings().isForceSurvivalMode()) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        if (currentStatus == AuthResponse.LOGIN_REQUIRED) {
            String title = ChatUtil.color(plugin.getLangFile().getMessages().getLogin().getTitle());
            String subtitle = ChatUtil.color(plugin.getLangFile().getMessages().getLogin().getSubtitle());
            player.sendTitle(title, subtitle, 10, plugin.getLangFile().getMessages().getLogin().getTitleDuration() * 20, 10);
        } else {
            String title = ChatUtil.color(plugin.getLangFile().getMessages().getRegister().getTitle());
            String subtitle = ChatUtil.color(plugin.getLangFile().getMessages().getRegister().getSubtitle());
            player.sendTitle(title, subtitle, 10, plugin.getLangFile().getMessages().getRegister().getTitleDuration() * 20, 10);
        }

        long joinTime = System.currentTimeMillis();
        AtomicInteger i = new AtomicInteger();
        plugin.getServer().getScheduler().runTaskTimer(plugin, (task) -> {
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
                if (currentStatus == AuthResponse.ACCOUNT_NOT_FOUND) {
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getMessage());
                } else {
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getLogin().getMessage());
                }
            }
        }, 10L, 20L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.sendStatus(player, currentStatus.isAuthenticated());
        }, 5);
    }

}
