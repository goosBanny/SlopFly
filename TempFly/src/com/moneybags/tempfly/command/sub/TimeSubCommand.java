package com.moneybags.tempfly.command.sub;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.command.SubCommand;
import com.moneybags.tempfly.command.player.CmdTime;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * SubCommand querying flight time for a player or target.
 */
public class TimeSubCommand implements SubCommand {

	private final TempFly tempfly;

	public TimeSubCommand(TempFly tempfly) {
		this.tempfly = Objects.requireNonNull(tempfly, "tempfly cannot be null");
	}

	@Override
	public String getName() {
		return "time";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList("bal", "balance");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		String[] full = new String[args.length + 1];
		full[0] = "time";
		System.arraycopy(args, 0, full, 1, args.length);
		new CmdTime(tempfly, full).executeAs(sender);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		String[] full = new String[args.length + 1];
		full[0] = "time";
		System.arraycopy(args, 0, full, 1, args.length);
		return new CmdTime(tempfly, full).getPotentialArguments(sender);
	}
}
