package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class BorderCommands implements CommandExecutor, TabCompleter {

    private final CPVPEventPlus plugin;

    public BorderCommands(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cpvpevent.admin")) {
            sender.sendMessage(plugin.configManager().message("general.no-permission"));
            return true;
        }

        switch (label.toLowerCase()) {
            case "border" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.configManager().message("general.invalid-usage")
                            .replace("%usage%", "/border <distance> <time>"));
                    return true;
                }
                try {
                    double distance = Double.parseDouble(args[0]);
                    long time = Long.parseLong(args[1]);
                    plugin.borderManager().shrinkBorder(distance, time);
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.configManager().message("general.invalid-number"));
                }
            }
            case "stopborder" -> plugin.borderManager().stopBorder();
            case "resumeborder" -> plugin.borderManager().resumeBorder();
            default -> { }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
