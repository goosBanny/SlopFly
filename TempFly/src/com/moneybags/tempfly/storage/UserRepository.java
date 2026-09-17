package com.moneybags.tempfly.storage;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage repository interface managing persistent user flight records.
 */
public interface UserRepository extends AutoCloseable {

	/**
	 * Asynchronously loads persistent flight data for the given player.
	 *
	 * @param uuid The player UUID
	 * @return A future resolving to the loaded or newly initialized UserFlightData
	 */
	CompletableFuture<UserFlightData> loadUser(UUID uuid);

	/**
	 * Synchronously retrieves persistent flight data for the given player.
	 *
	 * @param uuid The player UUID
	 * @return The UserFlightData instance
	 */
	UserFlightData getUser(UUID uuid);

	/**
	 * Asynchronously saves the user's flight data.
	 *
	 * @param data The user data to save
	 * @return A future completing when save is finished
	 */
	CompletableFuture<Void> saveUser(UserFlightData data);

	/**
	 * Asynchronously saves multiple user data records in batch.
	 *
	 * @param users Collection of user data records to save
	 * @return A future completing when all records are saved
	 */
	CompletableFuture<Void> saveUsers(Collection<UserFlightData> users);

	/**
	 * Atomically adjusts a user's flight time in storage.
	 *
	 * @param uuid The player UUID
	 * @param delta The delta in seconds (can be positive or negative)
	 * @param maxTime Maximum allowed flight time cap (-1 if uncapped)
	 * @return A future resolving to the updated flight time
	 */
	CompletableFuture<Double> adjustTime(UUID uuid, double delta, double maxTime);

	/**
	 * Directly updates a user's flight time in storage.
	 *
	 * @param uuid The player UUID
	 * @param time The new flight time in seconds
	 * @return A future completing when update is finished
	 */
	CompletableFuture<Void> setTime(UUID uuid, double time);

	/**
	 * Asynchronously queries the user's current flight time.
	 *
	 * @param uuid The player UUID
	 * @return A future resolving to the player's flight time in seconds
	 */
	CompletableFuture<Double> getTime(UUID uuid);

	/**
	 * Updates the player's infinite flight preference.
	 *
	 * @param uuid The player UUID
	 * @param infinite True if infinite flight is enabled
	 * @return A future completing when update is finished
	 */
	CompletableFuture<Void> setInfinite(UUID uuid, boolean infinite);

	/**
	 * Updates the player's flight requirement bypass preference.
	 *
	 * @param uuid The player UUID
	 * @param bypass True if bypass is enabled
	 * @return A future completing when update is finished
	 */
	CompletableFuture<Void> setBypass(UUID uuid, boolean bypass);

	/**
	 * Updates the player's selected particle trail.
	 *
	 * @param uuid The player UUID
	 * @param trail The trail identifier, or null/empty string for none
	 * @return A future completing when update is finished
	 */
	CompletableFuture<Void> setTrail(UUID uuid, String trail);

	/**
	 * Updates the player's preferred flight speed.
	 *
	 * @param uuid The player UUID
	 * @param speed The preferred flight speed (-999.0 for default)
	 * @return A future completing when update is finished
	 */
	CompletableFuture<Void> setSpeed(UUID uuid, double speed);

	/**
	 * Updates flight log flags on disconnect.
	 *
	 * @param uuid The player UUID
	 * @param loggedInFlight Whether the player was flying with TempFly
	 * @param compatLoggedInFlight Whether the player was flying with other plugins
	 * @return A future completing when update is finished
	 */
	CompletableFuture<Void> setFlightLogs(UUID uuid, boolean loggedInFlight, boolean compatLoggedInFlight);

	/**
	 * Updates the timestamp of the last claimed daily bonus.
	 *
	 * @param uuid The player UUID
	 * @param timestamp Epoch timestamp of claim
	 * @return A future completing when update is finished
	 */
	CompletableFuture<Void> setDailyBonus(UUID uuid, long timestamp);

	/**
	 * Creates a UserRepository backed by the configured DataBridge storage backend.
	 *
	 * @param bridge The active DataBridge
	 * @return A SqlUserRepository or YamlUserRepository instance
	 */
	static UserRepository create(com.moneybags.tempfly.util.data.DataBridge bridge) {
		if (bridge != null && bridge.hasSqlEnabled()) {
			return new SqlUserRepository(bridge.getDataSource(), bridge.getStorageType());
		} else if (bridge != null && bridge.getDataFile() != null) {
			return new YamlUserRepository(bridge.getDataFile(), bridge.getDataConfiguration());
		}
		throw new IllegalArgumentException("Cannot create UserRepository from uninitialized DataBridge");
	}

	/**
	 * Synchronously flushes all pending/staged changes to persistent storage.
	 */
	void flush();

	/**
	 * Closes the repository and releases any resources or executors.
	 */
	@Override
	void close();
}
