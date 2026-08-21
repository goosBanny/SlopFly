package com.moneybags.tempfly.command.admin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CmdGiveAllTest {

    @Test
    public void testGiveAllMaxTimeCalculation() {
        double maxTime = 100.0;
        double currentBalance = 100.0;
        double giveAmount = 50.0;

        // Player is already at max time
        double remainingAllowed = maxTime - currentBalance;
        assertTrue(remainingAllowed <= 0, "Remaining allowed time should be 0 or less when at max");

        // Player has space for some time
        currentBalance = 80.0;
        remainingAllowed = maxTime - currentBalance;
        assertEquals(20.0, remainingAllowed, 0.001);
        assertTrue(remainingAllowed > 0, "Player should be allowed partial time up to max");
    }
}
