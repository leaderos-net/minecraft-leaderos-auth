package net.leaderos.auth.bukkit.command;

import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Command;
import dev.triumphteam.cmd.core.annotation.SubCommand;
import lombok.RequiredArgsConstructor;
import net.leaderos.auth.bukkit.Bukkit;
import net.leaderos.auth.bukkit.helpers.ChatUtil;
import net.leaderos.auth.bukkit.helpers.LocationUtil;
import net.leaderos.shared.Shared;
import net.leaderos.shared.helpers.UrlUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

    /**
     * Set spawn command of plugin
     */
    @Permission("leaderos.setspawn")
    @SubCommand("setspawn")
    public void setSpawnCommand(CommandSender sender) {
        // Prevent console from using this command
        if (!(sender instanceof Player)) return;

        // Get Location
        Player player = (Player) sender;
        String location = LocationUtil.locationToString(player.getLocation());

        // Save to config
        Bukkit.getInstance().getConfigFile().getSettings().getSpawn().setLocation(location);
        Bukkit.getInstance().getConfigFile().save();

        ChatUtil.sendMessage(player, Bukkit.getInstance().getLangFile().getMessages().getSetSpawn());
    }

}