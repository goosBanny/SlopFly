package com.moneybags.tempfly.time;

import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.event.FlightUserInitializedEvent;
import com.moneybags.tempfly.storage.UserRepository;
import com.moneybags.tempfly.user.FlightUser;
import com.moneybags.tempfly.util.Console;
import com.moneybags.tempfly.util.DailyDate;
import com.moneybags.tempfly.util.U;
import com.moneybags.tempfly.util.V;
import com.moneybags.tempfly.util.data.DataBridge;
import com.moneybags.tempfly.util.data.DataPointer;

import net.milkbowl.vault.permission.Permission;

import com.moneybags.tempfly.util.data.DataBridge.DataValue;

public class TimeManager implements Listener {

	private TempFly tempfly;
	
	public TimeManager(TempFly tempfly) {
		this.tempfly = tempfly;
		if (tempfly != null && tempfly.getServer() != null) {
			tempfly.getServer().getPluginManager().registerEvents(this, tempfly);
		}
	}
	
	/**
	 * Async
	 * 
	 * Get a players time.
	 * If offline The databridge will return the most recent staged change
	 * held in memory for the players time if one exists. Otherwise it will need to pull it from the
	 * database / yaml data.
	 * @param u
	 * @return
	 */
	private boolean alreadyThrown;
	public double getTime(UUID u) {
		FlightUser user = tempfly.getFlightManager() != null ? tempfly.getFlightManager().getUser(u) : null;
		if (user != null) {
			return user.getTime();
		}
		UserRepository repo = tempfly.getUserRepository();
		if (repo != null) {
			return repo.getUser(u).getTime();
		}
		if (tempfly.getDataBridge() != null && tempfly.getDataBridge().hasSqlEnabled() && Bukkit.getServer().isPrimaryThread() && !alreadyThrown) {
			alreadyThrown = true;
			try {throw new IllegalStateException("Invocation of getTime() for an offline player should be performed from an asychronous thread! It is not safe to access a database on the main server thread!");} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
		DataBridge bridge = tempfly.getDataBridge();
		return bridge != null ? (double) bridge.getOrDefault(DataPointer.of(DataValue.PLAYER_TIME, u.toString()), 0d) : 0.0;
	}

	public java.util.concurrent.CompletableFuture<Double> getTimeAsync(UUID u) {
		FlightUser user = tempfly.getFlightManager() != null ? tempfly.getFlightManager().getUser(u) : null;
		if (user != null) {
			return java.util.concurrent.CompletableFuture.completedFuture(user.getTime());
		}
		UserRepository repo = tempfly.getUserRepository();
		if (repo != null) {
			return repo.getTime(u);
		}
		return java.util.concurrent.CompletableFuture.supplyAsync(() -> getTime(u));
	}
	
	/**
	 * Set a users time.
	 * If the user is online it will also update their FlightUser object with the new time
	 * stages the new time to the DataBridge.
	 * @param u the uuid of the player
	 * @param parameters The time parameters
	 */
	public void removeTime(UUID u, AsyncTimeParameters parameters) {
		double seconds = parameters.getAmount();
		if (seconds <= 0) {
			return;
		}
		FlightUser user = tempfly.getFlightManager() != null ? tempfly.getFlightManager().getUser(Bukkit.getPlayer(u)) : null;
		double bal = user == null ? parameters.getCurrentTime() : user.getTime();
		double remaining = (((bal-seconds) >= 0) ? (bal-seconds) : 0);
		
		if (user != null) {
			user.setTime(remaining);
		} else {
			UserRepository repo = tempfly.getUserRepository();
			if (repo != null) {
				repo.adjustTime(u, -seconds, -1);
			}
			if (tempfly.getDataBridge() != null) {
				tempfly.getDataBridge().stageChange(DataPointer.of(DataValue.PLAYER_TIME, u.toString()), remaining);
			}
		}
	}
	
	/**
	 * Add time to a user.
	 * If the user is online it will also update their FlightUser object with the new time
	 * stages the new time to the DataBridge.
	 * @param u the uuid of the player
	 * @param parameters The time parameters
	 */
	public void addTime(UUID u, AsyncTimeParameters parameters) {
		double seconds = parameters.getAmount();
		if (seconds <= 0) {
			return;
		}
		FlightUser user = tempfly.getFlightManager() != null ? tempfly.getFlightManager().getUser(Bukkit.getPlayer(u)) : null;
		double maxTime = parameters.getMaxTime();
		if (maxTime == -999) {
			return;
		}
		// If user is not online the data needs pulled from the database. Otherwise get it from memory.
		double bal = user == null ? parameters.getCurrentTime() : user.getTime();
		// This line prevents an overflow to -Double.MAX_VALUE.
		double remaining = (((bal+seconds) >= bal) ? (bal+seconds) : Double.MAX_VALUE);
		if (maxTime > -1 && remaining > maxTime) {
			remaining = maxTime;
		}
		
		if (user != null) {
			user.setTime(remaining);
		} else {
			UserRepository repo = tempfly.getUserRepository();
			if (repo != null) {
				repo.adjustTime(u, seconds, maxTime);
			}
			if (tempfly.getDataBridge() != null) {
				tempfly.getDataBridge().stageChange(DataPointer.of(DataValue.PLAYER_TIME, u.toString()), remaining);
			}
		}
	}
	
	/**
	 * Set a users time.
	 * If the user is online it will also update their FlightUser object with the new time
	 * stages the new time to the DataBridge.
	 * @param u the uuid of the player
	 * @param parameters The time parameters
	 */
	public void setTime(UUID u, AsyncTimeParameters parameters) {
		double seconds = parameters.getAmount();
		if (seconds < 0) {
			seconds = 0;
		}
		FlightUser user = tempfly.getFlightManager() != null ? tempfly.getFlightManager().getUser(Bukkit.getPlayer(u)) : null;
		double maxTime = parameters.getMaxTime();
		if (maxTime == -999) {
			return;
		}
		if (maxTime > -1 && seconds > maxTime) {
			seconds = maxTime;
		}
		
		if (user != null) {
			user.setTime(seconds);
		} else {
			UserRepository repo = tempfly.getUserRepository();
			if (repo != null) {
				repo.setTime(u, seconds);
			}
			if (tempfly.getDataBridge() != null) {
				DataPointer pointer = DataPointer.of(DataValue.PLAYER_TIME, u.toString());
				tempfly.getDataBridge().stageChange(pointer, seconds);
				tempfly.getDataBridge().manualCommit(pointer);
			}
		}
	}
	
	
	public double getMaxTime(UUID u) {
		Console.debug("-- Get max time --");
		Player p = Bukkit.getPlayer(u);
		double highest = 0;
		boolean hasGroup = false;
		if (p != null && p.isOnline()) {
			Console.debug("--| Player is online...");
			for (Entry<String, Double> group: V.maxTimeGroups.entrySet()) {
				double current = group.getValue();
				// If the group is less than the highest found so far continue.
				if (current < highest && current > -1) {
					continue;
				}
				if (p.hasPermission("tempfly.max." + group.getKey())) {
					Console.debug("--| Player has group: " + group.getKey() + " | " + group.getValue());
					hasGroup = true;
					if (current == -1) {
						return current;
					} else {
						highest = current;
					}
				}
			}
		} else {
			Console.debug("--| Player is offline...");
			if (!tempfly.getHookManager().hasPermissions()) {
				Console.debug("--|> No vault permissions, We cannot check max time!");
				// We are returning -999 to indicate something is wrong and we cannot check the players max balance.
				// In this case it is because the server does not have Vault and i can't check the offline players permissions.
				return -999;
			}
			OfflinePlayer op = Bukkit.getOfflinePlayer(u);
			Permission perms = tempfly.getHookManager().getPermissions();
			String worldName = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getName();
			for (Entry<String, Double> group: V.maxTimeGroups.entrySet()) {
				double current = group.getValue();
				if (current < highest && current > -1) {
					continue;
				}
				if (op.isOp() || (perms != null && perms.playerHas(worldName, op, "tempfly.max." + group.getKey()))) {
					Console.debug("--| Player has group: " + group.getKey() + " | " + group.getValue());
					hasGroup = true;
					if (current == -1) {
						return current;
					} else {
						highest = current;
					}
				}
			}
		}
		Console.debug("--|> Final value: " + (hasGroup ? highest : V.maxTimeBase));
		return hasGroup ? highest : V.maxTimeBase;
	}
	
	
	@EventHandler
	public void onUserInitialized(FlightUserInitializedEvent e) {
		Console.debug("", "--- Time manager will now process user initialization ---");
		Player p = e.getUser().getPlayer();
		if (V.timeDecay && p.hasPlayedBefore()) {
			Console.debug("--| Time decay is enabled...");
			long offline = (System.currentTimeMillis() - p.getLastPlayed()) / 1000;
			double lost = (offline / V.decayThresh) * V.decayAmount;
			double time = e.getUser().getTime();
			lost = lost > time ? time : lost;
			if (V.debug) Console.debug("--| Seconds offline: " + offline, "Threshold in seconds: " + V.decayThresh, "--| Seconds lost per threshold: " + V.decayAmount, "--| Seconds lost: " + lost, "");
			if (lost > 0) {
				new AsyncTimeParameters(tempfly, (AsyncTimeParameters parameters) -> {
					removeTime(p.getUniqueId(), parameters);
				}, p, lost).runAsync();
				U.m(p, regexString(V.timeDecayLost, lost));	
			}
		}
		
		Bukkit.getScheduler().runTaskAsynchronously(tempfly, () -> {
			double maxTime = getMaxTime(p.getUniqueId());
			if (maxTime == -999) {
				return;
			}
			if (!p.hasPlayedBefore() && V.firstJoinTime > 0) {
				Console.debug("--| User has not played before, do first join bonus...");
				double currentTime = getTime(p.getUniqueId());
				double bonus = maxTime > -1 && ((currentTime + V.firstJoinTime) > maxTime) ? maxTime - currentTime : V.firstJoinTime;
				if (bonus > 0) {
					new AsyncTimeParameters(tempfly, (AsyncTimeParameters parameters) -> {
						addTime(p.getUniqueId(), parameters);
					}, p, bonus).run();
					U.m(p, regexString(V.firstJoin, bonus));
				}
			}
			loginBonus(p, maxTime);
		});

	}
	
	/**
	 * Run the daily login bonus on a player.
	 * @param p
	 */
	public void loginBonus(Player p, double maxTime) {
		Console.debug("--| Checking daily login bonus...");
		DataBridge bridge = tempfly.getDataBridge();
		long lastBonus = (long) bridge.getOrDefault(DataPointer.of(DataValue.PLAYER_DAILY_BONUS, p.getUniqueId().toString()), 0L);
		long sys = System.currentTimeMillis();
		
		if (new DailyDate(lastBonus).equals(new DailyDate(sys))) {
			Console.debug("--|> Same day no daily bonus :(");
			return;
		}
		double currentTime = getTime(p.getUniqueId());
		double bonus = 0;
		if (V.legacyBonus > 0) {
			Console.debug("--| Using legacy bonus...");
			bonus = maxTime > -1 && ((currentTime + V.legacyBonus) > maxTime) ? maxTime - currentTime : V.legacyBonus;
			if (bonus > 0) {
				new AsyncTimeParameters(tempfly, (AsyncTimeParameters parameters) -> {
					addTime(p.getUniqueId(), parameters);
				}, p, bonus).run();
				U.m(p, regexString(V.dailyLogin, bonus));
			}
			bridge.stageChange(DataPointer.of(DataValue.PLAYER_DAILY_BONUS, p.getUniqueId().toString()), sys);
		} else if (V.dailyBonus.size() > 0) {
			Console.debug("--| Using permission based bonus...");
			for (Entry<String, Double> entry: V.dailyBonus.entrySet()) {
				if (p.hasPermission("tempfly.bonus." + entry.getKey())) {
					bonus += entry.getValue();
				}
			}
			bonus = maxTime > -1 && ((currentTime + bonus) > maxTime) ? maxTime - currentTime : bonus;
			if (bonus > 0) {
				new AsyncTimeParameters(tempfly, (AsyncTimeParameters parameters) -> {
					addTime(p.getUniqueId(), parameters);
				}, p, bonus).run();
				U.m(p, regexString(V.dailyLogin, bonus));
			}
			bridge.stageChange(DataPointer.of(DataValue.PLAYER_DAILY_BONUS, p.getUniqueId().toString()), sys);
		}
	}
	
	public String regexString(String s, double seconds) {
		return regexString(s, seconds, false);
	}

	public String regexString(String s, double seconds, boolean infinite) {
		if (s == null || s.isEmpty()) return "";
		String infSym = V.placeholderInfiniteYes != null ? V.placeholderInfiniteYes : (V.infinity != null ? V.infinity : "∞");
		if (s.contains("{INFINITY}")) {
			s = s.replace("{INFINITY}", infSym);
		}
		if (infinite) {
			if (s.contains("{FORMATTED_TIME}")) {
				s = s.replace("{FORMATTED_TIME}", infSym);
			}
			if (s.contains("{TIME_FORMATTED}")) {
				s = s.replace("{TIME_FORMATTED}", infSym);
			}
			if (s.contains("{DAYS}")) s = s.replace("{DAYS}", infSym);
			if (s.contains("{HOURS}")) s = s.replace("{HOURS}", infSym);
			if (s.contains("{MINUTES}")) s = s.replace("{MINUTES}", infSym);
			if (s.contains("{SECONDS}")) s = s.replace("{SECONDS}", infSym);
			return s;
		}
		//We dont care about the decimal here, it is only used internally for relative time regions.
		long
		days = formatTime(TimeUnit.DAYS, Math.ceil(seconds)),
		hours = formatTime(TimeUnit.HOURS, Math.ceil(seconds)),
		minutes = formatTime(TimeUnit.MINUTES, Math.ceil(seconds)),
		secs = formatTime(TimeUnit.SECONDS, Math.ceil(seconds));
		
		if (s.contains("{FORMATTED_TIME}") || s.contains("{TIME_FORMATTED}")) {
			StringBuilder sb = new StringBuilder();
			boolean addSpace = false;
			if (days > 0) {
				regexA(sb, days, V.unitDays, false);
				addSpace = true;
			}
			if (hours > 0) {
				regexA(sb, hours, V.unitHours, addSpace);
				addSpace = true;
			}
			if (minutes > 0) { 
				regexA(sb, minutes, V.unitMinutes, addSpace);
				addSpace = true;
			}
			if (secs > 0 || sb.length() == 0) {
				regexA(sb, secs, V.unitSeconds, addSpace);
			}
			String formatted = sb.toString();
			s = s.replace("{FORMATTED_TIME}", formatted).replace("{TIME_FORMATTED}", formatted);
		}
		if (s.contains("{DAYS}")) s = s.replace("{DAYS}", String.valueOf(days));
		if (s.contains("{HOURS}")) s = s.replace("{HOURS}", String.valueOf(hours));
		if (s.contains("{MINUTES}")) s = s.replace("{MINUTES}", String.valueOf(minutes));
		if (s.contains("{SECONDS}")) s = s.replace("{SECONDS}", String.valueOf(secs));
		return s;
	}
	
	private void regexA(StringBuilder sb, long quantity, String unit, boolean addSpace) {
		sb.append(addSpace ? " " : "").append(V.timeFormat
				.replace("{QUANTITY}", String.valueOf(quantity))
				.replace("{UNIT}", unit));
	}
	
	public long formatTime(TimeUnit unit, double seconds) {
		switch (unit) {
		case DAYS:
			return (long)seconds / 86400;
		case HOURS:
			return (long)seconds % 86400 / 3600;
		case MINUTES:
			return (long)seconds % 3600 / 60;
		case SECONDS:
			return (long)seconds % 60;
		default:
			return 0;
		}
	}
	
	public String getPlaceHolder(Player p, Placeholder type) {
		double supply = getTime(p.getUniqueId());
		FlightUser user = tempfly.getFlightManager().getUser(p);
		if (user == null) {
			return V.placeholderUnknownUser != null ? V.placeholderUnknownUser : "0s";
		}
		boolean infinite = user.hasInfiniteFlight();
		String infSym = V.placeholderInfiniteYes != null ? V.placeholderInfiniteYes : (V.infinity != null ? V.infinity : "∞");

		switch (type) {
		case TIME_FORMATTED:
		{
			if (infinite) {
				return infSym;
			}
			long
			days = formatTime(TimeUnit.DAYS, supply),
			hours = formatTime(TimeUnit.HOURS, supply),
			minutes = formatTime(TimeUnit.MINUTES, supply),
			seconds = formatTime(TimeUnit.SECONDS, supply);
			
			StringBuilder sb = new StringBuilder();
			if (days > 0) 
				sb.append(V.fbDays.replace("{DAYS}", String.valueOf(days)));
			if (hours > 0) 
				sb.append(V.fbHours.replace("{HOURS}", String.valueOf(hours)));
			if (minutes > 0) 
				sb.append(V.fbMinutes.replace("{MINUTES}", String.valueOf(minutes)));
			if (seconds > 0 || sb.length() == 0) 
				sb.append(V.fbSeconds.replace("{SECONDS}", String.valueOf(seconds)));
			return sb.toString();
		}
		case TIME_DAYS:
			if (infinite) return infSym;
			long days = formatTime(TimeUnit.DAYS, supply);
			return String.valueOf(days);
		case TIME_HOURS:
			if (infinite) return infSym;
			long hours = formatTime(TimeUnit.HOURS, supply);
			return String.valueOf(hours);
		case TIME_MINUTES:
			if (infinite) return infSym;
			long minutes = formatTime(TimeUnit.MINUTES, supply);
			return String.valueOf(minutes);
		case TIME_SECONDS:
			if (infinite) return infSym;
			long seconds = formatTime(TimeUnit.SECONDS, supply);
			return String.valueOf(seconds);
		case TIME_SECONDS_TOTAL:
			if (infinite) return infSym;
			return String.valueOf((int)Math.floor(supply));
		default:
			break;
		}
		return V.placeholderInvalid != null ? V.placeholderInvalid : "";
	}
	
	
	public static enum Placeholder {
		TIME_FORMATTED,
		TIME_DAYS,
		TIME_HOURS,
		TIME_MINUTES,
		TIME_SECONDS,
		TIME_SECONDS_TOTAL;
	}
}
