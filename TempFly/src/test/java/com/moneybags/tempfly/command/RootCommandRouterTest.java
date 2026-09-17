package com.moneybags.tempfly.command;

import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandSender.Spigot;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class RootCommandRouterTest {

	private RootCommandRouter router;

	@BeforeEach
	public void setUp() {
		router = new RootCommandRouter(null);
	}

	private static class FakeSubCommand implements SubCommand {
		private final String name;
		private final List<String> aliases;
		private final String permission;
		private final boolean consoleAllowed;
		private String[] lastExecutedArgs;
		private List<String> completions = new ArrayList<>();

		public FakeSubCommand(String name, List<String> aliases, String permission, boolean consoleAllowed) {
			this.name = name;
			this.aliases = aliases != null ? aliases : Collections.emptyList();
			this.permission = permission;
			this.consoleAllowed = consoleAllowed;
		}

		@Override public String getName() { return name; }
		@Override public List<String> getAliases() { return aliases; }
		@Override public String getPermission() { return permission; }
		@Override public boolean isConsoleAllowed() { return consoleAllowed; }
		@Override public void execute(CommandSender sender, String[] args) { this.lastExecutedArgs = args; }
		@Override public List<String> tabComplete(CommandSender sender, String[] args) { return completions; }
	}

	private static class FakeConsoleSender implements CommandSender {
		@Override public void sendMessage(String message) {}
		@Override public void sendMessage(String[] messages) {}
		@Override public void sendMessage(java.util.UUID sender, String message) {}
		@Override public void sendMessage(java.util.UUID sender, String[] messages) {}
		@Override public org.bukkit.Server getServer() { return null; }
		@Override public String getName() { return "CONSOLE"; }
		@Override public Spigot spigot() { return null; }
		@Override public boolean isPermissionSet(String name) { return true; }
		@Override public boolean isPermissionSet(org.bukkit.permissions.Permission perm) { return true; }
		@Override public boolean hasPermission(String name) { return true; }
		@Override public boolean hasPermission(org.bukkit.permissions.Permission perm) { return true; }
		@Override public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value) { return null; }
		@Override public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin) { return null; }
		@Override public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value, int ticks) { return null; }
		@Override public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, int ticks) { return null; }
		@Override public void removeAttachment(org.bukkit.permissions.PermissionAttachment attachment) {}
		@Override public void recalculatePermissions() {}
		@Override public java.util.Set<org.bukkit.permissions.PermissionAttachmentInfo> getEffectivePermissions() { return Collections.emptySet(); }
		@Override public boolean isOp() { return true; }
		@Override public void setOp(boolean value) {}
		@Override public net.kyori.adventure.text.Component name() { return net.kyori.adventure.text.Component.text("CONSOLE"); }
	}

	@Test
	public void testRegistrationAndLookup() {
		FakeSubCommand cmd = new FakeSubCommand("speed", Arrays.asList("sp", "velocity"), null, true);
		router.register(cmd);

		assertSame(cmd, router.getCommand("speed"));
		assertSame(cmd, router.getCommand("SPEED"));
		assertSame(cmd, router.getCommand("sp"));
		assertSame(cmd, router.getCommand("velocity"));
		assertNull(router.getCommand("unknown"));

		router.unregister("speed");
		assertNull(router.getCommand("speed"));
		assertNull(router.getCommand("sp"));
	}

	@Test
	public void testArgumentSlicing() {
		FakeSubCommand testCmd = new FakeSubCommand("test", null, null, true);
		router.register(testCmd);

		FakeConsoleSender sender = new FakeConsoleSender();
		boolean result = router.execute(sender, new String[]{"test", "alpha", "beta", "123"});

		assertTrue(result);
		assertNotNull(testCmd.lastExecutedArgs);
		assertEquals(3, testCmd.lastExecutedArgs.length);
		assertEquals("alpha", testCmd.lastExecutedArgs[0]);
		assertEquals("beta", testCmd.lastExecutedArgs[1]);
		assertEquals("123", testCmd.lastExecutedArgs[2]);
	}

	@Test
	public void testConsoleValidation() {
		FakeSubCommand playerOnlyCmd = new FakeSubCommand("playeronly", null, null, false);
		router.register(playerOnlyCmd);

		FakeConsoleSender console = new FakeConsoleSender();
		router.execute(console, new String[]{"playeronly"});
		assertNull(playerOnlyCmd.lastExecutedArgs, "Console should not execute player-only command");
	}

	@Test
	public void testTabCompletion() {
		FakeSubCommand cmd1 = new FakeSubCommand("speed", Collections.singletonList("sp"), null, true);
		cmd1.completions = Arrays.asList("1", "2", "reset");

		FakeSubCommand cmd2 = new FakeSubCommand("status", Collections.emptyList(), null, true);

		router.register(cmd1);
		router.register(cmd2);

		FakeConsoleSender sender = new FakeConsoleSender();

		// Top-level completion with "s" prefix -> should match "speed", "sp", "status"
		List<String> completions = router.tabComplete(sender, new String[]{"s"});
		assertTrue(completions.contains("speed"));
		assertTrue(completions.contains("sp"));
		assertTrue(completions.contains("status"));

		// Top-level completion with "sp" prefix -> should match "speed", "sp"
		completions = router.tabComplete(sender, new String[]{"sp"});
		assertTrue(completions.contains("speed"));
		assertTrue(completions.contains("sp"));
		assertFalse(completions.contains("status"));

		// Subcommand-level completion delegation
		List<String> subCompletions = router.tabComplete(sender, new String[]{"speed", "r"});
		assertEquals(3, subCompletions.size());
		assertTrue(subCompletions.contains("reset"));
	}
}
