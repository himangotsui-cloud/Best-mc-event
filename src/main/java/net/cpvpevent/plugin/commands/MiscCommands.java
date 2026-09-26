package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.util.Text;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MiscCommands implements CommandExecutor, TabCompleter {

    private final CPVPEventPlus plugin;
    private boolean joinSpectatorOverride;

    public MiscCommands(CPVPEventPlus plugin) {
        this.plugin = plugin;
        this.joinSpectatorOverride = plugin.configManager().config().getBoolean("event.join-spectator-by-default", true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (label.toLowerCase()) {
            case "announce" -> {
                if (!sender.hasPermission("cpvpevent.admin")) {
                    sender.sendMessage(plugin.configManager().message("general.no-permission"));
                    return true;
                }
                if (args.length < 1) {
                    sender.sendMessage(plugin.configManager().message("general.invalid-usage").replace("%usage%", "/announce <message>"));
                    return true;
                }
                plugin.getServer().broadcastMessage(Text.color(String.join(" ", args)));
            }
            case "announcewinner" -> plugin.eventManager().announceStoredWinner();
            case "joinspectator" -> {
                if (!sender.hasPermission("cpvpevent.admin")) {
                    sender.sendMessage(plugin.configManager().message("general.no-permission"));
                    return true;
                }
                if (args.length < 1) {
                    sender.sendMessage(plugin.configManager().message("general.invalid-usage").replace("%usage%", "/joinspectator <true|false>"));
                    return true;
                }
                joinSpectatorOverride = Boolean.parseBoolean(args[0]);
                plugin.configManager().config().set("event.join-spectator-by-default", joinSpectatorOverride);
            }
            case "clearblock" -> {
                if (!sender.hasPermission("cpvpevent.admin")) {
                    sender.sendMessage(plugin.configManager().message("general.no-permission"));
                    return true;
                }
                clearConfiguredBlocks();
            }
            case "clearspawn" -> {
                if (!sender.hasPermission("cpvpevent.admin")) {
                    sender.sendMessage(plugin.configManager().message("general.no-permission"));
                    return true;
                }
                clearSpawnArea();
            }
            case "setnetherite" -> convertBlocks(sender, true);
            case "setbedrock" -> convertBlocks(sender, false);
            case "kitall" -> {
                if (!sender.hasPermission("cpvpevent.admin")) {
                    sender.sendMessage(plugin.configManager().message("general.no-permission"));
                    return true;
                }
                if (args.length < 1 || !plugin.kitManager().isValidKit(args[0])) {
                    sender.sendMessage(plugin.configManager().message("general.invalid-usage").replace("%usage%", "/kitall <k1-k9>"));
                    return true;
                }
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    plugin.kitManager().giveKit(player, args[0]);
                }
            }
            case "tpspawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.configManager().message("general.player-only"));
                    return true;
                }
                World world = plugin.getServer().getWorld(plugin.configManager().config().getString("spawn.world", "world"));
                if (world == null) world = plugin.getServer().getWorlds().get(0);
                Location spawn = new Location(world,
                        plugin.configManager().config().getDouble("spawn.x"),
                        plugin.configManager().config().getDouble("spawn.y"),
                        plugin.configManager().config().getDouble("spawn.z"),
                        (float) plugin.configManager().config().getDouble("spawn.yaw"),
                        (float) plugin.configManager().config().getDouble("spawn.pitch"));
                player.teleport(spawn);
            }
            case "cpvpeventplusreload" -> {
                if (!sender.hasPermission("cpvpevent.admin")) {
                    sender.sendMessage(plugin.configManager().message("general.no-permission"));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(plugin.configManager().message("general.reloaded"));
            }
            case "help" -> sendHelp(sender);
            default -> { }
        }
        return true;
    }

    private void clearConfiguredBlocks() {
        List<Material> targets = new ArrayList<>();
        for (String name : plugin.configManager().config().getStringList("clear-block.blocks")) {
            Material material = Material.matchMaterial(name);
            if (material != null) targets.add(material);
        }

        World world = plugin.getServer().getWorlds().get(0);
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (targets.contains(block.getType())) {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }
    }

    private void clearSpawnArea() {
        World world = plugin.getServer().getWorlds().get(0);
        Location spawn = world.getSpawnLocation();
        int radius = 16;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block ground = world.getHighestBlockAt(spawn.getBlockX() + x, spawn.getBlockZ() + z);
                for (int y = ground.getY() + 1; y < ground.getY() + 5; y++) {
                    world.getBlockAt(spawn.getBlockX() + x, y, spawn.getBlockZ() + z).setType(Material.AIR, false);
                }
            }
        }
    }

    private void convertBlocks(CommandSender sender, boolean toNetherite) {
        if (!sender.hasPermission("cpvpevent.admin")) {
            sender.sendMessage(plugin.configManager().message("general.no-permission"));
            return;
        }

        Material from = Material.matchMaterial(plugin.configManager().config().getString(
                toNetherite ? "netherite-bedrock.bedrock-block" : "netherite-bedrock.netherite-block", "BEDROCK"));
        Material to = Material.matchMaterial(plugin.configManager().config().getString(
                toNetherite ? "netherite-bedrock.netherite-block" : "netherite-bedrock.bedrock-block", "NETHERITE_BLOCK"));

        if (from == null || to == null) return;

        for (String worldName : plugin.configManager().config().getStringList("netherite-bedrock.worlds")) {
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) continue;
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                            Block block = chunk.getBlock(x, y, z);
                            if (block.getType() == from) {
                                block.setType(to, false);
                            }
                        }
                    }
                }
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Text.color("&b&lCPVPEventPlus Commands"));
        sender.sendMessage(Text.color("&7/startevent, /startforce, /stopevent, /pauseevent, /resumeevent, /eventstatus"));
        sender.sendMessage(Text.color("&7/border, /stopborder, /resumeborder, /drop"));
        sender.sendMessage(Text.color("&7/revive, /reviveall, /rekit, /rekitset, /rekitstatus"));
        sender.sendMessage(Text.color("&7/pvp, /protection, /chat, /staffchat, /party, /revfights"));
        sender.sendMessage(Text.color("&7/eventsettings, /eventools, /regen, /tpspawn"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("kitall") && args.length == 1) {
            return List.of("k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8", "k9");
        }
        if (label.equalsIgnoreCase("joinspectator") && args.length == 1) {
            return List.of("true", "false");
        }
        return List.of();
    }
}
