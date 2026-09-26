package net.cpvpevent.plugin.listeners;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles players joining mid-event (auto-spectator) and players
 * disconnecting during events, RevFights, or with active rekit/revive state.
 */
public class PlayerConnectionListener implements Listener {

    private final CPVPEventPlus plugin;

    public PlayerConnectionListener(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.eventManager().onPlayerJoinDuringEvent(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.borderManager().handlePlayerQuit(player);
        plugin.revFightManager().handleDisconnect(player);
        plugin.partyManager().leaveParty(player);
    }
}
