package net.cpvpevent.plugin.gui;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.event.EventMode;
import net.cpvpevent.plugin.util.ItemBuilder;
import net.cpvpevent.plugin.util.Text;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Builds and handles clicks for the /eventsettings GUI and the multi-step
 * /startevent wizard opened through /eventools.
 */
public class GUIManager implements Listener {

    private final CPVPEventPlus plugin;

    public GUIManager(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Event settings main GUI
    // ------------------------------------------------------------------

    public void openSettingsGUI(Player player) {
        EventSettingsHolder holder = new EventSettingsHolder();
        Inventory inv = plugin.getServer().createInventory(holder, 27, Text.color("&8Event Settings"));
        holder.setInventory(inv);

        inv.setItem(10, toggleItem(Material.IRON_SWORD, "&cPvP Toggle", plugin.pvpManager().isPvpEnabled()));
        inv.setItem(11, toggleItem(Material.SHIELD, "&bProtection Toggle", plugin.pvpManager().isProtectionEnabled()));
        inv.setItem(12, toggleItem(Material.PAPER, "&eChat Toggle", plugin.chatManager().isGlobalChatEnabled()));
        inv.setItem(13, toggleItem(Material.GOLDEN_APPLE, "&aRekit Toggle", plugin.rekitManager().isGlobalEnabled()));
        inv.setItem(14, toggleItem(Material.BOOK, "&4Staff Chat", false));
        inv.setItem(15, toggleItem(Material.ENDER_EYE, "&dJoin Spectator", plugin.configManager().config().getBoolean("event.join-spectator-by-default")));

        inv.setItem(19, simpleItem(Material.EMERALD, "&a&lStart Event", "&7Opens the start wizard"));
        inv.setItem(20, simpleItem(Material.TOTEM_OF_UNDYING, "&b&lRevive All", "&7Revive every dead player"));
        inv.setItem(21, simpleItem(Material.TNT, "&c&lClear Blocks", "&7Remove configured PvP blocks"));
        inv.setItem(22, simpleItem(Material.CHEST, "&6&lKit All", "&7Give the default kit to everyone"));
        inv.setItem(23, simpleItem(Material.LAVA_BUCKET, "&4&lDrop", "&7Trigger a deepslate drop"));
        inv.setItem(24, simpleItem(Material.NETHERITE_BLOCK, "&5&lSet Netherite", "&7Convert bedrock -> netherite"));
        inv.setItem(25, simpleItem(Material.BEDROCK, "&8&lSet Bedrock", "&7Convert netherite -> bedrock"));
        inv.setItem(16, simpleItem(Material.NETHER_STAR, "&e&lAnnounce Winner", "&7Announce the stored winner"));

        player.openInventory(inv);
    }

    private ItemStack toggleItem(Material material, String name, boolean state) {
        String status = state ? "&aEnabled" : "&cDisabled";
        return simpleItem(material, name, status);
    }

    private ItemStack simpleItem(Material material, String name, String lore) {
        return new ItemBuilder(material).name(name).lore(List.of(lore)).build();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof EventSettingsHolder) {
            event.setCancelled(true);
            handleSettingsClick(event);
        } else if (event.getInventory().getHolder() instanceof WizardHolder wizardHolder) {
            event.setCancelled(true);
            handleWizardClick(event, wizardHolder);
        }
    }

    private void handleSettingsClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;

        playClickSound(player);
        String name = clicked.getItemMeta().getDisplayName();

