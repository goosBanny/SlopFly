package com.moneybags.tempfly.storage;

import com.moneybags.tempfly.util.data.DataBridge.StorageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SqlUserRepositoryTest {

	private org.sqlite.SQLiteDataSource dataSource;
	private SqlUserRepository repository;

	@BeforeEach
	public void setUp(@TempDir File tempDir) throws Exception {
		File dbFile = new File(tempDir, "test_repo.db");
		Class.forName("org.sqlite.JDBC");
		dataSource = new org.sqlite.SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());

		try (Connection conn = dataSource.getConnection()) {
			try (InputStream in = getClass().getClassLoader().getResourceAsStream("dbsetup.sql")) {
				assertNotNull(in, "dbsetup.sql resource must exist");
				String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
				String[] statements = sql.split(";");
				try (Statement stmt = conn.createStatement()) {
					for (String statement : statements) {
						if (!statement.isBlank()) {
							stmt.execute(statement.trim());
						}
					}
				}
			}
		}

		repository = new SqlUserRepository(dataSource, StorageType.SQLITE);
	}

	@AfterEach
	public void tearDown() {
		if (repository != null) {
			repository.close();
		}
	}

	@Test
	public void testLoadAndSaveUser() throws Exception {
		UUID uuid = UUID.randomUUID();

		// Initially non-existent user should return defaults
		UserFlightData initial = repository.loadUser(uuid).get();
		assertEquals(uuid, initial.getUuid());
		assertEquals(0.0, initial.getTime(), 0.001);

		// Mutate and save
		initial.setTime(1800.0);
		initial.setTrail("MAGIC");
		initial.setSpeed(2.5);
		initial.setInfinite(false);
		initial.setBypass(false);
		initial.setLoggedInFlight(true);
		initial.setLastDailyBonus(55555L);
		repository.saveUser(initial).get();

		// Reload
		UserFlightData reloaded = repository.loadUser(uuid).get();
		assertEquals(1800.0, reloaded.getTime(), 0.001);
		assertEquals("MAGIC", reloaded.getTrail());
		assertEquals(2.5, reloaded.getSpeed(), 0.001);
		assertFalse(reloaded.isInfinite());
		assertFalse(reloaded.isBypass());
		assertTrue(reloaded.isLoggedInFlight());
		assertEquals(55555L, reloaded.getLastDailyBonus());
	}

	@Test
	public void testAtomicAdjustTime() throws Exception {
		UUID uuid = UUID.randomUUID();

		// Set initial time
		repository.setTime(uuid, 100.0).get();
		assertEquals(100.0, repository.getTime(uuid).get(), 0.001);

		// Positive adjustment
		double newTime = repository.adjustTime(uuid, 50.0, -1).get();
		assertEquals(150.0, newTime, 0.001);
		assertEquals(150.0, repository.getTime(uuid).get(), 0.001);

		// Negative adjustment
		newTime = repository.adjustTime(uuid, -40.0, -1).get();
		assertEquals(110.0, newTime, 0.001);
		assertEquals(110.0, repository.getTime(uuid).get(), 0.001);

		// Decrement beyond zero should clamp to 0.0
		newTime = repository.adjustTime(uuid, -200.0, -1).get();
		assertEquals(0.0, newTime, 0.001);
		assertEquals(0.0, repository.getTime(uuid).get(), 0.001);

		// Adjustment with maxTime cap
		newTime = repository.adjustTime(uuid, 500.0, 300.0).get();
		assertEquals(300.0, newTime, 0.001);
		assertEquals(300.0, repository.getTime(uuid).get(), 0.001);
	}

	@Test
	public void testBatchSaveUsers() throws Exception {
		UUID u1 = UUID.randomUUID();
		UUID u2 = UUID.randomUUID();

		UserFlightData user1 = new UserFlightData(u1, 500.0, true, false, false, 0L, "FLAME", true, true, 1.2);
		UserFlightData user2 = new UserFlightData(u2, 1200.0, false, true, true, 1234L, "HEART", false, false, 2.0);

		repository.saveUsers(Arrays.asList(user1, user2)).get();

		UserFlightData r1 = repository.loadUser(u1).get();
		UserFlightData r2 = repository.loadUser(u2).get();

		assertEquals(500.0, r1.getTime(), 0.001);
		assertEquals("FLAME", r1.getTrail());
		assertTrue(r1.isLoggedInFlight());

		assertEquals(1200.0, r2.getTime(), 0.001);
		assertEquals("HEART", r2.getTrail());
		assertFalse(r2.isInfinite());
	}

	@Test
	public void testIndividualFieldUpdates() throws Exception {
		UUID uuid = UUID.randomUUID();

		repository.setInfinite(uuid, false).get();
		assertFalse(repository.loadUser(uuid).get().isInfinite());

		repository.setBypass(uuid, false).get();
		assertFalse(repository.loadUser(uuid).get().isBypass());

		repository.setTrail(uuid, "EXPLOSION").get();
		assertEquals("EXPLOSION", repository.loadUser(uuid).get().getTrail());

		repository.setTrail(uuid, null).get();
		assertNull(repository.loadUser(uuid).get().getTrail());

		repository.setSpeed(uuid, 3.0).get();
		assertEquals(3.0, repository.loadUser(uuid).get().getSpeed(), 0.001);

		repository.setFlightLogs(uuid, true, true).get();
		UserFlightData data = repository.loadUser(uuid).get();
		assertTrue(data.isLoggedInFlight());
		assertTrue(data.isCompatLoggedInFlight());

		repository.setDailyBonus(uuid, 987654321L).get();
		assertEquals(987654321L, repository.loadUser(uuid).get().getLastDailyBonus());
	}
}
