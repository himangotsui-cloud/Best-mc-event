package net.cpvpevent.plugin.listeners;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ProtectionListener implements Listener {

    private final CPVPEventPlus plugin;

    public ProtectionListener(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.pvpManager().isProtectionEnabled()) return;
        if (!plugin.configManager().config().getBoolean("pvp.protection.block-break-disabled", true)) return;
        if (event.getPlayer().hasPermission("cpvpevent.admin")) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.pvpManager().isProtectionEnabled()) return;
        if (!plugin.configManager().config().getBoolean("pvp.protection.block-place-disabled", true)) return;
        if (event.getPlayer().hasPermission("cpvpevent.admin")) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.pvpManager().isProtectionEnabled()) return;
        if (!plugin.configManager().config().getBoolean("pvp.protection.interact-restricted", true)) return;
        if (event.getPlayer().hasPermission("cpvpevent.admin")) return;

        if (event.getClickedBlock() != null) {
            switch (event.getClickedBlock().getType()) {
                case CHEST, ENDER_CHEST, CRAFTING_TABLE, FURNACE, ANVIL -> event.setCancelled(true);
                default -> { }
            }
        }
    }
}
