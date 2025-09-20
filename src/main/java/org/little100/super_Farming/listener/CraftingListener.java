package org.little100.super_Farming.listener;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.little100.super_Farming.Super_Farming;
import org.little100.super_Farming.item.ItemManager;

public class CraftingListener implements Listener {
    
    private final Super_Farming plugin;
    
    public CraftingListener(Super_Farming plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack result = inventory.getResult();
        Recipe recipe = event.getRecipe();
        
        if (result == null || recipe == null) return;
        
        // 检查配方
        if (recipe instanceof ShapelessRecipe) {
            ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
            if (isOurTier2Recipe(shapelessRecipe)) {
                if (plugin.isCraftingDebugEnabled()) {
                    plugin.getLogger().info("检测到Tier2配方: " + shapelessRecipe.getKey().getKey());
                }
                
                // 检查是否有锄头存在
                ItemStack[] matrix = inventory.getMatrix();
                boolean hasSuperHoe = false;
                
                for (ItemStack item : matrix) {
                    if (item != null && item.hasItemMeta() && 
                        item.getItemMeta().getPersistentDataContainer()
                            .has(ItemManager.SUPERHOE_KEY, PersistentDataType.STRING)) {
                        hasSuperHoe = true;
                        if (plugin.isCraftingDebugEnabled()) {
                            String hoeType = item.getItemMeta().getPersistentDataContainer()
                                    .get(ItemManager.SUPERHOE_KEY, PersistentDataType.STRING);
                            plugin.getLogger().info("发现超级锄头: " + hoeType);
                        }
                        break;
                    }
                }
                
                if (!hasSuperHoe) {
                    if (plugin.isCraftingDebugEnabled()) {
                        plugin.getLogger().info("没有发现超级锄头，取消合成");
                    }
                    inventory.setResult(null);
                } else {
                    if (plugin.isCraftingDebugEnabled()) {
                        plugin.getLogger().info("Tier2配方验证通过，允许合成");
                    }
                }
                
                return;
            }
        }
        
        if (result != null && isVanillaHoe(result.getType())) {
            ItemStack[] matrix = inventory.getMatrix();
            
            for (ItemStack item : matrix) {
                if (item != null && item.hasItemMeta() && 
                    item.getItemMeta().getPersistentDataContainer()
                        .has(ItemManager.SUPERHOE_KEY, PersistentDataType.STRING)) {
                    if (plugin.isCraftingDebugEnabled()) {
                        plugin.getLogger().info("阻止超级锄头被用于原版配方");
                    }
                    inventory.setResult(null);
                    return;
                }
            }
        }
    }
    
    /**
     * 检查是否为Tier2配方
     */
    private boolean isOurTier2Recipe(ShapelessRecipe recipe) {
        NamespacedKey key = recipe.getKey();
        return key.getNamespace().equals("super_farming") &&
               (key.getKey().equals("wooden_hoe_tier2") ||
                key.getKey().equals("stone_hoe_tier2") ||
                key.getKey().equals("iron_hoe_tier2") ||
                key.getKey().equals("golden_hoe_tier2") ||
                key.getKey().equals("diamond_hoe_tier2"));
    }
    
    /**
     * 验证Tier2配方是否使用了正确的Tier1超级锄头
     */
    private boolean validateTier2Recipe(CraftingInventory inventory, ShapelessRecipe recipe) {
        ItemStack[] matrix = inventory.getMatrix();
        
        String recipeKey = recipe.getKey().getKey();
        String requiredTier1Type = null;
        
        switch (recipeKey) {
            case "wooden_hoe_tier2":
                requiredTier1Type = "WOODEN_HOE_TIER1";
                break;
            case "stone_hoe_tier2":
                requiredTier1Type = "STONE_HOE_TIER1";
                break;
            case "iron_hoe_tier2":
                requiredTier1Type = "IRON_HOE_TIER1";
                break;
            case "golden_hoe_tier2":
                requiredTier1Type = "GOLDEN_HOE_TIER1";
                break;
            case "diamond_hoe_tier2":
                requiredTier1Type = "DIAMOND_HOE_TIER1";
                break;
            default:
                return false;
        }
        
        plugin.getLogger().info("需要的Tier1锄头类型: " + requiredTier1Type);
        
        // 检查材料中是否有正确的Tier1超级锄头
        for (ItemStack item : matrix) {
            if (item != null && item.hasItemMeta()) {
                String hoeType = item.getItemMeta().getPersistentDataContainer()
                        .get(ItemManager.SUPERHOE_KEY, PersistentDataType.STRING);
                
                plugin.getLogger().info("发现物品的锄头类型: " + hoeType);
                
                if (requiredTier1Type.equals(hoeType)) {
                    plugin.getLogger().info("找到了正确的Tier1锄头!");
                    return true; // 找到了正确的Tier1锄头
                }
            }
        }
        
        plugin.getLogger().info("没有找到正确的Tier1锄头");
        return false; // 没有找到正确的Tier1锄头
    }
    
    private boolean isVanillaHoe(Material material) {
        return material == Material.WOODEN_HOE ||
               material == Material.STONE_HOE ||
               material == Material.IRON_HOE ||
               material == Material.GOLDEN_HOE ||
               material == Material.DIAMOND_HOE ||
               material == Material.NETHERITE_HOE;
    }
}
