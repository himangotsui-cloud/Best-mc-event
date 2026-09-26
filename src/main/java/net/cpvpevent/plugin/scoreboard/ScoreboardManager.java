package net.cpvpevent.plugin.scoreboard;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.event.PlayerEventState;
import net.cpvpevent.plugin.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

/**
 * Drives a single centralized scoreboard-update task for all online players
 * rather than one task per player. Supports PlaceholderAPI placeholders
 * when installed, and falls back to internal values otherwise.
 */
public class ScoreboardManager {

    private final CPVPEventPlus plugin;
    private BukkitTask updateTask;
    private boolean placeholderApiPresent;

    public ScoreboardManager(CPVPEventPlus plugin) {
        this.plugin = plugin;
        this.placeholderApiPresent = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public void start() {
        if (!plugin.configManager().scoreboard().getBoolean("enabled", true)) return;

        int interval = plugin.configManager().scoreboard().getInt("update-interval-ticks", 20);
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateAll, 0L, interval);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void updateAll() {
        String title = Text.color(plugin.configManager().scoreboard().getString("title", "&bEvent"));
        List<String> lines = plugin.configManager().scoreboard().getStringList("lines");

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Scoreboard board = plugin.getServer().getScoreboardManager().getNewScoreboard();
            Objective objective = board.registerNewObjective("cpvp", "dummy", title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            int score = lines.size();
            for (String rawLine : lines) {
                String resolved = resolvePlaceholders(player, rawLine);
                // Scoreboard lines must be unique; pad with invisible color codes if needed.
                Team team = board.registerNewTeam("line" + score);
                String entry = uniqueEntry(score);
                team.addEntry(entry);
                team.setPrefix(cap(resolved, 64));
                objective.getScore(entry).setScore(score);
                score--;
            }

            player.setScoreboard(board);
        }
    }

    private String uniqueEntry(int index) {
        String[] colors = {"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"};
        return colors[index % colors.length] + "§r";
    }

    private String cap(String input, int max) {
        return input.length() > max ? input.substring(0, max) : input;
    }

    private String resolvePlaceholders(Player player, String line) {
        String working = line;

        if (placeholderApiPresent) {
            working = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, working);
        }

        // Internal fallbacks for our own placeholders, always applied so the
        // scoreboard works even without PlaceholderAPI installed.
        working = working.replace("%cpvp_ping%", String.valueOf(player.getPing()));
        working = working.replace("%cpvp_players_alive%", String.valueOf(plugin.eventManager().aliveCount()));
        working = working.replace("%cpvp_border%", String.valueOf(Math.round(plugin.borderManager().currentSize())));
        working = working.replace("%cpvp_rekit_deaths%", plugin.rekitManager().deathsSince(player) + "/" + plugin.rekitManager().deathsRequired());
        working = working.replace("%cpvp_rekit%", plugin.rekitManager().isGlobalEnabled() ? "Enabled" : "Disabled");
        working = working.replace("%cpvp_totems%", String.valueOf(countTotems(player)));
        working = working.replace("%cpvp_revive%", plugin.reviveManager().soloProgress(player) + "/" + plugin.reviveManager().killsRequired());
        working = working.replace("%cpvp_team%", plugin.partyManager().partyOf(player)
                .map(p -> String.valueOf(p.size())).orElse("Solo"));

        return Text.color(working);
    }

    private int countTotems(Player player) {
        int count = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == org.bukkit.Material.TOTEM_OF_UNDYING) {
                count += item.getAmount();
            }
        }
        if (player.getInventory().getItemInOffHand().getType() == org.bukkit.Material.TOTEM_OF_UNDYING) {
            count += player.getInventory().getItemInOffHand().getAmount();
        }
        return count;
    }
}
