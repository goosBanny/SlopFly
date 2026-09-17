package com.moneybags.tempfly.storage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.moneybags.tempfly.util.Console;

/**
 * YAML flatfile implementation of UserRepository reading and writing data.yml.
 */
public class YamlUserRepository implements UserRepository {

	private final File file;
	private final FileConfiguration config;
	private final Object lock = new Object();
	private final ExecutorService executor;
	private final boolean externalExecutor;

	public YamlUserRepository(File file) {
		this(file, YamlConfiguration.loadConfiguration(file), null);
	}

	public YamlUserRepository(File file, FileConfiguration config) {
		this(file, config, null);
	}

	public YamlUserRepository(File file, FileConfiguration config, ExecutorService executor) {
		this.file = Objects.requireNonNull(file, "file cannot be null");
		this.config = config != null ? config : YamlConfiguration.loadConfiguration(file);
		if (executor != null) {
			this.executor = executor;
			this.externalExecutor = true;
		} else {
			this.executor = Executors.newSingleThreadExecutor(r -> {
				Thread t = new Thread(r, "TempFly-YamlRepository-Worker");
				t.setDaemon(true);
				return t;
			});
			this.externalExecutor = false;
		}
	}

	public File getFile() {
		return file;
	}

	public FileConfiguration getConfig() {
		return config;
	}

