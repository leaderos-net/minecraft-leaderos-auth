package net.leaderos.auth.bungee.configuration;

import com.google.common.collect.Lists;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import lombok.Getter;
import lombok.Setter;
import net.leaderos.shared.enums.DebugMode;

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

        @Comment({
                "Debug mode for API requests.",
                "Available modes:",
                "DISABLED: No debug messages",
                "ENABLED: All debug messages",
                "ONLY_ERRORS: Only error messages"
        })
        private DebugMode debugMode = DebugMode.ONLY_ERRORS;

        @Comment("Players will be redirected to this server to login/register.")
        private String authServer = "auth_lobby";

        @Comment("List of commands that will be allowed")
        private List<String> allowedCommands = Lists.newArrayList("login", "log", "l", "giris", "giriş", "gir", "register", "reg", "kayit", "kayıt", "kaydol");

    }

}