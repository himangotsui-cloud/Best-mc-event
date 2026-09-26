package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class ChatCommands implements CommandExecutor, TabCompleter {

    private final CPVPEventPlus plugin;

    public ChatCommands(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("chat")) {
            if (!sender.hasPermission("cpvpevent.admin")) {
                sender.sendMessage(plugin.configManager().message("general.no-permission"));
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(plugin.configManager().message("general.invalid-usage").replace("%usage%", "/chat <enable|disable>"));
                return true;
            }
            boolean enable = args[0].equalsIgnoreCase("enable");
            plugin.chatManager().setGlobalChatEnabled(enable);
            sender.sendMessage(plugin.configManager().message(enable ? "chat.enabled" : "chat.disabled"));
            return true;
        }

        if (label.equalsIgnoreCase("staffchat")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.configManager().message("general.player-only"));
                return true;
            }
            if (!player.hasPermission("cpvpevent.staffchat")) {
                sender.sendMessage(plugin.configManager().message("general.no-permission"));
                return true;
            }
            boolean enable = args.length > 0 ? Boolean.parseBoolean(args[0]) : !plugin.chatManager().isStaffChatToggled(player);
            plugin.chatManager().toggleStaffChat(player, enable);
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("chat") && args.length == 1) return List.of("enable", "disable");
        if (label.equalsIgnoreCase("staffchat") && args.length == 1) return List.of("true", "false");
        return List.of();
    }
}
