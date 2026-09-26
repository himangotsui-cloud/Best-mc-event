package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class PvpCommands implements CommandExecutor, TabCompleter {

    private final CPVPEventPlus plugin;

    public PvpCommands(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cpvpevent.admin")) {
            sender.sendMessage(plugin.configManager().message("general.no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.configManager().message("general.invalid-usage")
                    .replace("%usage%", "/" + label + " <enable|disable>"));
            return true;
        }

        boolean enable = args[0].equalsIgnoreCase("enable");

        if (label.equalsIgnoreCase("pvp")) {
            if (enable) {
                plugin.pvpManager().runReEnableCountdown();
            } else {
                plugin.pvpManager().setPvpEnabled(false);
            }
        } else if (label.equalsIgnoreCase("protection")) {
            plugin.pvpManager().setProtectionEnabled(enable);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("enable", "disable");
        return List.of();
    }
}
