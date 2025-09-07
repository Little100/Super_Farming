package org.little100.super_Farming.data;

public class FarmData {
    private final String location;
    private final String ownerUuid;
    private final String hoeData;
    private final String armorStandUuid;

    public FarmData(String location, String ownerUuid, String hoeData, String armorStandUuid) {
        this.location = location;
        this.ownerUuid = ownerUuid;
        this.hoeData = hoeData;
        this.armorStandUuid = armorStandUuid;
    }

    public String getLocation() {
        return location;
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public String getHoeData() {
        return hoeData;
    }

    public String getArmorStandUuid() {
        return armorStandUuid;
    }
}