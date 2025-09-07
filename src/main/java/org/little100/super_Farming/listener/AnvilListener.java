package org.little100.super_Farming.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.little100.super_Farming.Super_Farming;

public class AnvilListener implements Listener {

    private final Super_Farming plugin;

    public AnvilListener(Super_Farming plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {

    }
}