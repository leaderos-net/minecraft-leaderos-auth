package net.leaderos.auth.bukkit.helpers;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.inventivetalent.bossbar.BossBarAPI;
import java.util.HashMap;

public class BossBarUtil {

    private static final HashMap<Player, BossBar> activeBars = new HashMap<>();

    /**
     * Checks if the current server version is legacy (requires NMS packet system)
     * @return true if legacy version (1.8.x), false if modern (1.9+)
     */
    private static boolean isLegacyVersion() {
        try {
            Class.forName("org.bukkit.boss.BossBar");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    /**
     * Shows or updates the boss bar for a player.
     */
    public static void showBossBar(Player player, String text, double progress, String color, String style) {
        text = ChatUtil.color(text);

        if (isLegacyVersion()) {
            BossBarAPI.removeAllBars(player);
            BossBarAPI.addBar(
                    player,
                    new TextComponent(text),
                    BossBarAPI.Color.valueOf(color),
                    BossBarAPI.Style.valueOf(style),
                    (float) progress
            );
        } else {
            try {
                BossBar bossBar = activeBars.get(player);

                if (bossBar == null) {
                    // Transform style name
                    if (style.equals("PROGRESS")) {
                        style = "SOLID";
                    }
                    if (style.equals("NOTCHED_6")) {
                        style = "SEGMENTED_6";
                    }
                    if (style.equals("NOTCHED_10")) {
                        style = "SEGMENTED_10";
                    }
                    if (style.equals("NOTCHED_12")) {
                        style = "SEGMENTED_12";
                    }
                    if (style.equals("NOTCHED_20")) {
                        style = "SEGMENTED_20";
                    }

                    bossBar = Bukkit.createBossBar(
                            text,
                            BarColor.valueOf(color),
                            BarStyle.valueOf(style)
                    );
                    bossBar.addPlayer(player);
                    activeBars.put(player, bossBar);
                } else {
                    bossBar.setTitle(text);
                    bossBar.setColor(BarColor.valueOf(color));
                }

                bossBar.setProgress(progress);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Hides and removes the boss bar for a player
     */
    public static void hideBossBar(Player player) {
        if (isLegacyVersion()) {
            BossBarAPI.removeAllBars(player);
        } else {
            BossBar bossBar = activeBars.remove(player);
            if (bossBar != null) {
                try {
                    bossBar.removeAll();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
