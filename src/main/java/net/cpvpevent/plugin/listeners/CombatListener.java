package net.cpvpevent.plugin.listeners;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.event.PlayerEventState;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class CombatListener implements Listener {

    private final CPVPEventPlus plugin;

    public CombatListener(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && !plugin.configManager().config().getBoolean("pvp.fall-damage-enabled", true)) {
            event.setCancelled(true);
            return;
        }

        if (plugin.eventManager().stateOf(player) == PlayerEventState.SPECTATOR
                || player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        if (!plugin.pvpManager().isPvpEnabled()) {
            event.setCancelled(true);
            attacker.sendMessage(plugin.configManager().message("pvp.action-blocked"));
            return;
        }

        if (plugin.partyManager().isFriendlyFire(attacker, victim)) {
            event.setCancelled(true);
        }
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        plugin.eventManager().markDead(victim);
        plugin.rekitManager().recordDeath(victim);

        if (killer != null && !killer.equals(victim)) {
            plugin.reviveManager().recordKill(killer, victim);
        }

        // RevFight matches take priority over normal spectator handling.
        plugin.revFightManager().handleDeathInArena(victim);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            victim.setGameMode(GameMode.SPECTATOR);
            if (plugin.rekitManager().consumeRekitIfAvailable(victim)) {
                victim.setGameMode(GameMode.SURVIVAL);
                plugin.eventManager().markAlive(victim);
            }
        });
    }
}
