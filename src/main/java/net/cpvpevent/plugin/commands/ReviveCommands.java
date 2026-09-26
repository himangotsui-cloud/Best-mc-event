package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReviveCommands implements CommandExecutor, TabCompleter {

    private final CPVPEventPlus plugin;

    public ReviveCommands(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (label.toLowerCase()) {
            case "revive" -> {
                if (args.length < 1) {
                    sender.sendMessage(plugin.configManager().message("general.invalid-usage")
                            .replace("%usage%", "/revive <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage(plugin.configManager().message("general.player-not-found").replace("%player%", args[0]));
                    return true;
                }

                if (sender.hasPermission("cpvpevent.admin")) {
                    plugin.reviveManager().opRevive(target);
                    sender.sendMessage(plugin.configManager().message("revive.revived-by-you").replace("%player%", target.getName()));
                    return true;
                }

                if (!(sender instanceof Player reviver)) {
                    sender.sendMessage(plugin.configManager().message("general.player-only"));
                    return true;
                }
                plugin.reviveManager().attemptPlayerRevive(reviver, target);
            }
            case "reviveall" -> {
                if (!sender.hasPermission("cpvpevent.admin")) {
                    sender.sendMessage(plugin.configManager().message("general.no-permission"));
                    return true;
                }
                plugin.reviveManager().reviveAll();
            }
            case "revivedisable" -> {
                if (!sender.hasPermission("cpvpevent.admin")) {
                    sender.sendMessage(plugin.configManager().message("general.no-permission"));
                    return true;
                }
                plugin.reviveManager().setEnabled(false);
            }
            case "reviveenable" -> {
                if (!sender.hasPermission("cpvpevent.admin")) {
                    sender.sendMessage(plugin.configManager().message("general.no-permission"));
                    return true;
                }
                plugin.reviveManager().setEnabled(true);
            }
            default -> { }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("revive") && args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
