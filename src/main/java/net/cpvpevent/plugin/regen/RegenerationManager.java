package net.cpvpevent.plugin.regen;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Regenerates configured worlds from clean templates stored under
 * plugins/CPVPEvent+/regen-templates/. Template folders are only ever
 * copied FROM, never modified, so the original stays clean.
 *
 * File copying (I/O) happens asynchronously; unloading/loading the actual
 * Bukkit World objects happens back on the main thread, since Paper's
 * world API is not thread-safe.
 */
public class RegenerationManager {

    private final AtomicBoolean regenerating = new AtomicBoolean(false);

    private final CPVPEventPlus plugin;

    public RegenerationManager(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    public boolean isRegenerating() {
        return regenerating.get();
    }

    public void regenerateAll(List<String> worldNames, Player requester) {
        if (!regenerating.compareAndSet(false, true)) {
            if (requester != null) requester.sendMessage(plugin.configManager().message("regen.locked"));
            return;
        }

        if (requester != null) {
            requester.sendMessage(plugin.configManager().message("regen.started")
                    .replace("%count%", String.valueOf(worldNames.size())));
        }

        File templateRoot = new File(plugin.getDataFolder(), "regen-templates");

        List<CompletableFuture<Void>> jobs = worldNames.stream()
                .map(worldName -> regenerateWorldAsync(worldName, templateRoot, requester))
                .toList();

        CompletableFuture.allOf(jobs.toArray(new CompletableFuture[0])).whenComplete((v, throwable) -> {
            regenerating.set(false);
            if (requester != null) {
                requester.sendMessage(plugin.configManager().message("regen.complete"));
            }
        });
    }

    private CompletableFuture<Void> regenerateWorldAsync(String worldName, File templateRoot, Player requester) {
        return CompletableFuture.runAsync(() -> {
            try {
                File templateDir = new File(templateRoot, worldName);
                if (!templateDir.exists()) {
                    throw new IOException("No template found for " + worldName);
                }

                // Unload/copy/load must happen on the main thread for Bukkit API safety.
                CompletableFuture<Void> mainThreadWork = new CompletableFuture<>();

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        World world = plugin.getServer().getWorld(worldName);
                        File worldFolder = world != null ? world.getWorldFolder()
                                : new File(plugin.getServer().getWorldContainer(), worldName);

                        if (world != null) {
                            for (Player p : world.getPlayers()) {
                                p.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                            }
                            plugin.getServer().unloadWorld(world, false);
                        }

                        deleteDirectory(worldFolder.toPath());
                        copyDirectory(templateDir.toPath(), worldFolder.toPath());

                        World reloaded = plugin.getServer().createWorld(new org.bukkit.WorldCreator(worldName));

                        if (requester != null) {
                            requester.sendMessage(plugin.configManager().message("regen.world-done")
                                    .replace("%world%", worldName));
                        }
                        mainThreadWork.complete(null);
                    } catch (Exception ex) {
                        mainThreadWork.completeExceptionally(ex);
                    }
                });

                mainThreadWork.join();
            } catch (Exception ex) {
                if (requester != null) {
                    requester.sendMessage(plugin.configManager().message("regen.world-failed")
                            .replace("%world%", worldName)
                            .replace("%error%", String.valueOf(ex.getMessage())));
                }
            }
        });
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)));
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
