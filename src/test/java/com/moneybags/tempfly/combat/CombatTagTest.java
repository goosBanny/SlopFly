package com.moneybags.tempfly.combat;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CombatTagTest {

    @Test
    public void testCombatTagProgressAndTick() {
        UUID uuid = UUID.randomUUID();
        CombatTag tag = new CombatTag(uuid, 3L);

        assertEquals(uuid, tag.getPlayer());
        assertEquals(3L, tag.getDuration());
        assertEquals(0L, tag.getProgress());
        assertEquals(3L, tag.getRemainingTime());

        assertFalse(tag.tick());
        assertEquals(1L, tag.getProgress());
        assertEquals(2L, tag.getRemainingTime());

        assertFalse(tag.tick());
        assertEquals(2L, tag.getProgress());
        assertEquals(1L, tag.getRemainingTime());

        assertTrue(tag.tick());
        assertEquals(3L, tag.getProgress());
        assertEquals(0L, tag.getRemainingTime());
    }

    @Test
    public void testCombatTagCancel() {
        UUID uuid = UUID.randomUUID();
        CombatTag tag = new CombatTag(uuid, 10L);

        assertEquals(10L, tag.getRemainingTime());
        tag.cancel();
        assertEquals(10L, tag.getProgress());
        assertEquals(0L, tag.getRemainingTime());
        assertTrue(tag.tick());
    }
}
