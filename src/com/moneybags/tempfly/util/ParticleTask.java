package com.moneybags.tempfly.util;

import org.bukkit.scheduler.BukkitRunnable;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.user.FlightUser;

public class ParticleTask extends BukkitRunnable {

	private TempFly tempfly;
	
	public ParticleTask(TempFly tempfly) {
		this.tempfly = tempfly;
	}
	
	@Override
	public void run() {
		for (FlightUser user : tempfly.getFlightManager().getUserValues()) {
			org.bukkit.entity.Player p = user.getPlayer();
			if (p != null && p.isOnline() && user.hasFlightEnabled() && p.isFlying()) {
				user.playTrail();
			}
		}
	}

}
