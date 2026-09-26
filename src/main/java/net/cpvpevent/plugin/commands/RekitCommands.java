package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class RekitCommands implements CommandExecutor, TabCompleter {

    private final CPVPEventPlus plugin;

    public RekitCommands(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cpvpevent.admin")) {
            sender.sendMessage(plugin.configManager().message("general.no-permission"));
            return true;
        }

        switch (label.toLowerCase()) {
            case "rekit" -> {
                if (args.length < 1) {
                    sender.sendMessage(plugin.configManager().message("general.invalid-usage").replace("%usage%", "/rekit <enable|disable>"));
                    return true;
                }
                boolean enabled = args[0].equalsIgnoreCase("enable");
                plugin.rekitManager().setGlobalEnabled(enabled);
                sender.sendMessage(plugin.configManager().message(enabled ? "rekit.enabled" : "rekit.disabled"));
            }
            case "rekitset" -> {
                if (args.length < 1) {
                    sender.sendMessage(plugin.configManager().message("general.invalid-usage").replace("%usage%", "/rekitset <deaths|disable>"));
                    return true;
                }
                if (args[0].equalsIgnoreCase("disable")) {
                    plugin.rekitManager().disableThreshold();
                } else {
                    try {
                        plugin.rekitManager().setDeathsRequired(Integer.parseInt(args[0]));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(plugin.configManager().message("general.invalid-number"));
                    }
                }
            }
            case "rekitstatus" -> sender.sendMessage(plugin.configManager().rawMessage("rekit.status")
                    .replace("%deaths%", "n/a")
                    .replace("%required%", String.valueOf(plugin.rekitManager().deathsRequired())));
            default -> { }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("rekit") && args.length == 1) return List.of("enable", "disable");
        if (label.equalsIgnoreCase("rekitset") && args.length == 1) return List.of("5", "disable");
        return List.of();
    }
}
