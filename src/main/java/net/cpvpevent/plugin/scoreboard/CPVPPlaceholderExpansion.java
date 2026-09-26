package net.cpvpevent.plugin.scoreboard;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Registers %cpvp_...% placeholders with PlaceholderAPI when it is present.
 * The plugin functions fully without PAPI installed; this only adds
 * compatibility for other plugins/scoreboards that want these values.
 */
public class CPVPPlaceholderExpansion extends PlaceholderExpansion {

    private final CPVPEventPlus plugin;

    public CPVPPlaceholderExpansion(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cpvp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "CPVPEventPlus";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        return switch (params) {
            case "totems" -> String.valueOf(countTotems(player));
            case "ping" -> String.valueOf(player.getPing());
            case "rekit_deaths" -> plugin.rekitManager().deathsSince(player) + "/" + plugin.rekitManager().deathsRequired();
            case "border" -> String.valueOf(Math.round(plugin.borderManager().currentSize()));
            case "players_alive" -> String.valueOf(plugin.eventManager().aliveCount());
            case "rekit" -> plugin.rekitManager().isGlobalEnabled() ? "Enabled" : "Disabled";
            case "revive" -> plugin.reviveManager().soloProgress(player) + "/" + plugin.reviveManager().killsRequired();
            case "team" -> plugin.partyManager().partyOf(player).map(p -> String.valueOf(p.size())).orElse("Solo");
            default -> null;
        };
    }

    private int countTotems(Player player) {
        int count = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == org.bukkit.Material.TOTEM_OF_UNDYING) {
                count += item.getAmount();
            }
        }
        return count;
    }
}
