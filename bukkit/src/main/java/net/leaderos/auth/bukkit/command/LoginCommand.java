package net.leaderos.auth.bukkit.command;

import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Default;
import lombok.RequiredArgsConstructor;
import net.leaderos.auth.bukkit.Bukkit;
import net.leaderos.auth.bukkit.helpers.ChatUtil;
import net.leaderos.auth.bukkit.helpers.TitleUtil;
import net.leaderos.shared.Shared;
import net.leaderos.shared.enums.ErrorCode;
import net.leaderos.shared.enums.SessionStatus;
import net.leaderos.shared.helpers.AuthUtil;
import net.leaderos.shared.helpers.Placeholder;
import net.leaderos.shared.helpers.UserAgentUtil;
import net.leaderos.shared.model.response.GameSessionResponse;
import org.bukkit.entity.Player;

import java.util.List;

@RequiredArgsConstructor
public class LoginCommand extends BaseCommand {

    private final Bukkit plugin;

    public LoginCommand(Bukkit plugin, String command, List<String> aliases) {
        super(command, aliases);
        this.plugin = plugin;
    }

    @Default
    public void onLogin(Player player, String password) {
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

            // Prevent trying to login if TFA is required
            if (session.getStatus() == SessionStatus.TFA_REQUIRED) {
                ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getTfa().getRequired());
                return;
            }

            // Prevent trying to login if need to register
            if (session.getStatus() == SessionStatus.ACCOUNT_NOT_FOUND) {
                ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getRegister().getMessage());
                return;
            }

            String ip = player.getAddress().getAddress().getHostAddress();
            String userAgent = UserAgentUtil.generateUserAgent(!plugin.getConfigFile().getSettings().isSession());
            AuthUtil.login(player.getName(), password, ip, userAgent).whenComplete((result, ex) -> {
                plugin.getFoliaLib().getScheduler().runNextTick((task) -> {
                    if (ex != null) {
                        ex.printStackTrace();
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
                        return;
                    }

                    if (result.isStatus()) {
                        // Set session token
                        session.setToken(result.getToken());

                        if (result.isTfaRequired()) {
                            // Change session status to TFA required
                            session.setStatus(SessionStatus.TFA_REQUIRED);

                            // Update title to TFA
                            String tfaTitle = ChatUtil.color(plugin.getLangFile().getMessages().getTfa().getTitle());
                            String tfaSubtitle = ChatUtil.color(plugin.getLangFile().getMessages().getTfa().getSubtitle());
                            TitleUtil.sendTitle(player, tfaTitle, tfaSubtitle, 0, plugin.getLangFile().getMessages().getTfa().getTitleDuration() * 20, 10);

                            ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getTfa().getRequired());
                            ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getTfa().getUsage());
                        } else {
                            // Clear title
                            TitleUtil.clearTitle(player);

                            // Change session status to authenticated
                            session.setStatus(SessionStatus.AUTHENTICATED);

                            ChatUtil.sendConsoleInfo(player.getName() + " has logged in successfully.");
                            ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getLogin().getSuccess());
                            plugin.sendStatus(player, true);

                            if (plugin.getConfigFile().getSettings().getSendAfterAuth().isEnabled()) {
                                plugin.getFoliaLib().getScheduler().runLater(() -> {
                                    plugin.sendPlayerToServer(player, plugin.getConfigFile().getSettings().getSendAfterAuth().getServer());
                                }, 20L);
                            }
                        }
                    } else if (result.getError() == ErrorCode.USER_NOT_FOUND) {
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getLogin().getAccountNotFound());
                    } else if (result.getError() == ErrorCode.WRONG_PASSWORD) {
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
        } catch (Exception e) {
            ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
        }
    }

}
