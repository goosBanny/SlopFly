package com.moneybags.tempfly.config;

import org.bukkit.configuration.file.FileConfiguration;

public record StorageConfig(
		String storageType,
		String mysqlHost,
		int mysqlPort,
		String mysqlDatabase,
		String mysqlUser,
		String mysqlPassword,
		int backupMinutes
) {
	public static StorageConfig from(FileConfiguration config) {
		String storageType = "SQLITE";
		if (config.contains("system.storage-type")) {
			storageType = config.getString("system.storage-type", "SQLITE").toUpperCase();
		} else if (config.contains("system.storage_type")) {
			storageType = config.getString("system.storage_type", "SQLITE").toUpperCase();
		} else if (config.getBoolean("system.mysql.enabled", false)) {
			storageType = "MYSQL";
		}

		if (!"SQLITE".equals(storageType) && !"MYSQL".equals(storageType) && !"YAML".equals(storageType)) {
			storageType = "SQLITE";
		}

		String mysqlHost = config.getString("system.mysql.host", "127.0.0.1");
		int mysqlPort = config.getInt("system.mysql.port", 3306);
		String mysqlDatabase = config.getString("system.mysql.name", "tempfly");
		String mysqlUser = config.getString("system.mysql.user", "root");
		String mysqlPassword = config.getString("system.mysql.pass", "");
		int backupMinutes = config.getInt("system.backup", 5);

		return new StorageConfig(
				storageType, mysqlHost, mysqlPort,
				mysqlDatabase, mysqlUser, mysqlPassword, backupMinutes
		);
	}
}
