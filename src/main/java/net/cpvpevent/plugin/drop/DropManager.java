package net.cpvpevent.plugin.drop;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.util.Text;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the "drop" mechanic: clearing the world above a Y level while
 * protecting participants from fall/void damage during the transition.
 * All Bukkit world-edit calls stay on the main thread (required by the
 * API); only the non-world-touching preparation work is done off-thread.
 */
public class DropManager {

    private final CPVPEventPlus plugin;
    private boolean dropInProgress = false;

    public DropManager(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    public void triggerDrop(String mode) {
        if (dropInProgress) return;
        int minY = "BEDROCK".equalsIgnoreCase(mode) ? -64 : 0;
        triggerDrop(minY);
    }

    public void triggerDrop(int aboveY) {
        if (dropInProgress) return;
        dropInProgress = true;

        boolean blindnessEnabled = plugin.configManager().config().getBoolean("drop.blindness-enabled", true);
        int blindnessSeconds = plugin.configManager().config().getInt("drop.blindness-duration-seconds", 5);
        int protectionSeconds = plugin.configManager().config().getInt("drop.protection-duration-seconds", 6);

        String title = plugin.configManager().config().getString("drop.announce-title", "&c&lDROP");
        String subtitle = plugin.configManager().config().getString("drop.announce-subtitle", "");

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendTitle(Text.color(title), Text.color(subtitle), 10, 40, 10);
            if (blindnessEnabled) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessSeconds * 20, 1, false, false));
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, protectionSeconds * 20, 4, false, false));
            player.setFallDistance(0f);
        }

        plugin.getServer().broadcastMessage(plugin.configManager().message("drop.triggered"));

        // Small delay so titles/effects land before the world changes.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> executeDrop(aboveY), 20L);
    }

    private void executeDrop(int aboveY) {
        World world = plugin.getServer().getWorlds().get(0);
        int batchSize = plugin.configManager().config().getInt("drop.chunk-batch-size", 4);

        List<Chunk> chunks = new ArrayList<>(List.of(world.getLoadedChunks()));

        processChunkBatch(world, chunks, 0, batchSize, aboveY);
    }

    /**
     * Processes chunks in small batches spread across ticks so the server
     * never freezes on a single huge synchronous loop, while still doing
     * every actual block edit on the main thread as required by the API.
     */
    private void processChunkBatch(World world, List<Chunk> chunks, int startIndex, int batchSize, int aboveY) {
        int endIndex = Math.min(chunks.size(), startIndex + batchSize);

        for (int i = startIndex; i < endIndex; i++) {
            clearChunkAbove(world, chunks.get(i), aboveY);
        }

        if (endIndex < chunks.size()) {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    processChunkBatch(world, chunks, endIndex, batchSize, aboveY));
        } else {
            dropInProgress = false;
            protectPlayersFromFalling();
        }
    }

    private void clearChunkAbove(World world, Chunk chunk, int aboveY) {
        int minHeight = Math.max(aboveY, world.getMinHeight());
        int maxHeight = world.getMaxHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minHeight; y < maxHeight; y++) {
                    Material type = chunk.getBlock(x, y, z).getType();
                    if (type != Material.AIR) {
                        chunk.getBlock(x, y, z).setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private void protectPlayersFromFalling() {
        // Resistance effect applied earlier already prevents fall damage
        // for the configured protection window; nothing further required.
    }

    public boolean isDropInProgress() {
        return dropInProgress;
    }
}
