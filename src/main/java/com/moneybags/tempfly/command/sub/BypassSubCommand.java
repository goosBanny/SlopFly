package com.moneybags.tempfly.command.sub;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.command.SubCommand;
import com.moneybags.tempfly.user.FlightUser;
import com.moneybags.tempfly.util.U;
import com.moneybags.tempfly.util.V;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Subcommand toggling flight requirement bypass for a player.
 */
public class BypassSubCommand implements SubCommand {

	private final TempFly tempfly;

	public BypassSubCommand(TempFly tempfly) {
		this.tempfly = Objects.requireNonNull(tempfly, "tempfly cannot be null");
	}

	@Override
	public String getName() {
		return "bypass";
	}

	@Override
	public String getPermission() {
		return "tempfly.bypass.toggle";
	}

	@Override
	public boolean isConsoleAllowed() {
		return false;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		FlightUser user = tempfly.getFlightManager().getUser((Player) sender);
		if (user == null) {
			return;
		}

		boolean toggleVal;
		if (args.length > 0) {
			switch (args[0].toLowerCase()) {
				case "on":
				case "enable":
					toggleVal = true;
					break;
				case "off":
				case "disable":
					toggleVal = false;
					break;
				default:
					U.m(sender, "&c/tempfly bypass [on/off]");
					return;
			}
		} else {
			toggleVal = !user.hasRequirementBypass();
		}

		U.m(sender, toggleVal ? V.flyBypassEnabled : V.flyBypassDisabled);
		user.setRequirementBypass(toggleVal);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length <= 1 && tempfly.getCommandManager() != null) {
			return tempfly.getCommandManager().getToggleCompletions(true);
		}
		return Collections.emptyList();
	}
}
