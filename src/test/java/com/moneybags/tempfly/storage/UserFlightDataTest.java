package com.moneybags.tempfly.storage;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UserFlightDataTest {

	@Test
	public void testDefaultValues() {
		UUID uuid = UUID.randomUUID();
		UserFlightData data = UserFlightData.defaultFor(uuid);

		assertEquals(uuid, data.getUuid());
		assertEquals(0.0, data.getTime(), 0.001);
		assertFalse(data.isLoggedInFlight());
		assertFalse(data.isCompatLoggedInFlight());
		assertFalse(data.hasDamageProtection());
		assertEquals(0L, data.getLastDailyBonus());
		assertNull(data.getTrail());
		assertTrue(data.isInfinite(), "Default infinite should be true to respect permissions");
		assertTrue(data.isBypass(), "Default bypass should be true to respect permissions");
		assertEquals(-999.0, data.getSpeed(), 0.001);
	}

	@Test
	public void testFieldMutationsAndGuards() {
		UUID uuid = UUID.randomUUID();
		UserFlightData data = new UserFlightData(uuid);

		data.setTime(-50.0);
		assertEquals(0.0, data.getTime(), 0.001, "Negative time should clamp to 0.0");

		data.setTime(120.5);
		assertEquals(120.5, data.getTime(), 0.001);

		data.setSpeed(0.0);
		assertEquals(-999.0, data.getSpeed(), 0.001, "Zero or negative speed should normalize to -999.0");

		data.setSpeed(1.5);
		assertEquals(1.5, data.getSpeed(), 0.001);

		data.setTrail("HEART");
		assertEquals("HEART", data.getTrail());

		data.setInfinite(false);
		assertFalse(data.isInfinite());

		data.setBypass(false);
		assertFalse(data.isBypass());

		data.setLoggedInFlight(true);
		assertTrue(data.isLoggedInFlight());

		data.setCompatLoggedInFlight(true);
		assertTrue(data.isCompatLoggedInFlight());

		data.setDamageProtection(true);
		assertTrue(data.hasDamageProtection());

		data.setLastDailyBonus(123456789L);
		assertEquals(123456789L, data.getLastDailyBonus());
	}

	@Test
	public void testCopyAndEquals() {
		UUID uuid = UUID.randomUUID();
		UserFlightData original = new UserFlightData(
				uuid, 3600.0, true, true, true, 99999L, "FLAME", false, false, 2.0
		);
		UserFlightData copy = original.copy();

		assertEquals(original, copy);
		assertEquals(original.hashCode(), copy.hashCode());
		assertEquals(3600.0, copy.getTime(), 0.001);
		assertEquals("FLAME", copy.getTrail());
		assertFalse(copy.isInfinite());
		assertFalse(copy.isBypass());
		assertEquals(2.0, copy.getSpeed(), 0.001);

		// Mutating copy shouldn't change original
		copy.setTime(500.0);
		assertEquals(500.0, copy.getTime(), 0.001);
		assertEquals(3600.0, original.getTime(), 0.001);
	}
}
