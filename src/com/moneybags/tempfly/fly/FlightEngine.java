package com.moneybags.tempfly.fly;

import org.bukkit.GameMode;
import com.moneybags.tempfly.config.GeneralConfig;

/**
 * Decoupled, unit-testable flight engine providing pure mathematical time deductions,
 * timer eligibility evaluations, and state progression without dependency on Bukkit Player runtime.
 */
public class FlightEngine {

	public enum TimerEligibility {
		ELIGIBLE,
		INFINITE,
		NO_TIME,
		GAME_MODE_RESTRICTED,
		IN_VEHICLE,
		NOT_FLYING,
		IDLE_RESTRICTED
	}

	public enum TickOutcome {
		SKIPPED,
		CONSUMED,
		EXPIRED
	}

	/**
	 * Evaluates whether flight time should be actively deducted for a player.
	 */
	public TimerEligibility evaluateEligibility(
			FlightState state,
			boolean isFlying,
			boolean isOnGround,
			boolean isIdle,
			GameMode gameMode,
			boolean inVehicle,
			GeneralConfig config
	) {
		if (state.hasInfiniteFlight()) {
			return TimerEligibility.INFINITE;
		}
		if (state.getTime() <= 0) {
			return TimerEligibility.NO_TIME;
		}
		if (config.permaTimer()) {
			return isIdle && !config.idleTimer() ? TimerEligibility.IDLE_RESTRICTED : TimerEligibility.ELIGIBLE;
		}
		if (gameMode == GameMode.CREATIVE && !config.creativeTimer()) {
			return TimerEligibility.GAME_MODE_RESTRICTED;
		}
		if (gameMode == GameMode.SPECTATOR && !config.spectatorTimer()) {
			return TimerEligibility.GAME_MODE_RESTRICTED;
		}
		if (inVehicle) {
			return TimerEligibility.IN_VEHICLE;
		}
		if (!isFlying) {
			if (config.groundTimer()) {
				return isIdle && !config.idleTimer() ? TimerEligibility.IDLE_RESTRICTED : TimerEligibility.ELIGIBLE;
			}
			return TimerEligibility.NOT_FLYING;
		}
		if (isIdle && !config.idleTimer()) {
			return TimerEligibility.IDLE_RESTRICTED;
		}
		return TimerEligibility.ELIGIBLE;
	}

	/**
	 * Calculates the time deduction for an elapsed duration given the environment multiplier.
	 */
	public double calculateDeduction(double baseSeconds, EnvironmentContext env) {
		double factor = env != null ? env.relativeTimeMultiplier() : 1.0;
		return Math.max(0, baseSeconds * factor);
	}

	/**
	 * Progresses the flight timer by 1 second cycle.
	 *
	 * @param state The flight state
	 * @param env   The environmental context
	 * @return The TickOutcome (CONSUMED or EXPIRED)
	 */
	public TickOutcome processOneSecondCycle(FlightState state, EnvironmentContext env) {
		if (state.hasInfiniteFlight()) {
			return TickOutcome.SKIPPED;
		}
		double current = state.getTime();
		if (current <= 0) {
			return TickOutcome.EXPIRED;
		}

		double cost = calculateDeduction(1.0, env);
		double newTime = Math.max(0, current - cost);
		state.setTime(newTime);

		return newTime <= 0 ? TickOutcome.EXPIRED : TickOutcome.CONSUMED;
	}
}
