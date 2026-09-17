package com.moneybags.tempfly.fly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class FlightStateTest {

	@Test
	public void testTimeMutations() {
		UUID uuid = UUID.randomUUID();
		FlightState state = new FlightState(uuid, 50.0, "VILLAGER_HAPPY", false, false, 1.0);

		assertEquals(50.0, state.getTime());

		state.addTime(25.0);
		assertEquals(75.0, state.getTime());

		state.removeTime(15.0);
		assertEquals(60.0, state.getTime());

		state.removeTime(100.0);
		assertEquals(0.0, state.getTime(), "Time should not drop below 0");
	}

	@Test
	public void testIdleTracking() {
		UUID uuid = UUID.randomUUID();
		FlightState state = new FlightState(uuid, 50.0, null, false, false, 1.0);

		assertEquals(0, state.getIdleTicks());
		assertFalse(state.isIdle(60));

		state.addIdleTicks(40);
		assertFalse(state.isIdle(60));

		state.addIdleTicks(25);
		assertTrue(state.isIdle(60));

		state.resetIdle();
		assertEquals(0, state.getIdleTicks());
		assertFalse(state.isIdle(60));
	}

	@Test
	public void testInfiniteFlightResolution() {
		UUID uuid = UUID.randomUUID();
		FlightState state = new FlightState(uuid, 50.0, null, false, false, 1.0);

		assertFalse(state.hasInfiniteFlight());

		state.setInfiniteRequirement(true);
		assertTrue(state.hasInfiniteFlight());

		state.setInfiniteRequirement(false);
		assertFalse(state.hasInfiniteFlight());

		state.setInfinite(true);
		assertTrue(state.hasInfiniteFlight());
	}
}
