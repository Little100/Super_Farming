package org.little100.super_Farming.item;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.little100.super_Farming.Super_Farming;

public class ItemManager {

    private static final Super_Farming plugin = Super_Farming.getPlugin();
    
    public static final NamespacedKey SUPERHOE_KEY = new NamespacedKey(plugin, "superhoe");
    public static final NamespacedKey HOEMODE_KEY = new NamespacedKey(plugin, "hoemode");
    public static final NamespacedKey HOERANGE_KEY = new NamespacedKey(plugin, "hoerange");
    public static final NamespacedKey HOEPLANTMODE_KEY = new NamespacedKey(plugin, "hoeplantmode");

    public static ItemStack WOODEN_HOE_TIER1;
    public static ItemStack STONE_HOE_TIER1;
    public static ItemStack IRON_HOE_TIER1;
    public static ItemStack GOLDEN_HOE_TIER1;
    public static ItemStack DIAMOND_HOE_TIER1;

    public static ItemStack WOODEN_HOE_TIER2;
    public static ItemStack STONE_HOE_TIER2;
    public static ItemStack IRON_HOE_TIER2;
    public static ItemStack GOLDEN_HOE_TIER2;
    public static ItemStack DIAMOND_HOE_TIER2;

    public static void init() {
        createTier1Hoes();
        createTier2Hoes();
    }

    private static void createTier1Hoes() {
        WOODEN_HOE_TIER1 = createHoe("WOODEN_HOE_TIER1", Material.WOODEN_HOE, "§f超级木锄 I", "§7一把经过改良的木锄，非常耐用。");
        STONE_HOE_TIER1 = createHoe("STONE_HOE_TIER1", Material.STONE_HOE, "§f超级石锄 I", "§7一把经过改良的石锄，非常耐用。");
        IRON_HOE_TIER1 = createHoe("IRON_HOE_TIER1", Material.IRON_HOE, "§f超级铁锄 I", "§7一把经过改良的铁锄，非常耐用。");
        GOLDEN_HOE_TIER1 = createHoe("GOLDEN_HOE_TIER1", Material.GOLDEN_HOE, "§f超级金锄 I", "§7一把经过改良的金锄，非常耐用。");
        DIAMOND_HOE_TIER1 = createHoe("DIAMOND_HOE_TIER1", Material.DIAMOND_HOE, "§f超级钻石锄 I", "§7一把经过改良的钻石锄，非常耐用。");
    }

    private static void createTier2Hoes() {
        WOODEN_HOE_TIER2 = createHoe("WOODEN_HOE_TIER2", Material.WOODEN_HOE, "§f超级木锄 II", true, "§7一把经过改良的木锄，非常耐用。", "§7拥有更快的挖掘速度。");
        STONE_HOE_TIER2 = createHoe("STONE_HOE_TIER2", Material.STONE_HOE, "§f超级石锄 II", true, "§7一把经过改良的石锄，非常耐用。", "§7拥有更快的挖掘速度。");
        IRON_HOE_TIER2 = createHoe("IRON_HOE_TIER2", Material.IRON_HOE, "§f超级铁锄 II", true, "§7一把经过改良的铁锄，非常耐用。", "§7拥有更快的挖掘速度。");
        GOLDEN_HOE_TIER2 = createHoe("GOLDEN_HOE_TIER2", Material.GOLDEN_HOE, "§f超级金锄 II", true, "§7一把经过改良的金锄，非常耐用。", "§7拥有更快的挖掘速度。");
        DIAMOND_HOE_TIER2 = createHoe("DIAMOND_HOE_TIER2", Material.DIAMOND_HOE, "§f超级钻石锄 II", true, "§7一把经过改良的钻石锄，非常耐用。", "§7拥有更快的挖掘速度。");
    }

    private static ItemStack createHoe(String key, Material material, String displayName, String... lore) {
        return createHoe(key, material, displayName, false, lore);
    }

    private static ItemStack createHoe(String key, Material material, String displayName, boolean enchanted, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        meta.getPersistentDataContainer().set(SUPERHOE_KEY, PersistentDataType.STRING, key);
        meta.getPersistentDataContainer().set(HOEMODE_KEY, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(HOERANGE_KEY, PersistentDataType.INTEGER, 1);
        meta.getPersistentDataContainer().set(HOEPLANTMODE_KEY, PersistentDataType.STRING, "LILY_PAD");

        if (enchanted) {
            meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta = Super_Farming.getPlugin().getGuiManager().updateHoeLore(meta);

        item.setItemMeta(meta);
        return item;
    }
}