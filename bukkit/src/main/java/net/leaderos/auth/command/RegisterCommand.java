package net.leaderos.auth.command;

import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Default;
import lombok.RequiredArgsConstructor;
import net.leaderos.auth.Bukkit;
import net.leaderos.auth.helpers.ChatUtil;
import net.leaderos.shared.Shared;
import net.leaderos.shared.helpers.AuthResponse;
import net.leaderos.shared.helpers.AuthUtil;
import net.leaderos.shared.helpers.Placeholder;
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
    public void onRegister(Player player, String password, String passwordConfirm) {
        AuthResponse currentStatus = STATUS_MAP.get(player.getName());
        if (currentStatus.isAuthenticated()) {
            ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAlreadyLoggedIn());
            return;
        }

        String ip = player.getAddress().getAddress().getHostAddress();

        if (!password.equals(passwordConfirm)) {
            ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getPasswordMismatch());
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

        AuthUtil.register(player.getName(), password, ip).whenComplete((result, ex) -> {
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
                } else {
                    Shared.getDebugAPI().send("An unexpected error occurred during register: " + result, true);
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
                }
            });
        });
    }

}
