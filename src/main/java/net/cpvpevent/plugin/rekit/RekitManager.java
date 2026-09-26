package net.cpvpevent.plugin.rekit;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player death counts toward the configured rekit threshold and
 * manages the temporary "rekit available" window.
 */
public class RekitManager {

    private final CPVPEventPlus plugin;

    private boolean globalEnabled = true;
    private int deathsRequired = 5;
    private int activeDurationSeconds = 20;

    private final Map<UUID, Integer> deathCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> rekitAvailable = new ConcurrentHashMap<>();

    public RekitManager(CPVPEventPlus plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        this.globalEnabled = plugin.configManager().config().getBoolean("rekit.enabled", true);
        this.deathsRequired = plugin.configManager().config().getInt("rekit.deaths-required", 5);
        this.activeDurationSeconds = plugin.configManager().config().getInt("rekit.active-duration-seconds", 20);
    }

    public void setGlobalEnabled(boolean enabled) {
        this.globalEnabled = enabled;
    }

    public boolean isGlobalEnabled() {
        return globalEnabled;
    }

    public void setDeathsRequired(int deaths) {
        this.deathsRequired = deaths;
    }

    public void disableThreshold() {
        this.deathsRequired = -1;
    }

    public int deathsRequired() {
        return deathsRequired;
    }

    public void recordDeath(Player player) {
        if (!globalEnabled || deathsRequired <= 0) return;

        int count = deathCounts.merge(player.getUniqueId(), 1, Integer::sum);
        if (count >= deathsRequired) {
            grantRekit(player);
            deathCounts.put(player.getUniqueId(), 0);
        }
    }

    public void grantRekit(Player player) {
        rekitAvailable.put(player.getUniqueId(), true);
        player.sendMessage(plugin.configManager().message("rekit.available")
                .replace("%seconds%", String.valueOf(activeDurationSeconds)));

        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> rekitAvailable.remove(player.getUniqueId()),
                activeDurationSeconds * 20L);
    }

    public boolean consumeRekitIfAvailable(Player player) {
        Boolean available = rekitAvailable.remove(player.getUniqueId());
        if (available != null && available) {
            plugin.kitManager().giveDefaultKit(player);
            player.sendMessage(plugin.configManager().message("rekit.used"));
            return true;
        }
        return false;
    }

    public int deathsSince(Player player) {
        return deathCounts.getOrDefault(player.getUniqueId(), 0);
    }

    public void resetPlayer(Player player) {
        deathCounts.remove(player.getUniqueId());
        rekitAvailable.remove(player.getUniqueId());
    }

    public void resetAll() {
        deathCounts.clear();
        rekitAvailable.clear();
    }
}
