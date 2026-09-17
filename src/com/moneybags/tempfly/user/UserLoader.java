package com.moneybags.tempfly.user;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.moneybags.tempfly.aesthetic.particle.Particles;
import com.moneybags.tempfly.fly.FlightManager;
import com.moneybags.tempfly.storage.UserFlightData;
import com.moneybags.tempfly.storage.UserRepository;
import com.moneybags.tempfly.time.TimeManager;
import com.moneybags.tempfly.util.data.DataBridge;
import com.moneybags.tempfly.util.data.DataPointer;
import com.moneybags.tempfly.util.data.DataBridge.DataValue;

public class UserLoader implements Runnable {

	private UUID u;
	private FlightManager manager;
	private boolean async;
	
	public UserLoader(UUID u, FlightManager manager, boolean async) {
		this.u = u;
		this.manager = manager;
		this.async = async;
	}
	
	double time;
	
	String particle;
	
	boolean
	infinite,
	bypass,
	logged,
	compatLogged,
	ready;
	
	double
	selectedSpeed;
	
	@Override
	public void run() {
		final UserRepository repo = manager.getTempFly() != null ? manager.getTempFly().getUserRepository() : null;
		if (repo != null) {
			UserFlightData data = repo.getUser(u);
			this.time = data.getTime();
			this.particle = data.getTrail() != null ? data.getTrail() : Particles.loadTrail(u);
			this.infinite = data.isInfinite();
			this.bypass = data.isBypass();
			this.logged = data.isLoggedInFlight();
			this.compatLogged = data.isCompatLoggedInFlight();
			this.selectedSpeed = data.getSpeed();
			if (selectedSpeed <= 0 && selectedSpeed != -999D) {
				selectedSpeed = -999D;
			}
		} else {
			final DataBridge bridge = manager.getTempFly() != null ? manager.getTempFly().getDataBridge() : null;
			final TimeManager timeManager = manager.getTempFly() != null ? manager.getTempFly().getTimeManager() : null;
			
			if (bridge != null && bridge.hasSqlEnabled()) {
				try (java.sql.Connection conn = bridge.getConnection();
				     PreparedStatement st = conn.prepareStatement(bridge.getInsertIgnoreQuery("tempfly_data", "uuid", "?"))) {
					st.setString(1, u.toString());
					st.executeUpdate();
				} catch (SQLException e) {
					e.printStackTrace();
					return;
				}
			}
			
			time = timeManager != null ? timeManager.getTime(u) : 0.0;
			particle = Particles.loadTrail(u);
			infinite = bridge != null ? (boolean) bridge.getOrDefault(DataPointer.of(DataValue.PLAYER_INFINITE, u.toString()), true) : true; 
			bypass = bridge != null ? (boolean) bridge.getOrDefault(DataPointer.of(DataValue.PLAYER_BYPASS, u.toString()), true) : true;
			logged = bridge != null ? (boolean) bridge.getOrDefault(DataPointer.of(DataValue.PLAYER_FLIGHT_LOG, u.toString()), false) : false;
			compatLogged = bridge != null ? (boolean) bridge.getOrDefault(DataPointer.of(DataValue.PLAYER_COMPAT_FLIGHT_LOG, u.toString()), false) : false;
			selectedSpeed = bridge != null ? (double) bridge.getOrDefault(DataPointer.of(DataValue.PLAYER_SPEED, u.toString()), -999D) : -999D;
			if (selectedSpeed <= 0 && selectedSpeed != -999D) {
				selectedSpeed = -999D;
			}
		}
		ready = true;
		if (async) {
			if (Bukkit.isPrimaryThread()) {
				Player p = Bukkit.getPlayer(u);
				if (p != null && p.isOnline()) {
					manager.addUser(p);
				}
			} else if (manager.getTempFly() != null) {
				Bukkit.getScheduler().runTask(manager.getTempFly(), () -> {
					Player p = Bukkit.getPlayer(u);
					if (p != null && p.isOnline()) {
						manager.addUser(p);
					}
				});
			}
		}
	}
	
	public boolean isReady() {
		return ready;
	}
	
	public FlightUser buildUser() {
		return new FlightUser(Bukkit.getPlayer(u), manager, time, particle, infinite, bypass, logged, compatLogged, selectedSpeed);
	}
	
	public FlightUser buildUser(Player p) {
		return new FlightUser(p, manager, time, particle, infinite, bypass, logged, compatLogged, selectedSpeed);
	}
	

}
