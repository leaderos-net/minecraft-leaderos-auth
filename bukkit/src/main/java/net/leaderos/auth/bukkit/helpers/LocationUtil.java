package net.leaderos.auth.bukkit.helpers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

public class LocationUtil {
    public static String locationToString(Location loc) {
        return Objects.requireNonNull(loc.getWorld()).getName() + "," +
                loc.getX() + "," +
                loc.getY() + "," +
                loc.getZ() + "," +
                loc.getYaw() + "," +
                loc.getPitch();
    }

    public static Location stringToLocation(String str) {
        // world,x,y,z,yaw,pitch
        String[] parts = str.split(",");
        if (parts.length < 4) return null; // minimum world,x,y,z

        World world = Bukkit.getWorld(parts[0]);
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);

        float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
        float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;

        return new Location(world, x, y, z, yaw, pitch);
    }
}
