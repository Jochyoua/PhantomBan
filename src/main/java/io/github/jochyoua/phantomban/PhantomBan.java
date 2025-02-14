package io.github.jochyoua.phantomban;

import io.github.jochyoua.phantomban.commands.PhantomBanCommand;
import io.github.jochyoua.phantomban.listeners.DynamicEventHandler;
import io.github.jochyoua.phantomban.listeners.PlayerConnectionListener;
import io.github.jochyoua.phantomban.permissions.DynamicPermissionHandler;
import io.github.jochyoua.phantomban.shaded.Metrics;
import net.jodah.expiringmap.ExpiringMap;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class PhantomBan extends JavaPlugin {

    private final Set<UUID> phantomBannedPlayers = new HashSet<>();
    private DynamicEventHandler dynamicEventHandler;
    private DynamicPermissionHandler dynamicPermissionHandler;
    private ExpiringMap<UUID, Long> onlineTimeTracker;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        new Metrics(this, 24779);

        dynamicEventHandler = new DynamicEventHandler(this);
        dynamicPermissionHandler = new DynamicPermissionHandler(this);

        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(dynamicEventHandler, this);

        dynamicEventHandler.registerConfiguredEvents();

        PluginCommand pluginCommand = getCommand("phantomban");
        if (pluginCommand != null) {
            PhantomBanCommand phantomBanCommand = new PhantomBanCommand(this);
            pluginCommand.setExecutor(phantomBanCommand);
            pluginCommand.setTabCompleter(phantomBanCommand);
        }

        dynamicPermissionHandler.registerConfiguredPermissions();

        onlineTimeTracker = ExpiringMap.builder()
                .variableExpiration()
                .expiration(getConfig().getLong("settings.loyalty-rewards.seconds-until-unban", 1800), TimeUnit.SECONDS)
                .expirationListener((UUID uuid, Long joinTime) -> {
                    if (!getConfig().getBoolean("settings.loyalty-rewards.enabled")) {
                        return;
                    }
                    if (!isPhantomBanned(uuid)) {
                        return;
                    }
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) {
                        return;
                    }
                    getConfig().set("data.expiringMap." + player.getUniqueId(), null);
                    saveConfig();

                    unbanPlayer(player);
                })
                .build();
    }

    @Override
    public void onDisable() {
        this.phantomBannedPlayers.clear();

        if (dynamicPermissionHandler != null) {
            dynamicPermissionHandler.unregisterConfiguredPermissions();
        }

        if (dynamicEventHandler != null) {
            dynamicEventHandler.unRegisterConfiguredEvents();
        }
    }

    public DynamicEventHandler getDynamicEventHandler() {
        return dynamicEventHandler;
    }

    public DynamicPermissionHandler getDynamicPermissionHandler() {
        return dynamicPermissionHandler;
    }

    public ExpiringMap<UUID, Long> getOnlineTimeTracker() {
        return onlineTimeTracker;
    }

    public boolean isBanned(String playerName, AsyncPlayerPreLoginEvent.Result result) {
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);

        return banList.isBanned(playerName) || result.equals(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
    }

    public boolean isPhantomBanned(UUID uuid) {
        return phantomBannedPlayers.contains(uuid);
    }

    public Set<UUID> getPhantomBannedPlayers() {
        return this.phantomBannedPlayers;
    }

    public void addPhantomBannedPlayer(UUID uuid) {
        phantomBannedPlayers.add(uuid);
    }

    public void removePhantomBannedPlayer(UUID uuid) {
        phantomBannedPlayers.remove(uuid);
    }

    public String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void unbanPlayer(Player player) {
        this.getLogger().info("Player " + player.getName() + " has spent enough time online to be unbanned. Running commands.");

        removePhantomBannedPlayer(player.getUniqueId());

        Bukkit.getScheduler().runTask(this, () -> executeBatchCommands(player));
    }

    private void executeBatchCommands(Player player) {
        for (String commandToExecute : getConfig().getStringList("settings.loyalty-rewards.commands-to-run")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format(commandToExecute, player.getName()));
        }
    }
}
