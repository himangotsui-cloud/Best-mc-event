package net.cpvpevent.plugin.revive;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.event.PlayerEventState;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReviveManager {

    private final CPVPEventPlus plugin;

    private boolean systemEnabled = true;
    private final Map<UUID, Integer> soloKillCounts = new ConcurrentHashMap<>();

    public ReviveManager(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    public void setEnabled(boolean enabled) {
        this.systemEnabled = enabled;
    }

    public boolean isEnabled() {
        return systemEnabled;
    }

    public void recordKill(Player killer, Player victim) {
        soloKillCounts.merge(killer.getUniqueId(), 1, Integer::sum);
        plugin.partyManager().addSharedReviveKill(killer);
    }

    public int killsRequired() {
        return plugin.configManager().config().getInt("revive.kills-required", 5);
    }

    public int soloProgress(Player player) {
        return soloKillCounts.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Attempts a non-op self-service revive. Returns true on success.
     */
    public boolean attemptPlayerRevive(Player reviver, Player target) {
        if (!systemEnabled || !plugin.configManager().config().getBoolean("revive.non-op-revive-enabled", true)) {
            reviver.sendMessage(plugin.configManager().message("revive.disabled-system"));
            return false;
        }

        boolean allowSelf = plugin.configManager().config().getBoolean("revive.allow-self-revive", false);
        if (reviver.equals(target) && !allowSelf) {
            reviver.sendMessage(plugin.configManager().message("revive.self-revive-disabled"));
            return false;
        }

        int required = killsRequired();
        int progress = Math.max(soloProgress(reviver), plugin.partyManager().reviveProgress(reviver));

        if (progress < required) {
            reviver.sendMessage(plugin.configManager().message("revive.not-enough-kills")
                    .replace("%kills%", String.valueOf(progress))
                    .replace("%required%", String.valueOf(required)));
            return false;
        }

        performRevive(target, true);
        consumeCounters(reviver, required);

        reviver.sendMessage(plugin.configManager().message("revive.revived-by-you").replace("%player%", target.getName()));
        target.sendMessage(plugin.configManager().message("revive.revived-target").replace("%reviver%", reviver.getName()));
        return true;
    }

    private void consumeCounters(Player reviver, int required) {
        soloKillCounts.merge(reviver.getUniqueId(), -required, Integer::sum);
        if (soloKillCounts.getOrDefault(reviver.getUniqueId(), 0) < 0) {
            soloKillCounts.put(reviver.getUniqueId(), 0);
        }
        plugin.partyManager().partyOf(reviver).ifPresent(party -> party.resetReviveKills());
    }

    /**
     * Operator/staff revive - bypasses kill requirements.
     */
    public void opRevive(Player target) {
        performRevive(target, true);
    }

    public void reviveAll() {
        List<Player> toRevive = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.eventManager().stateOf(player) == PlayerEventState.SPECTATOR) {
                toRevive.add(player);
            }
        }
        for (Player player : toRevive) {
            performRevive(player, true);
        }
        plugin.getServer().broadcastMessage(plugin.configManager().message("revive.all-revived"));
    }

    private void performRevive(Player target, boolean grantProtection) {
        target.setGameMode(GameMode.SURVIVAL);
        target.setHealth(target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
        target.setFoodLevel(20);
        plugin.eventManager().markAlive(target);

        Location world = plugin.getServer().getWorlds().get(0).getSpawnLocation();
        target.teleport(world);

        if (grantProtection) {
            int seconds = plugin.configManager().config().getInt("revive.protection-seconds", 5);
            target.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, seconds * 20, 4, false, true));
        }

        if (plugin.configManager().config().getBoolean("revive.give-rekit-on-revive", true)) {
            plugin.rekitManager().grantRekit(target);
        }

        boolean inventoryEmpty = target.getInventory().isEmpty();
        if (inventoryEmpty) {
            String kit = plugin.configManager().config().getString("revive.give-kit-if-empty", "k1");
            plugin.kitManager().giveKit(target, kit);
        }
    }

    public void resetAll() {
        soloKillCounts.clear();
    }
}
