package io.github.jochyoua.phantomban.listeners;

import io.github.jochyoua.phantomban.PhantomBan;
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
            ConfigurationSection config = phantomBan.getConfig().getConfigurationSection("settings.effects.events." + eventClassPath);
            if (config == null || !config.getBoolean("enabled", false)) {
                continue;
            }

            try {
                String cleanPathName = eventClassPath.replace('#', '.');
                Class<?> eventClass = Class.forName(cleanPathName);

                EventExecutor eventExecutor = ((listener, event) -> handleDynamicEvent(event, eventClassPath));
                pluginManager.registerEvent((Class<? extends Event>) eventClass, this, EventPriority.HIGHEST, eventExecutor, phantomBan);
                dynamicEventList.add((Class<? extends Event>) eventClass);

                phantomBan.getLogger().info("Successfully registered event: " + cleanPathName);
            } catch (ClassNotFoundException e) {
                phantomBan.getLogger().warning("Could not find event class for: " + eventClassPath);
            }
        }
    }

    private void handleDynamicEvent(Event event, String configKey) {
        ConfigurationSection config = phantomBan.getConfig().getConfigurationSection("settings.effects.events." + configKey);
        if (config == null) {
            return;
        }
        String message = config.getString("message");
        Player player = getPlayerFromEvent(event);

        if (player == null || !phantomBan.isPhantomBanned(player.getUniqueId())
                || player.hasPermission("phantomban.bypass." + event.getClass().getSimpleName())) {
            return;
        }
        if (event instanceof Cancellable) {
            ((Cancellable) event).setCancelled(true);
        }

        if (message != null && !message.isEmpty()) {
            String timeRemainingString = phantomBan.getConfig().getString("messages.time-until-unban");
            if (phantomBan.getOnlineTimeTracker().containsKey(player.getUniqueId())
                    && timeRemainingString != null && !timeRemainingString.isEmpty() && phantomBan.getConfig().getBoolean("settings.loyalty-rewards.enabled")) {
                timeRemainingString = String.format(timeRemainingString, TimeUnit.MILLISECONDS.toSeconds(phantomBan.getOnlineTimeTracker().getExpectedExpiration(player.getUniqueId())));
            } else {
                timeRemainingString = "";
            }
            player.sendMessage(String.format(phantomBan.formatMessage(message), timeRemainingString));
        }
    }

    private Player getPlayerFromEvent(Event event) {
        if (event instanceof PlayerEvent) {
            return ((PlayerEvent) event).getPlayer();
        } else if (event instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
            if (damager == ((EntityDamageByEntityEvent) event).getEntity()) {
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
        } else if (event instanceof EntityExplodeEvent) {
            Entity entity = ((EntityExplodeEvent) event).getEntity();
            if (entity instanceof TNTPrimed) {
                TNTPrimed tnt = (TNTPrimed) entity;
                if (tnt.getSource() instanceof Player) {
                    return (Player) tnt.getSource();
                }
            }
        } else if (event instanceof EntityDamageEvent) {
            EntityDamageEvent damageEvent = (EntityDamageEvent) event;
            Entity damager = damageEvent.getEntity();
            if (damager instanceof Player) {
                return (Player) damager;
            }
        } else if (event instanceof EntityCombustByEntityEvent) {
            EntityCombustByEntityEvent combustEvent = (EntityCombustByEntityEvent) event;
            Entity combuster = combustEvent.getCombuster();
            if (combuster instanceof Player) {
                return (Player) combuster;
            }
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

    public void unRegisterConfiguredEvents() {
        dynamicEventList.forEach(eventClass -> phantomBan.getLogger().info("Unregistered event " + eventClass.getName()));
        HandlerList.unregisterAll(this);
        dynamicEventList.clear();
    }

    public Set<Class<? extends Event>> getDynamicEventList() {
        return this.dynamicEventList;
    }
}
