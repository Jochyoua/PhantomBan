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
    public void onAsyncPlayerLogin(AsyncPlayerPreLoginEvent event) {
        handleAsyncPlayerLogin(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerLogin(PlayerLoginEvent event) {
        handlePlayerLogin(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        handlePlayerJoin(event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerQuit(event);
    }

    private void handleAsyncPlayerLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        List<String> blacklist = phantomBan.getConfig().getStringList("data.blacklist");

        if (phantomBan.isPhantomBanned(event.getUniqueId())) {
            phantomBan.removePhantomBannedPlayer(event.getUniqueId());
        }

        if (blacklist.contains(playerName) && phantomBan.getConfig().getBoolean("settings.blacklist-enabled")) {
            return;
        }

        if (phantomBan.isBanned(playerName, event.getLoginResult())) {
            event.allow();
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);
            phantomBan.addPhantomBannedPlayer(event.getUniqueId());
        }
    }

    private void handlePlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        List<String> blacklist = phantomBan.getConfig().getStringList("data.blacklist");

        if (blacklist.contains(player.getName()) && phantomBan.getConfig().getBoolean("settings.blacklist-enabled")) {
            return;
        }

        AsyncPlayerPreLoginEvent.Result result = event.getResult() == PlayerLoginEvent.Result.KICK_BANNED ?
                AsyncPlayerPreLoginEvent.Result.KICK_BANNED : AsyncPlayerPreLoginEvent.Result.ALLOWED;

        if (phantomBan.isBanned(player.getName(), result)) {
            event.allow();
            phantomBan.addPhantomBannedPlayer(player.getUniqueId());
        }
    }

    private void handlePlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (phantomBan.isPhantomBanned(player.getUniqueId())) {
            notifyPlayers(player);
            startTrackingOnlineTime(player.getUniqueId());

            if (phantomBan.getConfig().isSet("data.expiringMap." + player.getUniqueId())) {
                long expiration = phantomBan.getConfig().getLong("data.expiringMap." + player.getUniqueId());
                phantomBan.getOnlineTimeTracker().setExpiration(player.getUniqueId(), expiration, TimeUnit.SECONDS);
            }
        }

        if (phantomBan.getConfig().getBoolean("settings.effects.invisibility")) {
            hidePlayers();
        }
    }

    private void handlePlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (phantomBan.isPhantomBanned(player.getUniqueId())) {
            phantomBan.removePhantomBannedPlayer(player.getUniqueId());
            stopTrackingOnlineTime(player.getUniqueId());
        }
    }

    private void startTrackingOnlineTime(UUID uuid) {
        phantomBan.getOnlineTimeTracker().put(uuid, System.currentTimeMillis());
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