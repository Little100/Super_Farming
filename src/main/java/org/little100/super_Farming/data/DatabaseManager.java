package org.little100.super_Farming.data;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    private final String url;

    public DatabaseManager(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, "farms.db");
        this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        load();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void load() {
        String sql = "CREATE TABLE IF NOT EXISTS active_farms ("
                + "location TEXT PRIMARY KEY,"
                + "owner_uuid TEXT NOT NULL,"
                + "hoe_data TEXT NOT NULL,"
                + "armor_stand_uuid TEXT NOT NULL"
                + ");";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addFarm(String location, String ownerUuid, String hoeData, String armorStandUuid) {
        String sql = "INSERT INTO active_farms(location, owner_uuid, hoe_data, armor_stand_uuid) VALUES(?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, location);
            pstmt.setString(2, ownerUuid);
            pstmt.setString(3, hoeData);
            pstmt.setString(4, armorStandUuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean farmExists(String location) {
        String sql = "SELECT 1 FROM active_farms WHERE location = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, location);
            try (var rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void removeFarm(String location) {
        String sql = "DELETE FROM active_farms WHERE location = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, location);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public FarmData getFarm(String location) {
        String sql = "SELECT owner_uuid, hoe_data, armor_stand_uuid FROM active_farms WHERE location = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, location);
            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String ownerUuid = rs.getString("owner_uuid");
                    String hoeData = rs.getString("hoe_data");
                    String armorStandUuid = rs.getString("armor_stand_uuid");
                    return new FarmData(location, ownerUuid, hoeData, armorStandUuid);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public java.util.List<FarmData> getAllFarms() {
        java.util.List<FarmData> farms = new java.util.ArrayList<>();
        String sql = "SELECT location, owner_uuid, hoe_data, armor_stand_uuid FROM active_farms";
        try (Connection conn = getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                farms.add(new FarmData(
                        rs.getString("location"),
                        rs.getString("owner_uuid"),
                        rs.getString("hoe_data"),
                        rs.getString("armor_stand_uuid")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return farms;
    }
    public void saveFarm(FarmData farm) {
        if (farm == null) {
            return;
        }
        String sql = "INSERT OR REPLACE INTO active_farms (location, owner_uuid, hoe_data, armor_stand_uuid) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, farm.getLocation());
            pstmt.setString(2, farm.getOwnerUuid());
            pstmt.setString(3, farm.getHoeData());
            pstmt.setString(4, farm.getArmorStandUuid());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteFarm(String location) {
        String sql = "DELETE FROM active_farms WHERE location = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, location);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String itemStackToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    public static ItemStack itemStackFromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to decode class type.", e);
        }
    }
}