package io.github.jochyoua.phantomban.listeners;

import io.github.jochyoua.phantomban.PhantomBan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlayerConnectionListener implements Listener {

    private final PhantomBan phantomBan;

    public PlayerConnectionListener(PhantomBan phantomBan) {
        this.phantomBan = phantomBan;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncPlayerLogin(AsyncPlayerPreLoginEvent asyncPlayerPreLoginEvent) {
        String playerName = asyncPlayerPreLoginEvent.getName();
        List<String> blacklist = phantomBan.getConfig().getStringList("data.blacklist");

        if (phantomBan.isPhantomBanned(asyncPlayerPreLoginEvent.getUniqueId())) {
            phantomBan.removePhantomBannedPlayer(asyncPlayerPreLoginEvent.getUniqueId());
        }

        if (blacklist.contains(playerName) && phantomBan.getConfig().getBoolean("settings.blacklist-enabled")) {
            return;
        }

        if (phantomBan.isBanned(playerName, asyncPlayerPreLoginEvent.getLoginResult())) {
            asyncPlayerPreLoginEvent.allow();
            asyncPlayerPreLoginEvent.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);
            phantomBan.addPhantomBannedPlayer(asyncPlayerPreLoginEvent.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerLogin(PlayerLoginEvent playerLoginEvent) {
        Player player = playerLoginEvent.getPlayer();
        List<String> blacklist = phantomBan.getConfig().getStringList("data.blacklist");

        if (blacklist.contains(player.getName()) && phantomBan.getConfig().getBoolean("settings.blacklist-enabled")) {
            return;
        }

        AsyncPlayerPreLoginEvent.Result result = playerLoginEvent.getResult() == PlayerLoginEvent.Result.KICK_BANNED ?
                AsyncPlayerPreLoginEvent.Result.KICK_BANNED : AsyncPlayerPreLoginEvent.Result.ALLOWED;

        if (phantomBan.isBanned(player.getName(), result)) {
            playerLoginEvent.allow();
            phantomBan.addPhantomBannedPlayer(player.getUniqueId());
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent playerJoinEvent) {
        Player player = playerJoinEvent.getPlayer();
        if (phantomBan.isPhantomBanned(player.getUniqueId())) {
            this.notifyPlayers(player);
            startTrackingOnlineTime(player.getUniqueId());
            if (phantomBan.getConfig().isSet("data.expiringMap." + player.getUniqueId())) {
                long expiration = phantomBan.getConfig().getLong("data.expiringMap." + player.getUniqueId());
                phantomBan.getOnlineTimeTracker().setExpiration(player.getUniqueId(), expiration, TimeUnit.SECONDS);
            }
        }

        if (phantomBan.getConfig().getBoolean("settings.effects.invisibility")) {
            this.hidePlayers();
        }
    }

    private void startTrackingOnlineTime(UUID uuid) {
        phantomBan.getOnlineTimeTracker().put(uuid, System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        Player player = playerQuitEvent.getPlayer();
        if (phantomBan.isPhantomBanned(player.getUniqueId())) {
            phantomBan.removePhantomBannedPlayer(player.getUniqueId());
            stopTrackingOnlineTime(player.getUniqueId());
        }
    }

    private void stopTrackingOnlineTime(UUID uuid) {
        phantomBan.getConfig().set("data.expiringMap." + uuid, TimeUnit.MILLISECONDS.toSeconds(phantomBan.getOnlineTimeTracker().getExpectedExpiration(uuid)));
        phantomBan.saveConfig();
        phantomBan.getOnlineTimeTracker().remove(uuid);
    }

    private void hidePlayers() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer == null) {
                continue;
            }

            for (UUID bannedPlayerUuid : phantomBan.getPhantomBannedPlayers()) {
                Player bannedPlayer = Bukkit.getPlayer(bannedPlayerUuid);
                if (bannedPlayer != null) {
                    onlinePlayer.hidePlayer(phantomBan, bannedPlayer);
                }
            }
        }
    }

    private void notifyPlayers(Player player) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer == null) {
                continue;
            }
            if (onlinePlayer.hasPermission("phantomban.notify")) {
                String message = String.format(phantomBan.getConfig().getString("messages.notification"), player.getName());
                onlinePlayer.sendMessage(phantomBan.formatMessage(message));
            }
        }
    }
}
