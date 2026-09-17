package com.moneybags.tempfly.fly;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.moneybags.tempfly.fly.RequirementProvider.InquiryType;
import com.moneybags.tempfly.fly.result.FlightResult;

/**
 * Pure, decoupled state model representing a player's flight data and runtime status.
 */
public class FlightState {

	private final UUID playerId;

	private volatile double time;
	private volatile boolean infinite;
	private volatile boolean infiniteRequirement;
	private volatile boolean bypass;
	private volatile boolean enabled;
	private volatile boolean autoEnable;
	private volatile int idleTicks;
	private volatile double selectedSpeed;
	private volatile String trail;
	private volatile long accumulativeCycleMillis;

	private final Map<RequirementProvider, Map<InquiryType, FlightResult>> requirements;

	public FlightState(UUID playerId, double time, String trail, boolean infinite, boolean bypass, double selectedSpeed) {
		this.playerId = Objects.requireNonNull(playerId, "playerId cannot be null");
		this.time = Math.max(0, time);
		this.trail = trail;
		this.infinite = infinite;
		this.infiniteRequirement = false;
		this.bypass = bypass;
		this.enabled = false;
		this.autoEnable = false;
		this.idleTicks = 0;
		this.selectedSpeed = selectedSpeed;
		this.accumulativeCycleMillis = 0L;
		this.requirements = new ConcurrentHashMap<>();
	}

	public UUID getPlayerId() {
		return playerId;
	}

	public UUID getUuid() {
		return playerId;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = Math.max(0, time);
	}

	public void addTime(double seconds) {
		if (seconds > 0) {
			this.time += seconds;
		}
	}

	public void removeTime(double seconds) {
		if (seconds > 0) {
			this.time = Math.max(0, this.time - seconds);
		}
	}

	public boolean hasInfiniteFlight() {
		return infinite || infiniteRequirement;
	}

	public boolean isInfinite() {
		return infinite;
	}

	public void setInfinite(boolean infinite) {
		this.infinite = infinite;
	}

	public boolean hasInfiniteRequirement() {
		return infiniteRequirement;
	}

	public void setInfiniteRequirement(boolean infiniteRequirement) {
		this.infiniteRequirement = infiniteRequirement;
	}

	public boolean hasFlightBypass() {
		return bypass;
	}

	public boolean isBypass() {
		return bypass;
	}

	public void setBypass(boolean bypass) {
		this.bypass = bypass;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isAutoEnable() {
		return autoEnable;
	}

	public void setAutoEnable(boolean autoEnable) {
		this.autoEnable = autoEnable;
	}

	public int getIdleTicks() {
		return idleTicks;
	}

	public void resetIdle() {
		this.idleTicks = 0;
	}

	public void addIdleTicks(int ticks) {
		this.idleTicks += ticks;
	}

	public boolean isIdle(long thresholdTicks) {
		return thresholdTicks >= 0 && idleTicks >= thresholdTicks;
	}

	public double getSelectedSpeed() {
		return selectedSpeed;
	}

	public void setSelectedSpeed(double selectedSpeed) {
		this.selectedSpeed = selectedSpeed;
	}

	public String getTrail() {
		return trail;
	}

	public void setTrail(String trail) {
		this.trail = trail;
	}

	public long getAccumulativeCycleMillis() {
		return accumulativeCycleMillis;
	}

	public void setAccumulativeCycleMillis(long millis) {
		this.accumulativeCycleMillis = millis;
	}

	public void addAccumulativeCycleMillis(long millis) {
		this.accumulativeCycleMillis += millis;
	}

	// Requirement queries
	public boolean hasFlightRequirements() {
		return !requirements.isEmpty();
	}

	public Map<RequirementProvider, Map<InquiryType, FlightResult>> getRequirements() {
		return Collections.unmodifiableMap(requirements);
	}

	public boolean hasRequirement(RequirementProvider provider) {
		return requirements.containsKey(provider);
	}

	public boolean hasRequirement(RequirementProvider provider, InquiryType type) {
		Map<InquiryType, FlightResult> map = requirements.get(provider);
		return map != null && map.containsKey(type);
	}

	public void submitRequirement(RequirementProvider provider, FlightResult failedResult) {
		requirements.compute(provider, (k, types) -> {
			if (types == null) {
				types = new HashMap<>();
			}
			types.put(failedResult.getInquiryType(), failedResult);
			return types;
		});
	}

	public void removeRequirement(RequirementProvider provider, InquiryType type) {
		requirements.computeIfPresent(provider, (k, types) -> {
			types.remove(type);
			return types.isEmpty() ? null : types;
		});
	}

	public void removeRequirements(RequirementProvider provider) {
		requirements.remove(provider);
	}

	public void clearRequirements() {
		requirements.clear();
	}

	public FlightResult getCurrentRequirement() {
		if (!requirements.isEmpty()) {
			var outer = requirements.values().iterator();
			if (outer.hasNext()) {
				var inner = outer.next().values().iterator();
				if (inner.hasNext()) {
					return inner.next();
				}
			}
		}
		return null;
	}
}
