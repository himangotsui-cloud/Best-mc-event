package net.cpvpevent.plugin.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Fluent builder for ItemStacks, and a parser for the compact kit-item
 * string format used in kits.yml: MATERIAL,amount[,ench:level;...][,name:Display]
 */
public final class ItemBuilder {

    private final ItemStack stack;

    public ItemBuilder(Material material) {
        this.stack = new ItemStack(material);
    }

    public ItemBuilder(Material material, int amount) {
        this.stack = new ItemStack(material, amount);
    }

    public ItemBuilder name(String name) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            stack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setLore(lore.stream().map(Text::color).toList());
            stack.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        stack.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    public ItemStack build() {
        return stack;
    }

    /**
     * Parses a compact kit item definition such as:
     * "DIAMOND_SWORD,1,sharpness:2;knockback:1,name:My Sword"
     */
    public static ItemStack parse(String definition) {
        if (definition == null || definition.isBlank()) return null;
        String[] parts = definition.split(",", -1);
        if (parts.length == 0) return null;

        Material material = Material.matchMaterial(parts[0].trim());
        if (material == null) return null;

        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Math.max(1, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }

        ItemBuilder builder = new ItemBuilder(material, amount);

        for (int i = 2; i < parts.length; i++) {
            String segment = parts[i].trim();
            if (segment.isEmpty()) continue;

            if (segment.startsWith("name:")) {
                builder.name(segment.substring("name:".length()));
                continue;
            }

            // Enchantment group, e.g. "sharpness:2;knockback:1"
            for (String enchPart : segment.split(";")) {
                if (!enchPart.contains(":")) continue;
                String[] kv = enchPart.split(":", 2);
                Enchantment enchantment = matchEnchantment(kv[0].trim());
                if (enchantment == null) continue;
                int level;
                try {
                    level = Integer.parseInt(kv[1].trim());
                } catch (NumberFormatException ex) {
                    level = 1;
                }
                builder.enchant(enchantment, level);
            }
        }

        return builder.build();
    }

    private static Enchantment matchEnchantment(String key) {
        try {
            org.bukkit.NamespacedKey namespacedKey = org.bukkit.NamespacedKey.minecraft(key.toLowerCase());
            return Enchantment.getByKey(namespacedKey);
        } catch (Exception ex) {
            return null;
        }
    }
}
