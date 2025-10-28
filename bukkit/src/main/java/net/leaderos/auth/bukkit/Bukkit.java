package net.leaderos.auth.bukkit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.tcoded.folialib.FoliaLib;
import dev.triumphteam.cmd.bukkit.BukkitCommandManager;
import dev.triumphteam.cmd.bukkit.message.BukkitMessageKey;
import dev.triumphteam.cmd.core.message.MessageKey;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import lombok.Getter;
import net.leaderos.auth.bukkit.command.LeaderOSCommand;
import net.leaderos.auth.bukkit.command.LoginCommand;
import net.leaderos.auth.bukkit.command.RegisterCommand;
import net.leaderos.auth.bukkit.command.TfaCommand;
import net.leaderos.auth.bukkit.configuration.Config;
import net.leaderos.auth.bukkit.configuration.Language;
import net.leaderos.auth.bukkit.helpers.ChatUtil;
import net.leaderos.auth.bukkit.helpers.ConsoleLogger;
import net.leaderos.auth.bukkit.helpers.DebugBukkit;
import net.leaderos.auth.bukkit.listener.*;
import net.leaderos.auth.shared.Shared;
import net.leaderos.auth.shared.enums.SessionState;
import net.leaderos.auth.shared.helpers.UrlUtil;
import net.leaderos.auth.shared.model.response.GameSessionResponse;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Getter
public class Bukkit extends JavaPlugin {

    @Getter
    private static Bukkit instance;

    private FoliaLib foliaLib;

    private Language langFile;
    private Config configFile;

    private Shared shared;

    private BukkitCommandManager<CommandSender> commandManager;

    @Getter
    private List<String> allowedCommands;

    @Getter
    private final Map<String, GameSessionResponse> sessions = Maps.newHashMap();

    @Override
    public void onEnable() {
        instance = this;
        foliaLib = new FoliaLib(this);

        setupFiles();

        // Cache allowed commands
        cacheAllowedCommands();

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

        registerLoggerFilters(new ConsoleLogger());

        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Try to register 1.9 player listeners
        if (isClassLoaded("org.bukkit.event.player.PlayerSwapHandItemsEvent")) {
            getServer().getPluginManager().registerEvents(new PlayerListener19(this), this);
        }
        // Register listener for 1.11 events if available
        if (isClassLoaded("org.bukkit.event.entity.EntityAirChangeEvent")) {
            getServer().getPluginManager().registerEvents(new PlayerListener111(this), this);
        }
    }

    @Override
    public void onDisable() {
        foliaLib.getScheduler().cancelAllTasks();
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
            Class langClass = Class.forName("net.leaderos.auth.bukkit.configuration.lang." + langName);
            Class<Language> languageClass = langClass;
            this.langFile = ConfigManager.create(languageClass, (it) -> {
                it.withConfigurer(new YamlBukkitConfigurer());
                it.withBindFile(new File(this.getDataFolder().getAbsolutePath() + "/lang", langName + ".yml"));
                it.saveDefaults();
                it.load(true);
            });
        } catch (Exception exception) {
            getLogger().log(Level.WARNING, "ErrorCode loading config.yml", exception);
        }
    }

    private void setupCommands() {
        commandManager = BukkitCommandManager.create(this);

        commandManager.registerCommand(
                new LeaderOSCommand(),
                new LoginCommand(this, "login", getConfigFile().getSettings().getLoginCommands()),
                new RegisterCommand(this, "register", getConfigFile().getSettings().getRegisterCommands()),
                new TfaCommand(this, "tfa", getConfigFile().getSettings().getTfaCommands())
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

    public boolean isAuthenticated(Player player) {
        GameSessionResponse response = sessions.get(player.getName());
        return response != null && response.getState() == SessionState.AUTHENTICATED;
    }

    public void cacheAllowedCommands() {
        allowedCommands = Lists.newArrayList();
        allowedCommands.addAll(getConfigFile().getSettings().getLoginCommands());
        allowedCommands.addAll(getConfigFile().getSettings().getRegisterCommands());
        allowedCommands.addAll(getConfigFile().getSettings().getTfaCommands());
    }

    private void registerLoggerFilters(Filter... filters) {
        org.apache.logging.log4j.Logger rootLogger = LogManager.getRootLogger();
        if (!(rootLogger instanceof Logger)) {
            // in case the root logger is not the expected instance of Logger, just return
            // because there is something wrong
            return;
        }

        Logger logger = (Logger) rootLogger;
        for (Filter filter : filters) {
            // register all filters onto the root logger
            logger.addFilter(filter);
        }
    }

    /**
     * Returns whether the class exists in the current class loader.
     *
     * @param className the class name to check
     *
     * @return true if the class is loaded, false otherwise
     */
    public static boolean isClassLoaded(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
