package org.little100.super_Farming.recipe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.little100.super_Farming.Super_Farming;
import org.little100.super_Farming.item.ItemManager;

public class RecipeManager {

    private static final Super_Farming plugin = Super_Farming.getPlugin();

    public static void init() {
        createTier1Recipes();
        createTier2Recipes();
    }

    private static void createTier1Recipes() {
        ShapelessRecipe woodenHoe1 = new ShapelessRecipe(new NamespacedKey(plugin, "wooden_hoe_tier1"), ItemManager.WOODEN_HOE_TIER1);
        RecipeChoice.MaterialChoice woodPlanks = new RecipeChoice.MaterialChoice(Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS, Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.CRIMSON_PLANKS, Material.WARPED_PLANKS);
        woodenHoe1.addIngredient(woodPlanks);
        woodenHoe1.addIngredient(woodPlanks);
        woodenHoe1.addIngredient(woodPlanks);
        woodenHoe1.addIngredient(woodPlanks);
        woodenHoe1.addIngredient(woodPlanks);
        woodenHoe1.addIngredient(woodPlanks);
        woodenHoe1.addIngredient(woodPlanks);
        woodenHoe1.addIngredient(woodPlanks);
        woodenHoe1.addIngredient(Material.WOODEN_HOE);
        Bukkit.addRecipe(woodenHoe1);

        ShapelessRecipe stoneHoe1 = new ShapelessRecipe(new NamespacedKey(plugin, "stone_hoe_tier1"), ItemManager.STONE_HOE_TIER1);
        stoneHoe1.addIngredient(8, Material.COBBLESTONE);
        stoneHoe1.addIngredient(Material.STONE_HOE);
        Bukkit.addRecipe(stoneHoe1);

        ShapelessRecipe ironHoe1 = new ShapelessRecipe(new NamespacedKey(plugin, "iron_hoe_tier1"), ItemManager.IRON_HOE_TIER1);
        ironHoe1.addIngredient(8, Material.IRON_INGOT);
        ironHoe1.addIngredient(Material.IRON_HOE);
        Bukkit.addRecipe(ironHoe1);

        ShapelessRecipe goldenHoe1 = new ShapelessRecipe(new NamespacedKey(plugin, "golden_hoe_tier1"), ItemManager.GOLDEN_HOE_TIER1);
        goldenHoe1.addIngredient(8, Material.GOLD_INGOT);
        goldenHoe1.addIngredient(Material.GOLDEN_HOE);
        Bukkit.addRecipe(goldenHoe1);

        ShapelessRecipe diamondHoe1 = new ShapelessRecipe(new NamespacedKey(plugin, "diamond_hoe_tier1"), ItemManager.DIAMOND_HOE_TIER1);
        diamondHoe1.addIngredient(8, Material.DIAMOND);
        diamondHoe1.addIngredient(Material.DIAMOND_HOE);
        Bukkit.addRecipe(diamondHoe1);
    }

    private static void createTier2Recipes() {
        ShapelessRecipe woodenHoe2 = new ShapelessRecipe(new NamespacedKey(plugin, "wooden_hoe_tier2"), ItemManager.WOODEN_HOE_TIER2);
        woodenHoe2.addIngredient(Material.WOODEN_HOE);
        woodenHoe2.addIngredient(new RecipeChoice.MaterialChoice(Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.CRIMSON_STEM, Material.WARPED_STEM));
        Bukkit.addRecipe(woodenHoe2);

        ShapelessRecipe stoneHoe2 = new ShapelessRecipe(new NamespacedKey(plugin, "stone_hoe_tier2"), ItemManager.STONE_HOE_TIER2);
        stoneHoe2.addIngredient(Material.STONE_HOE);
        stoneHoe2.addIngredient(Material.STONE);
        Bukkit.addRecipe(stoneHoe2);

        ShapelessRecipe ironHoe2 = new ShapelessRecipe(new NamespacedKey(plugin, "iron_hoe_tier2"), ItemManager.IRON_HOE_TIER2);
        ironHoe2.addIngredient(Material.IRON_HOE);
        ironHoe2.addIngredient(Material.IRON_BLOCK);
        Bukkit.addRecipe(ironHoe2);

        ShapelessRecipe goldenHoe2 = new ShapelessRecipe(new NamespacedKey(plugin, "golden_hoe_tier2"), ItemManager.GOLDEN_HOE_TIER2);
        goldenHoe2.addIngredient(Material.GOLDEN_HOE);
        goldenHoe2.addIngredient(Material.GOLD_BLOCK);
        Bukkit.addRecipe(goldenHoe2);

        ShapelessRecipe diamondHoe2 = new ShapelessRecipe(new NamespacedKey(plugin, "diamond_hoe_tier2"), ItemManager.DIAMOND_HOE_TIER2);
        diamondHoe2.addIngredient(Material.DIAMOND_HOE);
        diamondHoe2.addIngredient(Material.DIAMOND_BLOCK);
        Bukkit.addRecipe(diamondHoe2);
    }
}