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
import net.leaderos.shared.model.response.GameSessionResponse;
import org.bukkit.entity.Player;

import java.util.List;

@RequiredArgsConstructor
public class TfaCommand extends BaseCommand {

    private final Bukkit plugin;

    public TfaCommand(Bukkit plugin, String command, List<String> aliases) {
        super(command, aliases);
        this.plugin = plugin;
    }

    @Default
    public void verifyTfa(Player player, String code) {
        try {
            GameSessionResponse session = plugin.getSessions().get(player.getName());

            if (session.isAuthenticated()) {
                ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAlreadyLoggedIn());
                return;
            }

            // Check if session status is TFA required
            if (session.getStatus() != SessionStatus.TFA_REQUIRED) {
                ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getTfa().getNotRequired());
                return;
            }

            // Check if code is exactly 6 characters long and numeric
            if (code.length() != 6 || !code.matches("\\d+")) {
                ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getTfa().getInvalidCode());
                return;
            }

            AuthUtil.verifyTfa(code, session.getToken()).whenComplete((result, ex) -> {
                plugin.getFoliaLib().getScheduler().runNextTick((task) -> {
                    if (ex != null) {
                        ex.printStackTrace();
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
                        return;
                    }

                    if (result.isStatus()) {
                        // Clear title
                        TitleUtil.clearTitle(player);

                        // Change session status to authenticated
                        session.setStatus(SessionStatus.AUTHENTICATED);

                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getTfa().getSuccess());
                        ChatUtil.sendConsoleInfo(player.getName() + " has completed TFA verification successfully.");

                        plugin.sendStatus(player, true);

                        if (plugin.getConfigFile().getSettings().getSendAfterAuth().isEnabled()) {
                            plugin.getFoliaLib().getScheduler().runLater(() -> {
                                plugin.sendPlayerToServer(player, plugin.getConfigFile().getSettings().getSendAfterAuth().getServer());
                            }, 20L);
                        }
                    } else if (result.getError() == ErrorCode.WRONG_CODE) {
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getTfa().getInvalidCode());
                    } else if (result.getError() == ErrorCode.SESSION_NOT_FOUND) {
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getTfa().getSessionNotFound());
                    } else if (result.getError() == ErrorCode.TFA_VERIFICATION_FAILED) {
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getTfa().getVerificationFailed());
                    } else {
                        Shared.getDebugAPI().send("An unexpected error occurred during TFA verification: " + result, true);
                        ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
                    }
                });
            });
        } catch (Exception e) {
            ChatUtil.sendMessage(player, plugin.getLangFile().getMessages().getAnErrorOccurred());
        }
    }

}
