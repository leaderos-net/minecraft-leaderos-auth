package net.leaderos.auth.velocity.configuration;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import lombok.Getter;
import lombok.Setter;
import net.leaderos.shared.enums.DebugMode;
import net.leaderos.shared.enums.RegisterSecondArg;

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
        private String url = "https://yourwebsite.com";

        @Comment({
                "API Key for request",
                "You can get your API key from Dashboard > API",
        })
        private String apiKey = "YOUR_API_KEY";

        @Comment({
                "Debug mode for API requests.",
                "Available modes:",
                "DISABLED: No debug messages",
                "ENABLED: All debug messages",
                "ONLY_ERRORS: Only error messages"
        })
        private DebugMode debugMode = DebugMode.ONLY_ERRORS;

        @Comment({
                "Should session system be enabled?",
                "If enabled, players will be able to join the server without authentication if they succeeded an auth before (with the same IP)."
        })
        private boolean session = false;

        @Comment("Should unregistered players be kicked immediately?")
        private boolean kickNonRegistered = false;

        @Comment("Should players be kicked if they fail to log in with the wrong password?")
        private boolean kickOnWrongPassword = true;

        @Comment("How many seconds should players who fail to log in or register be given before they are kicked?")
        private int authTimeout = 60; // in seconds

        @Comment("How many seconds should players wait before sending another command?")
        private int commandCooldown = 3; // in seconds

        @Comment("Minimum password length for registration.")
        private int minPasswordLength = 5;

        @Comment({
                "Second argument the /register command should take:",
                "NONE = no 2nd argument (/register <password>)",
                "PASSWORD_CONFIRM = password confirmation (/register <password> <password>)",
                "EMAIL = email address (/register <password> <email>)"
        })
        private RegisterSecondArg registerSecondArg = RegisterSecondArg.PASSWORD_CONFIRM;

        @Comment("List of commands that will be used to log in")
        private List<String> loginCommands = List.of("login", "log", "l", "giris", "giriş", "gir");

        @Comment("List of commands that will be used to register")
        private List<String> registerCommands = List.of("register", "reg", "kayit", "kayıt", "kaydol");

        @Comment("Blacklist of passwords that cannot be used")
        private List<String> unsafePasswords = List.of("123456", "password", "qwerty", "123456789", "help", "sifre", "12345", "asd123", "qwe123");

    }
}