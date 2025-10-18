package net.leaderos.auth.velocity.configuration;

import com.google.common.collect.Lists;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Language configuration class
 */
@Getter
@Setter
@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class Language extends OkaeriConfig {

    /**
     * Settings menu of config
     */
    @Comment("Main settings")
    private Messages messages = new Messages();

    /**
     * Messages of plugin
     */
    @Getter
    @Setter
    public static class Messages extends OkaeriConfig {

        @Comment("Prefix of messages")
        private String prefix = "&3LeaderOS-Auth &8»";

        private String wait = "{prefix} &cYou are sending commands too quickly. Please wait a moment.";

        private String anErrorOccurred = "{prefix} &cAn error occurred while processing your request. Please try again later.";

        private List<String> kickTimeout = List.of(
                "&cYou have been kicked due to inactivity.",
                "&cPlease rejoin the server to continue."
        );

        private List<String> kickAnError = List.of(
                "&cOur auth servers is down at this moment.",
                "&cPlease try again later."
        );

        private List<String> kickNotRegistered = List.of(
                "&cYou are not registered!",
                "&cPlease register from our website to continue."
        );

        private List<String> kickWrongPassword = List.of(
                "&cWrong password!"
        );

        private List<String> kickInvalidUsername = Lists.newArrayList(
                "&cInvalid username!"
        );

        private List<String> kickEmailNotVerified = Lists.newArrayList(
                "&cPlease verify your email on our website to continue."
        );

        private String unknownAuthCommand = "{prefix} &cUnknown authentication command! Please use &a/register <password> <password> &cor &a/login <password> &ccommands.";

        private String reload = "{prefix} &aPlugin reloaded successfully.";

        private String alreadyAuthenticated = "{prefix} &cYou are already authenticated!";

        private Register register = new Register();
        private Login login = new Login();
        private Tfa tfa = new Tfa();

        /**
         * Command object
         */
        private Command command = new Command();

        @Getter
        @Setter
        public static class Register extends OkaeriConfig {

            private String title = "&6REGISTER";

            private String subtitle = "&e/register <password> <password>";

            private String bossBar = "&fPlease register within {seconds} seconds!";

            private String message = "{prefix} &ePlease register using &a/register <password> <password> &ecommand.";

            private String passwordMismatch = "{prefix} &cPasswords do not match!";

            private String passwordTooShort = "{prefix} &cPassword must be at least {min} characters long!";

            private String passwordTooLong = "{prefix} &cPassword must be shorter than {max} characters long!";

            private String alreadyRegistered = "{prefix} &cYou are already registered!";

            private String invalidPassword = "{prefix} &cPassword is invalid! Please enter a valid password.";

            private String invalidName = "{prefix} &cYour name is invalid! Please use a valid name.";

            private String invalidEmail = "{prefix} &cEnter a valid email address.";

            private String emailInUse = "{prefix} &cThis email is already in use!";

            private String registerLimit = "{prefix} &cYou have reached the maximum number of registrations allowed!";

            private String success = "{prefix} &aYou have successfully registered!";

            private String unsafePassword = "{prefix} &cYour password is too weak! Please choose a stronger password.";

        }

        @Getter
        @Setter
        public static class Login extends OkaeriConfig {

            private String title = "&6LOGIN";

            private String subtitle = "&e/login <password>";

            private String bossBar = "&fPlease login within {seconds} seconds!";

            private String message = "{prefix} &ePlease login using &a/login <password> &ecommand.";

            private String incorrectPassword = "{prefix} &cIncorrect password!";

            private String accountNotFound = "{prefix} &cYou are not registered!";

            private String success = "{prefix} &aYou have successfully logged in!";

        }

        @Getter
        @Setter
        public static class Tfa extends OkaeriConfig {

            private String title = "&6TFA";

            private String subtitle = "&e/tfa <code>";

            private String bossBar = "&fEnter your TFA code within {seconds} seconds!";

            private String required = "{prefix} &eTwo-factor authentication is required! Please enter your TFA code using &a/tfa <code> &ecommand.";

            private String usage = "{prefix} &ePlease enter your TFA code using &a/tfa <code> &ecommand.";

            private String notRequired = "{prefix} &cTwo-factor authentication is not required at this time.";

            private String invalidCode = "{prefix} &cInvalid TFA code! Please try again.";

            private String sessionNotFound = "{prefix} &cSession not found! Please login again.";

            private String verificationFailed = "{prefix} &cTFA verification failed! Please try again.";

            private String success = "{prefix} &aTwo-factor authentication successful!";

        }

        /**
         * Command arguments class
         */
        @Getter
        @Setter
        public static class Command extends OkaeriConfig {

            /**
             * Invalid argument message
             */
            private String invalidArgument = "{prefix} &cInvalid argument!";

            /**
             * Unknown command message
             */
            private String unknownCommand = "{prefix} &cUnknown command!";

            /**
             * Not enough arguments message
             */
            private String notEnoughArguments = "{prefix} &cNot enough arguments!";

            /**
             * too many arguments message
             */
            private String tooManyArguments = "{prefix} &cToo many arguments!";

            /**
             * no perm message
             */
            private String noPerm = "{prefix} &cYou do not have permission to do this action!";

        }
    }
}
