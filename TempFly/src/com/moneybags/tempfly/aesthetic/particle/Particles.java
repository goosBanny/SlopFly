package com.moneybags.tempfly.aesthetic.particle;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.user.FlightUser;
import com.moneybags.tempfly.util.Console;
import com.moneybags.tempfly.util.V;
import com.moneybags.tempfly.util.data.DataBridge.DataValue;
import com.moneybags.tempfly.util.data.DataPointer;

public class Particles {

	private static TempFly tempfly;
	private static final Map<String, Particle> PARTICLE_CACHE = new ConcurrentHashMap<>();
	private static Particle defaultParticle = Particle.HAPPY_VILLAGER;
	
	public static void initialize(TempFly plugin) {
		tempfly = plugin;
		PARTICLE_CACHE.clear();
		Particle def = parseParticle(V.particleType);
		defaultParticle = def != null ? def : Particle.HAPPY_VILLAGER;
	}

	public static Particle parseParticle(String name) {
		if (name == null || name.isEmpty()) {
			return null;
		}
		return PARTICLE_CACHE.computeIfAbsent(name.toUpperCase(Locale.ROOT), key -> {
			try {
				return Particle.valueOf(key);
			} catch (IllegalArgumentException e) {
				return null;
			}
		});
	}
	
	public static void play(Location loc, String s) {
		if (loc == null || loc.getWorld() == null) return;
		Particle particle = parseParticle(s);
		if (particle == null) {
			particle = defaultParticle;
		}
		
		Class<?> c = particle.getDataType();
		try {
			if (DustOptions.class.equals(c)) {
				ThreadLocalRandom rand = ThreadLocalRandom.current();
				loc.getWorld().spawnParticle(particle, loc, 1, new DustOptions(Color.fromRGB(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)), 2f));	
			} else if (org.bukkit.block.data.BlockData.class.equals(c)) {
				loc.getWorld().spawnParticle(particle, loc, 1, Material.STONE.createBlockData());	
			} else {
				loc.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0.1);
			}
		} catch (Exception e) {
			loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 1, 0, 0, 0, 0.1);
		}
	}
	
	public static String loadTrail(UUID u) {
		String particle = (String) tempfly.getDataBridge().getOrDefault(DataPointer.of(DataValue.PLAYER_TRAIL, u.toString()), null);
		if (V.debug) {Console.debug("", "------Loading particle trail------", "Player: " + u.toString(), "Value from data: " + String.valueOf(particle), "Default trail enabled: " + V.particleDefault, "Default trail is: " + V.particleType, "Returning trail: " +  (particle != null ? particle: (V.particleDefault ? V.particleType : "")), "------End particle trail------", "");}
		return particle != null ? particle: (V.particleDefault ? V.particleType : "");
	}
	
	/**
	 * Set a players particle trail.
	 * If particle is set to null or if the trail specified does not exist TempFly will attempt to use the default trail if enabled in the config.
	 * If it is set to an empty string however the particle will be disabled, IE no trail. This is what the remove trail command does.
	 * @param u the player
	 * @param particle the particle
	 */
	public static void setTrail(UUID u, String particle) {
		FlightUser user = tempfly.getFlightManager().getUser(Bukkit.getPlayer(u));
		if (user != null) {
			user.setTrail(particle);
			return;
		}
		tempfly.getDataBridge().stageChange(DataPointer.of(DataValue.PLAYER_TRAIL, u.toString()), particle);
	}
	
}
