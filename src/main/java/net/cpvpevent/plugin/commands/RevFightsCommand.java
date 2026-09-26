package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class RevFightsCommand implements CommandExecutor, TabCompleter {

    private final CPVPEventPlus plugin;

    public RevFightsCommand(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cpvpevent.admin")) {
            sender.sendMessage(plugin.configManager().message("general.no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.configManager().message("general.invalid-usage")
                    .replace("%usage%", "/revfights <player1> <player2>"));
            return true;
        }
        Player p1 = Bukkit.getPlayerExact(args[0]);
        Player p2 = Bukkit.getPlayerExact(args[1]);
        if (p1 == null || p2 == null) {
            sender.sendMessage(plugin.configManager().message("general.player-not-found").replace("%player%", p1 == null ? args[0] : args[1]));
            return true;
        }
        plugin.revFightManager().startFight(p1, p2);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length <= 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return List.of();
    }
}
