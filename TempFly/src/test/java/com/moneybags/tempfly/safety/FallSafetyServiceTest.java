package com.moneybags.tempfly.safety;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FallSafetyServiceTest {

	private FallSafetyService fallSafetyService;

	@BeforeEach
	public void setUp() {
		fallSafetyService = new FallSafetyService(null);
	}

	@Test
	public void testAddAndCheckProtection() {
		UUID uuid = UUID.randomUUID();
		assertFalse(fallSafetyService.hasProtection(uuid));

		fallSafetyService.addProtection(uuid, 60L);
		assertTrue(fallSafetyService.hasProtection(uuid));
	}

	@Test
	public void testRemoveProtection() {
		UUID uuid = UUID.randomUUID();
		fallSafetyService.addProtection(uuid, 60L);
		assertTrue(fallSafetyService.hasProtection(uuid));

		fallSafetyService.removeProtection(uuid);
		assertFalse(fallSafetyService.hasProtection(uuid));
	}

	@Test
	public void testClearAllProtections() {
		UUID u1 = UUID.randomUUID();
		UUID u2 = UUID.randomUUID();
		fallSafetyService.addProtection(u1, 60L);
		fallSafetyService.addProtection(u2, 60L);

		assertTrue(fallSafetyService.hasProtection(u1));
		assertTrue(fallSafetyService.hasProtection(u2));

		fallSafetyService.clearAll();
		assertFalse(fallSafetyService.hasProtection(u1));
		assertFalse(fallSafetyService.hasProtection(u2));
	}

	@Test
	public void testNullSafety() {
		assertFalse(fallSafetyService.hasProtection(null));
		fallSafetyService.addProtection((UUID) null, 60L);
		fallSafetyService.removeProtection(null);
	}
}
