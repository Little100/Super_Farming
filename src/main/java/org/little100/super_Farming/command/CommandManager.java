package org.little100.super_Farming.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.little100.super_Farming.Super_Farming;
import org.little100.super_Farming.gui.GuiManager;
import org.little100.super_Farming.language.LanguageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final Super_Farming plugin;
    private final LanguageManager languageManager;
    private GuiManager guiManager;

    public CommandManager(Super_Farming plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (guiManager == null) {
            guiManager = plugin.getGuiManager();
        }
        
        if (command.getName().equalsIgnoreCase("super_farming")) {
            if (args.length == 0) {
                return false;
            }

            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "lang":
                    return handleLanguageCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                case "reload":
                    return handleReloadCommand(sender);
                case "editor":
                    return handleEditorCommand(sender);
                default:
                    return false;
            }
        }
        
        return false;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("super_farming.reload")) {
            sender.sendMessage(languageManager.getString("command.no_permission"));
            return true;
        }
        plugin.reload();
        sender.sendMessage(languageManager.getString("command.reload.success"));
        return true;
    }
    
    private boolean handleEditorCommand(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            guiManager.openEditor(player);
            sender.sendMessage(languageManager.getString("command.editor.open"));
        } else {
            sender.sendMessage(languageManager.getString("command.editor.player_only"));
        }
        return true;
    }

    private boolean handleLanguageCommand(CommandSender sender, String[] args) {
        // 获取可用语言列表
        List<String> availableLanguages = getAvailableLanguages();
        String langList = String.join(", ", availableLanguages);
        
        // 显示可用语言列表
        if (args.length == 0) {
            String message = languageManager.getString("command.language.list")
                    .replace("%langs%", langList);
            sender.sendMessage(message);
            return true;
        }
        
        // 切换语言
        String langCode = args[0].toLowerCase();
        if (availableLanguages.contains(langCode)) {
            languageManager.setDefaultLanguage(langCode);
            String message = languageManager.getString("command.language.change")
                    .replace("%lang%", langCode);
            sender.sendMessage(message);
        } else {
            String message = languageManager.getString("command.language.not_found")
                    .replace("%langs%", langList);
            sender.sendMessage(message);
        }
        
        return true;
    }

    private List<String> getAvailableLanguages() {
        List<String> languages = new ArrayList<>();
        File langDir = new File(plugin.getDataFolder(), "lang");
        
        if (langDir.exists() && langDir.isDirectory()) {
            File[] langFiles = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (langFiles != null) {
                languages = Arrays.stream(langFiles)
                        .map(file -> file.getName().replace(".yml", ""))
                        .collect(Collectors.toList());
            }
        }
        
        return languages;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("lang", "reload", "editor"), completions);
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("lang")) {
            StringUtil.copyPartialMatches(args[1], getAvailableLanguages(), completions);
            return completions;
        }

        return completions;
    }
}