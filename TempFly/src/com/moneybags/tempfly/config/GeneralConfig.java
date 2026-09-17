package com.moneybags.tempfly.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record GeneralConfig(
		boolean permaTimer,
		boolean groundTimer,
		boolean creativeTimer,
		boolean spectatorTimer,
		boolean autoFly,
		boolean autoFlyTimeReceived,
		boolean idleDrop,
		boolean idleTimer,
		long idleThreshold,
		float defaultSpeed,
		boolean allowSpeedPreference,
		int maxY,
		boolean damageCommand,
		boolean damageTime,
		boolean damageIdle,
		boolean damageCombat,
		boolean damageRegion,
		boolean damageWorld,
		boolean damageStruct,
		double firstJoinTime,
		double legacyBonus,
		Map<String, Double> dailyBonus,
		long decayThreshold,
		double decayAmount
) {
	public static GeneralConfig from(FileConfiguration config) {
		boolean permaTimer = config.getBoolean("general.timer.constant", false);
		boolean groundTimer = config.getBoolean("general.timer.ground", false);
		boolean creativeTimer = config.getBoolean("general.timer.creative", false);
		boolean spectatorTimer = config.getBoolean("general.timer.spectator", false);

		boolean autoFly = config.contains("general.flight.auto_enable")
				? config.getBoolean("general.flight.auto_enable")
				: config.getBoolean("general.flight.auto_fly", true);

		boolean autoFlyTimeReceived = config.contains("general.flight.enable_on_time_received")
				? config.getBoolean("general.flight.enable_on_time_received")
				: config.getBoolean("general.flight.auto_fly_time_received", false);

		boolean idleDrop = config.contains("general.idle.drop_player")
				? config.getBoolean("general.idle.drop_player")
				: config.getBoolean("general.idle.drop", false);
		boolean idleTimer = config.getBoolean("general.timer.idle", config.getBoolean("general.idle.timer", false));
		long idleThreshold = config.getLong("general.idle.threshold", 300);

		float defaultSpeed = (float) config.getDouble("general.flight.speed.default", 1.0);
		if (defaultSpeed <= 0) {
			defaultSpeed = (float) config.getDouble("general.flight.default_speed", 1.0);
		}
		if (defaultSpeed <= 0) {
			defaultSpeed = 1.0f;
		}
		boolean allowSpeedPreference = config.getBoolean("general.flight.speed.user_preference", true);
		int maxY = config.getInt("general.flight.maximum_height", 275);

		boolean damageCommand = config.contains("general.damage.on_command")
				? config.getBoolean("general.damage.on_command")
				: config.getBoolean("general.damage.command", false);
		boolean damageTime = config.contains("general.damage.out_of_time")
				? config.getBoolean("general.damage.out_of_time")
				: config.getBoolean("general.damage.time", false);
		boolean damageIdle = config.getBoolean("general.damage.idle", false);
		boolean damageCombat = config.getBoolean("general.damage.combat", false);
		boolean damageRegion = config.contains("general.damage.region")
				? config.getBoolean("general.damage.region")
				: config.getBoolean("general.damage.disabled_region", false);
		boolean damageWorld = config.contains("general.damage.world")
				? config.getBoolean("general.damage.world")
				: config.getBoolean("general.damage.disabled_world", false);
		boolean damageStruct = config.contains("general.damage.structure_proximity")
				? config.getBoolean("general.damage.structure_proximity")
				: config.getBoolean("general.damage.structure", false);

		double firstJoinTime = config.contains("general.bonus.first_join")
				? config.getDouble("general.bonus.first_join")
				: config.getDouble("general.time.first_join", 0.0);
		double legacyBonus = config.contains("general.bonus.daily_login") && config.isDouble("general.bonus.daily_login")
				? config.getDouble("general.bonus.daily_login")
				: config.getDouble("general.bonus.daily", 0.0);
		Map<String, Double> dailyBonus = new HashMap<>();
		ConfigurationSection csDaily = config.getConfigurationSection("general.bonus.daily_login");
		if (csDaily == null) {
			csDaily = config.getConfigurationSection("general.bonus.daily_permission");
		}
		if (csDaily != null) {
			for (String key : csDaily.getKeys(false)) {
				dailyBonus.put(key, csDaily.getDouble(key));
			}
		}

		long decayThreshold = config.contains("general.time_decay.threshold")
				? config.getLong("general.time_decay.threshold")
				: config.getLong("general.time.decay.threshold", 0);
		double decayAmount = config.contains("general.time_decay.seconds_lost")
				? config.getDouble("general.time_decay.seconds_lost")
				: config.getDouble("general.time.decay.amount", 0.0);

		return new GeneralConfig(
				permaTimer, groundTimer, creativeTimer, spectatorTimer,
				autoFly, autoFlyTimeReceived, idleDrop, idleTimer, idleThreshold,
				defaultSpeed, allowSpeedPreference, maxY,
				damageCommand, damageTime, damageIdle, damageCombat,
				damageRegion, damageWorld, damageStruct,
				firstJoinTime, legacyBonus, Collections.unmodifiableMap(dailyBonus),
				decayThreshold, decayAmount
		);
	}
}
