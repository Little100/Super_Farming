package org.little100.super_Farming.farm;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.little100.super_Farming.Super_Farming;
import org.little100.super_Farming.data.DatabaseManager;
import org.little100.super_Farming.data.FarmData;
import org.little100.super_Farming.item.ItemManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FarmManager {

    private final Super_Farming plugin;
    private final DatabaseManager dbManager;
    private final Map<String, FarmData> activeFarms = new ConcurrentHashMap<>();
    private Object farmTask;
    private final boolean isFoliaServer;
    
    // Y轴搜索范围限制
    private final int yAxisUpperLimit;
    private final int yAxisLowerLimit;
    private final int maxBoneMealUse;
    private final long taskInterval;

    public FarmManager(Super_Farming plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.isFoliaServer = isFoliaServer();
        
        // 从配置文件读取Y轴限制
        this.yAxisUpperLimit = plugin.getConfig().getInt("farm.tilling.y-axis.upper-limit", -1);
        this.yAxisLowerLimit = plugin.getConfig().getInt("farm.tilling.y-axis.lower-limit", -1);
        
        // 其他设置
        this.maxBoneMealUse = plugin.getConfig().getInt("farm.fertilizing.max-bone-meal", 10);
        this.taskInterval = plugin.getConfig().getLong("farm.scheduler.interval", 200L);
        
        plugin.getLogger().info("Y轴搜索范围: 上限=" + (yAxisUpperLimit == -1 ? "无限" : yAxisUpperLimit) +
                               ", 下限=" + (yAxisLowerLimit == -1 ? "无限" : yAxisLowerLimit));
    }

    private boolean isFoliaServer() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void loadFarms() {
        List<FarmData> farms = dbManager.getAllFarms();
        plugin.getLogger().info("Loading farms from database, found: " + farms.size());
        for (FarmData farm : farms) {
            activeFarms.put(farm.getLocation(), farm);
        }
        plugin.getLogger().info("Loaded " + activeFarms.size() + " farms into memory.");
        startFarmTask();
    }

    public void unloadFarms() {
        cancelFarmTask();
        activeFarms.clear();
        plugin.getLogger().info("Unloaded all farms.");
    }

    private void cancelFarmTask() {
        if (farmTask == null) {
            return;
        }
        try {
            if (isFoliaServer) {
                farmTask.getClass().getMethod("cancel").invoke(farmTask);
            } else if (farmTask instanceof BukkitTask) {
                ((BukkitTask) farmTask).cancel();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cancelling farm task: " + e.getMessage());
        }
        farmTask = null;
    }

    private void startFarmTask() {
        cancelFarmTask();

        if (isFoliaServer) {
            try {
                Object globalRegionScheduler = plugin.getServer().getClass().getMethod("getGlobalRegionScheduler").invoke(plugin.getServer());
                farmTask = globalRegionScheduler.getClass()
                    .getMethod("runAtFixedRate", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, long.class, long.class)
                    .invoke(globalRegionScheduler, plugin, (java.util.function.Consumer<Object>) task -> processAllFarms(), 20L, taskInterval);
                plugin.getLogger().info("Started farm task in Folia mode (" + taskInterval + " tick interval).");
            } catch (Exception e) {
                plugin.getLogger().severe("CRITICAL: Failed to start Folia farm task. Auto-farming will be disabled. Error: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            startBukkitFarmTask();
        }
    }

    private void startBukkitFarmTask() {
        farmTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processAllFarms, 100L, taskInterval);
        plugin.getLogger().info("Started farm task in Bukkit mode (" + taskInterval + " tick interval).");
    }

    private void processAllFarms() {
        if (activeFarms.isEmpty()) {
            return;
        }
        
        for (FarmData farm : activeFarms.values()) {
            try {
                Location center = stringToLocation(farm.getLocation());
                if (center == null || center.getWorld() == null) {
                    plugin.getLogger().warning("Invalid farm location or world not loaded: " + farm.getLocation());
                    continue;
                }

                if (!center.getWorld().isChunkLoaded(center.getBlockX() >> 4, center.getBlockZ() >> 4)) {
                    continue;
                }
                
                ItemStack hoe = DatabaseManager.itemStackFromBase64(farm.getHoeData());
                if (hoe == null) {
                    plugin.getLogger().warning("Invalid hoe data for farm at: " + farm.getLocation());
                    continue;
                }
                
                int range = getHoeRange(hoe);

                if (isFoliaServer) {
                    try {
                        Object regionScheduler = plugin.getServer().getClass().getMethod("getRegionScheduler").invoke(plugin.getServer());

                        java.lang.reflect.Method executeMethod = regionScheduler.getClass()
                            .getMethod("execute", org.bukkit.plugin.Plugin.class, org.bukkit.Location.class, java.lang.Runnable.class);

                        Runnable task = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (!activeFarms.containsKey(farm.getLocation())) {
                                        return;
                                    }
                                    FarmManager.this.tillSoil(center, range);
                                    wetFarmland(center, range);
                                    plantSeeds(center, range);
                                    fertilizeCrops(center, range);
                                    harvestAndCollect(center, range);
                                } catch (Exception e) {
                                    plugin.getLogger().severe("Error in region thread farm operations: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        };

                        executeMethod.invoke(regionScheduler, plugin, center, task);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to schedule farm on region thread: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    this.tillSoil(center, range);
                    wetFarmland(center, range);
                    plantSeeds(center, range);
                    fertilizeCrops(center, range);
                    harvestAndCollect(center, range);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error preparing farm at " + farm.getLocation() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private Location stringToLocation(String locString) {
        if (locString == null || locString.isEmpty()) return null;
        try {
            String[] parts = locString.split(",");
            if (parts.length < 4) return null;
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Location(world, x, y, z);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse location string: " + locString);
            return null;
        }
    }

    private int getHoeRange(ItemStack hoe) {
        if (hoe == null || !hoe.hasItemMeta()) return 1;
        ItemMeta meta = hoe.getItemMeta();
        if (meta == null) return 1;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Integer range = container.get(ItemManager.HOERANGE_KEY, PersistentDataType.INTEGER);
        return range != null ? range : 1;
    }

    private void wetFarmland(Location center, int range) {
        World world = center.getWorld();
        if (world == null) {
            plugin.getLogger().warning("wetFarmland: world is null");
            return;
        }
        
        int farmlandCount = 0;
        int moistenedCount = 0;
        
        try {
            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    for (int y = -1; y <= 1; y++) {
                        try {
                            int blockX = center.getBlockX() + x;
                            int blockY = center.getBlockY() + y;
                            int blockZ = center.getBlockZ() + z;

                            Block block = null;
                            try {
                                block = world.getBlockAt(blockX, blockY, blockZ);
                            } catch (Exception e) {
                                plugin.getLogger().warning("wetFarmland: Error getting block at [" + blockX + "," + blockY + "," + blockZ + "]: " + e.getMessage());
                                continue;
                            }
                            
                            if (block == null) continue;

                            Material type = null;
                            try {
                                type = block.getType();
                            } catch (Exception e) {
                                plugin.getLogger().warning("wetFarmland: Error getting block type: " + e.getMessage());
                                continue;
                            }
                            
                            if (type != Material.FARMLAND) {
                                continue;
                            }
                            
                            farmlandCount++;

                            try {
                                if (block.getBlockData() instanceof Farmland) {
                                    Farmland farmland = (Farmland) block.getBlockData();
                                    
                                    if (farmland.getMoisture() < farmland.getMaximumMoisture()) {
                                        farmland.setMoisture(farmland.getMaximumMoisture());
                                        block.setBlockData(farmland);
                                        moistenedCount++;
                                    }
                                }
                            } catch (Exception e) {
                                plugin.getLogger().warning("wetFarmland: Error moistening farmland: " + e.getMessage());
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("wetFarmland: Error processing block: " + e.getMessage());
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error in wetFarmland: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void plantSeeds(Location center, int range) {
        World world = center.getWorld();
        if (world == null) return;
        
        try {
            List<Block> farmlandList = new ArrayList<>();
            List<Inventory> containerInventories = new ArrayList<>();

            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    int minY = yAxisLowerLimit == -1 ? -256 : -yAxisLowerLimit;
                    int maxY = yAxisUpperLimit == -1 ? 256 : yAxisUpperLimit;
                    for (int y = minY; y <= maxY; y++) {
                        try {
                            int blockX = center.getBlockX() + x;
                            int blockY = center.getBlockY() + y;
                            int blockZ = center.getBlockZ() + z;

                            Block block = null;
                            try {
                                block = world.getBlockAt(blockX, blockY, blockZ);
                            } catch (Exception e) {
                                continue;
                            }
                            
                            if (block == null) continue;
                            
                            try {
                                if (block.getState() instanceof Container) {
                                    containerInventories.add(((Container) block.getState()).getInventory());
                                } else {
                                    Material type = block.getType();
                                    if (type == Material.FARMLAND) {
                                        Block aboveBlock = null;
                                        try {
                                            aboveBlock = block.getRelative(BlockFace.UP);
                                            if (aboveBlock != null && aboveBlock.getType() == Material.AIR) {
                                                farmlandList.add(block);
                                            }
                                        } catch (Exception e) {
                                            // 忽略
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // 忽略
                            }
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                }
            }
            
            if (farmlandList.isEmpty() || containerInventories.isEmpty()) return;
            
            Map<Material, Material> seedToCrop = getSeedCropMap();
            for (Block farmland : farmlandList) {
                try {
                    Block aboveBlock = farmland.getRelative(BlockFace.UP);
                    if (aboveBlock == null || aboveBlock.getType() != Material.AIR) continue;
                    
                    boolean planted = false;
                    for (Inventory inv : containerInventories) {
                        if (inv == null) continue;
                        
                        for (Material seed : seedToCrop.keySet()) {
                            try {
                                if (inv.contains(seed)) {
                                    inv.removeItem(new ItemStack(seed, 1));
                                    aboveBlock.setType(seedToCrop.get(seed));
                                    planted = true;
                                    break;
                                }
                            } catch (Exception e) {
                                // 忽略
                            }
                        }
                        if (planted) break;
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error in plantSeeds: " + e.getMessage());
        }
    }

    private Map<Material, Material> getSeedCropMap() {
        Map<Material, Material> map = new EnumMap<>(Material.class);
        map.put(Material.WHEAT_SEEDS, Material.WHEAT);
        map.put(Material.POTATO, Material.POTATOES);
        map.put(Material.CARROT, Material.CARROTS);
        map.put(Material.BEETROOT_SEEDS, Material.BEETROOTS);
        map.put(Material.PUMPKIN_SEEDS, Material.PUMPKIN_STEM);
        map.put(Material.MELON_SEEDS, Material.MELON_STEM);
        return map;
    }

    private void fertilizeCrops(Location center, int range) {
        World world = center.getWorld();
        if (world == null) return;
        
        try {
            List<Block> immatureCrops = new ArrayList<>();
            List<Inventory> inventories = new ArrayList<>();

            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    // 使用配置中的Y轴上下限
                    int minY = yAxisLowerLimit == -1 ? -256 : -yAxisLowerLimit;
                    int maxY = yAxisUpperLimit == -1 ? 256 : yAxisUpperLimit;
                    for (int y = minY; y <= maxY; y++) {
                        try {
                            Block block = null;
                            try {
                                block = world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                            } catch (Exception e) {
                                continue;
                            }
                            
                            if (block == null) continue;
                            
                            try {
                                if (block.getBlockData() instanceof Ageable) {
                                    Ageable ageable = (Ageable) block.getBlockData();
                                    if (ageable.getAge() < ageable.getMaximumAge()) {
                                        immatureCrops.add(block);
                                    }
                                }
                                
                                if (block.getState() instanceof Container) {
                                    inventories.add(((Container) block.getState()).getInventory());
                                }
                            } catch (Exception e) {
                                // 忽略
                            }
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                }
            }
            
            if (immatureCrops.isEmpty() || inventories.isEmpty()) return;

            int maxBoneMealToUse = Math.min(this.maxBoneMealUse, immatureCrops.size()); // 使用配置中的值
            int boneMealCount = 0;
            List<ItemStack> boneMealItems = new ArrayList<>();

            for (Inventory inv : inventories) {
                try {
                    if (inv == null) continue;

                    for (ItemStack item : inv.getContents()) {
                        if (item != null && item.getType() == Material.BONE_MEAL) {
                            int amount = Math.min(item.getAmount(), maxBoneMealToUse - boneMealCount);
                            boneMealItems.add(new ItemStack(Material.BONE_MEAL, amount));
                            boneMealCount += amount;
                            
                            if (boneMealCount >= maxBoneMealToUse) break;
                        }
                    }
                    
                    if (boneMealCount >= maxBoneMealToUse) break;
                } catch (Exception e) {
                    // 忽略
                }
            }

            if (boneMealCount > 0 && !immatureCrops.isEmpty()) {
                Collections.shuffle(immatureCrops);
                int fertilizedCount = 0;

                for (int i = 0; i < Math.min(boneMealCount, immatureCrops.size()); i++) {
                    try {
                        Block crop = immatureCrops.get(i);
                        if (crop != null && crop.getBlockData() instanceof Ageable) {
                            if (crop.applyBoneMeal(BlockFace.UP)) {
                                fertilizedCount++;
                                try {
                                    world.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, crop.getLocation().add(0.5, 0.5, 0.5), 5, 0.5, 0.5, 0.5, 0);
                                } catch (Exception e) {
                                    // 忽略
                                }
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error fertilizing crop: " + e.getMessage());
                    }
                }

                if (fertilizedCount > 0) {
                    int remainingToRemove = fertilizedCount;
                    for (Inventory inv : inventories) {
                        try {
                            if (inv == null || remainingToRemove <= 0) continue;
                            
                            if (inv.contains(Material.BONE_MEAL)) {
                                HashMap<Integer, ItemStack> notRemoved = inv.removeItem(new ItemStack(Material.BONE_MEAL, remainingToRemove));
                                remainingToRemove = notRemoved.isEmpty() ? 0 : notRemoved.values().iterator().next().getAmount();
                                
                                if (remainingToRemove <= 0) break;
                            }
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error in fertilizeCrops: " + e.getMessage());
        }
    }

    private void harvestAndCollect(Location center, int range) {
        World world = center.getWorld();
        if (world == null) return;
        
        try {
            List<Inventory> inventories = new ArrayList<>();
            List<ItemStack> drops = new ArrayList<>();

            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    int minY = yAxisLowerLimit == -1 ? -256 : -yAxisLowerLimit;
                    int maxY = yAxisUpperLimit == -1 ? 256 : yAxisUpperLimit;
                    for (int y = minY; y <= maxY; y++) {
                        try {
                            Block block = null;
                            try {
                                block = world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                            } catch (Exception e) {
                                continue;
                            }
                            
                            if (block == null) continue;
                            
                            try {
                                if (block.getState() instanceof Container) {
                                    inventories.add(((Container) block.getState()).getInventory());
                                } else {
                                    Material type = null;
                                    try {
                                        type = block.getType();
                                    } catch (Exception e) {
                                        continue;
                                    }
                                    
                                    if (type == Material.PUMPKIN || type == Material.MELON) {
                                        try {
                                            drops.addAll(block.getDrops());
                                            block.setType(Material.AIR);
                                        } catch (Exception e) {
                                            // 忽略
                                        }
                                    } else if (block.getBlockData() instanceof Ageable) {
                                        try {
                                            Ageable ageable = (Ageable) block.getBlockData();
                                            if (ageable.getAge() == ageable.getMaximumAge()) {
                                                drops.addAll(block.getDrops());
                                                block.setType(Material.AIR);
                                            }
                                        } catch (Exception e) {
                                            // 忽略
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // 忽略
                            }
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                }
            }

            try {
                Collection<Item> items = world.getEntitiesByClass(Item.class);
                for (Item item : items) {
                    try {
                        if (item != null && item.getLocation().distance(center) <= range) {
                            drops.add(item.getItemStack());
                            item.remove();
                        }
                    } catch (Exception e) {
                        // 忽略
                    }
                }
            } catch (Exception e) {
                // 忽略
            }
            
            // 存储掉落物到容器中
            for (ItemStack drop : drops) {
                if (drop == null) continue;
                
                try {
                    HashMap<Integer, ItemStack> leftover = new HashMap<>();
                    
                    for (Inventory inv : inventories) {
                        if (inv == null) continue;
                        
                        try {
                            leftover = inv.addItem(drop);
                            if (leftover.isEmpty()) {
                                break;
                            }
                            drop = leftover.get(0);
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                    
                    if (!leftover.isEmpty() && leftover.get(0) != null) {
                        try {
                            world.dropItemNaturally(center, leftover.get(0));
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error in harvestAndCollect: " + e.getMessage());
        }
    }

    private void tillSoil(Location center, int range) {
        World world = center.getWorld();
        if (world == null) {
            plugin.getLogger().warning("tillSoil: world is null");
            return;
        }
        
        int tilledCount = 0;
        
        try {
            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    int minY = yAxisLowerLimit == -1 ? -256 : -yAxisLowerLimit;
                    int maxY = yAxisUpperLimit == -1 ? 256 : yAxisUpperLimit;

                    for (int y = minY; y <= maxY; y++) {
                        try {
                            int blockX = center.getBlockX() + x;
                            int blockY = center.getBlockY() + y;
                            int blockZ = center.getBlockZ() + z;

                            Block block = null;
                            try {
                                block = world.getBlockAt(blockX, blockY, blockZ);
                            } catch (Exception e) {
                                continue;
                            }
                            
                            if (block == null) continue;

                            Material type = null;
                            try {
                                type = block.getType();
                            } catch (Exception e) {
                                continue;
                            }

                            Block aboveBlock = null;
                            try {
                                aboveBlock = block.getRelative(BlockFace.UP);
                            } catch (Exception e) {
                                continue;
                            }
                            
                            if (aboveBlock == null) continue;
                            
                            Material aboveType = null;
                            try {
                                aboveType = aboveBlock.getType();
                            } catch (Exception e) {
                                continue;
                            }

                            if (aboveType == Material.AIR &&
                               (type == Material.GRASS_BLOCK || type == Material.DIRT || type == Material.COARSE_DIRT)) {
                                
                                try {
                                    block.setType(Material.FARMLAND);
                                    tilledCount++;
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Error tilling soil: " + e.getMessage());
                                }
                            }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error processing block for tilling: " + e.getMessage());
                    }
                }
            }
        }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error in tillSoil: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addFarm(FarmData farm) {
        if (farm != null) {
            activeFarms.put(farm.getLocation(), farm);
            dbManager.saveFarm(farm);
        }
    }

    public void removeFarm(String location) {
        if (location != null) {
            activeFarms.remove(location);
            dbManager.deleteFarm(location);
        }
    }
}