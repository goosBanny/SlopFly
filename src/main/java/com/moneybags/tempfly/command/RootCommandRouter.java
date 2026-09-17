package com.moneybags.tempfly.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.util.U;
import com.moneybags.tempfly.util.V;

/**
 * Root router coordinating SubCommand execution, automatic argument slicing,
 * permission enforcement, and sender type validation.
 */
public class RootCommandRouter {

	private final TempFly tempfly;
	private final Map<String, SubCommand> commandMap = new LinkedHashMap<>();
	private final List<SubCommand> subCommands = new ArrayList<>();

	public RootCommandRouter(TempFly tempfly) {
		this.tempfly = tempfly;
	}

	public TempFly getTempFly() {
		return tempfly;
	}

	public void register(SubCommand cmd) {
		Objects.requireNonNull(cmd, "cmd cannot be null");
		if (!subCommands.contains(cmd)) {
			subCommands.add(cmd);
		}
		commandMap.put(cmd.getName().toLowerCase(Locale.ROOT), cmd);
		for (String alias : cmd.getAliases()) {
			commandMap.put(alias.toLowerCase(Locale.ROOT), cmd);
		}
	}

	public void unregister(String name) {
		if (name == null) return;
		SubCommand cmd = commandMap.remove(name.toLowerCase(Locale.ROOT));
		if (cmd != null) {
			subCommands.remove(cmd);
			for (String alias : cmd.getAliases()) {
				commandMap.remove(alias.toLowerCase(Locale.ROOT));
			}
		}
	}

	public SubCommand getCommand(String name) {
		if (name == null) return null;
		return commandMap.get(name.toLowerCase(Locale.ROOT));
	}

	public Collection<SubCommand> getSubCommands() {
		return Collections.unmodifiableList(subCommands);
	}

	public boolean execute(CommandSender sender, String[] fullArgs) {
		if (fullArgs == null || fullArgs.length == 0) {
			SubCommand fly = getCommand("fly");
			if (fly != null) {
				executeCommand(fly, sender, new String[0]);
				return true;
			}
			U.m(sender, V.invalidCommand);
			return true;
		}

		String subName = fullArgs[0].toLowerCase(Locale.ROOT);

		// Handle on / off / enable / disable aliases as flight toggle parameters
		CommandManager cm = tempfly != null ? tempfly.getCommandManager() : null;
		if (cm != null && (cm.getEnable().contains(subName) || cm.getDisable().contains(subName))) {
			SubCommand fly = getCommand("fly");
			if (fly != null) {
				executeCommand(fly, sender, fullArgs);
				return true;
			}
		}

		SubCommand target = getCommand(subName);
		if (target != null) {
			String[] sliced = Arrays.copyOfRange(fullArgs, 1, fullArgs.length);
			executeCommand(target, sender, sliced);
			return true;
		}

		// Fallback for legacy hook commands
		if (cm != null) {
			TempFlyCommand legacyCmd = cm.getCommand(fullArgs);
			if (legacyCmd != null) {
				legacyCmd.executeAs(sender);
				return true;
			}
		}

		U.m(sender, V.invalidCommand);
		return true;
	}

	private void executeCommand(SubCommand cmd, CommandSender sender, String[] args) {
		if (!cmd.isConsoleAllowed() && !(sender instanceof Player)) {
			U.m(sender, V.invalidSender);
			return;
		}

		String perm = cmd.getPermission();
		if (perm != null && !perm.isEmpty() && !U.hasPermission(sender, perm)) {
			U.m(sender, V.invalidPermission);
			return;
		}

		cmd.execute(sender, args);
	}

	public List<String> tabComplete(CommandSender sender, String[] fullArgs) {
		if (fullArgs == null || fullArgs.length == 0) {
			return getAvailableCommandNames(sender, "");
		}

		if (fullArgs.length == 1) {
			return getAvailableCommandNames(sender, fullArgs[0]);
		}

		SubCommand target = getCommand(fullArgs[0]);
		if (target != null) {
			String perm = target.getPermission();
			if (perm != null && !perm.isEmpty() && !U.hasPermission(sender, perm)) {
				return Collections.emptyList();
			}
			String[] sliced = Arrays.copyOfRange(fullArgs, 1, fullArgs.length);
			return target.tabComplete(sender, sliced);
		}

		return Collections.emptyList();
	}

	private List<String> getAvailableCommandNames(CommandSender sender, String prefix) {
		String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
		List<String> matches = new ArrayList<>();
		for (SubCommand cmd : subCommands) {
			String perm = cmd.getPermission();
			if (perm != null && !perm.isEmpty() && !U.hasPermission(sender, perm)) {
				continue;
			}
			if (cmd.getName().toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
				matches.add(cmd.getName());
			}
			for (String alias : cmd.getAliases()) {
				if (alias.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
					matches.add(alias);
				}
			}
		}
		return matches;
	}
}
