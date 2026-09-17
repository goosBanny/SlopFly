package com.moneybags.tempfly.command.sub;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.command.SubCommand;
import com.moneybags.tempfly.user.FlightUser;
import com.moneybags.tempfly.util.U;
import com.moneybags.tempfly.util.V;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Subcommand to configure flight speed preference for oneself or others.
 */
public class SpeedSubCommand implements SubCommand {

	private final TempFly tempfly;

	public SpeedSubCommand(TempFly tempfly) {
		this.tempfly = Objects.requireNonNull(tempfly, "tempfly cannot be null");
	}

	@Override
	public String getName() {
		return "speed";
	}

	@Override
	public String getPermission() {
		return null; // Handled per-branch (speed.self vs speed.other)
	}

	@Override
	public boolean isConsoleAllowed() {
		return true;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args.length < 1) {
			U.m(sender, U.cc("/tf speed [speed / reset] [player]"));
			return;
		}

		Player target = null;
		if (args.length >= 2) {
			if (!U.hasPermission(sender, "tempfly.speed.other")) {
				U.m(sender, V.invalidPermission);
				return;
			}
			target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				U.m(sender, V.invalidPlayer.replaceAll("\\{PLAYER}", args[1]));
				return;
			}
		} else {
			if (!U.isPlayer(sender)) {
				U.m(sender, V.invalidSender);
				return;
			}
			if (!U.hasPermission(sender, "tempfly.speed.self")) {
				U.m(sender, V.invalidPermission);
				return;
			}
			target = (Player) sender;
		}

		float speed;
		if (args[0].equalsIgnoreCase("reset")) {
			speed = -999;
		} else {
			try {
				speed = Float.parseFloat(args[0]);
				if (speed <= 0) {
					U.m(sender, V.invalidNumber);
					return;
				}
			} catch (Exception e) {
				U.m(sender, V.invalidNumber);
				return;
			}
		}

		FlightUser user = tempfly.getFlightManager().getUser(target);
		if (user == null) {
			return;
		}

		user.setSpeedPreference(speed);
		float old = user.getPlayer().getFlySpeed();
		float fin = user.applySpeedCorrect(false, 0);
		String result = new DecimalFormat("#.##").format(fin * 10);

		if (fin < (speed / 10) && target.equals(sender)) {
			U.m(target, V.flySpeedLimitSelf.replaceAll("\\{SPEED}", result));
			if (old == (speed / 10)) {
				return;
			}
		}

		U.m(target, V.flySpeedSelf.replaceAll("\\{SPEED}", speed == -999 ? "DEFAULT" : result));
		if (!sender.equals(target)) {
			if (fin < (speed / 10)) {
				U.m(sender, V.flySpeedLimitOther
						.replaceAll("\\{SPEED}", result)
						.replaceAll("\\{PLAYER}", target.getName()));
				if (old == (speed / 10)) {
					return;
				}
			}
			U.m(sender, V.flySpeedOther
					.replaceAll("\\{SPEED}", speed == -999 ? "DEFAULT" : result)
					.replaceAll("\\{PLAYER}", target.getName()));
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 1) {
			List<String> list = new ArrayList<>();
			list.add("reset");
			int max = 10;
			if (sender instanceof Player player) {
				FlightUser user = tempfly.getFlightManager().getUser(player);
				if (user != null) {
					max = (int) Math.floor(user.getMaxSpeed());
				}
			}
			for (int i = 1; i <= max; i++) {
				list.add(String.valueOf(i));
			}
			return list;
		}
		if (args.length == 2 && U.hasPermission(sender, "tempfly.speed.other")) {
			List<String> players = new ArrayList<>();
			for (Player p : Bukkit.getOnlinePlayers()) {
				players.add(p.getName());
			}
			return players;
		}
		return Collections.emptyList();
	}
}
