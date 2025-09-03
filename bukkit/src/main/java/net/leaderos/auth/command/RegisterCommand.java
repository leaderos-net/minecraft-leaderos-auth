package net.leaderos.auth.command;

import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Default;
import lombok.RequiredArgsConstructor;
import net.leaderos.auth.Bukkit;
import net.leaderos.auth.helpers.ChatUtil;
import net.leaderos.shared.Shared;
import net.leaderos.shared.enums.ErrorCode;
import net.leaderos.shared.enums.RegisterSecondArg;
import net.leaderos.shared.enums.SessionStatus;
import net.leaderos.shared.helpers.AuthUtil;
import net.leaderos.shared.helpers.Placeholder;
import net.leaderos.shared.helpers.UserAgentUtil;
import net.leaderos.shared.helpers.ValidationUtil;
import net.leaderos.shared.model.response.GameSessionResponse;
import org.bukkit.entity.Player;

import java.util.List;

@RequiredArgsConstructor
public class RegisterCommand extends BaseCommand {

    private final Bukkit plugin;

    public RegisterCommand(Bukkit plugin, String command, List<String> aliases) {
        super(command, aliases);
        this.plugin = plugin;
    }

    @Default
    public void onRegister(Player player, String password, String secondArg) {
        try {
            GameSessionResponse session = plugin.getSessions().get(player.getName());
            if (session.isAuthenticated()) {
                ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAlreadyLoggedIn());
                return;
            }

            if (player.getAddress() == null) {
                ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
                return;
            }

            // Prevent trying to register if TFA is required
            if (session.getStatus() == SessionStatus.TFA_REQUIRED) {
                ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getTfa().getRequired());
                return;
            }

            // Prevent trying to register if need to login
            if (session.getStatus() == SessionStatus.LOGIN_REQUIRED || session.getStatus() == SessionStatus.HAS_SESSION) {
                ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getLogin().getMessage());
                return;
            }

            String ip = player.getAddress().getAddress().getHostAddress();
            RegisterSecondArg secondArgType = plugin.getConfigFile().getSettings().getRegisterSecondArg();

            // Check if second arg is confirmation and if it matches
            if (secondArgType == RegisterSecondArg.PASSWORD_CONFIRM && !password.equals(secondArg)) {
                ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getPasswordMismatch());
                return;
            }

            // Check if second arg is email and if it is valid
            if (secondArgType == RegisterSecondArg.EMAIL && !ValidationUtil.isValidEmail(secondArg)) {
                ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getInvalidEmail());
                return;
            }

            int minPasswordLength = Math.max(plugin.getConfigFile().getSettings().getMinPasswordLength(), 4);
            if (password.length() < minPasswordLength) {
                ChatUtil.sendMessage(player, ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getRegister().getPasswordTooShort(),
                        new Placeholder("{min}", minPasswordLength + "")));
                return;
            }

            int maxPasswordLength = 32;
            if (password.length() > maxPasswordLength) {
                ChatUtil.sendMessage(player, ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getRegister().getPasswordTooLong(),
                        new Placeholder("{max}", maxPasswordLength + "")));
                return;
            }

            if (plugin.getConfigFile().getSettings().getUnsafePasswords().contains(password)) {
                ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getUnsafePassword());
                return;
            }

            String email = secondArgType == RegisterSecondArg.EMAIL ? secondArg : null;
            String userAgent = UserAgentUtil.generateUserAgent(!plugin.getConfigFile().getSettings().isSession());

            AuthUtil.register(player.getName(), password, email, ip, userAgent).whenComplete((result, ex) -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (ex != null) {
                        ex.printStackTrace();
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
                        return;
                    }

                    if (result.isStatus()) {
                        player.resetTitle();

                        session.setStatus(SessionStatus.AUTHENTICATED);
                        session.setToken(result.getToken());
                        plugin.getSessions().put(player.getName(), session);

                        ChatUtil.sendConsoleInfo(player.getName() + " has registered successfully.");
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getSuccess());
                        plugin.sendStatus(player, true);

                        if (plugin.getConfigFile().getSettings().getSendAfterAuth().isEnabled()) {
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                plugin.sendPlayerToServer(player, plugin.getConfigFile().getSettings().getSendAfterAuth().getServer());
                            }, 20L);
                        }
                    } else if (result.getError() == ErrorCode.USERNAME_ALREADY_EXIST) {
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getAlreadyRegistered());
                    } else if (result.getError() == ErrorCode.REGISTER_LIMIT) {
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getRegisterLimit());
                    } else if (result.getError() == ErrorCode.INVALID_USERNAME) {
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getInvalidName());
                    } else if (result.getError() == ErrorCode.INVALID_EMAIL) {
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getInvalidEmail());
                    } else if (result.getError() == ErrorCode.EMAIL_ALREADY_EXIST) {
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getEmailInUse());
                    } else if (result.getError() == ErrorCode.INVALID_PASSWORD) {
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getInvalidPassword());
                    } else {
                        Shared.getDebugAPI().send("An unexpected error occurred during register: " + result, true);
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
                    }
                });
            });
        } catch (Exception e) {
            ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
        }
    }

}
