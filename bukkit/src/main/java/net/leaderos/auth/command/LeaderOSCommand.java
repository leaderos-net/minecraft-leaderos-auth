package net.leaderos.auth.command;

import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Command;
import dev.triumphteam.cmd.core.annotation.SubCommand;
import lombok.RequiredArgsConstructor;
import net.leaderos.auth.Bukkit;
import net.leaderos.auth.helpers.ChatUtil;
import net.leaderos.shared.Shared;
import net.leaderos.shared.helpers.UrlUtil;
import org.bukkit.command.CommandSender;

@Command("leaderosauth")
@RequiredArgsConstructor
public class LeaderOSCommand extends BaseCommand {

    /**
     * reload command of plugin
     *
     * @param sender commandsender
     */
    @Permission("leaderos.reload")
    @SubCommand("reload")
    public void reloadCommand(CommandSender sender) {
        Bukkit.getInstance().getConfigFile().load(true);
        Bukkit.getInstance().getLangFile().load(true);

        Shared.setLink(UrlUtil.format(Bukkit.getInstance().getConfigFile().getSettings().getUrl()));
        Shared.setApiKey(Bukkit.getInstance().getConfigFile().getSettings().getApiKey());

        ChatUtil.sendMessage(sender, Bukkit.getInstance().getLangFile().getMessages().getReload());
    }

}