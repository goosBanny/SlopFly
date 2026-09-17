package com.moneybags.tempfly.fly;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of the environment affecting flight at a player's current location.
 */
public record EnvironmentContext(
		double relativeTimeMultiplier,
		boolean infiniteFlightArea,
		float maxSpeedLimit,
		List<String> activeRegions,
		String worldName
) {
	public static final EnvironmentContext DEFAULT = new EnvironmentContext(1.0, false, -999f, Collections.emptyList(), "world");

	public EnvironmentContext {
		Objects.requireNonNull(activeRegions, "activeRegions cannot be null");
		Objects.requireNonNull(worldName, "worldName cannot be null");
		if (relativeTimeMultiplier < 0) {
			relativeTimeMultiplier = 1.0;
		}
	}

	public static EnvironmentContext of(double relativeTimeMultiplier, boolean infiniteFlightArea, float maxSpeedLimit, List<String> activeRegions, String worldName) {
		return new EnvironmentContext(
				relativeTimeMultiplier,
				infiniteFlightArea,
				maxSpeedLimit,
				activeRegions != null ? List.copyOf(activeRegions) : Collections.emptyList(),
				worldName != null ? worldName : "world"
		);
	}

	public boolean hasSpeedLimit() {
		return maxSpeedLimit > 0;
	}
}
