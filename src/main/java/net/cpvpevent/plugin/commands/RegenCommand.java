package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RegenCommand implements CommandExecutor, TabCompleter {

    private final CPVPEventPlus plugin;

    public RegenCommand(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cpvpevent.regen")) {
            sender.sendMessage(plugin.configManager().message("general.no-permission"));
            return true;
        }

        File templateRoot = new File(plugin.getDataFolder(), "regen-templates");
        File[] templates = templateRoot.exists() ? templateRoot.listFiles(File::isDirectory) : null;

        List<String> worldNames = new ArrayList<>();
        if (templates != null) {
            for (File f : templates) worldNames.add(f.getName());
        }

        if (args.length >= 1) {
            worldNames = List.of(args[0]);
        }

        Player requester = sender instanceof Player p ? p : null;
        plugin.regenerationManager().regenerateAll(worldNames, requester);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
