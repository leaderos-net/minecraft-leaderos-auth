package net.leaderos.auth.command;

import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Default;
import lombok.RequiredArgsConstructor;
import net.leaderos.auth.Bukkit;
import net.leaderos.auth.helpers.ChatUtil;
import net.leaderos.shared.Shared;
import net.leaderos.shared.enums.AuthResponse;
import net.leaderos.shared.enums.RegisterSecondArg;
import net.leaderos.shared.helpers.AuthUtil;
import net.leaderos.shared.helpers.Placeholder;
import net.leaderos.shared.helpers.ValidationUtil;
import org.bukkit.entity.Player;

import java.util.List;

import static net.leaderos.auth.listener.ConnectionListener.RESPONSE_CACHE;
import static net.leaderos.auth.listener.ConnectionListener.STATUS_MAP;

@RequiredArgsConstructor
public class RegisterCommand extends BaseCommand {

    private final Bukkit plugin;

    public RegisterCommand(Bukkit plugin, String command, List<String> aliases) {
        super(command, aliases);
        this.plugin = plugin;
    }

    @Default
    public void onRegister(Player player, String password, String secondArg) {
        AuthResponse currentStatus = STATUS_MAP.get(player.getName());
        if (currentStatus.isAuthenticated()) {
            ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAlreadyLoggedIn());
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

        AuthUtil.register(player.getName(), password, email, ip).whenComplete((result, ex) -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (ex != null) {
                    ex.printStackTrace();
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
                    return;
                }

                if (result == AuthResponse.SUCCESS) {
                    player.resetTitle();
                    plugin.forceRegister(player);
                    RESPONSE_CACHE.invalidate(player.getName());
                } else if (result == AuthResponse.USERNAME_ALREADY_EXIST) {
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getAlreadyRegistered());
                } else if (result == AuthResponse.REGISTER_LIMIT) {
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getRegisterLimit());
                } else if (result == AuthResponse.INVALID_USERNAME) {
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getInvalidName());
                } else if (result == AuthResponse.INVALID_EMAIL) {
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getInvalidEmail());
                } else if (result == AuthResponse.EMAIL_ALREADY_EXIST) {
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getEmailInUse());
                } else {
                    Shared.getDebugAPI().send("An unexpected error occurred during register: " + result, true);
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
                }
            });
        });
    }

}
