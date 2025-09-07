package org.little100.super_Farming.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.little100.super_Farming.Super_Farming;
import org.little100.super_Farming.language.LanguageManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiManager {

    private final Super_Farming plugin;
    private final LanguageManager languageManager;
    public static final String EDITOR_TITLE = "Super Hoe GUI Editor";

    private final NamespacedKey hoeTypeKey;
    private final NamespacedKey hoeModeKey;
    private final NamespacedKey hoeRangeKey;
    private final NamespacedKey hoePlantModeKey;

    public GuiManager(Super_Farming plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        this.hoeTypeKey = new NamespacedKey(plugin, "superhoe");
        this.hoeModeKey = new NamespacedKey(plugin, "hoemode");
        this.hoeRangeKey = new NamespacedKey(plugin, "hoerange");
        this.hoePlantModeKey = new NamespacedKey(plugin, "hoeplantmode");
    }

    public void openGui(Player player) {
        ItemStack hoeItem = player.getInventory().getItemInMainHand();
        if (hoeItem.getType().isAir() || !hoeItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = hoeItem.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(hoeTypeKey, PersistentDataType.STRING)) {
            return;
        }

        boolean isFarmingMode = meta.getPersistentDataContainer().getOrDefault(hoeModeKey, PersistentDataType.BYTE, (byte) 1) == 1;
        int currentRange = meta.getPersistentDataContainer().getOrDefault(hoeRangeKey, PersistentDataType.INTEGER, 1);
        String hoeType = meta.getPersistentDataContainer().get(hoeTypeKey, PersistentDataType.STRING);
        String plantMode = meta.getPersistentDataContainer().getOrDefault(hoePlantModeKey, PersistentDataType.STRING, "LILY_PAD");

        int hoeLevel = getHoeLevel(hoeType);
        int maxRange = getMaxRangeForLevel(hoeLevel);

        String title = languageManager.getString("gui.title");
        Inventory gui = loadGuiFromFile(title);
        if (gui == null) {
            gui = Bukkit.createInventory(null, 54, title);
            fillBorder(gui);
        }

        updateModeButtons(gui, isFarmingMode);

        updateRangeButtons(gui, currentRange, maxRange);

        updateInfoPaper(gui, currentRange);

        updatePlantingButtons(gui, plantMode);

        player.openInventory(gui);
    }
    
    public void toggleHoeMode(Player player, ItemStack hoeItem) {
        if (hoeItem == null || !hoeItem.hasItemMeta()) return;
        
        ItemMeta meta = hoeItem.getItemMeta();
        boolean currentModeIsFarming = meta.getPersistentDataContainer().getOrDefault(hoeModeKey, PersistentDataType.BYTE, (byte)1) == 1;
        boolean newModeIsFarming = !currentModeIsFarming;

        meta.getPersistentDataContainer().set(hoeModeKey, PersistentDataType.BYTE, (byte)(newModeIsFarming ? 1 : 0));

        meta.removeEnchant(Enchantment.POWER);
        meta.removeEnchant(Enchantment.EFFICIENCY);

        if (newModeIsFarming) {
             meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        } else {
             meta.addEnchant(Enchantment.POWER, 1, true);
        }
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        meta = updateHoeLore(meta);
        hoeItem.setItemMeta(meta);

        openGui(player);
    }

    public void setHoeRange(Player player, ItemStack hoeItem, int newRange) {
        if (hoeItem == null || !hoeItem.hasItemMeta()) return;
        
        ItemMeta meta = hoeItem.getItemMeta();
        String hoeType = meta.getPersistentDataContainer().get(hoeTypeKey, PersistentDataType.STRING);
        int maxRange = getMaxRangeForLevel(getHoeLevel(hoeType));

        if (newRange > maxRange) {
            newRange = maxRange;
        }
        if (newRange < 1) {
            newRange = 1;
        }

        meta.getPersistentDataContainer().set(hoeRangeKey, PersistentDataType.INTEGER, newRange);

        meta = updateHoeLore(meta);
        hoeItem.setItemMeta(meta);

        openGui(player);
    }
    
    public void setHoePlantMode(Player player, ItemStack hoeItem, String newMode) {
        if (hoeItem == null || !hoeItem.hasItemMeta()) return;
        ItemMeta meta = hoeItem.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(hoePlantModeKey, PersistentDataType.STRING, newMode);

        meta = updateHoeLore(meta);
        hoeItem.setItemMeta(meta);
        openGui(player);
    }

    private void fillBorder(Inventory gui) {
        ItemStack borderItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.setDisplayName(" ");
        borderItem.setItemMeta(borderMeta);
        for (int i = 0; i < gui.getSize(); i++) {
            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, borderItem);
            }
        }
    }
    
    private void updateModeButtons(Inventory gui, boolean isFarmingMode) {
        ItemStack farmingItem = gui.getItem(3);
        if (farmingItem == null) farmingItem = new ItemStack(Material.FARMLAND);
        ItemMeta farmingMeta = farmingItem.getItemMeta();
        if (farmingMeta != null) {
            farmingMeta.setDisplayName(languageManager.getString("gui.mode.farming"));
            String status = isFarmingMode ? languageManager.getString("gui.selected") : languageManager.getString("gui.not_selected");
            updateLoreWithStatus(farmingMeta, status);
            farmingItem.setItemMeta(farmingMeta);
        }

        ItemStack bonemealItem = gui.getItem(5);
        if (bonemealItem == null) bonemealItem = new ItemStack(Material.BONE_MEAL);
        ItemMeta bonemealMeta = bonemealItem.getItemMeta();
        if (bonemealMeta != null) {
            bonemealMeta.setDisplayName(languageManager.getString("gui.mode.bonemeal"));
            String status = !isFarmingMode ? languageManager.getString("gui.selected") : languageManager.getString("gui.not_selected");
            updateLoreWithStatus(bonemealMeta, status);
            bonemealItem.setItemMeta(bonemealMeta);
        }
    }
    
    private void updateRangeButtons(Inventory gui, int currentRange, int maxRange) {
        for (int i = 1; i <= 5; i++) {
            int slot = 11 + (i - 1);
            ItemStack templateItem = gui.getItem(slot);
            ItemMeta templateMeta = (templateItem != null && templateItem.hasItemMeta()) ? templateItem.getItemMeta() : null;

            ItemStack displayItem;
            ItemMeta displayMeta;

            if (i > maxRange) {
                displayItem = new ItemStack(Material.BARRIER);
                displayMeta = displayItem.getItemMeta();
                if (displayMeta != null) {
                    displayMeta.setDisplayName(ChatColor.RED + languageManager.getString("gui.range.unavailable"));
                    displayMeta.setLore(Arrays.asList(ChatColor.GRAY + languageManager.getString("gui.range.upgrade_needed")));
                }
            } else {
                if (i == currentRange) {
                    displayItem = new ItemStack(Material.EMERALD_BLOCK);
                    displayMeta = displayItem.getItemMeta();
                    if (displayMeta != null) {
                        displayMeta.setDisplayName(ChatColor.GREEN + getRangeDescription(i));
                        if (templateMeta != null) {
                             if(templateMeta.hasLore()) displayMeta.setLore(templateMeta.getLore());
                        }
                        updateLoreWithStatus(displayMeta, languageManager.getString("gui.selected"));
                    }
                } else {
                    displayItem = new ItemStack(Material.REDSTONE_BLOCK);
                    displayMeta = displayItem.getItemMeta();
                    if (displayMeta != null) {
                        displayMeta.setDisplayName(ChatColor.YELLOW + getRangeDescription(i));
                         if (templateMeta != null) {
                             if(templateMeta.hasLore()) displayMeta.setLore(templateMeta.getLore());
                        }
                        updateLoreWithStatus(displayMeta, languageManager.getString("gui.range.click_to_select"));
                    }
                }
            }

            if (displayItem != null) {
                displayItem.setItemMeta(displayMeta);
                gui.setItem(slot, displayItem);
            }
        }
    }
    
    private void updateInfoPaper(Inventory gui, int currentRange) {
        ItemStack infoItem = gui.getItem(49);
        if (infoItem == null) infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.GOLD + languageManager.getString("gui.range.current"));
            String rangeLine = ChatColor.GRAY + languageManager.getString("gui.range.label") + " " + ChatColor.GREEN + getRangeDescription(currentRange);
            updateLoreWithStatus(infoMeta, rangeLine);
            infoItem.setItemMeta(infoMeta);
        }
    }

    private void updatePlantingButtons(Inventory gui, String currentPlantMode) {
        for (int i = 28; i <= 34; i++) {
            ItemStack item = gui.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            if (item.getType() == Material.GLOW_BERRIES) {
                ItemStack barrier = new ItemStack(Material.BARRIER);
                ItemMeta barrierMeta = barrier.getItemMeta();
                barrierMeta.setDisplayName(ChatColor.RED + languageManager.getString("gui.plant.glow_berries"));
                barrierMeta.setLore(Arrays.asList(ChatColor.GRAY + languageManager.getString("gui.range.unavailable")));
                barrier.setItemMeta(barrierMeta);
                gui.setItem(i, barrier);
                continue;
            }

            String materialName = item.getType().toString().toLowerCase();
            String langKey = "gui.plant." + materialName;
            String translatedName = languageManager.getString(langKey, materialName);
            meta.setDisplayName(ChatColor.AQUA + translatedName);

            if (item.getType().toString().equals(currentPlantMode)) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                updateLoreWithStatus(meta, languageManager.getString("gui.selected"));
            } else {
                for (Enchantment enchantment : meta.getEnchants().keySet()) {
                    meta.removeEnchant(enchantment);
                }
                updateLoreWithStatus(meta, languageManager.getString("gui.not_selected"));
            }
            item.setItemMeta(meta);
        }
    }

    private void updateLoreWithStatus(ItemMeta meta, String statusLine) {
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        List<String> statusLinesToRemove = Arrays.asList(
                languageManager.getString("gui.selected"),
                languageManager.getString("gui.not_selected"),
                languageManager.getString("gui.range.click_to_select"),
                ChatColor.GRAY + languageManager.getString("gui.range.label") + " " + ChatColor.GREEN + "1x1",
                ChatColor.GRAY + languageManager.getString("gui.range.label") + " " + ChatColor.GREEN + "3x3",
                ChatColor.GRAY + languageManager.getString("gui.range.label") + " " + ChatColor.GREEN + "5x5",
                ChatColor.GRAY + languageManager.getString("gui.range.label") + " " + ChatColor.GREEN + "7x7",
                ChatColor.GRAY + languageManager.getString("gui.range.label") + " " + ChatColor.GREEN + "9x9"
        );

        lore.removeIf(line -> {
            String cleanLine = ChatColor.stripColor(line);
            for (String toRemove : statusLinesToRemove) {
                if (cleanLine.equals(ChatColor.stripColor(toRemove))) {
                    return true;
                }
            }
            String rangeLabel = languageManager.getString("gui.range.label");
            if(cleanLine.startsWith(ChatColor.stripColor(rangeLabel))) {
                return true;
            }
            return false;
        });

        lore.add(statusLine);

        meta.setLore(lore);
    }

    private int getHoeLevel(String hoeType) {
        if (hoeType == null) return 0;
        if (hoeType.startsWith("WOODEN")) return 1;
        if (hoeType.startsWith("STONE")) return 2;
        if (hoeType.startsWith("IRON")) return 3;
        if (hoeType.startsWith("GOLDEN")) return 4;
        if (hoeType.startsWith("DIAMOND")) return 5;
        return 0;
    }

    private int getMaxRangeForLevel(int level) {
        return level;
    }

    private String getRangeDescription(int range) {
        switch (range) {
            case 1: return "1x1";
            case 2: return "3x3";
            case 3: return "5x5";
            case 4: return "7x7";
            case 5: return "9x9";
            default: return "Unknown";
        }
    }

    public void openEditor(Player player) {
        Inventory editorGui = loadGuiFromFile(EDITOR_TITLE);
        if (editorGui == null) {
            editorGui = Bukkit.createInventory(null, 54, EDITOR_TITLE);
            fillWithDefaultItems(editorGui);
        }
        player.openInventory(editorGui);
    }
    
    private void fillWithDefaultItems(Inventory inventory) {
        ItemStack borderItem = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.setDisplayName(" ");
        borderItem.setItemMeta(borderMeta);
        
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, borderItem);
            }
        }
    }
    
    private Inventory loadGuiFromFile(String title) {
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            return null;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
        int size = config.getInt("size", 54);

        Inventory gui = Bukkit.createInventory(null, size, title);
        
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String slotStr : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotStr);
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotStr);
                    if (itemSection != null) {
                        Material material = Material.valueOf(itemSection.getString("material", "AIR"));
                        ItemStack item = new ItemStack(material);

                        if (itemSection.contains("name") || itemSection.contains("lore")) {
                            ItemMeta meta = item.getItemMeta();
                            if (itemSection.contains("name")) {
                                String name = itemSection.getString("name");
                                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                            }
                            if (itemSection.contains("lore")) {
                                List<String> lore = itemSection.getStringList("lore");
                                List<String> coloredLore = new ArrayList<>();
                                for (String line : lore) {
                                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                                }
                                meta.setLore(coloredLore);
                            }
                            item.setItemMeta(meta);
                        }
                        
                        gui.setItem(slot, item);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid slot or material in gui.yml: " + slotStr);
                }
            }
        }
        
        return gui;
    }
    
    public void saveGuiToFile(Inventory inventory) {
        FileConfiguration config = new YamlConfiguration();
        config.set("title", inventory.getType().getDefaultTitle());
        config.set("size", inventory.getSize());

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                String path = "items." + i;
                config.set(path + ".material", item.getType().toString());
                
                if (item.hasItemMeta()) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta.hasDisplayName()) {
                        config.set(path + ".name", meta.getDisplayName().replace(ChatColor.COLOR_CHAR, '&'));
                    }
                    if (meta.hasLore()) {
                        List<String> lore = meta.getLore();
                        List<String> uncoloredLore = new ArrayList<>();
                        for (String line : lore) {
                            uncoloredLore.add(line.replace(ChatColor.COLOR_CHAR, '&'));
                        }
                        config.set(path + ".lore", uncoloredLore);
                    }
                }
            }
        }
        
        try {
            config.save(new File(plugin.getDataFolder(), "gui.yml"));
            plugin.getLogger().info("GUI saved to gui.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save GUI to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public ItemMeta updateHoeLore(ItemMeta meta) {
        if (meta == null) {
            return null;
        }

        List<String> newLore = new ArrayList<>();

        String hoeType = meta.getPersistentDataContainer().get(hoeTypeKey, PersistentDataType.STRING);
        if (hoeType != null) {
            List<String> baseLore = languageManager.getStringList("item.superhoe_lore");
            newLore.addAll(baseLore);
        }
        
        newLore.add(" ");
        boolean isFarmingMode = meta.getPersistentDataContainer().getOrDefault(hoeModeKey, PersistentDataType.BYTE, (byte) 1) == 1;
        String modeKey = isFarmingMode ? "gui.mode.farming" : "gui.mode.bonemeal";
        newLore.add(ChatColor.GRAY + languageManager.getOrDefault("gui.mode.label", "Mode:") + " " + ChatColor.YELLOW + languageManager.getString(modeKey));

        int currentRange = meta.getPersistentDataContainer().getOrDefault(hoeRangeKey, PersistentDataType.INTEGER, 1);
        newLore.add(ChatColor.GRAY + languageManager.getOrDefault("gui.range.label", "Range:") + " " + ChatColor.GREEN + getRangeDescription(currentRange));
        
        if(isFarmingMode) {
            String plantMode = meta.getPersistentDataContainer().getOrDefault(hoePlantModeKey, PersistentDataType.STRING, "LILY_PAD");
            String plantModeKey = "gui.plant." + plantMode.toLowerCase();
            String plantModeName = languageManager.getString(plantModeKey, plantMode);
            newLore.add(ChatColor.GRAY + languageManager.getOrDefault("gui.plant.label", "Planting:") + " " + ChatColor.AQUA + plantModeName);
        }

        meta.setLore(newLore);
        return meta;
    }
}