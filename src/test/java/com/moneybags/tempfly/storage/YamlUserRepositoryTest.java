package com.moneybags.tempfly.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class YamlUserRepositoryTest {

	private File yamlFile;
	private YamlUserRepository repository;

	@BeforeEach
	public void setUp(@TempDir File tempDir) {
		yamlFile = new File(tempDir, "test_data.yml");
		YamlConfiguration config = new YamlConfiguration();
		repository = new YamlUserRepository(yamlFile, config);
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

		UserFlightData initial = repository.loadUser(uuid).get();
		assertEquals(uuid, initial.getUuid());
		assertEquals(0.0, initial.getTime(), 0.001);

		initial.setTime(2400.0);
		initial.setTrail("VORTEX");
		initial.setSpeed(1.8);
		initial.setInfinite(false);
		initial.setBypass(false);
		initial.setLoggedInFlight(true);
		initial.setLastDailyBonus(999999L);
		repository.saveUser(initial).get();

		UserFlightData reloaded = repository.loadUser(uuid).get();
		assertEquals(2400.0, reloaded.getTime(), 0.001);
		assertEquals("VORTEX", reloaded.getTrail());
		assertEquals(1.8, reloaded.getSpeed(), 0.001);
		assertFalse(reloaded.isInfinite());
		assertFalse(reloaded.isBypass());
		assertTrue(reloaded.isLoggedInFlight());
		assertEquals(999999L, reloaded.getLastDailyBonus());
	}

	@Test
	public void testAtomicAdjustTime() throws Exception {
		UUID uuid = UUID.randomUUID();

		repository.setTime(uuid, 200.0).get();
		assertEquals(200.0, repository.getTime(uuid).get(), 0.001);

		double newTime = repository.adjustTime(uuid, 75.0, -1).get();
		assertEquals(275.0, newTime, 0.001);

		newTime = repository.adjustTime(uuid, -100.0, -1).get();
		assertEquals(175.0, newTime, 0.001);

		newTime = repository.adjustTime(uuid, -300.0, -1).get();
		assertEquals(0.0, newTime, 0.001);

		newTime = repository.adjustTime(uuid, 1000.0, 500.0).get();
		assertEquals(500.0, newTime, 0.001);
	}

	@Test
	public void testBatchSaveUsers() throws Exception {
		UUID u1 = UUID.randomUUID();
		UUID u2 = UUID.randomUUID();

		UserFlightData user1 = new UserFlightData(u1, 300.0, true, false, false, 0L, "PORTAL", true, true, 1.0);
		UserFlightData user2 = new UserFlightData(u2, 600.0, false, false, false, 0L, "CLOUD", false, true, 2.0);

		repository.saveUsers(Arrays.asList(user1, user2)).get();

		UserFlightData r1 = repository.loadUser(u1).get();
		UserFlightData r2 = repository.loadUser(u2).get();

		assertEquals(300.0, r1.getTime(), 0.001);
		assertEquals("PORTAL", r1.getTrail());

		assertEquals(600.0, r2.getTime(), 0.001);
		assertEquals("CLOUD", r2.getTrail());
		assertFalse(r2.isInfinite());
	}

	@Test
	public void testIndividualFieldUpdates() throws Exception {
		UUID uuid = UUID.randomUUID();

		repository.setInfinite(uuid, false).get();
		assertFalse(repository.loadUser(uuid).get().isInfinite());

		repository.setBypass(uuid, false).get();
		assertFalse(repository.loadUser(uuid).get().isBypass());

		repository.setTrail(uuid, "RAINBOW").get();
		assertEquals("RAINBOW", repository.loadUser(uuid).get().getTrail());

		repository.setSpeed(uuid, 2.2).get();
		assertEquals(2.2, repository.loadUser(uuid).get().getSpeed(), 0.001);

		repository.setFlightLogs(uuid, true, false).get();
		UserFlightData data = repository.loadUser(uuid).get();
		assertTrue(data.isLoggedInFlight());
		assertFalse(data.isCompatLoggedInFlight());

		repository.setDailyBonus(uuid, 11223344L).get();
		assertEquals(11223344L, repository.loadUser(uuid).get().getLastDailyBonus());
	}
}