        if (name.contains("PvP Toggle")) {
            plugin.pvpManager().setPvpEnabled(!plugin.pvpManager().isPvpEnabled());
        } else if (name.contains("Protection Toggle")) {
            plugin.pvpManager().setProtectionEnabled(!plugin.pvpManager().isProtectionEnabled());
        } else if (name.contains("Chat Toggle")) {
            plugin.chatManager().setGlobalChatEnabled(!plugin.chatManager().isGlobalChatEnabled());
        } else if (name.contains("Rekit Toggle")) {
            plugin.rekitManager().setGlobalEnabled(!plugin.rekitManager().isGlobalEnabled());
        } else if (name.contains("Start Event")) {
            player.closeInventory();
            openStartWizard(player);
        } else if (name.contains("Revive All")) {
            plugin.reviveManager().reviveAll();
        } else if (name.contains("Clear Blocks")) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "clearblock");
        } else if (name.contains("Kit All")) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "kitall k1");
        } else if (name.contains("Drop")) {
            plugin.dropManager().triggerDrop("DEEPSLATE");
        } else if (name.contains("Set Netherite")) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "setnetherite");
        } else if (name.contains("Set Bedrock")) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "setbedrock");
        } else if (name.contains("Announce Winner")) {
            plugin.eventManager().announceStoredWinner();
        }

        openSettingsGUI(player); // refresh state
    }

    // ------------------------------------------------------------------
    // Start Event Wizard (5 steps: type -> duration -> mode -> announce -> confirm)
    // ------------------------------------------------------------------

    public void openStartWizard(Player player) {
        WizardHolder holder = new WizardHolder();
        holder.setStep(1);
        Inventory inv = plugin.getServer().createInventory(holder, 27, Text.color("&8Start Event - Step 1/5"));
        holder.setInventory(inv);

        inv.setItem(11, simpleItem(Material.CLOCK, "&aTimed Start", "&7Countdown before starting"));
        inv.setItem(15, simpleItem(Material.FEATHER, "&cForce Start", "&7Start immediately"));
        inv.setItem(26, simpleItem(Material.BARRIER, "&cCancel", ""));

        player.openInventory(inv);
    }

    private void handleWizardClick(InventoryClickEvent event, WizardHolder holder) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;

        playClickSound(player);
        ItemMeta meta = clicked.getItemMeta();
        String name = meta.getDisplayName();
        int slot = event.getRawSlot();

        if (name.contains("Cancel")) {
            player.closeInventory();
            return;
        }
        if (name.contains("Back") && holder.step() > 1) {
            renderStep(player, holder, holder.step() - 1);
            return;
        }

        switch (holder.step()) {
            case 1 -> {
                holder.setChosenMinutes(name.contains("Force") ? 0 : plugin.configManager().config().getInt("event.countdown-seconds", 10) / 60);
                renderStep(player, holder, 2);
            }
            case 2 -> {
                // Duration presets at fixed slots
                if (slot == 11) holder.setChosenMinutes(1);
                else if (slot == 13) holder.setChosenMinutes(3);
                else if (slot == 15) holder.setChosenMinutes(5);
                renderStep(player, holder, 3);
            }
            case 3 -> {
                if (slot == 11) holder.setChosenMode(EventMode.AUTOMATIC.name());
                else if (slot == 12) holder.setChosenMode(EventMode.HIGH_PLAYERS.name());
                else if (slot == 13) holder.setChosenMode(EventMode.LOW_PLAYERS.name());
                else if (slot == 14) holder.setChosenMode(EventMode.MANUALLY.name());
                renderStep(player, holder, 4);
            }
            case 4 -> {
                holder.setChosenAnnounce(slot == 11);
                renderStep(player, holder, 5);
            }
            case 5 -> {
                if (name.contains("Confirm")) {
                    EventMode mode = EventMode.fromString(holder.chosenMode());
                    boolean announce = Boolean.TRUE.equals(holder.chosenAnnounce());
                    int minutes = holder.chosenMinutes() == null ? 0 : holder.chosenMinutes();
                    if (minutes <= 0) {
                        plugin.eventManager().startForce(mode, announce);
                    } else {
                        plugin.eventManager().startEvent(minutes, mode, announce);
                    }
                    player.closeInventory();
                }
            }
        }
    }

    private void renderStep(Player player, WizardHolder holder, int step) {
        holder.setStep(step);
        Inventory inv = holder.getInventory();
        inv.clear();
        player.openInventory(inv); // keep same inventory instance, re-render title via new one if needed

        // Since Bukkit inventories can't rename titles in place pre-1.20 easily,
        // we simply repopulate items for the new step in the same inventory.
        switch (step) {
            case 2 -> {
                inv.setItem(11, simpleItem(Material.CLOCK, "&a1 Minute", ""));
                inv.setItem(13, simpleItem(Material.CLOCK, "&a3 Minutes", ""));
                inv.setItem(15, simpleItem(Material.CLOCK, "&a5 Minutes", ""));
            }
            case 3 -> {
                inv.setItem(11, simpleItem(Material.REDSTONE, "&bAutomatic", "&7Runs configured stages"));
                inv.setItem(12, simpleItem(Material.PLAYER_HEAD, "&bHigh Players", ""));
                inv.setItem(13, simpleItem(Material.PLAYER_HEAD, "&bLow Players", ""));
                inv.setItem(14, simpleItem(Material.LEVER, "&bManually", ""));
            }
            case 4 -> {
                inv.setItem(11, simpleItem(Material.LIME_DYE, "&aAnnounce", ""));
                inv.setItem(15, simpleItem(Material.GRAY_DYE, "&7Silent", ""));
            }
            case 5 -> {
                inv.setItem(13, simpleItem(Material.EMERALD_BLOCK, "&a&lConfirm", "&7Start the event now"));
                inv.setItem(21, simpleItem(Material.BARRIER, "&cCancel", ""));
            }
            default -> { }
        }

        inv.setItem(26, simpleItem(Material.ARROW, "&eBack", ""));
    }

    private void playClickSound(Player player) {
        String soundName = plugin.configManager().config().getString("sounds.gui-click", "UI_BUTTON_CLICK");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 1f, 1f);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
