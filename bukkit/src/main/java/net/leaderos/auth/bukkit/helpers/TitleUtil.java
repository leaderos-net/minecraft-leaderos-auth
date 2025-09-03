package net.leaderos.auth.bukkit.helpers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class TitleUtil {

    private static final String SERVER_VERSION;
    private static final boolean IS_LEGACY;

    // NMS classes and methods for legacy versions (1.8-1.10)
    private static Class<?> packetPlayOutTitleClass;
    private static Class<?> chatComponentTextClass;
    private static Class<?> iChatBaseComponentClass;
    private static Class<?> packetClass;
    private static Object enumTitleAction_TITLE;
    private static Object enumTitleAction_SUBTITLE;
    private static Object enumTitleAction_TIMES;
    private static Object enumTitleAction_CLEAR;
    private static Method playerConnectionSendPacketMethod;
    private static Method getHandleMethod;

    static {
        SERVER_VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        IS_LEGACY = isLegacyVersion();

        if (IS_LEGACY) {
            try {
                initializeNMS();
            } catch (Exception e) {
                System.err.println("[TitleUtil] Failed to initialize NMS for version: " + SERVER_VERSION);
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if the current server version is legacy (requires NMS packet system)
     * @return true if legacy version, false if modern
     */
    private static boolean isLegacyVersion() {
        // Check if sendTitle method exists in Player class
        try {
            Player.class.getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            return false; // Method exists, modern version
        } catch (NoSuchMethodException e) {
            return true; // Method doesn't exist, legacy version
        }
    }

    /**
     * Initializes NMS classes and methods for legacy versions
     * @throws Exception if initialization fails
     */
    private static void initializeNMS() throws Exception {
        String nmsPackage = "net.minecraft.server." + SERVER_VERSION;
        String craftBukkitPackage = "org.bukkit.craftbukkit." + SERVER_VERSION;

        // Load NMS classes
        packetPlayOutTitleClass = Class.forName(nmsPackage + ".PacketPlayOutTitle");
        chatComponentTextClass = Class.forName(nmsPackage + ".ChatComponentText");
        iChatBaseComponentClass = Class.forName(nmsPackage + ".IChatBaseComponent");
        packetClass = Class.forName(nmsPackage + ".Packet");

        // Get enum values for title actions
        Class<?> enumTitleActionClass = Class.forName(nmsPackage + ".PacketPlayOutTitle$EnumTitleAction");
        enumTitleAction_TITLE = enumTitleActionClass.getField("TITLE").get(null);
        enumTitleAction_SUBTITLE = enumTitleActionClass.getField("SUBTITLE").get(null);
        enumTitleAction_TIMES = enumTitleActionClass.getField("TIMES").get(null);
        enumTitleAction_CLEAR = enumTitleActionClass.getField("CLEAR").get(null);

        // Get CraftPlayer class and getHandle method
        Class<?> craftPlayerClass = Class.forName(craftBukkitPackage + ".entity.CraftPlayer");
        getHandleMethod = craftPlayerClass.getMethod("getHandle");

        // Get PlayerConnection and sendPacket method
        Class<?> entityPlayerClass = Class.forName(nmsPackage + ".EntityPlayer");
        Class<?> playerConnectionClass = Class.forName(nmsPackage + ".PlayerConnection");
        playerConnectionSendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetClass);
    }

    /**
     * Sends a title to the specified player
     * @param player Target player
     * @param title Main title (can be null)
     * @param subtitle Subtitle (can be null)
     * @param fadeIn Fade in time in ticks
     * @param stay Stay time in ticks
     * @param fadeOut Fade out time in ticks
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }

        if (IS_LEGACY) {
            sendTitleLegacy(player, title, subtitle, fadeIn, stay, fadeOut);
        } else {
            sendTitleModern(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Sends a title to the player with default timings (1s fade in, 3s stay, 1s fade out)
     * @param player Target player
     * @param title Main title
     * @param subtitle Subtitle
     */
    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 20, 60, 20);
    }

    /**
     * Sends only a title to the player with default timings
     * @param player Target player
     * @param title Main title
     */
    public static void sendTitle(Player player, String title) {
        sendTitle(player, title, null, 20, 60, 20);
    }

    /**
     * Sends title using modern Bukkit API (1.11+)
     */
    private static void sendTitleModern(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            // Handle null values for modern API
            String safeTitle = (title != null) ? title : "";
            String safeSubtitle = (subtitle != null) ? subtitle : "";

            player.sendTitle(safeTitle, safeSubtitle, fadeIn, stay, fadeOut);
        } catch (Exception e) {
            System.err.println("[TitleUtil] Error sending title with modern API to player: " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Sends title using NMS packet system for legacy versions (1.8-1.10)
     */
    private static void sendTitleLegacy(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            // Get EntityPlayer and PlayerConnection
            Object entityPlayer = getHandleMethod.invoke(player);
            Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);

            // Send timing packet first
            Constructor<?> timesConstructor = packetPlayOutTitleClass.getConstructor(
                    enumTitleAction_TIMES.getClass(), iChatBaseComponentClass, int.class, int.class, int.class);
            Object timesPacket = timesConstructor.newInstance(enumTitleAction_TIMES, null, fadeIn, stay, fadeOut);
            playerConnectionSendPacketMethod.invoke(playerConnection, timesPacket);

            // Send main title if provided
            if (title != null && !title.isEmpty()) {
                Object titleComponent = chatComponentTextClass.getConstructor(String.class).newInstance(title);
                Constructor<?> titleConstructor = packetPlayOutTitleClass.getConstructor(
                        enumTitleAction_TITLE.getClass(), iChatBaseComponentClass);
                Object titlePacket = titleConstructor.newInstance(enumTitleAction_TITLE, titleComponent);
                playerConnectionSendPacketMethod.invoke(playerConnection, titlePacket);
            }

            // Send subtitle if provided
            if (subtitle != null && !subtitle.isEmpty()) {
                Object subtitleComponent = chatComponentTextClass.getConstructor(String.class).newInstance(subtitle);
                Constructor<?> subtitleConstructor = packetPlayOutTitleClass.getConstructor(
                        enumTitleAction_SUBTITLE.getClass(), iChatBaseComponentClass);
                Object subtitlePacket = subtitleConstructor.newInstance(enumTitleAction_SUBTITLE, subtitleComponent);
                playerConnectionSendPacketMethod.invoke(playerConnection, subtitlePacket);
            }

        } catch (Exception e) {
            System.err.println("[TitleUtil] Error sending title with NMS to player: " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Clears the title for the specified player
     * @param player Target player
     */
    public static void clearTitle(Player player) {
        if (player == null) {
            return;
        }

        if (IS_LEGACY) {
            clearTitleLegacy(player);
        } else {
            clearTitleModern(player);
        }
    }

    /**
     * Clears title using modern Bukkit API
     */
    private static void clearTitleModern(Player player) {
        try {
            // Try resetTitle first (1.11+), fallback to empty title if not available
            try {
                player.resetTitle();
            } catch (NoSuchMethodError e) {
                // Fallback for versions that have sendTitle but not resetTitle
                player.sendTitle("", "", 0, 0, 0);
            }
        } catch (Exception e) {
            System.err.println("[TitleUtil] Error clearing title with modern API for player: " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Clears title using NMS packet system for legacy versions
     */
    private static void clearTitleLegacy(Player player) {
        try {
            // Get EntityPlayer and PlayerConnection
            Object entityPlayer = getHandleMethod.invoke(player);
            Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);

            // Send clear packet
            Constructor<?> clearConstructor = packetPlayOutTitleClass.getConstructor(
                    enumTitleAction_CLEAR.getClass(), iChatBaseComponentClass);
            Object clearPacket = clearConstructor.newInstance(enumTitleAction_CLEAR, null);
            playerConnectionSendPacketMethod.invoke(playerConnection, clearPacket);

        } catch (Exception e) {
            System.err.println("[TitleUtil] Error clearing title with NMS for player: " + player.getName());
            e.printStackTrace();
        }
    }

    /**
     * Gets the server version string
     * @return Server version (e.g., "v1_8_R3")
     */
    public static String getServerVersion() {
        return SERVER_VERSION;
    }

    /**
     * Checks if the server is running a legacy version that requires NMS
     * @return true if legacy version, false if modern
     */
    public static boolean isLegacy() {
        return IS_LEGACY;
    }

    /**
     * Checks if the TitleUtil is properly initialized and ready to use
     * @return true if initialized successfully, false otherwise
     */
    public static boolean isInitialized() {
        if (IS_LEGACY) {
            return packetPlayOutTitleClass != null &&
                    chatComponentTextClass != null &&
                    enumTitleAction_TITLE != null &&
                    playerConnectionSendPacketMethod != null &&
                    getHandleMethod != null;
        } else {
            return true; // Modern versions don't need special initialization
        }
    }
}