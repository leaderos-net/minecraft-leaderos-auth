package net.leaderos.auth.command;

import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.annotation.Default;
import lombok.RequiredArgsConstructor;
import net.leaderos.auth.Bukkit;
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

    }

}
