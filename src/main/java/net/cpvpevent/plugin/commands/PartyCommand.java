package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class PartyCommand implements CommandExecutor, TabCompleter {

    private final CPVPEventPlus plugin;

    public PartyCommand(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.configManager().message("general.player-only"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.configManager().message("general.invalid-usage")
                    .replace("%usage%", "/party <create|invite|accept|leave|kick>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (plugin.partyManager().isInParty(player)) {
                    player.sendMessage(plugin.configManager().message("party.already-in-party"));
                    return true;
                }
                plugin.partyManager().createParty(player);
                player.sendMessage(plugin.configManager().message("party.created"));
            }
            case "invite" -> {
                if (args.length < 2) return true;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.configManager().message("general.player-not-found").replace("%player%", args[1]));
                    return true;
                }
                if (!plugin.partyManager().isInParty(player)) {
                    plugin.partyManager().createParty(player);
                }
                plugin.partyManager().invite(player, target);
                player.sendMessage(plugin.configManager().message("party.invited").replace("%player%", target.getName()));
                target.sendMessage(plugin.configManager().message("party.invite-received").replace("%player%", player.getName()));
            }
            case "accept" -> {
                boolean success = plugin.partyManager().acceptInvite(player);
                if (success) {
                    player.sendMessage(plugin.configManager().message("party.joined").replace("%player%", "party"));
                } else {
                    player.sendMessage(plugin.configManager().message("party.invite-not-found"));
                }
            }
            case "leave" -> {
                if (!plugin.partyManager().isInParty(player)) {
                    player.sendMessage(plugin.configManager().message("party.not-in-party"));
                    return true;
                }
                plugin.partyManager().leaveParty(player);
                player.sendMessage(plugin.configManager().message("party.left"));
            }
            case "kick" -> {
                if (args.length < 2) return true;
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(plugin.configManager().message("general.player-not-found").replace("%player%", args[1]));
                    return true;
                }
                boolean success = plugin.partyManager().kick(player, target);
                if (success) {
                    player.sendMessage(plugin.configManager().message("party.kicked").replace("%player%", target.getName()));
                } else {
                    player.sendMessage(plugin.configManager().message("party.not-leader"));
                }
            }
            case "disband" -> {
                if (!player.hasPermission("cpvpevent.party.admin")) {
                    player.sendMessage(plugin.configManager().message("general.no-permission"));
                    return true;
                }
                plugin.partyManager().partyOf(player).ifPresent(plugin.partyManager()::disband);
                player.sendMessage(plugin.configManager().message("party.disbanded"));
            }
            case "resetall" -> {
                if (!player.hasPermission("cpvpevent.party.admin")) {
                    player.sendMessage(plugin.configManager().message("general.no-permission"));
                    return true;
                }
                plugin.partyManager().resetAll();
            }
            default -> { }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("create", "invite", "accept", "leave", "kick", "disband", "resetall");
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return List.of();
    }
}
