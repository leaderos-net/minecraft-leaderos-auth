package net.leaderos.auth.bukkit.listener;

import lombok.RequiredArgsConstructor;
import net.leaderos.auth.bukkit.Bukkit;
import net.leaderos.auth.bukkit.helpers.BossBarUtil;
import net.leaderos.auth.bukkit.helpers.ChatUtil;
import net.leaderos.auth.bukkit.helpers.LocationUtil;
import net.leaderos.auth.bukkit.helpers.TitleUtil;
import net.leaderos.auth.shared.Shared;
import net.leaderos.auth.shared.enums.SessionStatus;
import net.leaderos.auth.shared.helpers.Placeholder;
import net.leaderos.auth.shared.model.response.GameSessionResponse;

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
                if (plugin.getConfigFile().getSettings().isShowTitle()) {
                    TitleUtil.sendTitle(
                            player,
                            ChatUtil.color(plugin.getLangFile().getMessages().getLogin().getTitle()),
                            ChatUtil.color(plugin.getLangFile().getMessages().getLogin().getSubtitle()),
                            10,
                            plugin.getConfigFile().getSettings().getAuthTimeout() * 20, 10
                    );
                }
            }
            if (session.getStatus() == SessionStatus.ACCOUNT_NOT_FOUND) {
                if (plugin.getConfigFile().getSettings().isShowTitle()) {
                    TitleUtil.sendTitle(
                            player,
                            ChatUtil.color(plugin.getLangFile().getMessages().getRegister().getTitle()),
                            ChatUtil.color(plugin.getLangFile().getMessages().getRegister().getSubtitle()),
                            10,
                            plugin.getConfigFile().getSettings().getAuthTimeout() * 20, 10
                    );
                }
            }
            if (session.getStatus() == SessionStatus.TFA_REQUIRED) {
                if (plugin.getConfigFile().getSettings().isShowTitle()) {
                    TitleUtil.sendTitle(
                            player,
                            ChatUtil.color(plugin.getLangFile().getMessages().getTfa().getTitle()),
                            ChatUtil.color(plugin.getLangFile().getMessages().getTfa().getSubtitle()),
                            10,
                            plugin.getConfigFile().getSettings().getAuthTimeout() * 20, 10
                    );
                }
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

                // Update boss bar progress
                if (plugin.getConfigFile().getSettings().getBossBar().isEnabled()) {
                    int remainingSeconds = (int) ((plugin.getConfigFile().getSettings().getAuthTimeout() * 1000L - (System.currentTimeMillis() - joinTime)) / 1000L);
                    float progress = 1.0f - ((System.currentTimeMillis() - joinTime) / (float) (plugin.getConfigFile().getSettings().getAuthTimeout() * 1000L));
                    float barProgress = (Math.max(0f, Math.min(1f, progress)));

                    String barTitle = "";
                    if (session.getStatus() == SessionStatus.LOGIN_REQUIRED) {
                        barTitle = plugin.getLangFile().getMessages().getLogin().getBossBar().replace("{seconds}", String.valueOf(remainingSeconds));
                    }
                    if (session.getStatus() == SessionStatus.ACCOUNT_NOT_FOUND) {
                        barTitle = plugin.getLangFile().getMessages().getRegister().getBossBar().replace("{seconds}", String.valueOf(remainingSeconds));
                    }
                    if (session.getStatus() == SessionStatus.TFA_REQUIRED) {
                        barTitle = plugin.getLangFile().getMessages().getTfa().getBossBar().replace("{seconds}", String.valueOf(remainingSeconds));
                    }

                    // Set bar color to auto if enabled
                    String barColor;
                    if (plugin.getConfigFile().getSettings().getBossBar().getColor().equals("AUTO")) {
                        if (progress > 0.5f) {
                            barColor = "GREEN";
                        } else if (progress > 0.25f) {
                            barColor = "YELLOW";
                        } else {
                            barColor = "RED";
                        }
                    } else {
                        barColor = plugin.getConfigFile().getSettings().getBossBar().getColor();
                    }

                    BossBarUtil.showBossBar(player, barTitle, barProgress, barColor, plugin.getConfigFile().getSettings().getBossBar().getStyle());
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
