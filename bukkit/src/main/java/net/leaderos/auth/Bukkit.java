package net.leaderos.auth;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.triumphteam.cmd.bukkit.BukkitCommandManager;
import dev.triumphteam.cmd.bukkit.message.BukkitMessageKey;
import dev.triumphteam.cmd.core.message.MessageKey;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import lombok.Getter;
import net.leaderos.auth.command.LeaderOSCommand;
import net.leaderos.auth.command.LoginCommand;
import net.leaderos.auth.command.RegisterCommand;
import net.leaderos.auth.configuration.Config;
import net.leaderos.auth.configuration.Language;
import net.leaderos.auth.helpers.ChatUtil;
import net.leaderos.auth.helpers.DebugBukkit;
import net.leaderos.auth.listener.ConnectionListener;
import net.leaderos.auth.listener.JoinListener;
import net.leaderos.auth.listener.PlayerListener;
import net.leaderos.shared.Shared;
import net.leaderos.shared.helpers.AuthResponse;
import net.leaderos.shared.helpers.UrlUtil;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

import static net.leaderos.auth.listener.ConnectionListener.STATUS_MAP;

@Getter
public class Bukkit extends JavaPlugin {

    @Getter
    private static Bukkit instance;

    private Language langFile;
    private Config configFile;

    private Shared shared;

    private BukkitCommandManager<CommandSender> commandManager;

    @Override
    public void onEnable() {
        instance = this;
        setupFiles();

        this.shared = new Shared(
                UrlUtil.format(getConfigFile().getSettings().getUrl()),
                getConfigFile().getSettings().getApiKey(),
                new DebugBukkit()
        );

        if (getConfigFile().getSettings().getUrl().equals("https://yourwebsite.com")) {
            getLogger().warning("You have not set the API URL in the config.yml file. Please set it to your LeaderOS URL.");
        } else if (getConfigFile().getSettings().getUrl().startsWith("http://")) {
            getLogger().warning("You are using an insecure URL (http://) for the API. Please use https:// for security reasons.");
        }

        new Metrics(this, 26804);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        setupCommands();

        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    public void setupFiles() {
        try {
            this.configFile = ConfigManager.create(Config.class, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(new File(this.getDataFolder().getAbsolutePath(), "config.yml"));
                it.saveDefaults();
                it.load(true);
            });
            String langName = configFile.getSettings().getLang();
            Class langClass = Class.forName("net.leaderos.auth.configuration.lang." + langName);
            Class<Language> languageClass = langClass;
            this.langFile = ConfigManager.create(languageClass, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(new File(this.getDataFolder().getAbsolutePath() + "/lang", langName + ".yml"));
                it.saveDefaults();
                it.load(true);
            });
        } catch (Exception exception) {
            getLogger().log(Level.WARNING, "Error loading config.yml", exception);
        }
    }

    private void setupCommands() {
        commandManager = BukkitCommandManager.create(this);

        commandManager.registerCommand(
                new LeaderOSCommand(),
                new LoginCommand(this),
                new RegisterCommand(this)
        );

        commandManager.registerMessage(MessageKey.INVALID_ARGUMENT, (sender, invalidArgumentContext) ->
                ChatUtil.sendMessage(sender, getLangFile().getMessages().getCommand().getInvalidArgument()));

        commandManager.registerMessage(MessageKey.UNKNOWN_COMMAND, (sender, invalidArgumentContext) ->
                ChatUtil.sendMessage(sender, getLangFile().getMessages().getCommand().getUnknownCommand()));

        commandManager.registerMessage(MessageKey.NOT_ENOUGH_ARGUMENTS, (sender, invalidArgumentContext) ->
                ChatUtil.sendMessage(sender, getLangFile().getMessages().getCommand().getNotEnoughArguments()));

        commandManager.registerMessage(MessageKey.TOO_MANY_ARGUMENTS, (sender, invalidArgumentContext) ->
                ChatUtil.sendMessage(sender, getLangFile().getMessages().getCommand().getTooManyArguments()));

        commandManager.registerMessage(BukkitMessageKey.NO_PERMISSION, (sender, invalidArgumentContext) ->
                ChatUtil.sendMessage(sender, getLangFile().getMessages().getCommand().getNoPerm()));
    }

    public void sendPlayerToServer(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    public void sendStatus(Player player, boolean isAuthenticated) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ONLINE");
        out.writeUTF("losauth:status");
        ByteArrayDataOutput dataOut = ByteStreams.newDataOutput();
        dataOut.writeUTF(player.getName());
        dataOut.writeBoolean(isAuthenticated);
        byte[] dataBytes = dataOut.toByteArray();
        out.writeShort(dataBytes.length);
        out.write(dataBytes);
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    public void forceLogin(Player player) {
        STATUS_MAP.put(player.getName(), AuthResponse.SUCCESS);
        ChatUtil.sendMessage(player, langFile.getMessages().getLogin().getSuccess());
        this.sendStatus(player, true);

        if (configFile.getSettings().getSendAfterAuth().isEnabled()) {
            this.getServer().getScheduler().runTaskLater(this, () -> {
                this.sendPlayerToServer(player, configFile.getSettings().getSendAfterAuth().getServer());
            }, 20L);
        }
    }

    public void forceRegister(Player player) {
        STATUS_MAP.put(player.getName(), AuthResponse.SUCCESS);
        ChatUtil.sendMessage(player, langFile.getMessages().getRegister().getSuccess());
        this.sendStatus(player, true);

        if (configFile.getSettings().getSendAfterAuth().isEnabled()) {
            this.getServer().getScheduler().runTaskLater(this, () -> {
                this.sendPlayerToServer(player, configFile.getSettings().getSendAfterAuth().getServer());
            }, 20L);
        }
    }

    public boolean isAuthenticated(Player player) {
        AuthResponse response = STATUS_MAP.get(player.getName());
        return response != null && response.isAuthenticated();
    }

}
