package net.leaderos.auth.shared.helpers;

/**
 * URL utility class for formatting URLs
 */
public class UrlUtil {

    /**
     * Formats url
     *
     * @param url of api
     * @return String of url
     */
    public static String format(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }
}
