package com.moneybags.tempfly.command.sub;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.command.SubCommand;
import com.moneybags.tempfly.command.player.CmdFly;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SubCommand managing flight enabling and toggling.
 */
public class FlySubCommand implements SubCommand {

	private final TempFly tempfly;

	public FlySubCommand(TempFly tempfly) {
		this.tempfly = Objects.requireNonNull(tempfly, "tempfly cannot be null");
	}

	@Override
	public String getName() {
		return "fly";
	}

	@Override
	public List<String> getAliases() {
		List<String> aliases = new ArrayList<>();
		aliases.add("toggle");
		return aliases;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		new CmdFly(tempfly, args).executeAs(sender);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return new CmdFly(tempfly, args).getPotentialArguments(sender);
	}
}
