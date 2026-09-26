package net.cpvpevent.plugin.border;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.event.PlayerEventState;
import net.cpvpevent.plugin.util.Text;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the world border animation and outside-border damage from a single
 * centralized repeating task rather than per-player tasks, keeping the
 * system cheap even with many players outside the border simultaneously.
 */
public class BorderManager {

    private final CPVPEventPlus plugin;

    private BukkitTask animationTask;
    private BukkitTask damageTask;

    private double startSize;
    private double targetSize;
    private long totalTicks;
    private long elapsedTicks;
    private boolean paused;

    // Tracks how long (in interval-ticks) each player has been continuously outside the border.
    private final Map<UUID, Long> timeOutside = new HashMap<>();

    public BorderManager(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    public void shrinkBorder(double targetDistance, long timeSeconds) {
        World world = plugin.getServer().getWorlds().get(0);
        WorldBorder border = world.getWorldBorder();

        stopBorder();

        this.startSize = border.getSize();
        this.targetSize = targetDistance;
        this.totalTicks = Math.max(1, timeSeconds * 20L);
        this.elapsedTicks = 0;
        this.paused = false;

        plugin.getServer().broadcastMessage(Text.color(
                plugin.configManager().message("border.shrinking")
                        .replace("%size%", String.valueOf((long) targetDistance))
                        .replace("%time%", String.valueOf(timeSeconds))));

        long updateFrequencyTicks = 10L; // update every half second - smooth without spamming packets
        animationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (paused) return;

            elapsedTicks += updateFrequencyTicks;
            double progress = Math.min(1.0, (double) elapsedTicks / (double) totalTicks);
            double currentSize = startSize + (targetSize - startSize) * progress;

            border.setSize(Math.max(1, currentSize));

            if (plugin.configManager().config().getBoolean("border.action-bar-enabled", true)) {
                String format = plugin.configManager().config().getString("border.action-bar-format", "&bBorder: &f%size%");
                String actionBar = Text.replace(format, "%size%", String.valueOf(Math.round(currentSize)));
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    player.sendActionBar(actionBar);
                }
            }

            if (progress >= 1.0) {
                animationTask.cancel();
                animationTask = null;
            }
        }, 0L, updateFrequencyTicks);

        startDamageTask();
    }

    public void stopBorder() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
        if (damageTask != null) {
            damageTask.cancel();
            damageTask = null;
        }
        timeOutside.clear();
        plugin.getServer().broadcastMessage(plugin.configManager().message("border.stopped"));
    }

    public void pauseBorder() {
        paused = true;
    }

    public void resumeBorder() {
        paused = false;
        plugin.getServer().broadcastMessage(plugin.configManager().message("border.resumed"));
    }

    public double currentSize() {
        World world = plugin.getServer().getWorlds().get(0);
        return world.getWorldBorder().getSize();
    }

    private void startDamageTask() {
        if (damageTask != null) return;

        int intervalTicks = plugin.configManager().config().getInt("border.damage.interval-ticks", 20);
        double baseDamage = plugin.configManager().config().getDouble("border.damage.base-damage", 1.0);
        double maxDamage = plugin.configManager().config().getDouble("border.damage.max-damage", 8.0);
        double perDistance = plugin.configManager().config().getDouble("border.damage.damage-per-distance", 0.5);

        boolean stormEnabled = plugin.configManager().config().getBoolean("border.storm-sickness.enabled", true);
        long stormDelayIntervals = Math.max(1, plugin.configManager().config().getInt("border.storm-sickness.delay-seconds", 30) * 20L / intervalTicks);

        damageTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (paused) return;

            World world = plugin.getServer().getWorlds().get(0);
            WorldBorder border = world.getWorldBorder();

            for (Player player : world.getPlayers()) {
                if (plugin.eventManager().stateOf(player) == PlayerEventState.SPECTATOR) {
                    timeOutside.remove(player.getUniqueId());
                    continue;
                }

                Location loc = player.getLocation();
                double distanceOutside = distanceOutsideBorder(border, loc);

                if (distanceOutside <= 0) {
                    timeOutside.remove(player.getUniqueId());
                    continue;
                }

                double damage = Math.min(maxDamage, baseDamage + (distanceOutside * perDistance));
                player.damage(damage);
                player.sendActionBar(plugin.configManager().message("border.storm-warning"));

                long intervalsOutside = timeOutside.merge(player.getUniqueId(), 1L, Long::sum);

                if (stormEnabled && intervalsOutside >= stormDelayIntervals) {
                    player.setHealth(0.0);
                    player.sendMessage(plugin.configManager().message("border.storm-death"));
                    timeOutside.remove(player.getUniqueId());
                }
            }
        }, intervalTicks, intervalTicks);
    }

    private double distanceOutsideBorder(WorldBorder border, Location loc) {
        Location center = border.getCenter();
        double half = border.getSize() / 2.0;

        double dx = Math.abs(loc.getX() - center.getX());
        double dz = Math.abs(loc.getZ() - center.getZ());

        double outsideX = dx - half;
        double outsideZ = dz - half;

        return Math.max(outsideX, outsideZ);
    }

    public void handlePlayerQuit(Player player) {
        timeOutside.remove(player.getUniqueId());
    }
}
