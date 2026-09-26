package net.cpvpevent.plugin.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Random;

/**
 * Finds a safe teleport location within the event border on a configured
 * whitelist of ground blocks, with clearance checks so players never spawn
 * inside caves, water, lava, trees or without headroom.
 */
public final class SafeLocationFinder {

    private final Random random = new Random();

    /**
     * Attempts to find a safe location centered around (0,0) within radius,
     * standing on one of the allowed ground materials with the required
     * vertical clearance above it.
     */
    public Location findSafeLocation(World world, double radius, List<Material> allowedGround, int clearance, int attempts) {
        if (world == null) return null;

        for (int i = 0; i < attempts; i++) {
            double x = (random.nextDouble() * 2 - 1) * radius;
            double z = (random.nextDouble() * 2 - 1) * radius;

            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);

            int highestY = world.getHighestBlockYAt(blockX, blockZ);
            Block ground = world.getBlockAt(blockX, highestY, blockZ);

            if (!allowedGround.contains(ground.getType())) {
                continue;
            }

            if (!hasClearance(world, blockX, highestY + 1, blockZ, clearance)) {
                continue;
            }

            if (isNearHazard(world, blockX, highestY, blockZ)) {
                continue;
            }

            return new Location(world, blockX + 0.5, highestY + 1, blockZ + 0.5);
        }

        return null;
    }

    private boolean hasClearance(World world, int x, int y, int z, int clearance) {
        for (int dy = 0; dy < clearance; dy++) {
            Material type = world.getBlockAt(x, y + dy, z).getType();
            if (type.isSolid()) {
                return false;
            }
        }
        return true;
    }

    private boolean isNearHazard(World world, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Material below = world.getBlockAt(x + dx, y, z + dz).getType();
                if (below == Material.LAVA || below == Material.WATER
                        || below == Material.FIRE || below == Material.CACTUS
                        || below == Material.MAGMA_BLOCK) {
                    return true;
                }
            }
        }
        return false;
    }
}
