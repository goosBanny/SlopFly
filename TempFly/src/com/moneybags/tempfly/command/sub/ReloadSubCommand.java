package com.moneybags.tempfly.command.sub;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.command.SubCommand;
import com.moneybags.tempfly.util.U;
import com.moneybags.tempfly.util.V;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Subcommand to reload plugin configuration, language templates, and cache.
 */
public class ReloadSubCommand implements SubCommand {

	private final TempFly tempfly;

	public ReloadSubCommand(TempFly tempfly) {
		this.tempfly = Objects.requireNonNull(tempfly, "tempfly cannot be null");
	}

	@Override
	public String getName() {
		return "reload";
	}

	@Override
	public String getPermission() {
		return "tempfly.reload";
	}

	@Override
	public boolean isConsoleAllowed() {
		return true;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		tempfly.reloadTempfly();
		U.m(sender, V.reload);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return Collections.emptyList();
	}
}
