package net.leaderos.auth.listener;

import lombok.RequiredArgsConstructor;
import net.leaderos.auth.Bukkit;
import net.leaderos.auth.helpers.LocationUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

@RequiredArgsConstructor
public class PlayerListener implements Listener {

    private final Bukkit plugin;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && event.getFrom().getY() - event.getTo().getY() >= 0) {
            return;
        }

        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
        event.setTo(event.getFrom());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        Location spawn = LocationUtil.stringToLocation(plugin.getConfigFile().getSettings().getSpawn().getLocation());
        if (spawn != null && spawn.getWorld() != null) {
            event.setRespawnLocation(spawn);
        }
    }

    // block listeners
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerShearEntityEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        String message = event.getMessage().toLowerCase().substring(1).split(" ")[0];

        if (plugin.getAllowedCommands().stream().noneMatch(message::startsWith)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }


    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerHeldItem(PlayerItemHeldEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerConsumeItem(PlayerItemConsumeEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (plugin.isAuthenticated((Player) event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (plugin.isAuthenticated(event.getPlayer())) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (plugin.isAuthenticated((Player) event.getEntity())) return;

        event.getEntity().setFireTicks(0);
        event.setDamage(0);
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (plugin.isAuthenticated((Player) event.getDamager())) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (plugin.isAuthenticated((Player) event.getEntity())) return;

        event.setTarget(null);
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (plugin.isAuthenticated((Player) event.getEntity())) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void entityRegainHealthEvent(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (plugin.isAuthenticated((Player) event.getEntity())) return;

        event.setAmount(0);
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLowestEntityInteract(EntityInteractEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (plugin.isAuthenticated((Player) event.getEntity())) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        if (plugin.isAuthenticated((Player) event.getEntity().getShooter())) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (plugin.isAuthenticated((Player) event.getEntity())) return;

        event.setCancelled(true);
    }

}
