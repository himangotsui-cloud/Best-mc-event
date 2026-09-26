package net.cpvpevent.plugin.kits;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.util.ItemBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class KitManager {

    private final CPVPEventPlus plugin;

    public KitManager(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    public boolean giveKit(Player player, String kitName) {
        ConfigurationSection kitSection = plugin.configManager().kits().getConfigurationSection(kitName);
        if (kitSection == null) return false;

        PlayerInventory inv = player.getInventory();
        inv.clear();

        ConfigurationSection armor = kitSection.getConfigurationSection("armor");
        if (armor != null) {
            setIfPresent(armor, "helmet", item -> inv.setHelmet(item));
            setIfPresent(armor, "chestplate", item -> inv.setChestplate(item));
            setIfPresent(armor, "leggings", item -> inv.setLeggings(item));
            setIfPresent(armor, "boots", item -> inv.setBoots(item));
        }

        String offhandDef = kitSection.getString("offhand");
        if (offhandDef != null) {
            ItemStack offhand = ItemBuilder.parse(offhandDef);
            if (offhand != null) inv.setItemInOffHand(offhand);
        }

        ConfigurationSection inventorySection = kitSection.getConfigurationSection("inventory");
        if (inventorySection != null) {
            for (String key : inventorySection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = ItemBuilder.parse(inventorySection.getString(key));
                    if (item != null && slot >= 0 && slot < inv.getSize()) {
                        inv.setItem(slot, item);
                    }
                } catch (NumberFormatException ignored) {
                    // Non-numeric keys are ignored.
                }
            }
        }

        return true;
    }

    private void setIfPresent(ConfigurationSection section, String key, java.util.function.Consumer<ItemStack> consumer) {
        String def = section.getString(key);
        if (def == null) return;
        ItemStack item = ItemBuilder.parse(def);
        if (item != null) consumer.accept(item);
    }

    public void giveDefaultKit(Player player) {
        String defaultKit = plugin.configManager().config().getString("revive.give-kit-if-empty", "k1");
        giveKit(player, defaultKit);
    }

    public boolean isValidKit(String name) {
        return plugin.configManager().kits().getConfigurationSection(name) != null;
    }
}
