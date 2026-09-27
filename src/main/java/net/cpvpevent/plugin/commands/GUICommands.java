package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.util.ItemBuilder;
import net.cpvpevent.plugin.util.Text;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class GUICommands implements CommandExecutor, TabCompleter {

    private final CPVPEventPlus plugin;

    public GUICommands(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.configManager().message("general.player-only"));
            return true;
        }
        if (!player.hasPermission("cpvpevent.eventsettings")) {
            player.sendMessage(plugin.configManager().message("general.no-permission"));
            return true;
        }

        if (label.equalsIgnoreCase("eventsettings")) {
            plugin.guiManager().openSettingsGUI(player);
        } else if (label.equalsIgnoreCase("eventools")) {
            plugin.guiManager().giveEventTools(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
