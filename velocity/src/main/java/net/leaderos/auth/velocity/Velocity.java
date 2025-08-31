package net.leaderos.auth.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Getter;
import lombok.Setter;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.command.LimboCommandMeta;
import net.elytrium.limboapi.api.player.GameMode;
import net.leaderos.auth.velocity.commands.LeaderOSCommand;
import net.leaderos.auth.velocity.configuration.Config;
import net.leaderos.auth.velocity.configuration.Language;
import net.leaderos.auth.velocity.helpers.DebugVelocity;
import net.leaderos.auth.velocity.listener.ConnectionListener;
import net.leaderos.shared.Shared;
import net.leaderos.shared.helpers.UrlUtil;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

/**
 * Main class of velocity
 */
@Getter
@Setter
@Plugin(
        id = "leaderosauth",
        name = "LeaderOS-Auth",
        version = "1.0.1",
        url = "https://leaderos.net",
        description = "LeaderOS Auth for Velocity",
        authors = {"leaderos", "efekurbann"},
        dependencies = {@Dependency(id = "limboapi")}
)
public class Velocity {

    /**
     * Instance of plugin
     */
    @Getter
    private static Velocity instance;
    /**
     * Instance of server
     */
    private final ProxyServer server;
    /**
     * Logger of server
     */
    private final Logger logger;

    /**
     * Data directory of server
     */
    private final Path dataDirectory;
    /**
     * bStats metrics
     */
    private final Metrics.Factory metricsFactory;
    /**
     * Config file of plugin
     */
    private Config configFile;
    /**
     * Lang file of plugin
     */
    private Language langFile;
    @Getter
    private CommandManager commandManager;
    /**
     * Shared holder
     */
    private Shared shared;
    @Getter
    private LimboFactory factory;
    @Getter
    private Limbo limboServer;

    /**
     * Constructor of main class
     *
     * @param server         proxyserver
     * @param logger         logger class
     * @param dataDirectory  data path
     * @param metricsFactory bStats metrics
     */
    @Inject
    public Velocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory,
                    Metrics.Factory metricsFactory) {
        instance = this;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    /**
     * onEnable event of velocity
     *
     * @param event of startup
     */
    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        commandManager = getServer().getCommandManager();
        setupFiles();

        this.shared = new Shared(
                UrlUtil.format(getConfigFile().getSettings().getUrl()),
                getConfigFile().getSettings().getApiKey(),
                new DebugVelocity());

        if (getConfigFile().getSettings().getUrl().equals("https://yourwebsite.com")) {
            getLogger()
                    .warn("You have not set the API URL in the config.yml file. Please set it to your LeaderOS URL.");
        } else if (getConfigFile().getSettings().getUrl().startsWith("http://")) {
            getLogger().warn(
                    "You are using an insecure URL (http://) for the API. Please use https:// for security reasons.");
        }

        CommandMeta commandMeta = Velocity.getInstance().getCommandManager().metaBuilder("leaderosauth")
                .plugin(Velocity.getInstance())
                .build();
        Velocity.getInstance().getCommandManager().register(commandMeta, new LeaderOSCommand());

        // bStats
        metricsFactory.make(this, 26806);

        this.factory = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi")
                .flatMap(PluginContainer::getInstance).orElseThrow();

        createServer();

        this.server.getEventManager().register(this, new ConnectionListener(this));
    }

    /**
     * onDisable event of velocity
     *
     * @param event of disable
     */
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {

    }

    /**
     * Setups config, lang and modules file file
     */
    public void setupFiles() {
        try {
            this.configFile = ConfigManager.create(Config.class, (it) -> {
                it.withConfigurer(new YamlSnakeYamlConfigurer());
                it.withBindFile(new File(getDataDirectory().toFile().getAbsolutePath(), "config.yml"));
                it.saveDefaults();
                it.load(true);
            });
            String langName = configFile.getSettings().getLang();
            Class langClass = Class.forName("net.leaderos.auth.velocity.configuration.lang." + langName);
            Class<Language> languageClass = langClass;
            this.langFile = ConfigManager.create(languageClass, (it) -> {
                it.withConfigurer(new YamlSnakeYamlConfigurer());
                it.withBindFile(new File(getDataDirectory().toFile().getAbsolutePath() + "/lang", langName + ".yml"));
                it.saveDefaults();
                it.load(true);
            });
        } catch (Exception exception) {
            getLogger().error("Error loading config.yml", exception);
        }
    }

    private void createServer() {
        VirtualWorld world = this.factory.createVirtualWorld(Dimension.THE_END, 0, 256, 0, 0, 0);

        limboServer = factory.createLimbo(world)
                .setName("LeaderOS-Auth")
                .setWorldTime(0)
                .setGameMode(GameMode.ADVENTURE);

        for (String command : this.configFile.getSettings().getLoginCommands()) {
            limboServer.registerCommand(new LimboCommandMeta(Collections.singleton(command)));
        }

        for (String command : this.configFile.getSettings().getRegisterCommands()) {
            limboServer.registerCommand(new LimboCommandMeta(Collections.singleton(command)));
        }
    }
}