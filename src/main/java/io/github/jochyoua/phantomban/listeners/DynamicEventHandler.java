package io.github.jochyoua.phantomban.listeners;

import io.github.jochyoua.phantomban.PhantomBan;
import io.github.jochyoua.phantomban.debug.DebugLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DynamicEventHandler implements Listener {

    private final PhantomBan phantomBan;
    public final Set<Class<? extends Event>> dynamicEventList = new HashSet<>();

    public DynamicEventHandler(PhantomBan phantomBan) {
        this.phantomBan = phantomBan;
    }

    public void registerConfiguredEvents() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        Set<String> eventConfigKeys = phantomBan.getConfig().getConfigurationSection("settings.effects.events").getKeys(false);

        for (String eventClassPath : eventConfigKeys) {
            registerEvent(pluginManager, eventClassPath);
        }
    }

    private void registerEvent(PluginManager pluginManager, String eventClassPath) {
        ConfigurationSection config = phantomBan.getConfig().getConfigurationSection("settings.effects.events." + eventClassPath);
        if (config == null || !config.getBoolean("enabled", false)) {
            DebugLogger.logMessage(Level.INFO, "Config is null or event is disabled for " + eventClassPath);
            return;
        }

        try {
            String cleanPathName = eventClassPath.replace('#', '.');
            Class<?> eventClass = Class.forName(cleanPathName);

            EventExecutor eventExecutor = ((listener, event) -> handleDynamicEvent(event, eventClassPath));
            pluginManager.registerEvent((Class<? extends Event>) eventClass, this, EventPriority.HIGHEST, eventExecutor, phantomBan);
            dynamicEventList.add((Class<? extends Event>) eventClass);

            phantomBan.getLogger().info("Successfully registered event: " + cleanPathName);
        } catch (ClassNotFoundException e) {
            DebugLogger.logMessage(Level.WARNING, "Could not find event class for: " + eventClassPath);
        }
    }

    private void handleDynamicEvent(Event event, String configKey) {
        ConfigurationSection config = phantomBan.getConfig().getConfigurationSection("settings.effects.events." + configKey);
        if (config == null) {
            DebugLogger.logMessage(Level.WARNING, "ConfigSection is null for this event:" + configKey);
            return;
        }

        Player player = getPlayerFromEvent(event);
        if (player == null) {
            DebugLogger.logMessage(Level.INFO, "Player is null for this event entry: " + configKey);
            return;
        }

        if (!phantomBan.isPhantomBanned(player.getUniqueId())) {
            DebugLogger.logMessage(Level.INFO, "Player is not phantom banned for this event entry: " + configKey);
            return;
        }

        if (player.hasPermission("phantomban.bypass." + event.getClass().getSimpleName())) {
            DebugLogger.logMessage(Level.INFO, "Player has bypass permission for this event entry: " + configKey);
            return;
        }


        if (event instanceof Cancellable) {
            DebugLogger.logMessage(Level.INFO, "Event was cancelled for this event entry: " + configKey);
            ((Cancellable) event).setCancelled(true);
        }

        String message = config.getString("message");
        if (message != null && !message.isEmpty()) {
            sendMessage(player, message);
        }
    }

    private void sendMessage(Player player, String message) {
        String timeRemainingString = phantomBan.getConfig().getString("messages.time-until-unban");
        if (phantomBan.getOnlineTimeTracker().containsKey(player.getUniqueId())
                && timeRemainingString != null && !timeRemainingString.isEmpty()
                && phantomBan.getConfig().getBoolean("settings.loyalty-rewards.enabled")) {
            timeRemainingString = String.format(timeRemainingString, TimeUnit.MILLISECONDS.toSeconds(phantomBan.getOnlineTimeTracker().getExpectedExpiration(player.getUniqueId())));
        } else {
            timeRemainingString = "";
        }

        player.sendMessage(phantomBan.formatMessage(String.format(message, timeRemainingString)));
    }

    private Player getPlayerFromEvent(Event event) {
        if (event instanceof PlayerEvent) {
            return ((PlayerEvent) event).getPlayer();
        } else if (event instanceof EntityDamageByEntityEvent) {
            return getPlayerFromEntityDamageByEntityEvent((EntityDamageByEntityEvent) event);
        } else if (event instanceof EntityExplodeEvent) {
            return getPlayerFromEntityExplodeEvent((EntityExplodeEvent) event);
        } else if (event instanceof EntityDamageEvent) {
            return getPlayerFromEntityDamageEvent((EntityDamageEvent) event);
        } else if (event instanceof EntityCombustByEntityEvent) {
            return getPlayerFromEntityCombustByEntityEvent((EntityCombustByEntityEvent) event);
        } else if (event instanceof BlockBreakEvent) {
            return ((BlockBreakEvent) event).getPlayer();
        } else if (event instanceof BlockPlaceEvent) {
            return ((BlockPlaceEvent) event).getPlayer();
        } else if (event instanceof InventoryClickEvent) {
            return (Player) ((InventoryClickEvent) event).getWhoClicked();
        } else if (event instanceof InventoryOpenEvent) {
            return (Player) ((InventoryOpenEvent) event).getPlayer();
        }
        return null;
    }

    private Player getPlayerFromEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager == event.getEntity()) {
            return null;
        }
        if (damager instanceof Player) {
            return (Player) damager;
        } else if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        return null;
    }

    private Player getPlayerFromEntityExplodeEvent(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) entity;
            if (tnt.getSource() instanceof Player) {
                return (Player) tnt.getSource();
            }
        }
        return null;
    }

    private Player getPlayerFromEntityDamageEvent(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            return (Player) entity;
        }
        return null;
    }

    private Player getPlayerFromEntityCombustByEntityEvent(EntityCombustByEntityEvent event) {
        Entity combuster = event.getCombuster();
        if (combuster instanceof Player) {
            return (Player) combuster;
        }
        return null;
    }

    public void unRegisterConfiguredEvents() {
        dynamicEventList.forEach(eventClass -> phantomBan.getLogger().info("Unregistered event " + eventClass.getName()));
        HandlerList.unregisterAll(this);
        dynamicEventList.clear();
    }

    public Set<Class<? extends Event>> getDynamicEventList() {
        return dynamicEventList;
    }
}