package net.leaderos.auth.shared.helpers;

import java.util.Random;

public class UserAgentUtil {
    
    private static final String BASE_USERAGENT = "MINECRAFT_LEADEROS_AUTH";
    private static final Random RANDOM = new Random();
    
    /**
     * Generate useragent based on session setting
     * @param isUnique true if unique useragent is needed
     * @return useragent string
     */
    public static String generateUserAgent(boolean isUnique) {
        if (isUnique) {
            // Generate 8 character unique ID
            String uniqueId = generateUniqueId();
            return BASE_USERAGENT + "_" + uniqueId;
        }

        return BASE_USERAGENT;
    }
    
    /**
     * Generate 8 character unique ID
     * @return 8 character alphanumeric string
     */
    private static String generateUniqueId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}

