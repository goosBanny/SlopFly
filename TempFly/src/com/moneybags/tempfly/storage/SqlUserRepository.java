package com.moneybags.tempfly.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

import com.moneybags.tempfly.util.Console;
import com.moneybags.tempfly.util.data.DataBridge.StorageType;

/**
 * SQL-backed implementation of UserRepository supporting SQLite and MySQL.
 */
public class SqlUserRepository implements UserRepository {

	private final DataSource dataSource;
	private final StorageType storageType;
	private final ExecutorService executor;
	private final boolean externalExecutor;

	public SqlUserRepository(DataSource dataSource, StorageType storageType) {
		this(dataSource, storageType, null);
	}

	public SqlUserRepository(DataSource dataSource, StorageType storageType, ExecutorService executor) {
		this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
		this.storageType = Objects.requireNonNull(storageType, "storageType cannot be null");
		if (executor != null) {
			this.executor = executor;
			this.externalExecutor = true;
		} else {
			this.executor = Executors.newSingleThreadExecutor(r -> {
				Thread t = new Thread(r, "TempFly-SqlRepository-Worker");
				t.setDaemon(true);
				return t;
			});
			this.externalExecutor = false;
		}
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public StorageType getStorageType() {
		return storageType;
	}

	private boolean isSqlite() {
		return storageType == StorageType.SQLITE;
	}

	private String getInsertIgnoreSql() {
		return isSqlite()
				? "INSERT OR IGNORE INTO tempfly_data(uuid) VALUES(?)"
				: "INSERT IGNORE INTO tempfly_data(uuid) VALUES(?)";
	}

	private void ensureRowExists(Connection conn, UUID uuid) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement(getInsertIgnoreSql())) {
			ps.setString(1, uuid.toString());
			ps.executeUpdate();
		}
	}

	@Override
	public CompletableFuture<UserFlightData> loadUser(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> getUser(uuid), executor);
	}

	@Override
	public UserFlightData getUser(UUID uuid) {
		Objects.requireNonNull(uuid, "uuid cannot be null");
		try (Connection conn = dataSource.getConnection()) {
			ensureRowExists(conn, uuid);

			String sql = "SELECT player_time, logged_in_flight, compat_logged_in_flight, " +
					"damage_protection, last_daily_bonus, trail, infinite, bypass, speed " +
					"FROM tempfly_data WHERE uuid = ?";
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, uuid.toString());
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						double time = rs.getDouble("player_time");
						boolean logged = rs.getBoolean("logged_in_flight");
						boolean compatLogged = rs.getBoolean("compat_logged_in_flight");
						boolean protection = rs.getBoolean("damage_protection");
						long dailyBonus = rs.getLong("last_daily_bonus");
						String trail = rs.getString("trail");
						boolean infinite = rs.getBoolean("infinite");
						boolean bypass = rs.getBoolean("bypass");
						double speed = rs.getDouble("speed");
						return new UserFlightData(uuid, time, logged, compatLogged, protection, dailyBonus, trail, infinite, bypass, speed);
					}
				}
			}
		} catch (SQLException e) {
			Console.severe("Failed to load user flight data for " + uuid + ": " + e.getMessage());
			e.printStackTrace();
		}
		return UserFlightData.defaultFor(uuid);
	}

	@Override
	public CompletableFuture<Void> saveUser(UserFlightData data) {
		Objects.requireNonNull(data, "data cannot be null");
		return CompletableFuture.runAsync(() -> {
			try (Connection conn = dataSource.getConnection()) {
				ensureRowExists(conn, data.getUuid());

				String sql = "UPDATE tempfly_data SET player_time = ?, logged_in_flight = ?, " +
						"compat_logged_in_flight = ?, damage_protection = ?, last_daily_bonus = ?, " +
						"trail = ?, infinite = ?, bypass = ?, speed = ? WHERE uuid = ?";
				try (PreparedStatement ps = conn.prepareStatement(sql)) {
					ps.setDouble(1, data.getTime());
					ps.setBoolean(2, data.isLoggedInFlight());
					ps.setBoolean(3, data.isCompatLoggedInFlight());
					ps.setBoolean(4, data.hasDamageProtection());
					ps.setLong(5, data.getLastDailyBonus());
					if (data.getTrail() != null) {
						ps.setString(6, data.getTrail());
					} else {
						ps.setNull(6, Types.VARCHAR);
					}
					ps.setBoolean(7, data.isInfinite());
					ps.setBoolean(8, data.isBypass());
					ps.setDouble(9, data.getSpeed());
					ps.setString(10, data.getUuid().toString());
					ps.executeUpdate();
				}
			} catch (SQLException e) {
				Console.severe("Failed to save user flight data for " + data.getUuid() + ": " + e.getMessage());
				e.printStackTrace();
			}
		}, executor);
	}

	@Override
	public CompletableFuture<Void> saveUsers(Collection<UserFlightData> users) {
		if (users == null || users.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}
		return CompletableFuture.runAsync(() -> {
			try (Connection conn = dataSource.getConnection()) {
				conn.setAutoCommit(false);
				try {
					for (UserFlightData user : users) {
						ensureRowExists(conn, user.getUuid());
					}

					String sql = "UPDATE tempfly_data SET player_time = ?, logged_in_flight = ?, " +
							"compat_logged_in_flight = ?, damage_protection = ?, last_daily_bonus = ?, " +
							"trail = ?, infinite = ?, bypass = ?, speed = ? WHERE uuid = ?";
					try (PreparedStatement ps = conn.prepareStatement(sql)) {
						for (UserFlightData user : users) {
							ps.setDouble(1, user.getTime());
							ps.setBoolean(2, user.isLoggedInFlight());
							ps.setBoolean(3, user.isCompatLoggedInFlight());
							ps.setBoolean(4, user.hasDamageProtection());
							ps.setLong(5, user.getLastDailyBonus());
							if (user.getTrail() != null) {
								ps.setString(6, user.getTrail());
							} else {
								ps.setNull(6, Types.VARCHAR);
							}
							ps.setBoolean(7, user.isInfinite());
							ps.setBoolean(8, user.isBypass());
							ps.setDouble(9, user.getSpeed());
							ps.setString(10, user.getUuid().toString());
							ps.addBatch();
						}
						ps.executeBatch();
					}
					conn.commit();
				} catch (SQLException ex) {
					conn.rollback();
					throw ex;
				} finally {
					conn.setAutoCommit(true);
				}
			} catch (SQLException e) {
				Console.severe("Failed to batch save user flight data: " + e.getMessage());
				e.printStackTrace();
			}
		}, executor);
	}

	@Override
	public CompletableFuture<Double> adjustTime(UUID uuid, double delta, double maxTime) {
		Objects.requireNonNull(uuid, "uuid cannot be null");
		return CompletableFuture.supplyAsync(() -> {
			try (Connection conn = dataSource.getConnection()) {
				ensureRowExists(conn, uuid);

				String updateSql;
				if (maxTime > -1) {
					updateSql = "UPDATE tempfly_data SET player_time = CASE " +
							"WHEN (player_time + ?) < 0 THEN 0 " +
							"WHEN (player_time + ?) > ? THEN ? " +
							"ELSE (player_time + ?) END WHERE uuid = ?";
				} else {
					updateSql = "UPDATE tempfly_data SET player_time = CASE " +
							"WHEN (player_time + ?) < 0 THEN 0 " +
							"ELSE (player_time + ?) END WHERE uuid = ?";
				}

				try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
					if (maxTime > -1) {
						ps.setDouble(1, delta);
						ps.setDouble(2, delta);
						ps.setDouble(3, maxTime);
						ps.setDouble(4, maxTime);
						ps.setDouble(5, delta);
						ps.setString(6, uuid.toString());
					} else {
						ps.setDouble(1, delta);
						ps.setDouble(2, delta);
						ps.setString(3, uuid.toString());
					}
					ps.executeUpdate();
				}

				try (PreparedStatement psSelect = conn.prepareStatement("SELECT player_time FROM tempfly_data WHERE uuid = ?")) {
					psSelect.setString(1, uuid.toString());
					try (ResultSet rs = psSelect.executeQuery()) {
						if (rs.next()) {
							return rs.getDouble("player_time");
						}
					}
				}
			} catch (SQLException e) {
				Console.severe("Failed to adjust flight time for " + uuid + ": " + e.getMessage());
				e.printStackTrace();
			}
			return 0.0;
		}, executor);
	}

	@Override
	public CompletableFuture<Void> setTime(UUID uuid, double time) {
		Objects.requireNonNull(uuid, "uuid cannot be null");
		double safeTime = Math.max(0.0, time);
		return CompletableFuture.runAsync(() -> {
			try (Connection conn = dataSource.getConnection()) {
				ensureRowExists(conn, uuid);
				try (PreparedStatement ps = conn.prepareStatement("UPDATE tempfly_data SET player_time = ? WHERE uuid = ?")) {
					ps.setDouble(1, safeTime);
					ps.setString(2, uuid.toString());
					ps.executeUpdate();
				}
			} catch (SQLException e) {
				Console.severe("Failed to set flight time for " + uuid + ": " + e.getMessage());
				e.printStackTrace();
			}
		}, executor);
	}

	@Override
	public CompletableFuture<Double> getTime(UUID uuid) {
		Objects.requireNonNull(uuid, "uuid cannot be null");
		return CompletableFuture.supplyAsync(() -> {
			try (Connection conn = dataSource.getConnection()) {
				ensureRowExists(conn, uuid);
				try (PreparedStatement ps = conn.prepareStatement("SELECT player_time FROM tempfly_data WHERE uuid = ?")) {
					ps.setString(1, uuid.toString());
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							return rs.getDouble("player_time");
						}
					}
				}
			} catch (SQLException e) {
				Console.severe("Failed to query flight time for " + uuid + ": " + e.getMessage());
				e.printStackTrace();
			}
			return 0.0;
		}, executor);
	}

	@Override
	public CompletableFuture<Void> setInfinite(UUID uuid, boolean infinite) {
		return executeSingleUpdate(uuid, "UPDATE tempfly_data SET infinite = ? WHERE uuid = ?", (ps) -> ps.setBoolean(1, infinite));
	}

	@Override
	public CompletableFuture<Void> setBypass(UUID uuid, boolean bypass) {
		return executeSingleUpdate(uuid, "UPDATE tempfly_data SET bypass = ? WHERE uuid = ?", (ps) -> ps.setBoolean(1, bypass));
	}

	@Override
	public CompletableFuture<Void> setTrail(UUID uuid, String trail) {
		return executeSingleUpdate(uuid, "UPDATE tempfly_data SET trail = ? WHERE uuid = ?", (ps) -> {
			if (trail != null) {
				ps.setString(1, trail);
			} else {
				ps.setNull(1, Types.VARCHAR);
			}
		});
	}

	@Override
	public CompletableFuture<Void> setSpeed(UUID uuid, double speed) {
		double safeSpeed = (speed <= 0 && speed != -999.0) ? -999.0 : speed;
		return executeSingleUpdate(uuid, "UPDATE tempfly_data SET speed = ? WHERE uuid = ?", (ps) -> ps.setDouble(1, safeSpeed));
	}

	@Override
	public CompletableFuture<Void> setFlightLogs(UUID uuid, boolean loggedInFlight, boolean compatLoggedInFlight) {
		return executeSingleUpdate(uuid, "UPDATE tempfly_data SET logged_in_flight = ?, compat_logged_in_flight = ? WHERE uuid = ?", (ps) -> {
			ps.setBoolean(1, loggedInFlight);
			ps.setBoolean(2, compatLoggedInFlight);
		});
	}

	@Override
	public CompletableFuture<Void> setDailyBonus(UUID uuid, long timestamp) {
		return executeSingleUpdate(uuid, "UPDATE tempfly_data SET last_daily_bonus = ? WHERE uuid = ?", (ps) -> ps.setLong(1, timestamp));
	}

	@FunctionalInterface
	private interface StatementBinder {
		void bind(PreparedStatement ps) throws SQLException;
	}

	private CompletableFuture<Void> executeSingleUpdate(UUID uuid, String sql, StatementBinder binder) {
		Objects.requireNonNull(uuid, "uuid cannot be null");
		return CompletableFuture.runAsync(() -> {
			try (Connection conn = dataSource.getConnection()) {
				ensureRowExists(conn, uuid);
				try (PreparedStatement ps = conn.prepareStatement(sql)) {
					binder.bind(ps);
					int paramCount = ps.getParameterMetaData().getParameterCount();
					ps.setString(paramCount, uuid.toString());
					ps.executeUpdate();
				}
			} catch (SQLException e) {
				Console.severe("Failed to execute update for " + uuid + ": " + e.getMessage());
				e.printStackTrace();
			}
		}, executor);
	}

	@Override
	public void flush() {
		// All writes are direct to DB or through executor. Await queued tasks if any.
	}

	@Override
	public void close() {
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
