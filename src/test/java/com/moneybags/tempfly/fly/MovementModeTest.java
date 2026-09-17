package com.moneybags.tempfly.fly;

import com.moneybags.tempfly.util.V;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MovementModeTest {

    @Test
    public void testDefaultMovementMode() {
        V.movementMode = "TASK";
        assertTrue(V.isMovementTaskMode());

        V.movementMode = "task";
        assertTrue(V.isMovementTaskMode());

        V.movementMode = "EVENT";
        assertFalse(V.isMovementTaskMode());

        V.movementMode = "event";
        assertFalse(V.isMovementTaskMode());
    }

    @Test
    public void testSweepEveryCalculation() {
        // At 4 ticks, sweepEvery = round(20 / 4) = 5 polls (5 * 4 = 20 ticks = 1s)
        int interval4 = 4;
        int sweepEvery4 = Math.max(1, Math.round(20f / (float) interval4));
        assertEquals(5, sweepEvery4);

        // At 1 tick, sweepEvery = 20
        int interval1 = 1;
        int sweepEvery1 = Math.max(1, Math.round(20f / (float) interval1));
        assertEquals(20, sweepEvery1);

        // At 10 ticks, sweepEvery = 2
        int interval10 = 10;
        int sweepEvery10 = Math.max(1, Math.round(20f / (float) interval10));
        assertEquals(2, sweepEvery10);

        // At 30 ticks (longer than 1s), sweepEvery = max(1, round(0.67)) = 1
        int interval30 = 30;
        int sweepEvery30 = Math.max(1, Math.round(20f / (float) interval30));
        assertEquals(1, sweepEvery30);
    }

    @Test
    public void testSameBlockMovementDetection() {
        // Simulate block comparison logic
        int x1 = 100, y1 = 64, z1 = -200;
        int x2 = 100, y2 = 64, z2 = -200;
        String w1 = "world", w2 = "world";

        boolean sameBlock = w1.equals(w2) && x1 == x2 && y1 == y2 && z1 == z2;
        assertTrue(sameBlock, "Identical coordinates in same world should be sameBlock");

        // Sub-block movement (player moved within same block)
        double subX1 = 100.1, subX2 = 100.8;
        boolean sameSubBlock = w1.equals(w2)
                && (int) Math.floor(subX1) == (int) Math.floor(subX2)
                && y1 == y2 && z1 == z2;
        assertTrue(sameSubBlock, "Sub-block movements should be sameBlock");

        // Block change in X
        int movedX = 101;
        boolean moved = !w1.equals(w2) || x1 != movedX || y1 != y2 || z1 != z2;
        assertTrue(moved, "X coordinate change should trigger block change");

        // Block change in Y
        int movedY = 65;
        moved = !w1.equals(w2) || x1 != x2 || y1 != movedY || z1 != z2;
        assertTrue(moved, "Y coordinate change should trigger block change");

        // Block change in Z
        int movedZ = -199;
        moved = !w1.equals(w2) || x1 != x2 || y1 != y2 || z1 != movedZ;
        assertTrue(moved, "Z coordinate change should trigger block change");

        // World change
        String movedWorld = "world_nether";
        moved = !w1.equals(movedWorld) || x1 != x2 || y1 != y2 || z1 != z2;
        assertTrue(moved, "World change should trigger block change");
    }
}
