package net.leaderos.auth.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.leaderos.auth.velocity.Velocity;
import net.leaderos.auth.velocity.helpers.ChatUtil;
import net.leaderos.shared.Shared;
import net.leaderos.shared.helpers.UrlUtil;

public class LeaderOSCommand implements SimpleCommand {

    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (args.length == 1 && args[0].equals("reload")) {
            if (source.hasPermission("leaderosauth.reload")) {
                Velocity.getInstance().getConfigFile().load(true);
                Velocity.getInstance().getLangFile().load(true);

                Shared.setLink(UrlUtil.format(Velocity.getInstance().getConfigFile().getSettings().getUrl()));
                Shared.setApiKey(Velocity.getInstance().getConfigFile().getSettings().getApiKey());

                ChatUtil.sendMessage(source, Velocity.getInstance().getLangFile().getMessages().getReload());
            } else
                ChatUtil.sendMessage(source, Velocity.getInstance().getLangFile().getMessages().getCommand().getNoPerm());
        } else
            ChatUtil.sendMessage(source, Velocity.getInstance().getLangFile().getMessages().getCommand().getInvalidArgument());
    }
}
