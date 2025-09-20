package org.little100.super_Farming;

import org.bukkit.plugin.java.JavaPlugin;
import org.little100.super_Farming.command.CommandManager;
import org.little100.super_Farming.data.DatabaseManager;
import org.little100.super_Farming.farm.FarmManager;
import org.little100.super_Farming.gui.GuiManager;
import org.little100.super_Farming.item.ItemManager;
import org.little100.super_Farming.language.LanguageManager;
import org.little100.super_Farming.listener.*;
import org.little100.super_Farming.recipe.RecipeManager;

import java.io.File;

public final class Super_Farming extends JavaPlugin {

    private static Super_Farming plugin;
    private GuiManager guiManager;
    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    private FarmManager farmManager;
    private static boolean isFolia;

    @Override
    public void onEnable() {
        plugin = this;

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            getLogger().info("Folia detected. Enabling Folia-specific features.");
        } catch (ClassNotFoundException e) {
            isFolia = false;
            getLogger().info("Folia not detected. Running in standard Bukkit/Paper mode.");
        }

        saveDefaultConfig();

        databaseManager = new DatabaseManager(getDataFolder());

        saveDefaultGuiConfig();

        languageManager = new LanguageManager(this);

        guiManager = new GuiManager(this, languageManager);

        ItemManager.init();

        RecipeManager.init();

        getServer().getPluginManager().registerEvents(new AnvilListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, guiManager), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this), this);
        farmManager = new FarmManager(this, databaseManager);
        farmManager.loadFarms();

        getServer().getPluginManager().registerEvents(new GuiListener(guiManager, languageManager, this), this);
        getServer().getPluginManager().registerEvents(new GuiSaveListener(guiManager), this);
        getServer().getPluginManager().registerEvents(new FarmListener(this, farmManager), this);

        CommandManager commandManager = new CommandManager(this);
        getCommand("super_farming").setExecutor(commandManager);
        getCommand("super_farming").setTabCompleter(commandManager);
    }

    private void saveDefaultGuiConfig() {
        File guiFile = new File(getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            saveResource("gui.yml", false);
        }
    }

    @Override
    public void onDisable() {
        if (farmManager != null) {
            farmManager.unloadFarms();
        }
    }

    public void reload() {
        reloadConfig();
        if (farmManager != null) {
            farmManager.unloadFarms();
        }
        farmManager = new FarmManager(this, databaseManager);
        farmManager.loadFarms();
        if (languageManager != null) {
            languageManager.reloadLanguages();
        }
    }

    public static boolean isFolia() {
        return isFolia;
    }

    public static Super_Farming getPlugin() {
        return plugin;
    }
    
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    public GuiManager getGuiManager() {
        return guiManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public FarmManager getFarmManager() {
        return farmManager;
    }
    
    public boolean isDebugEnabled() {
        return getConfig().getBoolean("debug.enabled", false);
    }
    
    public boolean isCraftingDebugEnabled() {
        return getConfig().getBoolean("debug.crafting", false);
    }
}
