package org.little100.super_Farming.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.little100.super_Farming.gui.GuiManager;

public class GuiSaveListener implements Listener {

    private final GuiManager guiManager;

    public GuiSaveListener(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(GuiManager.EDITOR_TITLE)) {
            guiManager.saveGuiToFile(event.getInventory());
        }
    }
}