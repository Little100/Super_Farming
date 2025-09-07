package org.little100.super_Farming.listener;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.little100.super_Farming.Super_Farming;
import org.little100.super_Farming.gui.GuiManager;

import java.util.*;


public class PlayerListener implements Listener {

    private final Super_Farming plugin;
    private final GuiManager guiManager;
    private final Random random = new Random();
    private static final List<String> ALLOWED_HOES = Arrays.asList(
            "WOODEN_HOE_TIER1", "WOODEN_HOE_TIER2",
            "STONE_HOE_TIER1", "STONE_HOE_TIER2",
            "IRON_HOE_TIER1", "IRON_HOE_TIER2",
            "GOLDEN_HOE_TIER1", "GOLDEN_HOE_TIER2",
            "DIAMOND_HOE_TIER1", "DIAMOND_HOE_TIER2"
    );

    public PlayerListener(Super_Farming plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        
        if (mainHandItem == null || !mainHandItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = mainHandItem.getItemMeta();
        String hoeType = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "superhoe"), PersistentDataType.STRING);

        if (hoeType != null && ALLOWED_HOES.contains(hoeType)) {
            event.setCancelled(true);
            guiManager.openGui(player);
        }
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (player.isSneaking() && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.COMPOSTER) {
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType().isAir() || !itemInHand.hasItemMeta()) {
            return;
        }

        ItemMeta meta = itemInHand.getItemMeta();
        NamespacedKey hoeTypeKey = new NamespacedKey(plugin, "superhoe");
        NamespacedKey hoeModeKey = new NamespacedKey(plugin, "hoemode");
        NamespacedKey hoeRangeKey = new NamespacedKey(plugin, "hoerange");
        NamespacedKey hoePlantModeKey = new NamespacedKey(plugin, "hoeplantmode");

        if (!meta.getPersistentDataContainer().has(hoeTypeKey, PersistentDataType.STRING)) {
            return;
        }

        event.setCancelled(true);

        boolean isFarmingMode = meta.getPersistentDataContainer().getOrDefault(hoeModeKey, PersistentDataType.BYTE, (byte) 1) == 1;
        if (!isFarmingMode) {
            int rangeLevel = meta.getPersistentDataContainer().getOrDefault(hoeRangeKey, PersistentDataType.INTEGER, 1);
            int range = getRangeFromLevel(rangeLevel);
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) return;

            int durabilityLost = applyBonemealEffect(player, clickedBlock.getLocation(), range);
            if (durabilityLost > 0) {
                 org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
                 damageable.setDamage(damageable.getDamage() + durabilityLost);
                 itemInHand.setItemMeta(meta);
            }
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Material clickedType = clickedBlock.getType();
        if (clickedType != Material.DIRT && clickedType != Material.GRASS_BLOCK && clickedType != Material.FARMLAND) {
            return;
        }


        int rangeLevel = meta.getPersistentDataContainer().getOrDefault(hoeRangeKey, PersistentDataType.INTEGER, 1);
        int range = getRangeFromLevel(rangeLevel);


        Location center = clickedBlock.getLocation();
        int durabilityLost = 0;
        List<Block> farmlandToPlant = new ArrayList<>();


        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                Block currentBlock = center.clone().add(x, 0, z).getBlock();
                Material currentType = currentBlock.getType();

