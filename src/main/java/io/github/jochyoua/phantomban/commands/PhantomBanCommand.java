package io.github.jochyoua.phantomban.commands;

import io.github.jochyoua.phantomban.PhantomBan;
import io.github.jochyoua.phantomban.listeners.DynamicEventHandler;
import io.github.jochyoua.phantomban.permissions.DynamicPermissionHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PhantomBanCommand implements CommandExecutor, TabCompleter {
    private final List<String> defaultArguments = Arrays.asList("help", "add", "remove", "reload");
    final PhantomBan phantomBan;

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
            switch (args[0]) {
                case "add":
                    handleAddArgs(commandSender, args[1]);
                    break;
                case "remove":
                    handleRemoveArgs(commandSender, args[1]);
                    break;
                case "reload":
                    handleReload(commandSender);
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

    private void handleRemoveArgs(CommandSender commandSender, String target) {
        List<String> blacklist = phantomBan.getConfig().getStringList("data.blacklist");

        if (blacklist.remove(target)) {
            commandSender.sendMessage(phantomBan.formatMessage(
                    phantomBan.getConfig().getString("messages.remove-success")
            ));
            phantomBan.getConfig().set("data.blacklist", blacklist);
            phantomBan.saveConfig();
        } else {
            commandSender.sendMessage(phantomBan.formatMessage(
                    phantomBan.getConfig().getString("messages.remove-failure")
            ));
        }
    }

    private void handleAddArgs(CommandSender commandSender, String target) {
        List<String> blacklist = phantomBan.getConfig().getStringList("data.blacklist");

        if (blacklist.contains(target)) {
            commandSender.sendMessage(phantomBan.formatMessage(
                    phantomBan.getConfig().getString("messages.add-failure")
            ));
            return;
        }
        blacklist.add(target);
        phantomBan.getConfig().set("data.blacklist", blacklist);
        phantomBan.saveConfig();
        commandSender.sendMessage(phantomBan.formatMessage(
                phantomBan.getConfig().getString("messages.add-success")
        ));
    }

    private void displayHelp(CommandSender commandSender) {
        List<String> helpMessages = phantomBan.getConfig().getStringList("messages.help");
        for (String message : helpMessages) {
            commandSender.sendMessage(phantomBan.formatMessage(message));
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
            commandSender.sendMessage(phantomBan.formatMessage(
                    phantomBan.getConfig().getString("messages.reload-success")
            ));
            dynamicEventHandler.registerConfiguredEvents();
            dynamicPermissionHandler.registerConfiguredPermissions();

            phantomBan.getLogger().info(String.format("Finalized in %.1fms.", (System.nanoTime() - operationStartTime) / 1E6F));
            return;
        }

        commandSender.sendMessage(phantomBan.formatMessage(
                phantomBan.getConfig().getString("messages.reload-failure")
        ));
        Bukkit.getPluginManager().disablePlugin(phantomBan);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
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
