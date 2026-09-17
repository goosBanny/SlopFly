package com.moneybags.tempfly.storage;

import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing a player's persistent flight and account data.
 */
public class UserFlightData {

	private final UUID uuid;
	private double time;
	private boolean loggedInFlight;
	private boolean compatLoggedInFlight;
	private boolean damageProtection;
	private long lastDailyBonus;
	private String trail;
	private boolean infinite;
	private boolean bypass;
	private double speed;

	public UserFlightData(UUID uuid) {
		this(uuid, 0.0, false, false, false, 0L, null, true, true, -999.0);
	}

	public UserFlightData(UUID uuid, double time, boolean loggedInFlight, boolean compatLoggedInFlight,
			boolean damageProtection, long lastDailyBonus, String trail, boolean infinite,
			boolean bypass, double speed) {
		this.uuid = Objects.requireNonNull(uuid, "uuid cannot be null");
		this.time = Math.max(0.0, time);
		this.loggedInFlight = loggedInFlight;
		this.compatLoggedInFlight = compatLoggedInFlight;
		this.damageProtection = damageProtection;
		this.lastDailyBonus = lastDailyBonus;
		this.trail = trail;
		this.infinite = infinite;
		this.bypass = bypass;
		this.speed = (speed <= 0 && speed != -999.0) ? -999.0 : speed;
	}

	public static UserFlightData defaultFor(UUID uuid) {
		return new UserFlightData(uuid);
	}

	public UUID getUuid() {
		return uuid;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = Math.max(0.0, time);
	}

	public boolean isLoggedInFlight() {
		return loggedInFlight;
	}

	public void setLoggedInFlight(boolean loggedInFlight) {
		this.loggedInFlight = loggedInFlight;
	}

	public boolean isCompatLoggedInFlight() {
		return compatLoggedInFlight;
	}

	public void setCompatLoggedInFlight(boolean compatLoggedInFlight) {
		this.compatLoggedInFlight = compatLoggedInFlight;
	}

	public boolean hasDamageProtection() {
		return damageProtection;
	}

	public void setDamageProtection(boolean damageProtection) {
		this.damageProtection = damageProtection;
	}

	public long getLastDailyBonus() {
		return lastDailyBonus;
	}

	public void setLastDailyBonus(long lastDailyBonus) {
		this.lastDailyBonus = lastDailyBonus;
	}

	public String getTrail() {
		return trail;
	}

	public void setTrail(String trail) {
		this.trail = trail;
	}

	public boolean isInfinite() {
		return infinite;
	}

	public void setInfinite(boolean infinite) {
		this.infinite = infinite;
	}

	public boolean isBypass() {
		return bypass;
	}

	public void setBypass(boolean bypass) {
		this.bypass = bypass;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = (speed <= 0 && speed != -999.0) ? -999.0 : speed;
	}

	public UserFlightData copy() {
		return new UserFlightData(
				uuid, time, loggedInFlight, compatLoggedInFlight,
				damageProtection, lastDailyBonus, trail, infinite, bypass, speed
		);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserFlightData that = (UserFlightData) o;
		return uuid.equals(that.uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	@Override
	public String toString() {
		return "UserFlightData{" +
				"uuid=" + uuid +
				", time=" + time +
				", loggedInFlight=" + loggedInFlight +
				", compatLoggedInFlight=" + compatLoggedInFlight +
				", damageProtection=" + damageProtection +
				", lastDailyBonus=" + lastDailyBonus +
				", trail='" + trail + '\'' +
				", infinite=" + infinite +
				", bypass=" + bypass +
				", speed=" + speed +
				'}';
	}
}