	private void saveConfigDirect() {
		synchronized (lock) {
			try {
				config.save(file);
			} catch (IOException e) {
				Console.severe("Failed to save YAML data file " + file.getName() + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private String pathOf(UUID uuid, String key) {
		return "players." + uuid.toString() + "." + key;
	}

	@Override
	public CompletableFuture<UserFlightData> loadUser(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> getUser(uuid), executor);
	}

	@Override
	public UserFlightData getUser(UUID uuid) {
		Objects.requireNonNull(uuid, "uuid cannot be null");
		synchronized (lock) {
			String base = "players." + uuid.toString();
			if (!config.contains(base)) {
				return UserFlightData.defaultFor(uuid);
			}
			double time = config.getDouble(base + ".time", 0.0);
			boolean logged = config.getBoolean(base + ".logged_in_flight", false);
			boolean compatLogged = config.getBoolean(base + ".compat_logged_in_flight", false);
			boolean protection = config.getBoolean(base + ".damage_protection", false);
			long dailyBonus = config.getLong(base + ".last_daily_bonus", 0L);
			String trail = config.getString(base + ".trail", null);
			boolean infinite = config.getBoolean(base + ".infinite", true);
			boolean bypass = config.getBoolean(base + ".bypass", true);
			double speed = config.getDouble(base + ".speed", -999.0);
			return new UserFlightData(uuid, time, logged, compatLogged, protection, dailyBonus, trail, infinite, bypass, speed);
		}
	}

	@Override
	public CompletableFuture<Void> saveUser(UserFlightData data) {
		Objects.requireNonNull(data, "data cannot be null");
		return CompletableFuture.runAsync(() -> {
			synchronized (lock) {
				String base = "players." + data.getUuid().toString();
				config.set(base + ".time", data.getTime());
				config.set(base + ".logged_in_flight", data.isLoggedInFlight());
				config.set(base + ".compat_logged_in_flight", data.isCompatLoggedInFlight());
				config.set(base + ".damage_protection", data.hasDamageProtection());
				config.set(base + ".last_daily_bonus", data.getLastDailyBonus());
				config.set(base + ".trail", data.getTrail());
				config.set(base + ".infinite", data.isInfinite());
				config.set(base + ".bypass", data.isBypass());
				config.set(base + ".speed", data.getSpeed());
				saveConfigDirect();
			}
		}, executor);
	}

	@Override
	public CompletableFuture<Void> saveUsers(Collection<UserFlightData> users) {
		if (users == null || users.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}
		return CompletableFuture.runAsync(() -> {
			synchronized (lock) {
				for (UserFlightData data : users) {
					String base = "players." + data.getUuid().toString();
					config.set(base + ".time", data.getTime());
					config.set(base + ".logged_in_flight", data.isLoggedInFlight());
					config.set(base + ".compat_logged_in_flight", data.isCompatLoggedInFlight());
					config.set(base + ".damage_protection", data.hasDamageProtection());
					config.set(base + ".last_daily_bonus", data.getLastDailyBonus());
					config.set(base + ".trail", data.getTrail());
					config.set(base + ".infinite", data.isInfinite());
					config.set(base + ".bypass", data.isBypass());
					config.set(base + ".speed", data.getSpeed());
				}
				saveConfigDirect();
			}
		}, executor);
	}

	@Override
	public CompletableFuture<Double> adjustTime(UUID uuid, double delta, double maxTime) {
		Objects.requireNonNull(uuid, "uuid cannot be null");
		return CompletableFuture.supplyAsync(() -> {
			synchronized (lock) {
				String path = pathOf(uuid, "time");
				double current = config.getDouble(path, 0.0);
				double remaining = Math.max(0.0, current + delta);
				if (maxTime > -1 && remaining > maxTime) {
					remaining = maxTime;
				}
				config.set(path, remaining);
				saveConfigDirect();
				return remaining;
			}
		}, executor);
	}

	@Override
	public CompletableFuture<Void> setTime(UUID uuid, double time) {
		Objects.requireNonNull(uuid, "uuid cannot be null");
		return CompletableFuture.runAsync(() -> {
			synchronized (lock) {
				config.set(pathOf(uuid, "time"), Math.max(0.0, time));
				saveConfigDirect();
			}
		}, executor);
	}

	@Override
	public CompletableFuture<Double> getTime(UUID uuid) {
		Objects.requireNonNull(uuid, "uuid cannot be null");
		return CompletableFuture.supplyAsync(() -> {
			synchronized (lock) {
				return config.getDouble(pathOf(uuid, "time"), 0.0);
			}
		}, executor);
	}

	@Override
	public CompletableFuture<Void> setInfinite(UUID uuid, boolean infinite) {
		return setProperty(uuid, "infinite", infinite);
	}

	@Override
	public CompletableFuture<Void> setBypass(UUID uuid, boolean bypass) {
		return setProperty(uuid, "bypass", bypass);
	}

	@Override
	public CompletableFuture<Void> setTrail(UUID uuid, String trail) {
		return setProperty(uuid, "trail", trail);
	}

	@Override
	public CompletableFuture<Void> setSpeed(UUID uuid, double speed) {
		double safeSpeed = (speed <= 0 && speed != -999.0) ? -999.0 : speed;
		return setProperty(uuid, "speed", safeSpeed);
	}

	@Override
	public CompletableFuture<Void> setFlightLogs(UUID uuid, boolean loggedInFlight, boolean compatLoggedInFlight) {
		return CompletableFuture.runAsync(() -> {
			synchronized (lock) {
				config.set(pathOf(uuid, "logged_in_flight"), loggedInFlight);
				config.set(pathOf(uuid, "compat_logged_in_flight"), compatLoggedInFlight);
				saveConfigDirect();
			}
		}, executor);
	}

	@Override
	public CompletableFuture<Void> setDailyBonus(UUID uuid, long timestamp) {
		return setProperty(uuid, "last_daily_bonus", timestamp);
	}

	private CompletableFuture<Void> setProperty(UUID uuid, String key, Object value) {
		Objects.requireNonNull(uuid, "uuid cannot be null");
		return CompletableFuture.runAsync(() -> {
			synchronized (lock) {
				config.set(pathOf(uuid, key), value);
				saveConfigDirect();
			}
		}, executor);
	}

	@Override
	public void flush() {
		synchronized (lock) {
			saveConfigDirect();
		}
	}

	@Override
	public void close() {
		flush();
		if (!externalExecutor && executor != null) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}
}
