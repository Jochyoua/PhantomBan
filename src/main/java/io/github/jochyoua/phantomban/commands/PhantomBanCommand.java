package io.github.jochyoua.phantomban.commands;

import io.github.jochyoua.phantomban.PhantomBan;
import io.github.jochyoua.phantomban.debug.DebugLogger;
import io.github.jochyoua.phantomban.listeners.DynamicEventHandler;
import io.github.jochyoua.phantomban.permissions.DynamicPermissionHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class PhantomBanCommand implements CommandExecutor, TabCompleter {

    private final List<String> defaultArguments = Arrays.asList("help", "add", "remove", "reload", "debug");
    private final PhantomBan phantomBan;

    public PhantomBanCommand(PhantomBan phantomBan) {
        this.phantomBan = phantomBan;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (args.length == 0 || !commandSender.hasPermission("phantomban.modify")) {
            displayHelp(commandSender);
            return false;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "add":
                    handleAddArgs(commandSender, args[1]);
                    break;
                case "remove":
                    handleRemoveArgs(commandSender, args[1]);
                    break;
                case "reload":
                    handleReload(commandSender);
                    break;
                case "debug":
                    handleDebug(commandSender);
                    break;
                case "help":
                default:
                    displayHelp(commandSender);
                    break;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
        return true;
    }

    private void handleDebug(CommandSender commandSender) {
        commandSender.sendMessage(phantomBan.formatMessage(String.format("PhantomBan v%s by Jochyoua", phantomBan.getDescription().getVersion())));
        commandSender.sendMessage(phantomBan.formatMessage(String.format("Running on %s %s", System.getProperty("os.name"), System.getProperty("os.version"))));
        commandSender.sendMessage(phantomBan.formatMessage(String.format("Java %s by %s", System.getProperty("java.version"), System.getProperty("java.vendor"))));
        commandSender.sendMessage(phantomBan.formatMessage(String.format("Server version %s by %s", Bukkit.getVersion(), Bukkit.getBukkitVersion())));
        commandSender.sendMessage(phantomBan.formatMessage(String.format("Loaded %d events", phantomBan.getDynamicEventHandler().dynamicEventList.size())));

        File debugDir = new File(phantomBan.getDataFolder(), "debug");
        if(!debugDir.exists() && !debugDir.mkdirs()){
            commandSender.sendMessage(phantomBan.formatMessage("Failed to save debug logs from memory"));
            return;
        }
        DebugLogger.saveToFile(
                new File(debugDir, "INFO.log").getPath(),
                new File(debugDir, "SEVERE.log").getPath(),
                new File(debugDir, "WARNING.log").getPath()
        );
        commandSender.sendMessage(phantomBan.formatMessage("Debug logs have been saved from memory to file"));
    }

    private void handleAddArgs(CommandSender commandSender, String target) {
        List<String> blacklist = phantomBan.getConfig().getStringList("data.blacklist");

        if (blacklist.contains(target)) {
            commandSender.sendMessage(phantomBan.formatMessage(phantomBan.getConfig().getString("messages.add-failure")));
            return;
        }
        blacklist.add(target);
        phantomBan.getConfig().set("data.blacklist", blacklist);
        phantomBan.saveConfig();
        commandSender.sendMessage(phantomBan.formatMessage(phantomBan.getConfig().getString("messages.add-success")));
        DebugLogger.logMessage(Level.INFO, String.format("Added %s to blacklist", target));
    }

    private void handleRemoveArgs(CommandSender commandSender, String target) {
        List<String> blacklist = phantomBan.getConfig().getStringList("data.blacklist");

        if (blacklist.remove(target)) {
            commandSender.sendMessage(phantomBan.formatMessage(phantomBan.getConfig().getString("messages.remove-success")));
            phantomBan.getConfig().set("data.blacklist", blacklist);
            phantomBan.saveConfig();
            DebugLogger.logMessage(Level.INFO, String.format("Removed %s from blacklist", target));
        } else {
            commandSender.sendMessage(phantomBan.formatMessage(phantomBan.getConfig().getString("messages.remove-failure")));
        }
    }

    private void handleReload(CommandSender commandSender) {
        long operationStartTime = System.nanoTime();
        phantomBan.reloadConfig();
        DynamicEventHandler dynamicEventHandler = phantomBan.getDynamicEventHandler();
        DynamicPermissionHandler dynamicPermissionHandler = phantomBan.getDynamicPermissionHandler();

        dynamicPermissionHandler.unregisterConfiguredPermissions();
        dynamicEventHandler.unRegisterConfiguredEvents();

        if (dynamicEventHandler.dynamicEventList.isEmpty()) {
            commandSender.sendMessage(phantomBan.formatMessage(phantomBan.getConfig().getString("messages.reload-success")));
            dynamicEventHandler.registerConfiguredEvents();
            dynamicPermissionHandler.registerConfiguredPermissions();

            phantomBan.getLogger().info(String.format("Finalized in %.1fms.", (System.nanoTime() - operationStartTime) / 1E6F));
        } else {
            commandSender.sendMessage(phantomBan.formatMessage(phantomBan.getConfig().getString("messages.reload-failure")));
            Bukkit.getPluginManager().disablePlugin(phantomBan);
            DebugLogger.logMessage(Level.SEVERE, "Failed to reload config. Event list did not reset properly and plugin has been disabled.");
        }
    }

    private void displayHelp(CommandSender commandSender) {
        List<String> helpMessages = phantomBan.getConfig().getStringList("messages.help");
        for (String message : helpMessages) {
            commandSender.sendMessage(phantomBan.formatMessage(message));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], defaultArguments, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            StringUtil.copyPartialMatches(args[1], phantomBan.getConfig().getStringList("data.blacklist"), completions);
        }
        Collections.sort(completions);
        return completions;
    }
}
