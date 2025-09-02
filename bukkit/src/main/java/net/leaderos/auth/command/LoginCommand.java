package net.leaderos.auth.command;

import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Default;
import lombok.RequiredArgsConstructor;
import net.leaderos.auth.Bukkit;
import net.leaderos.auth.helpers.ChatUtil;
import net.leaderos.shared.Shared;
import net.leaderos.shared.enums.AuthResponse;
import net.leaderos.shared.helpers.AuthUtil;
import net.leaderos.shared.helpers.Placeholder;
import net.leaderos.shared.helpers.UserAgentUtil;
import org.bukkit.entity.Player;

import java.util.List;

import static net.leaderos.auth.listener.ConnectionListener.RESPONSE_CACHE;
import static net.leaderos.auth.listener.ConnectionListener.STATUS_MAP;

@RequiredArgsConstructor
public class LoginCommand extends BaseCommand {

    private final Bukkit plugin;

    public LoginCommand(Bukkit plugin, String command, List<String> aliases) {
        super(command, aliases);
        this.plugin = plugin;
    }

    @Default
    public void onLogin(Player player, String password) {
        AuthResponse currentStatus = STATUS_MAP.get(player.getName());
        if (currentStatus.isAuthenticated()) {
            ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAlreadyLoggedIn());
            return;
        }

        String ip = player.getAddress().getAddress().getHostAddress();
        String userAgent = UserAgentUtil.generateUserAgent(!plugin.getConfigFile().getSettings().isSession());
        AuthUtil.login(player.getName(), password, ip, userAgent).whenComplete((result, ex) -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (ex != null) {
                    ex.printStackTrace();
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
                    return;
                }

                if (result == AuthResponse.SUCCESS) {
                    player.resetTitle();
                    plugin.forceLogin(player);
                    RESPONSE_CACHE.invalidate(player.getName());
                } else if (result == AuthResponse.USER_NOT_FOUND) {
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getLogin().getAccountNotFound());
                } else if (result == AuthResponse.WRONG_PASSWORD) {
                    if (plugin.getConfigFile().getSettings().isKickOnWrongPassword()) {
                        player.kickPlayer(String.join("\n",
                                ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickWrongPassword(),
                                        new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix()))
                        ));
                        return;
                    }

                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getLogin().getIncorrectPassword());
                } else {
                    Shared.getDebugAPI().send("An unexpected error occurred during login: " + result, true);
                    ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
                }
            });
        });
    }

}
