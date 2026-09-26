package net.cpvpevent.plugin.revfight;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Manages 1v1 "gulag" style revival fights in dedicated arena worlds under
 * RevFights-Worlds/. Arenas are never permanently modified; each match gets
 * a fresh border and the arena is simply reset/cleared of match state after.
 */
public class RevFightManager {

    private record Match(UUID player1, UUID player2, World arenaWorld, BukkitTask timeoutTask) {
    }

    private final CPVPEventPlus plugin;
    private final Random random = new Random();
    private final List<World> availableArenas = new ArrayList<>();
    private final Map<UUID, Match> activeMatchesByArena = new HashMap<>();

    public RevFightManager(CPVPEventPlus plugin) {
        this.plugin = plugin;
        discoverArenas();
    }

    public void discoverArenas() {
        availableArenas.clear();
        String folderName = plugin.configManager().config().getString("revfights.arena-folder", "RevFights-Worlds");
        File folder = new File(plugin.getServer().getWorldContainer(), folderName);
        if (!folder.exists() || !folder.isDirectory()) return;

        File[] children = folder.listFiles(File::isDirectory);
        if (children == null) return;

        for (File child : children) {
            String worldName = folderName + File.separator + child.getName();
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                world = plugin.getServer().createWorld(new org.bukkit.WorldCreator(worldName));
            }
            if (world != null) {
                availableArenas.add(world);
            }
        }
    }

    public boolean startFight(Player p1, Player p2) {
        World arena = pickAvailableArena();
        if (arena == null) {
            p1.sendMessage(plugin.configManager().message("revfights.no-arena"));
            return false;
        }

        int borderSize = plugin.configManager().config().getInt("revfights.border-size", 50);
        WorldBorder border = arena.getWorldBorder();
        border.setCenter(arena.getSpawnLocation());
        border.setSize(borderSize);

        Location spawn1 = arena.getSpawnLocation().clone().add(5, 0, 0);
        Location spawn2 = arena.getSpawnLocation().clone().add(-5, 0, 0);

        p1.teleport(spawn1);
        p2.teleport(spawn2);
        p1.setGameMode(GameMode.SURVIVAL);
        p2.setGameMode(GameMode.SURVIVAL);
        p1.setHealth(p1.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
        p2.setHealth(p2.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());

        plugin.kitManager().giveKit(p1, "k1");
        plugin.kitManager().giveKit(p2, "k1");

        int matchSeconds = plugin.configManager().config().getInt("revfights.match-time-seconds", 180);

        plugin.getServer().broadcastMessage(plugin.configManager().message("revfights.starting")
                .replace("%player1%", p1.getName())
                .replace("%player2%", p2.getName()));

        BukkitTask timeout = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            endMatch(arena, null);
            plugin.getServer().broadcastMessage(plugin.configManager().message("revfights.timeout"));
        }, matchSeconds * 20L);

        activeMatchesByArena.put(arena.getUID(), new Match(p1.getUniqueId(), p2.getUniqueId(), arena, timeout));
        availableArenas.remove(arena);
        return true;
    }

    public void handleDeathInArena(Player loser) {
        Match match = findMatchFor(loser.getUniqueId());
        if (match == null) return;

        UUID winnerId = match.player1().equals(loser.getUniqueId()) ? match.player2() : match.player1();
        Player winner = plugin.getServer().getPlayer(winnerId);

        endMatch(match.arenaWorld(), winner);

        if (winner != null) {
            plugin.reviveManager().opRevive(winner);
            plugin.getServer().broadcastMessage(plugin.configManager().message("revfights.winner")
                    .replace("%winner%", winner.getName())
                    .replace("%loser%", loser.getName()));
        }
    }

    public void handleDisconnect(Player player) {
        Match match = findMatchFor(player.getUniqueId());
        if (match == null) return;
        UUID otherId = match.player1().equals(player.getUniqueId()) ? match.player2() : match.player1();
        Player other = plugin.getServer().getPlayer(otherId);
        endMatch(match.arenaWorld(), other);
    }

    private Match findMatchFor(UUID playerId) {
        for (Match match : activeMatchesByArena.values()) {
            if (match.player1().equals(playerId) || match.player2().equals(playerId)) {
                return match;
            }
        }
        return null;
    }

    private void endMatch(World arena, Player winner) {
        Match match = activeMatchesByArena.remove(arena.getUID());
        if (match != null && match.timeoutTask() != null) {
            match.timeoutTask().cancel();
        }

        // Reset match-only state; the arena template itself is never edited on disk.
        World spawnWorld = plugin.getServer().getWorlds().get(0);
        if (match != null) {
            for (UUID id : List.of(match.player1(), match.player2())) {
                Player player = plugin.getServer().getPlayer(id);
                if (player != null && !player.equals(winner)) {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.teleport(spawnWorld.getSpawnLocation());
                }
            }
        }

        availableArenas.add(arena);
    }

    private World pickAvailableArena() {
        if (availableArenas.isEmpty()) return null;
        return availableArenas.get(random.nextInt(availableArenas.size()));
    }
}
