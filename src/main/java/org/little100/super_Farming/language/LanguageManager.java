package org.little100.super_Farming.language;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.little100.super_Farming.Super_Farming;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageManager {

    private final Super_Farming plugin;
    private final Map<String, FileConfiguration> languageFiles = new HashMap<>();
    private String defaultLanguage = "zh_cn";

    public LanguageManager(Super_Farming plugin) {
        this.plugin = plugin;
        createDefaultLanguageFiles();
        loadLanguages();
    }

    public void reloadLanguages() {
        languageFiles.clear();
        loadLanguages();
    }
    private void loadLanguages() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        File[] langFiles = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (langFiles != null) {
            for (File file : langFiles) {
                String langCode = file.getName().replace(".yml", "");
                languageFiles.put(langCode, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    private void createDefaultLanguageFiles() {
        createLanguageFileIfNotExists("zh_cn");
        createLanguageFileIfNotExists("en_us");
        createLanguageFileIfNotExists("lzh");
    }

    private void createLanguageFileIfNotExists(String langCode) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        
        File langFile = new File(langFolder, langCode + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + langCode + ".yml", false);
        }
    }

    public String getString(String key) {
        return getString(key, defaultLanguage);
    }

    public String getString(String key, String lang) {
        FileConfiguration config = languageFiles.get(lang);
        if (config != null && config.contains(key)) {
            String message = config.getString(key);
            return message != null ? ChatColor.translateAlternateColorCodes('&', message) : key;
        }
        
        if (!lang.equals(defaultLanguage)) {
            config = languageFiles.get(defaultLanguage);
            if (config != null && config.contains(key)) {
                String message = config.getString(key);
                return message != null ? ChatColor.translateAlternateColorCodes('&', message) : key;
            }
        }
        
        return key;
    }

    public String getOrDefault(String key, String defaultValue) {
        String value = getString(key);
        return value.equals(key) ? defaultValue : value;
    }
    
    public List<String> getStringList(String key) {
        return getStringList(key, defaultLanguage);
    }

    public List<String> getStringList(String key, String lang) {
        FileConfiguration config = languageFiles.get(lang);
        List<String> result = new ArrayList<>();
        if (config != null && config.isList(key)) {
            for (String line : config.getStringList(key)) {
                result.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            return result;
        }

        if (!lang.equals(defaultLanguage)) {
            config = languageFiles.get(defaultLanguage);
            if (config != null && config.isList(key)) {
                for (String line : config.getStringList(key)) {
                    result.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                return result;
            }
        }
        
        return result;
    }

    public void setDefaultLanguage(String langCode) {
        if (languageFiles.containsKey(langCode)) {
            this.defaultLanguage = langCode;
        }
    }
}