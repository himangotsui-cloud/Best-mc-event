package net.cpvpevent.plugin.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class WizardHolder implements InventoryHolder {
    private Inventory inventory;
    private int step = 1;
    private Integer chosenMinutes;
    private String chosenMode;
    private Boolean chosenAnnounce;

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public int step() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public Integer chosenMinutes() {
        return chosenMinutes;
    }

    public void setChosenMinutes(Integer chosenMinutes) {
        this.chosenMinutes = chosenMinutes;
    }

    public String chosenMode() {
        return chosenMode;
    }

    public void setChosenMode(String chosenMode) {
        this.chosenMode = chosenMode;
    }

    public Boolean chosenAnnounce() {
        return chosenAnnounce;
    }

    public void setChosenAnnounce(Boolean chosenAnnounce) {
        this.chosenAnnounce = chosenAnnounce;
    }
}
