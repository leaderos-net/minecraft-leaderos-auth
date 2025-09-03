package net.leaderos.auth.bukkit.listener;

import lombok.RequiredArgsConstructor;
import net.leaderos.auth.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;

@RequiredArgsConstructor
public class PlayerListener111 implements Listener {

    private final Bukkit plugin;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerAirChange(EntityAirChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (plugin.isAuthenticated((Player) event.getEntity())) return;

        event.setCancelled(true);
    }
}
