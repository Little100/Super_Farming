package org.little100.super_Farming.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.little100.super_Farming.Super_Farming;
import org.little100.super_Farming.data.DatabaseManager;
import org.little100.super_Farming.data.FarmData;
import org.little100.super_Farming.farm.FarmManager;
import org.little100.super_Farming.item.ItemManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FarmListener implements Listener {

    private final Super_Farming plugin;
    private final FarmManager farmManager;
    private final Map<java.util.UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TIME_MS = 1000;

    public FarmListener(Super_Farming plugin, FarmManager farmManager) {
        this.plugin = plugin;
        this.farmManager = farmManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (!player.isSneaking() || event.getAction() != Action.RIGHT_CLICK_BLOCK || clickedBlock == null) {
            return;
        }

        if (clickedBlock.getType() == Material.COMPOSTER) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            DatabaseManager dbManager = plugin.getDatabaseManager();
            String locationString = locationToString(clickedBlock.getLocation());

            if (dbManager.farmExists(locationString)) {
                event.setCancelled(true);
                handleFarmRetrieval(player, dbManager, locationString);
                return;
            }

            if (isTier2SuperHoe(itemInHand)) {
                 event.setCancelled(true);
                 handleFarmCreation(player, itemInHand, dbManager, locationString, clickedBlock.getLocation());
            }
        }
    }

    private void handleFarmCreation(Player player, ItemStack itemInHand, DatabaseManager dbManager, String locationString, Location blockLocation) {
        Location armorStandLocation = blockLocation.clone().add(0.5, 1, 0.5);
        ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(armorStandLocation, EntityType.ARMOR_STAND);

        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setSilent(true);
        armorStand.addScoreboardTag("super_farm_armor_stand");
        armorStand.getEquipment().setItemInMainHand(itemInHand.clone());

        String hoeData = DatabaseManager.itemStackToBase64(itemInHand);
        dbManager.addFarm(locationString, player.getUniqueId().toString(), hoeData, armorStand.getUniqueId().toString());
        farmManager.addFarm(new FarmData(locationString, player.getUniqueId().toString(), hoeData, armorStand.getUniqueId().toString()));

        itemInHand.setAmount(itemInHand.getAmount() - 1);

        player.sendMessage(plugin.getLanguageManager().getString("farm.activated"));
    }

    private void handleFarmRetrieval(Player player, DatabaseManager dbManager, String locationString) {
        long now = System.currentTimeMillis();
        long lastAction = playerCooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (now - lastAction < COOLDOWN_TIME_MS) {
            return;
        }
        playerCooldowns.put(player.getUniqueId(), now);

        FarmData farmData = dbManager.getFarm(locationString);
        if (farmData == null) return;

        if (!farmData.getOwnerUuid().equals(player.getUniqueId().toString()) && !player.hasPermission("super_farming.admin.bypass")) {
            player.sendMessage(plugin.getLanguageManager().getString("farm.not_yours"));
            return;
        }

        farmManager.removeFarm(locationString);
        dbManager.removeFarm(locationString);

        Runnable removeTask = () -> {
            Entity armorStandEntity = Bukkit.getServer().getEntity(java.util.UUID.fromString(farmData.getArmorStandUuid()));
            if (armorStandEntity != null) {
                if (armorStandEntity instanceof ArmorStand) {
                    ArmorStand armorStand = (ArmorStand) armorStandEntity;
                    if (armorStand.getEquipment() != null) {
                        armorStand.getEquipment().setItemInMainHand(null);
                    }
                }
                armorStandEntity.remove();
            }
        };

        if (Super_Farming.isFolia()) {
            try {
                java.lang.reflect.Method getSchedulerMethod = player.getClass().getMethod("getScheduler");
                Object playerScheduler = getSchedulerMethod.invoke(player);
                java.lang.reflect.Method runDelayedMethod = playerScheduler.getClass().getMethod("runDelayed", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, Runnable.class, long.class);
                java.util.function.Consumer<Object> task = (scheduledTask) -> removeTask.run();
                runDelayedMethod.invoke(playerScheduler, plugin, task, null, 1L);
            } catch (Exception e) {
                plugin.getLogger().warning("Folia reflection for Player.getScheduler() failed! This may cause a crash. Error: " + e.getMessage());
                Bukkit.getScheduler().runTaskLater(plugin, removeTask, 1L);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, removeTask, 1L);
        }

        ItemStack hoe = DatabaseManager.itemStackFromBase64(farmData.getHoeData());
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), hoe);
            player.sendMessage(plugin.getLanguageManager().getString("farm.inventory_full"));
        } else {
            player.getInventory().addItem(hoe);
        }

        player.sendMessage(plugin.getLanguageManager().getString("farm.deactivated"));
    }

    private String locationToString(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand armorStand = event.getRightClicked();
        if (!armorStand.getScoreboardTags().contains("super_farm_armor_stand")) {
            return;
        }

        Player player = event.getPlayer();
        String locationString = locationToString(armorStand.getLocation().subtract(0.5, 1, 0.5));
        FarmData farmData = plugin.getDatabaseManager().getFarm(locationString);

        if (farmData == null) {
            if (player.isOp()) {
                armorStand.remove();
            }
            event.setCancelled(true);
            return;
        }

        if (!farmData.getOwnerUuid().equals(player.getUniqueId().toString()) && !player.hasPermission("super_farming.admin.bypass")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLanguageManager().getString("farm.not_yours"));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.COMPOSTER) {
            return;
        }

        String locationString = locationToString(block.getLocation());
        DatabaseManager dbManager = plugin.getDatabaseManager();

        if (dbManager.farmExists(locationString)) {
            Player player = event.getPlayer();
            FarmData farmData = dbManager.getFarm(locationString);
            if (farmData == null) return; 

            if (farmData.getOwnerUuid().equals(player.getUniqueId().toString())) {
                handleFarmRetrieval(player, dbManager, locationString);
            } else {
                event.setCancelled(true);
                player.sendMessage(plugin.getLanguageManager().getString("farm.cannot_break"));
            }
        }
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand) {
            ArmorStand armorStand = (ArmorStand) event.getRightClicked();
            if (armorStand.getScoreboardTags().contains("super_farm_armor_stand")) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isTier2SuperHoe(ItemStack item) {
        if (item == null || !item.getType().toString().endsWith("_HOE")) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (!data.has(ItemManager.SUPERHOE_KEY, PersistentDataType.STRING)) {
            return false;
        }
        
        String key = data.get(ItemManager.SUPERHOE_KEY, PersistentDataType.STRING);
        return key != null && key.endsWith("_TIER2");
    }
}