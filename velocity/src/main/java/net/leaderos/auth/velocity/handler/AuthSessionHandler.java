package net.leaderos.auth.velocity.handler;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.title.Title;
import net.leaderos.auth.velocity.Velocity;
import net.leaderos.auth.velocity.helpers.ChatUtil;
import net.leaderos.shared.Shared;
import net.leaderos.shared.enums.AuthResponse;
import net.leaderos.shared.helpers.AuthUtil;
import net.leaderos.shared.helpers.Placeholder;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static net.leaderos.auth.velocity.listener.ConnectionListener.RESPONSE_CACHE;

@RequiredArgsConstructor
public class AuthSessionHandler implements LimboSessionHandler {

    private final Player proxyPlayer;
    private final String ip;
    private final AuthResponse initialResponse;

    private final Velocity plugin;

    private final long joinTime = System.currentTimeMillis();
    private long lastCommand = -1L;

    private LimboPlayer limboPlayer;

    private ScheduledFuture<?> authMainTask;

    @Override
    public void onSpawn(Limbo server, LimboPlayer player) {
        this.limboPlayer = player;

        player.disableFalling();

        if (initialResponse == AuthResponse.ACCOUNT_NOT_FOUND || initialResponse == null) {
            proxyPlayer.showTitle(Title.title(ChatUtil.color(plugin.getLangFile().getMessages().getRegister().getTitle()),
                    ChatUtil.color(plugin.getLangFile().getMessages().getRegister().getSubtitle()),
                    Title.Times.times(
                            Duration.ZERO,
                            Duration.ofSeconds(plugin.getLangFile().getMessages().getRegister().getTitleDuration()),
                            Duration.ZERO
                    ))
            );
            ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getMessage());
        } else {
            ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getLogin().getMessage());
            proxyPlayer.showTitle(Title.title(ChatUtil.color(plugin.getLangFile().getMessages().getLogin().getTitle()),
                    ChatUtil.color(plugin.getLangFile().getMessages().getLogin().getSubtitle()),
                    Title.Times.times(
                            Duration.ZERO,
                            Duration.ofSeconds(plugin.getLangFile().getMessages().getLogin().getTitleDuration()),
                            Duration.ZERO
                    ))
            );
        }

        AtomicInteger i = new AtomicInteger();
        this.authMainTask = this.limboPlayer.getScheduledExecutor().scheduleWithFixedDelay(() -> {
            if (System.currentTimeMillis() - this.joinTime > plugin.getConfigFile().getSettings().getAuthTimeout() * 1000L) {
                proxyPlayer.disconnect(Component.join(JoinConfiguration.newlines(),
                        ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickTimeout(), new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))));
                return;
            }

            // We'll send a message every 5 seconds to remind the player to login or register
            if (i.incrementAndGet() % 5 == 0) {
                if (initialResponse == AuthResponse.ACCOUNT_NOT_FOUND || initialResponse == null) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getMessage());
                } else {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getLogin().getMessage());
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onChat(String message) {
        if (lastCommand > 0 && System.currentTimeMillis() - lastCommand < (plugin.getConfigFile().getSettings().getCommandCooldown() * 1000L)) {
            ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getWait());
            return;
        }

        lastCommand = System.currentTimeMillis();

        String[] args = message.split(" ");
        String command = args[0].startsWith("/") ? args[0].toLowerCase().substring(1) : args[0].toLowerCase();
        if (plugin.getConfigFile().getSettings().getLoginCommands().contains(command) && args.length > 1) { // Login command
            String password = args[1];
            AuthUtil.login(proxyPlayer.getUsername(), password, ip).whenComplete((result, ex) -> {
                if (ex != null) {
                    ex.printStackTrace();
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getAnErrorOccurred());
                    return;
                }

                if (result == AuthResponse.SUCCESS) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getLogin().getSuccess());
                    ChatUtil.sendConsoleInfo(proxyPlayer.getUsername() + " has logged in successfully.");
                    this.limboPlayer.getScheduledExecutor().schedule(() -> this.limboPlayer.disconnect(), 500, TimeUnit.MILLISECONDS);
                } else if (result == AuthResponse.USER_NOT_FOUND) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getLogin().getAccountNotFound());
                } else if (result == AuthResponse.WRONG_PASSWORD) {
                    if (plugin.getConfigFile().getSettings().isKickOnWrongPassword()) {
                        proxyPlayer.disconnect(Component.join(JoinConfiguration.newlines(),
                                ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickWrongPassword(), new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))));
                        return;
                    }

                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getLogin().getIncorrectPassword());
                } else {
                    Shared.getDebugAPI().send("An unexpected error occurred during login: " + result, true);
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getAnErrorOccurred());
                }
            });
        } else if (plugin.getConfigFile().getSettings().getRegisterCommands().contains(command) && args.length > 2) { // Register command
            String password = args[1];
            String passwordRepeat = args[2];

            if (!password.equals(passwordRepeat)) {
                ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getPasswordMismatch());
                return;
            }

            int minPasswordLength = Math.max(plugin.getConfigFile().getSettings().getMinPasswordLength(), 4);
            if (password.length() < minPasswordLength) {
                ChatUtil.sendMessage(proxyPlayer, ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getRegister().getPasswordTooShort(),
                        new Placeholder("{min}", minPasswordLength + "")));
                return;
            }

            int maxPasswordLength = 32;
            if (password.length() > maxPasswordLength) {
                ChatUtil.sendMessage(proxyPlayer, ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getRegister().getPasswordTooLong(),
                        new Placeholder("{max}", maxPasswordLength + "")));
                return;
            }

            if (plugin.getConfigFile().getSettings().getUnsafePasswords().contains(password)) {
                ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getUnsafePassword());
                return;
            }

            AuthUtil.register(proxyPlayer.getUsername(), password, ip).whenComplete((result, ex) -> {
                if (ex != null) {
                    ex.printStackTrace();
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getAnErrorOccurred());
                    return;
                }

                if (result == AuthResponse.SUCCESS) {
                    RESPONSE_CACHE.invalidate(proxyPlayer.getUsername());
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getSuccess());
                    ChatUtil.sendConsoleInfo(proxyPlayer.getUsername() + " has registered successfully.");
                    this.limboPlayer.getScheduledExecutor().schedule(() -> this.limboPlayer.disconnect(), 500, TimeUnit.MILLISECONDS);
                } else if (result == AuthResponse.USERNAME_ALREADY_EXIST) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getAlreadyRegistered());
                } else if (result == AuthResponse.REGISTER_LIMIT) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getRegisterLimit());
                } else if (result == AuthResponse.INVALID_USERNAME) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getInvalidName());
                } else {
                    Shared.getDebugAPI().send("An unexpected error occurred during register: " + result, true);
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getAnErrorOccurred());
                }
            });
        } else {
            ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getUnknownAuthCommand());
        }
    }

    @Override
    public void onDisconnect() {
        if (authMainTask != null && !authMainTask.isCancelled()) {
            authMainTask.cancel(true);
        }

        proxyPlayer.clearTitle();
    }
}
