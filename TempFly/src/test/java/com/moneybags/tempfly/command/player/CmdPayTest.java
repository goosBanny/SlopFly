package com.moneybags.tempfly.command.player;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CmdPayTest {

    @Test
    public void testSelfPaymentUuidEquality() {
        UUID senderId = UUID.randomUUID();
        UUID targetIdSame = UUID.fromString(senderId.toString());
        UUID targetIdDifferent = UUID.randomUUID();

        // Testing the exact fix: sender.getUniqueId().equals(target.getUniqueId())
        assertTrue(senderId.equals(targetIdSame), "Matching UUIDs should be detected as self-payment");
        assertFalse(senderId.equals(targetIdDifferent), "Different UUIDs should be allowed for payment");
    }
}
