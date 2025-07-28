package net.leaderos.auth.velocity.configuration;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import lombok.Getter;
import lombok.Setter;
import net.leaderos.shared.model.DebugMode;

import java.util.List;

/**
 * Main config file
 */
@Getter
@Setter
@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class Config extends OkaeriConfig {

    /**
     * Settings menu of config
     */
    @Comment("Main settings")
    private Settings settings = new Settings();

    /**
     * Settings configuration of config
     */
    @Getter
    @Setter
    public static class Settings extends OkaeriConfig {
        @Comment("Language of plugin")
        private String lang = "en";

        @Comment("Url of your website")
        private String url = "https://v6.leaderos.com.tr";

        @Comment({
                "Debug mode for API requests.",
                "Available modes:",
                "DISABLED: No debug messages",
                "ENABLED: All debug messages",
                "ONLY_ERRORS: Only error messages"
        })
        private DebugMode debugMode = DebugMode.ONLY_ERRORS;

        @Comment("API Key for request")
        private String apiKey = "TR_474fc1c8ed2ebf582f225f6e0220ff8b";

        @Comment({"Should session system be enabled?",
                "If enabled, players will be able to join the server without authentication if they succeeded an auth before (with the same IP)."})
        private boolean session = false;

        @Comment("Should we block players who are not registered?")
        private boolean blockNotRegistered = false;

        @Comment("In seconds, how long should the authentication process take before timing out?")
        private int authTimeout = 60; // in seconds

        private List<String> loginCommands = List.of("login", "log", "l");

        private List<String> registerCommands = List.of("register", "reg", "r");

        private int minPasswordLength = 5;
        private int maxPasswordLength = 32;

        private List<String> unsafePasswords = List.of("123456", "password", "qwerty", "123456789", "help");

        private boolean kickOnWrongPassword = true;

    }
}