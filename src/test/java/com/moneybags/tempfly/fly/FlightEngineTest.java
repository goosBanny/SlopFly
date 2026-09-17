package com.moneybags.tempfly.fly;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moneybags.tempfly.config.GeneralConfig;
import com.moneybags.tempfly.fly.FlightEngine.TickOutcome;
import com.moneybags.tempfly.fly.FlightEngine.TimerEligibility;

public class FlightEngineTest {

	private FlightEngine engine;
	private GeneralConfig defaultConfig;

	@BeforeEach
	public void setUp() {
		engine = new FlightEngine();
		defaultConfig = new GeneralConfig(
				false, // permaTimer
				false, // groundTimer
				false, // creativeTimer
				false, // spectatorTimer
				true,  // autoFly
				true,  // autoFlyTimeReceived
				false, // idleDrop
				false, // idleTimer
				300,   // idleThreshold
				1.0f,  // defaultSpeed
				true,  // allowSpeedPreference
				275,   // maxY
				false, false, false, false, false, false, false, // damages
				0.0, 0.0, Map.of(), // bonuses
				0, 0.0 // decay
		);
	}

	@Test
	public void testEvaluateEligibilityNormalFlying() {
		FlightState state = new FlightState(UUID.randomUUID(), 100.0, null, false, false, 1.0);
		TimerEligibility result = engine.evaluateEligibility(
				state, true, false, false, GameMode.SURVIVAL, false, defaultConfig
		);
		assertEquals(TimerEligibility.ELIGIBLE, result);
	}

	@Test
	public void testEvaluateEligibilityInfinite() {
		FlightState state = new FlightState(UUID.randomUUID(), 100.0, null, true, false, 1.0);
		TimerEligibility result = engine.evaluateEligibility(
				state, true, false, false, GameMode.SURVIVAL, false, defaultConfig
		);
		assertEquals(TimerEligibility.INFINITE, result);
	}

	@Test
	public void testEvaluateEligibilityCreative() {
		FlightState state = new FlightState(UUID.randomUUID(), 100.0, null, false, false, 1.0);
		// Creative flight when creativeTimer is false
		TimerEligibility result = engine.evaluateEligibility(
				state, true, false, false, GameMode.CREATIVE, false, defaultConfig
		);
		assertEquals(TimerEligibility.GAME_MODE_RESTRICTED, result);

		// When creativeTimer is true
		GeneralConfig creativeConfig = new GeneralConfig(
				false, false, true, false, true, true, false, false, 300, 1.0f, true, 275,
				false, false, false, false, false, false, false, 0.0, 0.0, Map.of(), 0, 0.0
		);
		result = engine.evaluateEligibility(
				state, true, false, false, GameMode.CREATIVE, false, creativeConfig
		);
		assertEquals(TimerEligibility.ELIGIBLE, result);
	}

	@Test
	public void testEvaluateEligibilityGroundTimer() {
		FlightState state = new FlightState(UUID.randomUUID(), 100.0, null, false, false, 1.0);

		// Not flying, groundTimer is false
		TimerEligibility result = engine.evaluateEligibility(
				state, false, true, false, GameMode.SURVIVAL, false, defaultConfig
		);
		assertEquals(TimerEligibility.NOT_FLYING, result);

		// When groundTimer is true
		GeneralConfig groundConfig = new GeneralConfig(
				false, true, false, false, true, true, false, false, 300, 1.0f, true, 275,
				false, false, false, false, false, false, false, 0.0, 0.0, Map.of(), 0, 0.0
		);
		result = engine.evaluateEligibility(
				state, false, true, false, GameMode.SURVIVAL, false, groundConfig
		);
		assertEquals(TimerEligibility.ELIGIBLE, result);
	}

	@Test
	public void testCalculateDeductionMultipliers() {
		EnvironmentContext standard = EnvironmentContext.DEFAULT;
		assertEquals(1.0, engine.calculateDeduction(1.0, standard), 0.001);

		EnvironmentContext fast = EnvironmentContext.of(1.5, false, -999f, Collections.emptyList(), "world");
		assertEquals(1.5, engine.calculateDeduction(1.0, fast), 0.001);

		EnvironmentContext slow = EnvironmentContext.of(0.5, false, -999f, Collections.emptyList(), "world");
		assertEquals(0.5, engine.calculateDeduction(1.0, slow), 0.001);
	}

	@Test
	public void testProcessOneSecondCycle() {
		FlightState state = new FlightState(UUID.randomUUID(), 10.0, null, false, false, 1.0);
		EnvironmentContext env = EnvironmentContext.DEFAULT;

		TickOutcome outcome = engine.processOneSecondCycle(state, env);
		assertEquals(TickOutcome.CONSUMED, outcome);
		assertEquals(9.0, state.getTime(), 0.001);

		// Drain almost to zero
		state.setTime(0.5);
		outcome = engine.processOneSecondCycle(state, env);
		assertEquals(TickOutcome.EXPIRED, outcome);
		assertEquals(0.0, state.getTime(), 0.001);
	}
}
