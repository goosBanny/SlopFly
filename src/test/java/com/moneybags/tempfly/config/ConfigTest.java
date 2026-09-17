package com.moneybags.tempfly.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

public class ConfigTest {

	@Test
	public void testGeneralConfigDefaults() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("general.flight.speed.default", 2.5);
		yaml.set("general.flight.maximum_height", 320);
		yaml.set("general.damage.combat", true);

		GeneralConfig config = GeneralConfig.from(yaml);

		assertEquals(2.5f, config.defaultSpeed(), 0.001f);
		assertEquals(320, config.maxY());
		assertTrue(config.damageCombat());
		assertFalse(config.permaTimer());
	}

	@Test
	public void testCombatConfigDefaults() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("general.combat.pvp_tag", 15);
		yaml.set("general.combat.pve_tag", 8);
		yaml.set("general.combat.attack_mob", false);
		yaml.set("general.combat.attack_player", true);

		CombatConfig config = CombatConfig.from(yaml);

		assertEquals(15, config.tagPvp());
		assertEquals(300, config.tagPvpTicks());
		assertEquals(8, config.tagPve());
		assertEquals(160, config.tagPveTicks());
		assertFalse(config.attackMob());
		assertTrue(config.attackPlayer());
	}

	@Test
	public void testStorageConfigParsing() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("system.storage-type", "MYSQL");
		yaml.set("system.mysql.host", "db.example.com");
		yaml.set("system.mysql.port", 3307);
		yaml.set("system.mysql.name", "fly_db");
		yaml.set("system.mysql.user", "fly_admin");
		yaml.set("system.mysql.pass", "secret123");
		yaml.set("system.backup", 10);

		StorageConfig config = StorageConfig.from(yaml);

		assertEquals("MYSQL", config.storageType());
		assertEquals("db.example.com", config.mysqlHost());
		assertEquals(3307, config.mysqlPort());
		assertEquals("fly_db", config.mysqlDatabase());
		assertEquals("fly_admin", config.mysqlUser());
		assertEquals("secret123", config.mysqlPassword());
		assertEquals(10, config.backupMinutes());
	}

	@Test
	public void testAestheticConfigSymbols() {
		YamlConfiguration configYaml = new YamlConfiguration();
		configYaml.set("aesthetic.action_bar.enabled", true);
		configYaml.set("aesthetic.action_bar.text", "<gold>Flight: <yellow>{FORMATTED_TIME}");
		configYaml.set("aesthetic.identifier.particles.enabled", true);
		configYaml.set("aesthetic.identifier.particles.type", "FLAME");
		configYaml.set("aesthetic.identifier.tab_list.enabled", true);
		configYaml.set("aesthetic.identifier.tab_list.name", "&a[FLY] {PLAYER}");

		YamlConfiguration langYaml = new YamlConfiguration();
		langYaml.set("aesthetic.symbols.infinity", "INF");

		AestheticConfig config = AestheticConfig.from(configYaml, langYaml);

		assertTrue(config.actionBar());
		assertEquals("<gold>Flight: <yellow>{FORMATTED_TIME}", config.actionText());
		assertEquals("INF", config.infinitySymbol());
		assertTrue(config.particles());
		assertEquals("FLAME", config.particleType());
		assertTrue(config.listDynamic());
		assertEquals("&a[FLY] {PLAYER}", config.listName());
	}
}
