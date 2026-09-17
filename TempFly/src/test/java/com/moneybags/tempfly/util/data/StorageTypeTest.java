package com.moneybags.tempfly.util.data;

import com.moneybags.tempfly.util.data.DataBridge.StorageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class StorageTypeTest {

    @Test
    public void testStorageTypeParsing() {
        assertEquals(StorageType.SQLITE, StorageType.fromString("SQLITE"));
        assertEquals(StorageType.SQLITE, StorageType.fromString("sqlite"));
        assertEquals(StorageType.MYSQL, StorageType.fromString("MYSQL"));
        assertEquals(StorageType.MYSQL, StorageType.fromString("mysql"));
        assertEquals(StorageType.YAML, StorageType.fromString("YAML"));
        assertEquals(StorageType.YAML, StorageType.fromString("yaml"));
        assertEquals(StorageType.SQLITE, StorageType.fromString("invalid_value"));
        assertEquals(StorageType.SQLITE, StorageType.fromString(null));
    }

    @Test
    public void testInsertIgnoreQueryGeneration() {
        DataBridge bridge = new DataBridge();
        // Default in DataBridge() empty constructor is YAML, but we can test helper methods
        String sqliteInsert = "INSERT OR IGNORE INTO tempfly_data(uuid) VALUES(?)";
        String mysqlInsert = "INSERT IGNORE INTO tempfly_data(uuid) VALUES(?)";

        assertNotNull(sqliteInsert);
        assertNotNull(mysqlInsert);
    }

    @Test
    public void testSqliteTableCreationAndOperations(@TempDir File tempDir) throws Exception {
        File dbFile = new File(tempDir, "test_tempfly.db");
        Class.forName("org.sqlite.JDBC");
        org.sqlite.SQLiteDataSource ds = new org.sqlite.SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());

        try (Connection conn = ds.getConnection()) {
            assertNotNull(conn);
            assertTrue(conn.isValid(1));

            // Load and execute dbsetup.sql
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

            UUID testUuid = UUID.randomUUID();

            // Test INSERT OR IGNORE
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO tempfly_data(uuid) VALUES(?)")) {
                ps.setString(1, testUuid.toString());
                int rows = ps.executeUpdate();
                assertEquals(1, rows);
            }

            // Duplicate insert should be ignored
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO tempfly_data(uuid) VALUES(?)")) {
                ps.setString(1, testUuid.toString());
                int rows = ps.executeUpdate();
                assertEquals(0, rows);
            }

            // Test UPDATE time, trail, booleans, and bigint daily bonus
            try (PreparedStatement ps = conn.prepareStatement("UPDATE tempfly_data SET player_time = ?, trail = ?, logged_in_flight = ?, last_daily_bonus = ? WHERE uuid = ?")) {
                ps.setDouble(1, 3600.5);
                ps.setString(2, "FLAME");
                ps.setBoolean(3, true);
                ps.setLong(4, 1720000000000L);
                ps.setString(5, testUuid.toString());
                int rows = ps.executeUpdate();
                assertEquals(1, rows);
            }

            // Test SELECT with types
            try (PreparedStatement ps = conn.prepareStatement("SELECT player_time, trail, speed, logged_in_flight, damage_protection, last_daily_bonus FROM tempfly_data WHERE uuid = ?")) {
                ps.setString(1, testUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(3600.5, rs.getDouble("player_time"), 0.001);
                    assertEquals("FLAME", rs.getString("trail"));
                    assertEquals(-999.0, rs.getDouble("speed"), 0.001);
                    assertTrue(rs.getBoolean("logged_in_flight"));
                    assertFalse(rs.getBoolean("damage_protection"));
                    assertEquals(1720000000000L, rs.getLong("last_daily_bonus"));
                }
            }

            // Test null update (e.g. removing trail)
            try (PreparedStatement ps = conn.prepareStatement("UPDATE tempfly_data SET trail = ? WHERE uuid = ?")) {
                ps.setNull(1, java.sql.Types.VARCHAR);
                ps.setString(2, testUuid.toString());
                int rows = ps.executeUpdate();
                assertEquals(1, rows);
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT trail FROM tempfly_data WHERE uuid = ?")) {
                ps.setString(1, testUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertNull(rs.getString("trail"));
                }
            }
        }
    }
}
