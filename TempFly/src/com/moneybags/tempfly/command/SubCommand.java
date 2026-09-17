package com.moneybags.tempfly.command;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * Composite SubCommand interface for modular, zero-offset command handling.
 */
public interface SubCommand {

	/**
	 * Primary identifier/name for this subcommand (e.g. "time", "speed", "pay").
	 */
	String getName();

	/**
	 * Aliases for this subcommand (e.g. "bal", "balance").
	 */
	default List<String> getAliases() {
		return Collections.emptyList();
	}

	/**
	 * Base permission node required to run this subcommand.
	 * Return null or empty string if no permission is required.
	 */
	default String getPermission() {
		return null;
	}

	/**
	 * Whether non-player senders (console, rcon) can execute this subcommand.
	 */
	default boolean isConsoleAllowed() {
		return true;
	}

	/**
	 * Executes the subcommand with pre-sliced arguments (args[0] is the first argument after the command name).
	 *
	 * @param sender The command sender
	 * @param args The sliced arguments
	 */
	void execute(CommandSender sender, String[] args);

	/**
	 * Provides tab completion candidates for the sliced arguments.
	 *
	 * @param sender The command sender
	 * @param args The sliced arguments
	 * @return List of suggestions
	 */
	default List<String> tabComplete(CommandSender sender, String[] args) {
		return Collections.emptyList();
	}

	/**
	 * Short usage string.
	 */
	default String getUsage() {
		return "/" + getName();
	}
}
