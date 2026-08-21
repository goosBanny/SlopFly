package com.moneybags.tempfly.combat;

import java.util.UUID;

public class CombatTag {

	private final UUID u;
	private final long duration;
	private long progress;
	
	public CombatTag(UUID u, long duration, CombatHandler combat) {
		this.u = u;
		this.duration = duration;
		this.progress = 0;
	}

	public CombatTag(UUID u, long duration) {
		this(u, duration, null);
	}
	
	public UUID getPlayer() {
		return u;
	}
	
	public long getDuration() {
		return duration;
	}
	
	public long getProgress() {
		return progress;
	}
	
	public long getRemainingTime() {
		return Math.max(0, duration - progress);
	}

	public boolean tick() {
		progress++;
		return progress >= duration;
	}

	public void cancel() {
		this.progress = duration;
	}

}
