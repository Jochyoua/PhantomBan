package io.github.jochyoua.phantomban.permissions;

import io.github.jochyoua.phantomban.PhantomBan;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.Set;

public class DynamicPermissionHandler {
    PhantomBan phantomBan;

    public DynamicPermissionHandler(PhantomBan phantomBan) {
        this.phantomBan = phantomBan;
    }

    public void registerConfiguredPermissions() {
        Set<Class<? extends Event>> dynamicEventList = phantomBan.getDynamicEventHandler().getDynamicEventList();

        dynamicEventList.forEach(eventClass -> {
            Permission permission = new Permission("phantomban.bypass." + eventClass.getSimpleName(),
                    "Allows user to bypass " + eventClass.getSimpleName() + " cancellation if phantom-banned", PermissionDefault.FALSE);
            Bukkit.getPluginManager().addPermission(permission);
            phantomBan.getLogger().info("Registered permission " + permission.getName());
        });
    }

    public void unregisterConfiguredPermissions() {
        Set<Class<? extends Event>> dynamicEventList = phantomBan.getDynamicEventHandler().getDynamicEventList();

        dynamicEventList.forEach(eventClass -> {
            Permission permission = Bukkit.getPluginManager().getPermission("phantomban.bypass." + eventClass.getSimpleName());
            if (permission != null) {
                phantomBan.getLogger().info("Unregistered permission " + permission.getName());
                Bukkit.getPluginManager().removePermission(permission);
            }
        });
    }
}
