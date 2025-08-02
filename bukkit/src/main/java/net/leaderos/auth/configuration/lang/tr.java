package net.leaderos.auth.configuration.lang;

import com.google.common.collect.Lists;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.NameModifier;
import eu.okaeri.configs.annotation.NameStrategy;
import eu.okaeri.configs.annotation.Names;
import lombok.Getter;
import lombok.Setter;
import net.leaderos.auth.configuration.Language;

import java.util.List;

/**
 * Turkish language configuration
 */
@Getter
@Setter
@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class tr extends Language {

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
    public static class Messages extends Language.Messages {

        private String prefix = "&3LeaderOS-Auth &8»";

        private String wait = "{prefix} &cÇok hızlı komut gönderiyorsunuz. Lütfen bir saniye bekleyin.";

        private String anErrorOccurred = "{prefix} &cİsteğiniz işlenirken bir hata oluştu. Lütfen daha sonra tekrar deneyin.";

        private List<String> kickTimeout = Lists.newArrayList(
                "&cSüre dolduğu için sunucudan atıldınız.",
                "&cDevam etmek için lütfen sunucuya tekrar bağlanın."
        );

        private List<String> kickAnError = Lists.newArrayList(
                "&cGiriş sistemlerine şu anda erişilemiyor.",
                "&cLütfen daha sonra tekrar deneyin."
        );

        private List<String> kickNotRegistered = Lists.newArrayList(
                "&cSunucuda kayıtlı değilsiniz!",
                "&cLütfen devam etmek için sitemizden kayıt olun."
        );

        private List<String> kickWrongPassword = Lists.newArrayList(
                "&cHatalı şifre!"
        );

        private String unknownAuthCommand = "{prefix} &cBilinmeyen komut! Lütfen &a/register <şifre> <şifre> &aveya &a/login <şifre> &ckomutlarını kullanın.";

        private String reload = "{prefix} &aEklenti başarıyla yeniden yüklendi.";

        private Register register = new Register();
        private Login login = new Login();
        /**
         * Command object
         */
        private Command command = new Command();

        @Getter
        @Setter
        public static class Register extends Language.Messages.Register {

            private String title = "&6KAYIT";

            private String subtitle = "&e/register <şifre> <şifre>";

            private int titleDuration = 30; // in seconds

            private String message = "{prefix} &eLütfen &a/register <şifre> <şifre> &ekomutu ile kayıt olun.";

            private String passwordMismatch = "{prefix} &cŞifreler uyuşmuyor!";

            private String passwordTooShort = "{prefix} &cŞifre en az {min} karakter uzunluğunda olmalıdır!";

            private String passwordTooLong = "{prefix} &cŞifre {max} karakterden kısa olmalıdır!";

            private String alreadyRegistered = "{prefix} &cZaten kayıtlısınız! Lütfen giriş yapınız.";

            private String invalidName = "{prefix} &cKullanıcı adınız geçersiz! Lütfen geçerli bir kullanıcı ad kullanınız.";

            private String registerLimit = "{prefix} &cİzin verilen maksimum kayıt sayısına ulaştınız!";

            private String success = "{prefix} &aBaşarıyla kayıt oldunuz!";

            private String unsafePassword = "{prefix} &cŞifreniz çok zayıf! Lütfen daha güçlü bir şifre seçiniz.";

        }

        @Getter
        @Setter
        public static class Login extends Language.Messages.Login {

            private String title = "&6GIRIŞ";

            private String subtitle = "&e/login <şifre>";

            private int titleDuration = 30; // in seconds

            private String message = "{prefix} &eLütfen &a/login <şifre> &ekomutu ile giriş yapın.";

            private String incorrectPassword = "{prefix} &cYanlış şifre!";

            private String accountNotFound = "{prefix} &cSunucumuza kayıtlı değilsiniz! Lütfen kayıt olunuz.";

            private String success = "{prefix} &aBaşarıyla giriş yaptınız!";

        }

        /**
         * Command arguments class
         */
        @Getter
        @Setter
        public static class Command extends Language.Messages.Command {

            /**
             * Invalid argument message
             */
            private String invalidArgument = "{prefix} &cGeçersiz argüman girdiniz!";

            /**
             * Unknown command message
             */
            private String unknownCommand = "{prefix} &cBilinmeyen komut!";

            /**
             * Not enough arguments message
             */
            private String notEnoughArguments = "{prefix} &cGerekli argümanları girmediniz!";

            /**
             * too many arguments message
             */
            private String tooManyArguments = "{prefix} &cÇok fazla argüman girdiniz!";

            /**
             * no perm message
             */
            private String noPerm = "{prefix} &cBu işlemi yapabilmek için yeterli yetkiye sahip değilsin!";

        }
    }
}