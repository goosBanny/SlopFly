package com.moneybags.tempfly.command;

import com.moneybags.tempfly.command.CommandManager.CommandType;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandSender.Spigot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommandManagerTest {

    @Test
    public void testCommandTypeFactoryInstantiation() {
        for (CommandType type : CommandType.values()) {
            assertNotNull(type.getBase(), "Base alias should not be null for " + type);
            TempFlyCommand cmd = type.createCommand(null, new String[]{"test"});
            assertNotNull(cmd, "Factory should instantiate command for " + type);
        }
    }

    @Test
    public void testCommandTypeBaseNames() {
        assertEquals("give", CommandType.GIVE.getBase());
        assertEquals("giveall", CommandType.GIVE_ALL.getBase());
        assertEquals("reload", CommandType.RELOAD.getBase());
        assertEquals("remove", CommandType.REMOVE.getBase());
        assertEquals("set", CommandType.SET.getBase());
        assertEquals("remove_trail", CommandType.TRAIL_REMOVE.getBase());
        assertEquals("set_trail", CommandType.TRAIL_SET.getBase());
        assertEquals("bypass", CommandType.BYPASS.getBase());
        assertEquals("toggle", CommandType.FLY.getBase());
        assertEquals("help", CommandType.HELP.getBase());
        assertEquals("infinite", CommandType.INFINITE.getBase());
        assertEquals("pay", CommandType.PAY.getBase());
        assertEquals("shop", CommandType.SHOP.getBase());
        assertEquals("speed", CommandType.SPEED.getBase());
        assertEquals("time", CommandType.TIME.getBase());
        assertEquals("trails", CommandType.TRAILS.getBase());
        assertEquals("migrate", CommandType.MIGRATE.getBase());
    }

    private static class DummyConsoleSender implements CommandSender {
        @Override public void sendMessage(String message) {}
        @Override public void sendMessage(String[] messages) {}
        @Override public void sendMessage(java.util.UUID sender, String message) {}
        @Override public void sendMessage(java.util.UUID sender, String[] messages) {}
        @Override public org.bukkit.Server getServer() { return null; }
        @Override public String getName() { return "CONSOLE"; }
        @Override public Spigot spigot() { return null; }
        @Override public boolean isPermissionSet(String name) { return true; }
        @Override public boolean isPermissionSet(org.bukkit.permissions.Permission perm) { return true; }
        @Override public boolean hasPermission(String name) { return true; }
        @Override public boolean hasPermission(org.bukkit.permissions.Permission perm) { return true; }
        @Override public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value) { return null; }
        @Override public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin) { return null; }
        @Override public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value, int ticks) { return null; }
        @Override public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, int ticks) { return null; }
        @Override public void removeAttachment(org.bukkit.permissions.PermissionAttachment attachment) {}
        @Override public void recalculatePermissions() {}
        @Override public java.util.Set<org.bukkit.permissions.PermissionAttachmentInfo> getEffectivePermissions() { return java.util.Collections.emptySet(); }
        @Override public boolean isOp() { return true; }
        @Override public void setOp(boolean value) {}
        @Override public net.kyori.adventure.text.Component name() { return net.kyori.adventure.text.Component.text("CONSOLE"); }
    }

    @Test
    public void testMigratePermissionConsoleOnly() {
        CommandSender dummyConsole = new DummyConsoleSender();
        assertTrue(CommandType.MIGRATE.hasPermission(null, dummyConsole));
    }
}
