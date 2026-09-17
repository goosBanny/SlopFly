package com.moneybags.tempfly.user;

import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class FlightSpeedTest {

    private Permissible permissible;

    private static class DummyPermissible implements Permissible {
        @Override public boolean isOp() { return false; }
        @Override public void setOp(boolean value) {}
        @Override public boolean isPermissionSet(String name) { return false; }
        @Override public boolean isPermissionSet(Permission perm) { return false; }
        @Override public boolean hasPermission(String name) { return false; }
        @Override public boolean hasPermission(Permission perm) { return false; }
        @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) { return null; }
        @Override public PermissionAttachment addAttachment(Plugin plugin) { return null; }
        @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) { return null; }
        @Override public PermissionAttachment addAttachment(Plugin plugin, int ticks) { return null; }
        @Override public void removeAttachment(PermissionAttachment attachment) {}
        @Override public void recalculatePermissions() {}
        @Override public Set<PermissionAttachmentInfo> getEffectivePermissions() { return Collections.emptySet(); }
    }

    @BeforeEach
    public void setUp() {
        permissible = new DummyPermissible();
    }

    @Test
    public void testExactPermissionSpeed() {
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.add(new PermissionAttachmentInfo(permissible, "tempfly.speed.world.world_nether.2.5", null, true));

        float speed = FlightUser.calculatePermissionSpeed(perms, "world.world_nether", "world.*");

        assertEquals(2.5f, speed, 0.001f);
    }

    @Test
    public void testWildcardPermissionSpeed() {
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.add(new PermissionAttachmentInfo(permissible, "tempfly.speed.world.*.3.0", null, true));

        float speed = FlightUser.calculatePermissionSpeed(perms, "world.world_the_end", "world.*");

        assertEquals(3.0f, speed, 0.001f);
    }

    @Test
    public void testMaxPermissionSpeedSelection() {
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.add(new PermissionAttachmentInfo(permissible, "tempfly.speed.world.survival.1.5", null, true));
        perms.add(new PermissionAttachmentInfo(permissible, "tempfly.speed.world.survival.3.5", null, true));
        perms.add(new PermissionAttachmentInfo(permissible, "tempfly.speed.world.survival.2.0", null, true));

        float speed = FlightUser.calculatePermissionSpeed(perms, "world.survival", "world.*");

        assertEquals(3.5f, speed, 0.001f);
    }

    @Test
    public void testNegatedPermissionIgnored() {
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.add(new PermissionAttachmentInfo(permissible, "tempfly.speed.world.survival.5.0", null, false));
        perms.add(new PermissionAttachmentInfo(permissible, "tempfly.speed.world.survival.1.5", null, true));

        float speed = FlightUser.calculatePermissionSpeed(perms, "world.survival", "world.*");

        assertEquals(1.5f, speed, 0.001f);
    }

    @Test
    public void testBracketsFormatting() {
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.add(new PermissionAttachmentInfo(permissible, "tempfly.speed.world.survival.[4.0]", null, true));

        float speed = FlightUser.calculatePermissionSpeed(perms, "world.survival", "world.*");

        assertEquals(4.0f, speed, 0.001f);
    }

    @Test
    public void testNoPermissionFallback() {
        float speed = FlightUser.calculatePermissionSpeed(Collections.emptySet(), "world.survival", "world.*");

        assertEquals(-999f, speed, 0.001f);
    }

    @Test
    public void testZeroAndNegativeSpeedsIgnoredInPermission() {
        Set<PermissionAttachmentInfo> perms = new HashSet<>();
        perms.add(new PermissionAttachmentInfo(permissible, "tempfly.speed.world.survival.0", null, true));
        perms.add(new PermissionAttachmentInfo(permissible, "tempfly.speed.world.survival.-1", null, true));

        float speed = FlightUser.calculatePermissionSpeed(perms, "world.survival", "world.*");

        assertEquals(-999f, speed, 0.001f);
    }
}
