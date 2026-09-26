package net.cpvpevent.plugin.pvp;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.util.Text;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class PvPManager {

    private final CPVPEventPlus plugin;

    private boolean pvpEnabled = true;
    private boolean protectionEnabled = false;

    public PvPManager(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public boolean isProtectionEnabled() {
        return protectionEnabled;
    }

    public void setPvpEnabled(boolean enabled) {
        this.pvpEnabled = enabled;
        String key = enabled ? "pvp.enabled" : "pvp.disabled";
        plugin.getServer().broadcastMessage(plugin.configManager().message(key));
    }

    /** Sets state without broadcasting - used internally at event start. */
    public void forceState(boolean enabled) {
        this.pvpEnabled = enabled;
    }

    public void setProtectionEnabled(boolean enabled) {
        this.protectionEnabled = enabled;
        String key = enabled ? "pvp.protection-enabled" : "pvp.protection-disabled";
        plugin.getServer().broadcastMessage(plugin.configManager().message(key));
    }

    /**
     * Runs the 3-2-1-GO countdown, then re-enables PvP. Uses a single
     * repeating task rather than several chained delayed tasks.
     */
    public void runReEnableCountdown() {
        if (!plugin.configManager().config().getBoolean("pvp.countdown.enabled", true)) {
            setPvpEnabled(true);
            return;
        }

        setPvpEnabled(false);
        int seconds = plugin.configManager().config().getInt("pvp.countdown.seconds", 3);
        Sound tickSound = safeSound(plugin.configManager().config().getString("pvp.countdown.sound", "ENTITY_EXPERIENCE_ORB_PICKUP"));
        Sound goSound = safeSound(plugin.configManager().config().getString("pvp.countdown.go-sound", "ENTITY_ENDER_DRAGON_GROWL"));

        int[] remaining = { seconds };

        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (remaining[0] <= 0) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    player.sendTitle(Text.color(plugin.configManager().rawMessage("pvp.countdown-go")), "", 5, 20, 5);
                    if (goSound != null) player.playSound(player.getLocation(), goSound, 1f, 1f);
                }
                setPvpEnabled(true);
                task.cancel();
                return;
            }

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendTitle(Text.color("&e" + remaining[0]), "", 0, 20, 0);
                if (tickSound != null) player.playSound(player.getLocation(), tickSound, 1f, 1f);
            }
            remaining[0]--;
        }, 0L, 20L);
    }

    private Sound safeSound(String name) {
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (Exception ex) {
            return null;
        }
    }
}