                if (currentType == Material.DIRT || currentType == Material.GRASS_BLOCK) {
                    currentBlock.setType(Material.FARMLAND);
                    farmlandToPlant.add(currentBlock);
                    durabilityLost++;
                } else if (currentType == Material.FARMLAND) {
                    farmlandToPlant.add(currentBlock);
                }
            }
        }

        if (!farmlandToPlant.isEmpty()) {
            String plantMode = meta.getPersistentDataContainer().getOrDefault(hoePlantModeKey, PersistentDataType.STRING, "LILY_PAD");
            int plantedCount = plantCrops(player, center, range, plantMode);
            durabilityLost += plantedCount;
        }


        if (durabilityLost > 0) {
             org.bukkit.inventory.meta.Damageable damageable = (org.bukkit.inventory.meta.Damageable) meta;
             damageable.setDamage(damageable.getDamage() + durabilityLost);
             itemInHand.setItemMeta(meta);
        }
    }

    private int plantCrops(Player player, Location center, int range, String plantMode) {
        switch (plantMode) {
            case "SWEET_BERRIES":
                return plantIntervalCrop(player, center, range, Material.SWEET_BERRIES, false);
            case "GLOW_BERRIES":
                return 0;
            case "LILY_PAD":
                return plantRandomly(player, center, range);
            default:
                try {
                    Material specificSeed = Material.valueOf(plantMode);
                    return plantSpecificCrop(player, center, range, specificSeed);
                } catch (IllegalArgumentException e) {
                    return 0;
                }
        }
    }
    
    private int plantRandomly(Player player, Location center, int range) {
        int plantedCount = 0;
        List<Material> regularSeeds = Arrays.asList(Material.WHEAT_SEEDS, Material.CARROT, Material.POTATO, Material.BEETROOT_SEEDS);
        Map<Material, Material> seedToCropMap = getSeedToCropMap();

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                Block block = center.clone().add(x, 0, z).getBlock();
                if (block.getType() != Material.FARMLAND) continue;

                Block above = block.getRelative(0, 1, 0);
                if (above.getType() == Material.AIR) {
                    List<Material> availableSeeds = new ArrayList<>();
                    for(Material seed : regularSeeds) {
                        if(player.getInventory().contains(seed)) {
                            availableSeeds.add(seed);
                        }
                    }

                    if (!availableSeeds.isEmpty()) {
                        Material seedToUse = availableSeeds.get(random.nextInt(availableSeeds.size()));
                        if (consumeItem(player, seedToUse)) {
                             above.setType(seedToCropMap.get(seedToUse));
                             plantedCount++;
                        }
                    }
                }
            }
        }
        return plantedCount;
    }
    
    private int plantIntervalCrop(Player player, Location center, int range, Material cropMaterial, boolean isHanging) {
        int plantedCount = 0;
        Material blockToPlace;
        if (cropMaterial == Material.SWEET_BERRIES) {
            blockToPlace = Material.SWEET_BERRY_BUSH;
        } else if (cropMaterial == Material.GLOW_BERRIES) {
            blockToPlace = Material.CAVE_VINES;
        } else {
            return 0;
        }

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                if ((Math.abs(x) + Math.abs(z)) % 2 != 0) continue;

                Block baseBlock = center.clone().add(x, 0, z).getBlock();
                if (baseBlock.getType() != Material.FARMLAND && !isHanging) continue;
                if (baseBlock.getType() == Material.AIR && isHanging) continue;


                Block targetBlock = isHanging ? baseBlock.getRelative(0, -1, 0) : baseBlock.getRelative(0, 1, 0);

                if (targetBlock.getType() == Material.AIR) {
                    if (consumeItem(player, cropMaterial)) {
                        targetBlock.setType(blockToPlace);
                        if (isHanging) {
                            org.bukkit.block.data.type.CaveVines plantData = (org.bukkit.block.data.type.CaveVines) targetBlock.getBlockData();
                            plantData.setBerries(true);
                            targetBlock.setBlockData(plantData);
                        }
                        plantedCount++;
                    }
                }
            }
        }
        return plantedCount;
    }

    private int plantSpecificCrop(Player player, Location center, int range, Material specificSeed) {
        int plantedCount = 0;
        Map<Material, Material> seedToCropMap = getSeedToCropMap();
        Material cropType = seedToCropMap.get(specificSeed);
        if (cropType == null) return 0;

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                 Block block = center.clone().add(x, 0, z).getBlock();
                if (block.getType() != Material.FARMLAND) continue;

                Block above = block.getRelative(0, 1, 0);
                if (above.getType() == Material.AIR) {
                    if (consumeItem(player, specificSeed)) {
                        above.setType(cropType);
                        plantedCount++;
                    }
                }
            }
        }
        return plantedCount;
    }

    private boolean consumeItem(Player player, Material material) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        if (player.getInventory().contains(material)) {
            player.getInventory().removeItem(new ItemStack(material, 1));
            return true;
        }
        return false;
    }

    private Map<Material, Material> getSeedToCropMap() {
        Map<Material, Material> map = new HashMap<>();
        map.put(Material.WHEAT_SEEDS, Material.WHEAT);
        map.put(Material.CARROT, Material.CARROTS);
        map.put(Material.POTATO, Material.POTATOES);
        map.put(Material.BEETROOT_SEEDS, Material.BEETROOTS);
        return map;
    }

    private int getMaxRangeLevelFromHoe(ItemStack hoe) {
        if (hoe == null || !hoe.hasItemMeta()) {
            return 1;
        }
        ItemMeta meta = hoe.getItemMeta();
        NamespacedKey hoeTypeKey = new NamespacedKey(plugin, "superhoe");
        if (!meta.getPersistentDataContainer().has(hoeTypeKey, PersistentDataType.STRING)) {
            return 1;
        }
        String hoeType = meta.getPersistentDataContainer().get(hoeTypeKey, PersistentDataType.STRING);
        if (hoeType == null) {
            return 1;
        }
    
        if (hoeType.startsWith("DIAMOND")) {
            return 5;
        } else if (hoeType.startsWith("GOLDEN")) {
            return 4;
        } else if (hoeType.startsWith("IRON")) {
            return 3;
        } else if (hoeType.startsWith("STONE")) {
            return 2;
        } else {
            return 1;
        }
    }

    private int getRangeFromLevel(int level) {
        switch (level) {
            case 2: return 1;
            case 3: return 2;
            case 4: return 3;
            case 5: return 4;
            default: return 0;
        }
    }

    private int applyBonemealEffect(Player player, Location center, int range) {
        int processedCount = 0;
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                Block block = center.clone().add(x, 0, z).getBlock();
                BlockData blockData = block.getBlockData();

                if (blockData instanceof Ageable) {
                    Ageable ageable = (Ageable) blockData;
                    if (ageable.getAge() == ageable.getMaximumAge()) {
                        Material cropType = block.getType();
                        Material seedType = getSeedFromCrop(cropType);

                        block.breakNaturally();
                        if (seedType != null) {
                             block.setType(cropType);
                             Ageable newAgeable = (Ageable) block.getBlockData();
                             newAgeable.setAge(0);
                             block.setBlockData(newAgeable);
                        }
                        processedCount++;
                    } else {
                        if (consumeItem(player, Material.BONE_MEAL)) {
                            if (block.applyBoneMeal(BlockFace.UP)) {
                                player.getWorld().playSound(block.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1.0f, 1.0f);
                                processedCount++;
                            } else {
                                if (player.getGameMode() != GameMode.CREATIVE) {
                                    player.getInventory().addItem(new ItemStack(Material.BONE_MEAL, 1));
                                }
                            }
                        }
                    }
                }
            }
        }
        return processedCount;
    }

    private Material getSeedFromCrop(Material crop) {
        switch (crop) {
            case WHEAT: return Material.WHEAT_SEEDS;
            case CARROTS: return Material.CARROT;
            case POTATOES: return Material.POTATO;
            case BEETROOTS: return Material.BEETROOTS;
            default: return null;
        }
    }
}