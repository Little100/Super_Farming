package org.little100.super_Farming.listener;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.little100.super_Farming.Super_Farming;
import org.little100.super_Farming.gui.GuiManager;
import org.little100.super_Farming.language.LanguageManager;

import java.util.Arrays;
import java.util.List;

public class GuiListener implements Listener {

    private final GuiManager guiManager;
    private final LanguageManager languageManager;
    private final Super_Farming plugin;
    private static final List<Integer> PLANTING_SLOTS = Arrays.asList(28, 29, 30, 31, 32, 33, 34);


    public GuiListener(GuiManager guiManager, LanguageManager languageManager, Super_Farming plugin) {
        this.guiManager = guiManager;
        this.languageManager = languageManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        String guiTitle = languageManager.getString("gui.title");
        if (!event.getView().getTitle().equals(guiTitle)) {
            return;
        }
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        int clickedSlot = event.getRawSlot();
        
        if (clickedSlot >= event.getView().getTopInventory().getSize() || clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        ItemStack hoeItem = player.getInventory().getItemInMainHand();
        if (hoeItem.getType().isAir()) {
            return;
        }

        if (clickedSlot == 3 || clickedSlot == 5) {
            guiManager.toggleHoeMode(player, hoeItem);
            return;
        }

        if (clickedSlot >= 11 && clickedSlot <= 15) {
            if (clickedItem.getType() == Material.BARRIER) {
                return;
            }
            int newRange = (clickedSlot - 11) + 1;
            guiManager.setHoeRange(player, hoeItem, newRange);
            return;
        }

        if (PLANTING_SLOTS.contains(clickedSlot)) {
            ItemMeta hoeMeta = hoeItem.getItemMeta();
            if (hoeMeta == null) return;

            NamespacedKey plantModeKey = new NamespacedKey(plugin, "hoeplantmode");
            String currentPlantMode = hoeMeta.getPersistentDataContainer().getOrDefault(plantModeKey, PersistentDataType.STRING, "LILY_PAD");
            String newPlantMode = clickedItem.getType().toString();

            if (currentPlantMode.equals(newPlantMode)) {
                newPlantMode = "LILY_PAD";
            }

            guiManager.setHoePlantMode(player, hoeItem, newPlantMode);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String guiTitle = languageManager.getString("gui.title");
        if (event.getView().getTitle().equals(guiTitle)) {
            event.setCancelled(true);
        }
    }
}