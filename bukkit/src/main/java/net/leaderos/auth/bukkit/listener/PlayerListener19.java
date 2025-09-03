package net.leaderos.auth.bukkit.listener;

import lombok.RequiredArgsConstructor;
import net.leaderos.auth.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

@RequiredArgsConstructor
public class PlayerListener19 implements Listener {

    private final Bukkit plugin;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }
}
