package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.event.EventMode;
import net.cpvpevent.plugin.event.EventState;
import net.cpvpevent.plugin.util.Text;
import org.bukkit.Bukkit;
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
import java.util.stream.Collectors;

public class EventDispatchCommand implements CommandExecutor, TabCompleter {

    private final CPVPEventPlus plugin;

    public EventDispatchCommand(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        String[] rest = args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (sub) {
            case "start" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 3) { usage(sender, "/event start <min> <mode> <announce>"); return true; }
                if (plugin.eventManager().state() != EventState.IDLE) { sender.sendMessage(plugin.configManager().message("event.already-running")); return true; }
                try {
                    plugin.eventManager().startEvent(Integer.parseInt(rest[0]), EventMode.fromString(rest[1]), Boolean.parseBoolean(rest[2]));
                } catch (NumberFormatException e) { sender.sendMessage(plugin.configManager().message("general.invalid-number")); }
            }
            case "force" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 2) { usage(sender, "/event force <mode> <announce>"); return true; }
                if (plugin.eventManager().state() != EventState.IDLE) { sender.sendMessage(plugin.configManager().message("event.already-running")); return true; }
                plugin.eventManager().startForce(EventMode.fromString(rest[0]), Boolean.parseBoolean(rest[1]));
            }
            case "stop" -> { if (requireAdmin(sender)) plugin.eventManager().stopEvent(); }
            case "pause" -> { if (requireAdmin(sender)) plugin.eventManager().pauseEvent(); }
            case "resume" -> { if (requireAdmin(sender)) plugin.eventManager().resumeEvent(); }
            case "status" -> {
                sender.sendMessage(plugin.configManager().message("event.status-header"));
                sender.sendMessage(plugin.configManager().message("event.status-line")
                        .replace("%state%", plugin.eventManager().state().name())
                        .replace("%mode%", plugin.eventManager().mode().name())
                        .replace("%stage%", plugin.eventManager().currentStageNumber() + "/" + plugin.eventManager().totalStages()));
            }
            case "arena" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 1) { usage(sender, "/event arena <material>"); return true; }
                Material material = Material.matchMaterial(rest[0].toUpperCase());
                if (material == null) { sender.sendMessage(Text.color("&cUnknown material.")); return true; }
                plugin.eventManager().setArenaMaterial(material);
                sender.sendMessage(Text.color("&aArena floor set to " + material.name() + "."));
            }
            case "border" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 2) { usage(sender, "/event border <distance> <time>"); return true; }
                try {
                    plugin.borderManager().shrinkBorder(Double.parseDouble(rest[0]), Long.parseLong(rest[1]));
                } catch (NumberFormatException e) { sender.sendMessage(plugin.configManager().message("general.invalid-number")); }
            }
            case "stopborder" -> { if (requireAdmin(sender)) plugin.borderManager().stopBorder(); }
            case "resumeborder" -> { if (requireAdmin(sender)) plugin.borderManager().resumeBorder(); }
            case "drop" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 1) { usage(sender, "/event drop <deepslate|bedrock>"); return true; }
                plugin.dropManager().triggerDrop(rest[0].toUpperCase());
            }
            case "pvp" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 1) { usage(sender, "/event pvp <enable|disable>"); return true; }
                if (rest[0].equalsIgnoreCase("enable")) plugin.pvpManager().runReEnableCountdown();
                else plugin.pvpManager().setPvpEnabled(false);
            }
            case "protection" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 1) { usage(sender, "/event protection <enable|disable>"); return true; }
                plugin.pvpManager().setProtectionEnabled(rest[0].equalsIgnoreCase("enable"));
            }
            case "rekit" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 1) { usage(sender, "/event rekit <enable|disable>"); return true; }
                boolean enabled = rest[0].equalsIgnoreCase("enable");
                plugin.rekitManager().setGlobalEnabled(enabled);
                sender.sendMessage(plugin.configManager().message(enabled ? "rekit.enabled" : "rekit.disabled"));
            }
            case "rekitset" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 1) { usage(sender, "/event rekitset <deaths|disable>"); return true; }
                if (rest[0].equalsIgnoreCase("disable")) plugin.rekitManager().disableThreshold();
                else {
                    try { plugin.rekitManager().setDeathsRequired(Integer.parseInt(rest[0])); }
                    catch (NumberFormatException e) { sender.sendMessage(plugin.configManager().message("general.invalid-number")); }
                }
            }
            case "kit" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 1 || !plugin.kitManager().isValidKit(rest[0])) { usage(sender, "/event kit <k1-k9>"); return true; }
                for (Player p : Bukkit.getOnlinePlayers()) plugin.kitManager().giveKit(p, rest[0]);
            }
            case "chat" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 1) { usage(sender, "/event chat <enable|disable>"); return true; }
                boolean enable = rest[0].equalsIgnoreCase("enable");
                plugin.chatManager().setGlobalChatEnabled(enable);
                sender.sendMessage(plugin.configManager().message(enable ? "chat.enabled" : "chat.disabled"));
            }
            case "staffchat" -> {
                if (!(sender instanceof Player player) || !player.hasPermission("cpvpevent.staffchat")) {
                    sender.sendMessage(plugin.configManager().message("general.no-permission")); return true;
                }
                boolean enable = rest.length > 0 ? Boolean.parseBoolean(rest[0]) : !plugin.chatManager().isStaffChatToggled(player);
                plugin.chatManager().toggleStaffChat(player, enable);
            }
            case "announce" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 1) { usage(sender, "/event announce <message>"); return true; }
                plugin.getServer().broadcastMessage(Text.color(String.join(" ", rest)));
            }
            case "announcewinner" -> plugin.eventManager().announceStoredWinner();
            case "joinspectator" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 1) { usage(sender, "/event joinspectator <true|false>"); return true; }
                plugin.configManager().config().set("event.join-spectator-by-default", Boolean.parseBoolean(rest[0]));
            }
            case "clearblock" -> { if (requireAdmin(sender)) clearConfiguredBlocks(); }
            case "clearspawn" -> { if (requireAdmin(sender)) clearSpawnArea(); }
            case "setnetherite" -> { if (requireAdmin(sender)) convertBlocks(true); }
            case "setbedrock" -> { if (requireAdmin(sender)) convertBlocks(false); }
            case "revivedisable" -> { if (requireAdmin(sender)) plugin.reviveManager().setEnabled(false); }
            case "reviveenable" -> { if (requireAdmin(sender)) plugin.reviveManager().setEnabled(true); }
            case "revive" -> {
                if (rest.length < 1) { usage(sender, "/event revive <player>"); return true; }
                Player target = Bukkit.getPlayerExact(rest[0]);
                if (target == null) { sender.sendMessage(plugin.configManager().message("general.player-not-found").replace("%player%", rest[0])); return true; }
                if (sender.hasPermission("cpvpevent.admin")) { plugin.reviveManager().opRevive(target); return true; }
                if (!(sender instanceof Player reviver)) { sender.sendMessage(plugin.configManager().message("general.player-only")); return true; }
                plugin.reviveManager().attemptPlayerRevive(reviver, target);
            }
            case "reviveall" -> { if (requireAdmin(sender)) plugin.reviveManager().reviveAll(); }
            case "party" -> {
                PartyCommand delegate = new PartyCommand(plugin);
                delegate.onCommand(sender, command, "party", rest);
            }
            case "revfights" -> {
                if (!requireAdmin(sender)) return true;
                if (rest.length < 2) { usage(sender, "/event revfights <p1> <p2>"); return true; }
                Player p1 = Bukkit.getPlayerExact(rest[0]);
                Player p2 = Bukkit.getPlayerExact(rest[1]);
                if (p1 == null || p2 == null) { sender.sendMessage(plugin.configManager().message("general.player-not-found").replace("%player%", p1 == null ? rest[0] : rest[1])); return true; }
                plugin.revFightManager().startFight(p1, p2);
            }
            case "settings" -> {
                if (!(sender instanceof Player player) || !player.hasPermission("cpvpevent.eventsettings")) {
                    sender.sendMessage(plugin.configManager().message("general.no-permission")); return true;
                }
                plugin.guiManager().openSettingsGUI(player);
            }
            case "tools" -> {
                if (!(sender instanceof Player player) || !player.hasPermission("cpvpevent.eventsettings")) {
                    sender.sendMessage(plugin.configManager().message("general.no-permission")); return true;
                }
                Material material = Material.matchMaterial(plugin.configManager().config().getString("eventools.material", "NETHER_STAR"));
                if (material == null) material = Material.NETHER_STAR;
                player.getInventory().addItem(new net.cpvpevent.plugin.util.ItemBuilder(material)
                        .name(plugin.configManager().config().getString("eventools.name", "&d&lEvent Tools"))
                        .lore(plugin.configManager().config().getStringList("eventools.lore"))
                        .build());
            }
            case "regen" -> {
                if (!sender.hasPermission("cpvpevent.regen")) { sender.sendMessage(plugin.configManager().message("general.no-permission")); return true; }
                List<String> worlds = rest.length > 0 ? List.of(rest[0]) : new ArrayList<>();
                plugin.regenerationManager().regenerateAll(worlds, sender instanceof Player p ? p : null);
            }
            case "tpspawn" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(plugin.configManager().message("general.player-only")); return true; }
                World world = plugin.getServer().getWorld(plugin.configManager().config().getString("spawn.world", "world"));
                if (world == null) world = plugin.getServer().getWorlds().get(0);
                player.teleport(new Location(world,
                        plugin.configManager().config().getDouble("spawn.x"),
                        plugin.configManager().config().getDouble("spawn.y"),
                        plugin.configManager().config().getDouble("spawn.z")));
            }
            case "reload" -> {
                if (!requireAdmin(sender)) return true;
                plugin.reload();
                sender.sendMessage(plugin.configManager().message("general.reloaded"));
            }
            case "help" -> sendHelp(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("cpvpevent.admin")) {
            sender.sendMessage(plugin.configManager().message("general.no-permission"));
            return false;
        }
        return true;
    }

    private void usage(CommandSender sender, String usage) {
        sender.sendMessage(plugin.configManager().message("general.invalid-usage").replace("%usage%", usage));
    }

    private void clearConfiguredBlocks() {
        List<Material> targets = new ArrayList<>();
        for (String name : plugin.configManager().config().getStringList("clear-block.blocks")) {
            Material m = Material.matchMaterial(name);
            if (m != null) targets.add(m);
        }
        World world = plugin.getServer().getWorlds().get(0);
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (targets.contains(block.getType())) block.setType(Material.AIR, false);
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

    private void convertBlocks(boolean toNetherite) {
        Material from = Material.matchMaterial(plugin.configManager().config().getString(toNetherite ? "netherite-bedrock.bedrock-block" : "netherite-bedrock.netherite-block", "BEDROCK"));
        Material to = Material.matchMaterial(plugin.configManager().config().getString(toNetherite ? "netherite-bedrock.netherite-block" : "netherite-bedrock.bedrock-block", "NETHERITE_BLOCK"));
        if (from == null || to == null) return;
        for (String worldName : plugin.configManager().config().getStringList("netherite-bedrock.worlds")) {
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) continue;
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block.getType() == from) block.setType(to, false);
                    }
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Text.color("&b&lCPVPEventPlus - /event <sub>"));
        sender.sendMessage(Text.color("&7start, force, stop, pause, resume, status, arena"));
        sender.sendMessage(Text.color("&7border, stopborder, resumeborder, drop, pvp, protection"));
        sender.sendMessage(Text.color("&7rekit, rekitset, kit, chat, staffchat, announce, announcewinner"));
        sender.sendMessage(Text.color("&7revive, reviveall, party, revfights, settings, tools, regen, tpspawn, reload"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("start", "force", "stop", "pause", "resume", "status", "arena", "border", "stopborder",
                    "resumeborder", "drop", "pvp", "protection", "rekit", "rekitset", "kit", "chat", "staffchat",
                    "announce", "announcewinner", "joinspectator", "clearblock", "clearspawn", "setnetherite",
                    "setbedrock", "revivedisable", "reviveenable", "revive", "reviveall", "party", "revfights",
                    "settings", "tools", "regen", "tpspawn", "reload", "help");
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "arena" -> List.of("grass_block", "sand", "dirt", "snow_block", "mycelium");
                case "drop" -> List.of("deepslate", "bedrock");
                case "pvp", "protection", "rekit", "chat" -> List.of("enable", "disable");
                case "kit" -> List.of("k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8", "k9");
                case "revive", "revfights" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                case "party" -> List.of("create", "invite", "accept", "leave", "kick", "disband", "resetall");
                default -> List.of();
            };
        }
        return List.of();
    }
}
