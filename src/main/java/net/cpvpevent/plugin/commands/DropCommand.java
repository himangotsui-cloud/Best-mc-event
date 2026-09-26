package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class DropCommand implements CommandExecutor, TabCompleter {

    private final CPVPEventPlus plugin;

    public DropCommand(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cpvpevent.admin")) {
            sender.sendMessage(plugin.configManager().message("general.no-permission"));
            return true;
        }
        if (args.length < 1 || (!args[0].equalsIgnoreCase("deepslate") && !args[0].equalsIgnoreCase("bedrock"))) {
            sender.sendMessage(plugin.configManager().message("general.invalid-usage")
                    .replace("%usage%", "/drop <deepslate|bedrock>"));
            return true;
        }
        plugin.dropManager().triggerDrop(args[0].toUpperCase());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("deepslate", "bedrock");
        return List.of();
    }
}
