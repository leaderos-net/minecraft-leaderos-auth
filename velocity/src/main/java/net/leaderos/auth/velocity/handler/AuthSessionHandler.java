package net.leaderos.auth.velocity.handler;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.title.Title;
import net.leaderos.auth.shared.helpers.ValidationUtil;
import net.leaderos.auth.velocity.Velocity;
import net.leaderos.auth.velocity.helpers.ChatUtil;
import net.leaderos.auth.shared.Shared;
import net.leaderos.auth.shared.enums.ErrorCode;
import net.leaderos.auth.shared.enums.RegisterSecondArg;
import net.leaderos.auth.shared.enums.SessionStatus;
import net.leaderos.auth.shared.helpers.AuthUtil;
import net.leaderos.auth.shared.helpers.Placeholder;
import net.leaderos.auth.shared.helpers.UserAgentUtil;
import net.leaderos.auth.shared.model.response.GameSessionResponse;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class AuthSessionHandler implements LimboSessionHandler {
    private final Velocity plugin;
    private final Player proxyPlayer;
    private final String ip;
    private final GameSessionResponse session;

    private long joinTime = System.currentTimeMillis();
    private long lastCommand = -1L;

    private LimboPlayer limboPlayer;

    private ScheduledFuture<?> authMainTask;

    @Override
    public void onSpawn(Limbo server, LimboPlayer player) {
        this.limboPlayer = player;

        player.disableFalling();

        if (session == null) {
            Shared.getDebugAPI().send("Session response is null for player " + proxyPlayer.getUsername(), true);
            proxyPlayer.disconnect(Component.join(JoinConfiguration.newlines(),
                    ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickAnError(), new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))));
            return;
        }

        if (session.getStatus() == SessionStatus.LOGIN_REQUIRED) {
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
        if (session.getStatus() == SessionStatus.ACCOUNT_NOT_FOUND) {
            proxyPlayer.showTitle(Title.title(ChatUtil.color(plugin.getLangFile().getMessages().getRegister().getTitle()),
                    ChatUtil.color(plugin.getLangFile().getMessages().getRegister().getSubtitle()),
                    Title.Times.times(
                            Duration.ZERO,
                            Duration.ofSeconds(plugin.getLangFile().getMessages().getRegister().getTitleDuration()),
                            Duration.ZERO
                    ))
            );
            ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getMessage());
        }
        if (session.getStatus() == SessionStatus.TFA_REQUIRED) {
            proxyPlayer.showTitle(Title.title(ChatUtil.color(plugin.getLangFile().getMessages().getTfa().getTitle()),
                    ChatUtil.color(plugin.getLangFile().getMessages().getTfa().getSubtitle()),
                    Title.Times.times(
                            Duration.ZERO,
                            Duration.ofSeconds(plugin.getLangFile().getMessages().getTfa().getTitleDuration()),
                            Duration.ZERO
                    ))
            );
            ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getTfa().getRequired());
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
                if (session.getStatus() == SessionStatus.LOGIN_REQUIRED) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getLogin().getMessage());
                }
                if (session.getStatus() == SessionStatus.ACCOUNT_NOT_FOUND) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getMessage());
                }
                if (session.getStatus() == SessionStatus.TFA_REQUIRED) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getTfa().getRequired());
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onChat(String message) {
        if (!message.startsWith("/")) return;

        if (lastCommand > 0 && System.currentTimeMillis() - lastCommand < (plugin.getConfigFile().getSettings().getCommandCooldown() * 1000L)) {
            ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getWait());
            return;
        }

        lastCommand = System.currentTimeMillis();

        String[] args = message.split(" ");
        String command = args[0].startsWith("/") ? args[0].toLowerCase().substring(1) : args[0].toLowerCase();
        if (plugin.getConfigFile().getSettings().getLoginCommands().contains(command) && args.length > 1) { // Login command
            // Prevent trying to login if already authenticated
            if (session.getStatus() == SessionStatus.AUTHENTICATED) {
                ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getAlreadyAuthenticated());
                return;
            }

            // Prevent trying to login if TFA is required
            if (session.getStatus() == SessionStatus.TFA_REQUIRED) {
                ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getTfa().getRequired());
                return;
            }

            // Prevent trying to login if need to register
            if (session.getStatus() == SessionStatus.ACCOUNT_NOT_FOUND) {
                ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getMessage());
                return;
            }

            String password = args[1];
            String userAgent = UserAgentUtil.generateUserAgent(!plugin.getConfigFile().getSettings().isSession());
            AuthUtil.login(proxyPlayer.getUsername(), password, ip, userAgent).whenComplete((result, ex) -> {
                if (ex != null) {
                    ex.printStackTrace();
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getAnErrorOccurred());
                    return;
                }

                if (result.isStatus()) {
                    // Set session token
                    session.setToken(result.getToken());

                    if (result.isTfaRequired()) {
                        // Change session status to TFA required
                        session.setStatus(SessionStatus.TFA_REQUIRED);

                        // Reset timeout timer for TFA verification
                        this.joinTime = System.currentTimeMillis();

                        // Update title to TFA
                        proxyPlayer.showTitle(Title.title(ChatUtil.color(plugin.getLangFile().getMessages().getTfa().getTitle()),
                                ChatUtil.color(plugin.getLangFile().getMessages().getTfa().getSubtitle()),
                                Title.Times.times(
                                        Duration.ZERO,
                                        Duration.ofSeconds(plugin.getLangFile().getMessages().getTfa().getTitleDuration()),
                                        Duration.ZERO
                                ))
                        );

                        ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getTfa().getRequired());
                        ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getTfa().getUsage());
                    } else {
                        // Change session status to authenticated
                        session.setStatus(SessionStatus.AUTHENTICATED);

                        ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getLogin().getSuccess());
                        ChatUtil.sendConsoleInfo(proxyPlayer.getUsername() + " has logged in successfully.");
                        this.limboPlayer.getScheduledExecutor().schedule(() -> this.limboPlayer.disconnect(), 500, TimeUnit.MILLISECONDS);
                    }
                } else if (result.getError() == ErrorCode.USER_NOT_FOUND) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getLogin().getAccountNotFound());
                } else if (result.getError() == ErrorCode.WRONG_PASSWORD) {
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
            // Prevent trying to register if already authenticated
            if (session.getStatus() == SessionStatus.AUTHENTICATED) {
                ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getAlreadyAuthenticated());
                return;
            }

            // Prevent trying to register if TFA is required
            if (session.getStatus() == SessionStatus.TFA_REQUIRED) {
                ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getTfa().getRequired());
                return;
            }

            // Prevent trying to register if need to login
            if (session.getStatus() == SessionStatus.LOGIN_REQUIRED || session.getStatus() == SessionStatus.HAS_SESSION) {
                ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getLogin().getMessage());
                return;
            }

            RegisterSecondArg secondArgType = plugin.getConfigFile().getSettings().getRegisterSecondArg();

            String password = args[1];
            String secondArg = args[2];

            // Check if second arg is confirmation and if it matches
            if (secondArgType == RegisterSecondArg.PASSWORD_CONFIRM && !password.equals(secondArg)) {
                ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getPasswordMismatch());
                return;
            }

            // Check if second arg is email and if it is valid
            if (secondArgType == RegisterSecondArg.EMAIL && !ValidationUtil.isValidEmail(secondArg)) {
                ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getInvalidEmail());
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

            String email = secondArgType == RegisterSecondArg.EMAIL ? secondArg : null;
            String userAgent = UserAgentUtil.generateUserAgent(!plugin.getConfigFile().getSettings().isSession());

            AuthUtil.register(proxyPlayer.getUsername(), password, email, ip, userAgent).whenComplete((result, ex) -> {
                if (ex != null) {
                    ex.printStackTrace();
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getAnErrorOccurred());
                    return;
                }

                if (result.isStatus()) {
                    // Change session status to authenticated
                    session.setStatus(SessionStatus.AUTHENTICATED);

                    // Set session token
                    session.setToken(result.getToken());

                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getSuccess());
                    ChatUtil.sendConsoleInfo(proxyPlayer.getUsername() + " has registered successfully.");
                    this.limboPlayer.getScheduledExecutor().schedule(() -> this.limboPlayer.disconnect(), 500, TimeUnit.MILLISECONDS);
                } else if (result.getError() == ErrorCode.USERNAME_ALREADY_EXIST) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getAlreadyRegistered());
                } else if (result.getError() == ErrorCode.REGISTER_LIMIT) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getRegisterLimit());
                } else if (result.getError() == ErrorCode.INVALID_USERNAME) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getInvalidName());
                } else if (result.getError() == ErrorCode.INVALID_EMAIL) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getInvalidEmail());
                } else if (result.getError() == ErrorCode.EMAIL_ALREADY_EXIST) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getEmailInUse());
                } else if (result.getError() == ErrorCode.INVALID_PASSWORD) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getRegister().getInvalidPassword());
                } else {
                    Shared.getDebugAPI().send("An unexpected error occurred during register: " + result, true);
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getAnErrorOccurred());
                }
            });
        } else if (plugin.getConfigFile().getSettings().getTfaCommands().contains(command) && args.length > 1) { // TFA command
            String code = args[1];

            // Prevent trying to verify TFA if already authenticated
            if (session.getStatus() == SessionStatus.AUTHENTICATED) {
                ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getAlreadyAuthenticated());
                return;
            }

            // Prevent trying to verify TFA if not TFA required
            if (session.getStatus() != SessionStatus.TFA_REQUIRED) {
                ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getTfa().getNotRequired());
                return;
            }

            // Check if code is exactly 6 characters long and numeric
            if (code.length() != 6 || !code.matches("\\d+")) {
                ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getTfa().getInvalidCode());
                return;
            }

            AuthUtil.verifyTfa(code, session.getToken()).whenComplete((result, ex) -> {
                if (ex != null) {
                    ex.printStackTrace();
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getAnErrorOccurred());
                    return;
                }

                if (result.isStatus()) {
                    // Change session status to authenticated
                    session.setStatus(SessionStatus.AUTHENTICATED);

                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getTfa().getSuccess());
                    ChatUtil.sendConsoleInfo(proxyPlayer.getUsername() + " has completed TFA verification successfully.");

                    // Disconnect player to allow reconnection with valid session
                    this.limboPlayer.getScheduledExecutor().schedule(() -> this.limboPlayer.disconnect(), 500, TimeUnit.MILLISECONDS);
                } else if (result.getError() == ErrorCode.WRONG_CODE) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getTfa().getInvalidCode());
                } else if (result.getError() == ErrorCode.SESSION_NOT_FOUND) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getTfa().getSessionNotFound());
                } else if (result.getError() == ErrorCode.TFA_VERIFICATION_FAILED) {
                    ChatUtil.sendMessage(proxyPlayer, plugin.getLangFile().getMessages().getTfa().getVerificationFailed());
                } else {
                    Shared.getDebugAPI().send("An unexpected error occurred during TFA verification: " + result, true);
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
