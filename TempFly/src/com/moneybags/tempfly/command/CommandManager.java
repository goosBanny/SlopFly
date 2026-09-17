package com.moneybags.tempfly.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.moneybags.tempfly.command.player.CmdTrails;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.command.admin.CmdGive;
import com.moneybags.tempfly.command.admin.CmdGiveAll;
import com.moneybags.tempfly.command.admin.CmdMigrate;
import com.moneybags.tempfly.command.admin.CmdReload;
import com.moneybags.tempfly.command.admin.CmdRemove;
import com.moneybags.tempfly.command.admin.CmdSet;
import com.moneybags.tempfly.command.admin.CmdTrailRemove;
import com.moneybags.tempfly.command.admin.CmdTrailSet;
import com.moneybags.tempfly.command.player.CmdBypass;
import com.moneybags.tempfly.command.player.CmdFly;
import com.moneybags.tempfly.command.player.CmdHelp;
import com.moneybags.tempfly.command.player.CmdInfinite;
import com.moneybags.tempfly.command.player.CmdPay;
import com.moneybags.tempfly.command.player.CmdSpeed;
import com.moneybags.tempfly.command.player.CmdTime;
import com.moneybags.tempfly.command.sub.BypassSubCommand;
import com.moneybags.tempfly.command.sub.FlySubCommand;
import com.moneybags.tempfly.command.sub.InfiniteSubCommand;
import com.moneybags.tempfly.command.sub.ReloadSubCommand;
import com.moneybags.tempfly.command.sub.SpeedSubCommand;
import com.moneybags.tempfly.command.sub.TimeSubCommand;
import com.moneybags.tempfly.user.FlightUser;
import com.moneybags.tempfly.util.U;
import com.moneybags.tempfly.util.V;
import com.moneybags.tempfly.util.data.Files;

public class CommandManager {

	private final TempFly tempfly;
	private final TempFlyExecutor executor;
	private final RootCommandRouter router;

	private final List<String> enable = new ArrayList<>();
	private final List<String> disable = new ArrayList<>();

	private final Map<CommandType, List<String>> subCommands = new HashMap<>();
	private final Map<TimeUnit, List<String>> timeArgs = new HashMap<>();
	private final Map<TimeUnit, String> timeComplete = new HashMap<>();

	private final Map<String, Class<? extends TempFlyCommand>> hookRegistry = new HashMap<>();

	public static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS = (ctx, builder) -> {
		if (V.disableTab && !U.hasPermission(ctx.getSource().getSender(), "tempfly.disable_tab.bypass")) {
			return builder.buildFuture();
		}
		String remaining = builder.getRemainingLowerCase();
		for (Player p : Bukkit.getOnlinePlayers()) {
			String name = p.getName();
			if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
				builder.suggest(name);
			}
		}
		return builder.buildFuture();
	};

	public static final SuggestionProvider<CommandSourceStack> PARTICLE_SUGGESTIONS = (ctx, builder) -> {
		if (V.disableTab && !U.hasPermission(ctx.getSource().getSender(), "tempfly.disable_tab.bypass")) {
			return builder.buildFuture();
		}
		String remaining = builder.getRemainingLowerCase();
		for (Particle particle : Particle.values()) {
			String name = particle.name().toLowerCase(Locale.ROOT);
			if (name.startsWith(remaining)) {
				builder.suggest(name);
			}
		}
		return builder.buildFuture();
	};

	public CommandManager(TempFly tempfly) {
		this.tempfly = tempfly;
		this.router = new RootCommandRouter(tempfly);
		this.executor = new TempFlyExecutor(this);

		for (CommandType type : CommandType.values()) {
			if (!type.isEnabled(tempfly)) {
				continue;
			}
			List<String> subs = Files.lang.getStringList("command.base." + type.toString().toLowerCase());
			if (subs == null || subs.size() == 0) {
				subCommands.put(type, Arrays.asList(type.getBase()));
				continue;
			}
			subCommands.put(type, subs);
		}

		ConfigurationSection csUnits = Files.lang.getConfigurationSection("command.unit");
		if (csUnits != null) {
			String path = "command.unit";
			for (String key : csUnits.getKeys(false)) {
				try {
					TimeUnit unit = TimeUnit.valueOf(key.toUpperCase());
					List<String> recognized = Files.lang.getStringList(path + "." + key + ".recognized");
					String complete = Files.lang.getString(path + "." + key + ".tab_complete");

					if (recognized != null) timeArgs.put(unit, recognized);
					if (complete != null) timeComplete.put(unit, complete);
				} catch (Exception ignored) {}
			}
		}
		if (!timeArgs.containsKey(TimeUnit.SECONDS))
			timeArgs.put(TimeUnit.SECONDS, Arrays.asList(TimeType.SECONDS.getBase()));
		if (!timeArgs.containsKey(TimeUnit.MINUTES))
			timeArgs.put(TimeUnit.MINUTES, Arrays.asList(TimeType.MINUTES.getBase()));
		if (!timeArgs.containsKey(TimeUnit.HOURS))
			timeArgs.put(TimeUnit.HOURS, Arrays.asList(TimeType.HOURS.getBase()));
		if (!timeArgs.containsKey(TimeUnit.DAYS))
			timeArgs.put(TimeUnit.DAYS, Arrays.asList(TimeType.DAYS.getBase()));

		if (!timeComplete.containsKey(TimeUnit.SECONDS))
			timeComplete.put(TimeUnit.SECONDS, TimeType.SECONDS.getCompletion());
		if (!timeComplete.containsKey(TimeUnit.MINUTES))
			timeComplete.put(TimeUnit.MINUTES, TimeType.MINUTES.getCompletion());
		if (!timeComplete.containsKey(TimeUnit.HOURS))
			timeComplete.put(TimeUnit.HOURS, TimeType.HOURS.getCompletion());
		if (!timeComplete.containsKey(TimeUnit.DAYS))
			timeComplete.put(TimeUnit.DAYS, TimeType.DAYS.getCompletion());

		List<String> temp;
		enable.addAll((temp = Files.lang.getStringList("command.enable")) == null || temp.size() == 0 ?
				Arrays.asList("on", "enable") : temp);
		disable.addAll((temp = Files.lang.getStringList("command.disable")) == null || temp.size() == 0 ?
				Arrays.asList("off", "disable") : temp);

		initSubCommands();

		tempfly.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
			registerCommands(event.registrar());
		});
	}

	private void initSubCommands() {
		router.register(new FlySubCommand(tempfly));
		router.register(new TimeSubCommand(tempfly));
		router.register(new SpeedSubCommand(tempfly));
		router.register(new InfiniteSubCommand(tempfly));
		router.register(new BypassSubCommand(tempfly));
		router.register(new ReloadSubCommand(tempfly));

		// Wrap any remaining CommandTypes in LegacyCommandAdapter
		for (CommandType type : CommandType.values()) {
			if (!type.isEnabled(tempfly)) continue;
			List<String> bases = getCommandBases(type);
			if (bases == null || bases.isEmpty()) continue;
			String primary = bases.get(0);
			if (router.getCommand(primary) != null) {
				continue;
			}
			router.register(new LegacyCommandAdapter(tempfly, primary, bases, type::createCommand));
		}
	}

	public RootCommandRouter getRouter() {
		return router;
	}

	public TempFly getTempFly() {
		return tempfly;
	}

	public List<String> getEnable() {
		return enable;
	}

	public List<String> getDisable() {
		return disable;
	}

	public List<String> getAllTimeArguments() {
		List<String> args = new ArrayList<>();
		for (Entry<TimeUnit, List<String>> unit : timeArgs.entrySet()) {
			args.addAll(unit.getValue());
		}
		return args;
	}

	public List<String> getTimeArguments(TimeUnit unit) {
		return timeArgs.getOrDefault(unit, Arrays.asList("{unit}"));
	}

	public TimeUnit parseUnit(String s) {
		for (Entry<TimeUnit, List<String>> entry : timeArgs.entrySet()) {
			if (entry.getValue().contains(s)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public List<String> getAllTimeCompletions() {
		List<String> args = new ArrayList<>();
		for (Entry<TimeUnit, String> unit : timeComplete.entrySet()) {
			args.add(unit.getValue());
		}
		return args;
	}

	public List<String> getTimeCompletions(List<TimeUnit> exclude) {
		List<String> args = new ArrayList<>();
		for (Entry<TimeUnit, String> entry : timeComplete.entrySet()) {
			if (exclude.contains(entry.getKey())) continue;
			args.add(entry.getValue());
		}
		return args;
	}

	public String getTimeCompletion(TimeUnit unit) {
		return timeComplete.getOrDefault(unit, "{unit}");
	}

	public List<String> getToggleCompletions(boolean filter) {
		if (filter) return Arrays.asList(enable.get(0), disable.get(0));
		List<String> all = new ArrayList<>();
		all.addAll(enable);
		all.addAll(disable);
		return all;
	}

	public TempFlyCommand getCommand(String[] args) {
		if (args == null) {
			return null;
		}
		if (args.length == 0 || getEnable().contains(args[0]) || getDisable().contains(args[0])) {
			return new CmdFly(tempfly, args);
		} else {
			for (CommandType type : CommandType.values()) {
				for (String base : getCommandBases(type)) {
					if (base.equalsIgnoreCase(args[0])) {
						return type.createCommand(tempfly, args);
					}
				}
			}
			for (Entry<String, Class<? extends TempFlyCommand>> entry : hookRegistry.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(args[0])) {
					try {
						return entry.getValue().getConstructor(TempFly.class, String[].class).newInstance(tempfly, args);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}

	public void executeCommand(CommandSender sender, String[] args) {
		if (router != null) {
			router.execute(sender, args);
			return;
		}
		TempFlyCommand command = getCommand(args);
		if (command != null) {
			command.executeAs(sender);
			return;
		}
		U.m(sender, V.invalidCommand);
	}

	public void executeTimeCommand(CommandSender sender, String base, String player, String timeString) {
		List<String> argsList = new ArrayList<>();
		argsList.add(base);
		if (player != null && !player.isEmpty()) {
			argsList.add(player);
		}
		if (timeString != null && !timeString.trim().isEmpty()) {
			for (String part : timeString.trim().split("\\s+")) {
				if (!part.isEmpty()) {
					argsList.add(part);
				}
			}
		}
		executeCommand(sender, argsList.toArray(new String[0]));
	}

	public void registerHookCommand(String base, Class<? extends TempFlyCommand> command) throws IllegalArgumentException {
		if (hookRegistry.containsKey(base) || (router != null && router.getCommand(base) != null)) {
			throw new IllegalArgumentException("Sub command bases must be unique! This command is already taken: " + base);
		}

		try {
			command.getConstructor(TempFly.class, String[].class).newInstance(tempfly, new String[0]);
		} catch (Exception e) {
			throw new IllegalArgumentException("This sub command is not properly structured: " + base);
		}
		hookRegistry.put(base, command);
		if (router != null) {
			router.register(new LegacyCommandAdapter(tempfly, base, Collections.singletonList(base), command));
		}
	}

	public void unregisterHookCommand(String base) {
		hookRegistry.remove(base);
		if (router != null) {
			router.unregister(base);
		}
	}

	public List<String> getAllCommandBases() {
		List<String> bases = new ArrayList<>();
		bases.addAll(getToggleCompletions(true));
		for (CommandType type : CommandType.values()) {
			bases.addAll(getCommandBases(type));
		}
		bases.addAll(hookRegistry.keySet());
		return bases;
	}

	public List<String> getCommandBases(CommandType type) {
		return subCommands.getOrDefault(type, Arrays.asList(type.getBase()));
	}

	private boolean hasHookPermission(CommandSender s, Class<? extends TempFlyCommand> clazz) {
		try {
			TempFlyCommand cmd = clazz.getConstructor(TempFly.class, String[].class).newInstance(tempfly, new String[0]);
			return cmd.hasPermission(s);
		} catch (Exception e) {
			return true;
		}
	}

	private SuggestionProvider<CommandSourceStack> createSpeedSuggestions(TempFly tempfly) {
		return (ctx, builder) -> {
			if (V.disableTab && !U.hasPermission(ctx.getSource().getSender(), "tempfly.disable_tab.bypass")) {
				return builder.buildFuture();
			}
			String remaining = builder.getRemainingLowerCase();
			CommandSender s = ctx.getSource().getSender();
			if ("reset".startsWith(remaining)) {
				builder.suggest("reset");
			}
			int max = 10;
			if (s instanceof Player player) {
				FlightUser user = tempfly.getFlightManager().getUser(player);
				if (user != null) {
					max = (int) Math.floor(user.getMaxSpeed());
				}
			}
			for (int i = 1; i <= max; i++) {
				String str = String.valueOf(i);
				if (str.startsWith(remaining)) {
					builder.suggest(str);
				}
			}
			return builder.buildFuture();
		};
	}

	private SuggestionProvider<CommandSourceStack> createTimeSuggestions(TempFly tempfly) {
		return (ctx, builder) -> {
			if (V.disableTab && !U.hasPermission(ctx.getSource().getSender(), "tempfly.disable_tab.bypass")) {
				return builder.buildFuture();
			}
			String remaining = builder.getRemaining();
			String[] split = remaining.split(" ", -1);
			String currentToken = split[split.length - 1];
			String tokenLower = currentToken.toLowerCase(Locale.ROOT);

			int tokenStart = builder.getStart() + remaining.length() - currentToken.length();
			SuggestionsBuilder tokenBuilder = builder.createOffset(tokenStart);

			List<TimeUnit> exclusion = new ArrayList<>();
			for (int i = 0; i < split.length - 1; i++) {
				TimeUnit u = tempfly.getCommandManager().parseUnit(split[i]);
				if (u != null) {
					exclusion.add(u);
				}
			}

			if (split.length > 1 && !split[split.length - 2].isEmpty() && isNumeric(split[split.length - 2])) {
				for (String unitComp : tempfly.getCommandManager().getTimeCompletions(exclusion)) {
					if (unitComp.toLowerCase(Locale.ROOT).startsWith(tokenLower)) {
						tokenBuilder.suggest(unitComp);
					}
				}
			} else {
				for (int i = 1; i <= 9; i++) {
					String num = String.valueOf(i);
					if (num.startsWith(tokenLower)) {
						tokenBuilder.suggest(num);
					}
				}
			}
			return builder.add(tokenBuilder).buildFuture();
		};
	}

	private SuggestionProvider<CommandSourceStack> createTrailArg1Suggestions() {
		return (ctx, builder) -> {
			if (V.disableTab && !U.hasPermission(ctx.getSource().getSender(), "tempfly.disable_tab.bypass")) {
				return builder.buildFuture();
			}
			String remaining = builder.getRemainingLowerCase();
			CommandSender s = ctx.getSource().getSender();
			if (U.hasPermission(s, "tempfly.trails.set.self")) {
				for (Particle particle : Particle.values()) {
					String name = particle.name().toLowerCase(Locale.ROOT);
					if (name.startsWith(remaining)) {
						builder.suggest(name);
					}
				}
			}
			if (U.hasPermission(s, "tempfly.trails.set.other")) {
				for (Player p : Bukkit.getOnlinePlayers()) {
					String name = p.getName();
					if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
						builder.suggest(name);
					}
				}
			}
			return builder.buildFuture();
		};
	}

	private static boolean isNumeric(String s) {
		if (s == null || s.isEmpty()) return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (!Character.isDigit(c) && c != '.') {
				return false;
			}
		}
		return true;
	}

	public void registerCommands(Commands registrar) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("tempfly")
				.executes(ctx -> {
					executeCommand(ctx.getSource().getSender(), new String[0]);
					return Command.SINGLE_SUCCESS;
				});

		// Toggle literals: /tf on, /tf off, /tf enable, /tf disable [player]
		for (String toggle : getToggleCompletions(false)) {
			root.then(Commands.literal(toggle)
					.requires(source -> U.hasPermission(source.getSender(), "tempfly.toggle.self") || U.hasPermission(source.getSender(), "tempfly.toggle.other"))
					.executes(ctx -> {
						executeCommand(ctx.getSource().getSender(), new String[]{toggle});
						return Command.SINGLE_SUCCESS;
					})
					.then(Commands.argument("player", StringArgumentType.word())
							.requires(source -> U.hasPermission(source.getSender(), "tempfly.toggle.other"))
							.suggests(PLAYER_SUGGESTIONS)
							.executes(ctx -> {
								executeCommand(ctx.getSource().getSender(), new String[]{toggle, StringArgumentType.getString(ctx, "player")});
								return Command.SINGLE_SUCCESS;
							})
					)
			);
		}

		// Registered CommandTypes
		for (CommandType type : CommandType.values()) {
			if (!type.isEnabled(tempfly)) {
				continue;
			}
			for (String base : getCommandBases(type)) {
				LiteralArgumentBuilder<CommandSourceStack> sub = Commands.literal(base)
						.requires(source -> type.hasPermission(tempfly, source.getSender()));

				switch (type) {
				case GIVE:
				case SET:
				case REMOVE:
				case PAY:
					sub.executes(ctx -> {
						executeCommand(ctx.getSource().getSender(), new String[]{base});
						return Command.SINGLE_SUCCESS;
					}).then(Commands.argument("player", StringArgumentType.word())
							.suggests(PLAYER_SUGGESTIONS)
							.executes(ctx -> {
								executeCommand(ctx.getSource().getSender(), new String[]{base, StringArgumentType.getString(ctx, "player")});
								return Command.SINGLE_SUCCESS;
							})
							.then(Commands.argument("time", StringArgumentType.greedyString())
									.suggests(createTimeSuggestions(tempfly))
									.executes(ctx -> {
										String player = StringArgumentType.getString(ctx, "player");
										String time = StringArgumentType.getString(ctx, "time");
										executeTimeCommand(ctx.getSource().getSender(), base, player, time);
										return Command.SINGLE_SUCCESS;
									})
							)
					);
					break;

				case GIVE_ALL:
					sub.executes(ctx -> {
						executeCommand(ctx.getSource().getSender(), new String[]{base});
						return Command.SINGLE_SUCCESS;
					}).then(Commands.argument("time", StringArgumentType.greedyString())
							.suggests(createTimeSuggestions(tempfly))
							.executes(ctx -> {
								String time = StringArgumentType.getString(ctx, "time");
								executeTimeCommand(ctx.getSource().getSender(), base, null, time);
								return Command.SINGLE_SUCCESS;
							})
					);
					break;

				case TIME:
					sub.executes(ctx -> {
						executeCommand(ctx.getSource().getSender(), new String[]{base});
						return Command.SINGLE_SUCCESS;
					}).then(Commands.argument("player", StringArgumentType.word())
							.requires(source -> U.hasPermission(source.getSender(), "tempfly.time.other"))
							.suggests(PLAYER_SUGGESTIONS)
							.executes(ctx -> {
								executeCommand(ctx.getSource().getSender(), new String[]{base, StringArgumentType.getString(ctx, "player")});
								return Command.SINGLE_SUCCESS;
							})
					);
					break;

				case SPEED:
					sub.executes(ctx -> {
						executeCommand(ctx.getSource().getSender(), new String[]{base});
						return Command.SINGLE_SUCCESS;
					}).then(Commands.argument("speed", StringArgumentType.word())
							.suggests(createSpeedSuggestions(tempfly))
							.executes(ctx -> {
								executeCommand(ctx.getSource().getSender(), new String[]{base, StringArgumentType.getString(ctx, "speed")});
								return Command.SINGLE_SUCCESS;
							})
							.then(Commands.argument("player", StringArgumentType.word())
									.requires(source -> U.hasPermission(source.getSender(), "tempfly.speed.other"))
									.suggests(PLAYER_SUGGESTIONS)
									.executes(ctx -> {
										executeCommand(ctx.getSource().getSender(), new String[]{base, StringArgumentType.getString(ctx, "speed"), StringArgumentType.getString(ctx, "player")});
										return Command.SINGLE_SUCCESS;
									})
							)
					);
					break;

				case TRAIL_SET:
					sub.executes(ctx -> {
						executeCommand(ctx.getSource().getSender(), new String[]{base});
						return Command.SINGLE_SUCCESS;
					}).then(Commands.argument("arg1", StringArgumentType.word())
							.suggests(createTrailArg1Suggestions())
							.executes(ctx -> {
								executeCommand(ctx.getSource().getSender(), new String[]{base, StringArgumentType.getString(ctx, "arg1")});
								return Command.SINGLE_SUCCESS;
							})
							.then(Commands.argument("particle", StringArgumentType.word())
									.requires(source -> U.hasPermission(source.getSender(), "tempfly.trails.set.other"))
									.suggests(PARTICLE_SUGGESTIONS)
									.executes(ctx -> {
										executeCommand(ctx.getSource().getSender(), new String[]{base, StringArgumentType.getString(ctx, "arg1"), StringArgumentType.getString(ctx, "particle")});
										return Command.SINGLE_SUCCESS;
									})
							)
					);
					break;

				case TRAIL_REMOVE:
					sub.executes(ctx -> {
						executeCommand(ctx.getSource().getSender(), new String[]{base});
						return Command.SINGLE_SUCCESS;
					}).then(Commands.argument("player", StringArgumentType.word())
							.requires(source -> U.hasPermission(source.getSender(), "tempfly.trails.remove.other"))
							.suggests(PLAYER_SUGGESTIONS)
							.executes(ctx -> {
								executeCommand(ctx.getSource().getSender(), new String[]{base, StringArgumentType.getString(ctx, "player")});
								return Command.SINGLE_SUCCESS;
							})
					);
					break;

				case INFINITE:
				case BYPASS:
					sub.executes(ctx -> {
						executeCommand(ctx.getSource().getSender(), new String[]{base});
						return Command.SINGLE_SUCCESS;
					});
					for (String toggleVal : getToggleCompletions(false)) {
						sub.then(Commands.literal(toggleVal)
								.executes(ctx -> {
									executeCommand(ctx.getSource().getSender(), new String[]{base, toggleVal});
									return Command.SINGLE_SUCCESS;
								})
						);
					}
					break;

				case FLY:
					sub.executes(ctx -> {
						executeCommand(ctx.getSource().getSender(), new String[]{base});
						return Command.SINGLE_SUCCESS;
					}).then(Commands.argument("player", StringArgumentType.word())
							.requires(source -> U.hasPermission(source.getSender(), "tempfly.toggle.other"))
							.suggests(PLAYER_SUGGESTIONS)
							.executes(ctx -> {
								executeCommand(ctx.getSource().getSender(), new String[]{base, StringArgumentType.getString(ctx, "player")});
								return Command.SINGLE_SUCCESS;
							})
					);
					break;

				case TRAILS:
				case HELP:
				case RELOAD:
				case MIGRATE:
				default:
					sub.executes(ctx -> {
						executeCommand(ctx.getSource().getSender(), new String[]{base});
						return Command.SINGLE_SUCCESS;
					});
					break;
				}

				root.then(sub);
			}
		}

		// Registered hook commands
		for (Entry<String, Class<? extends TempFlyCommand>> entry : hookRegistry.entrySet()) {
			String base = entry.getKey();
			Class<? extends TempFlyCommand> clazz = entry.getValue();
			root.then(Commands.literal(base)
					.requires(source -> hasHookPermission(source.getSender(), clazz))
					.executes(ctx -> {
						executeCommand(ctx.getSource().getSender(), new String[]{base});
						return Command.SINGLE_SUCCESS;
					})
					.then(Commands.argument("args", StringArgumentType.greedyString())
							.executes(ctx -> {
								String remainingArgs = StringArgumentType.getString(ctx, "args");
								String[] split = remainingArgs.split("\\s+");
								String[] fullArgs = new String[split.length + 1];
								fullArgs[0] = base;
								System.arraycopy(split, 0, fullArgs, 1, split.length);
								executeCommand(ctx.getSource().getSender(), fullArgs);
								return Command.SINGLE_SUCCESS;
							})
					)
			);
		}

		registrar.register(root.build(), "TempFly base command", Arrays.asList("tf", "tfly"));
	}

	public static enum TimeType {
		SECONDS("s", "sec", "second", "seconds"),
		MINUTES("m", "min", "minute", "minutes"),
		HOURS("h", "hour", "hours"),
		DAYS("d", "day", "days");

		private final String[] base;

		private TimeType(String... base) {
			this.base = base;
		}

		public String[] getBase() {
			return base;
		}

		public String getCompletion() {
			return base[base.length - 1];
		}

		public TimeType valueOf(TimeUnit unit) {
			return TimeType.valueOf(unit.toString());
		}
	}

	public static enum CommandType {
		GIVE(CmdGive::new, "give"),
		GIVE_ALL(CmdGiveAll::new, "giveall"),
		RELOAD(CmdReload::new, "reload"),
		REMOVE(CmdRemove::new, "remove"),
		SET(CmdSet::new, "set"),
		TRAIL_REMOVE(CmdTrailRemove::new, "remove_trail"),
		TRAIL_SET(CmdTrailSet::new, "set_trail"),
		BYPASS(CmdBypass::new, "bypass"),
		FLY(CmdFly::new, "toggle"),
		HELP(CmdHelp::new, "help"),
		INFINITE(CmdInfinite::new, "infinite"),
		PAY(CmdPay::new, "pay"),
		SPEED(CmdSpeed::new, "speed"),
		TIME(CmdTime::new, "time"),
		TRAILS(CmdTrails::new, "trails"),
		MIGRATE(CmdMigrate::new, "migrate");

		private final java.util.function.BiFunction<TempFly, String[], TempFlyCommand> factory;
		private final String base;

		private CommandType(java.util.function.BiFunction<TempFly, String[], TempFlyCommand> factory, String base) {
			this.factory = factory;
			this.base = base;
		}

		public TempFlyCommand createCommand(TempFly tempfly, String[] args) {
			return factory.apply(tempfly, args);
		}

		public boolean isEnabled(TempFly tempfly) {
			return true;
		}

		public boolean hasPermission(TempFly tempfly, CommandSender s) {
			switch (this) {
			case GIVE:
				return U.hasPermission(s, "tempfly.give");
			case GIVE_ALL:
				return U.hasPermission(s, "tempfly.giveall");
			case RELOAD:
				return U.hasPermission(s, "tempfly.reload");
			case REMOVE:
				return U.hasPermission(s, "tempfly.remove");
			case SET:
				return U.hasPermission(s, "tempfly.set");
			case TRAIL_REMOVE:
				return U.hasPermission(s, "tempfly.trails.remove.self") || U.hasPermission(s, "tempfly.trails.remove.other");
			case TRAIL_SET:
				return U.hasPermission(s, "tempfly.trails.set.self") || U.hasPermission(s, "tempfly.trails.set.other");
			case BYPASS:
				return U.hasPermission(s, "tempfly.bypass.toggle");
			case FLY:
				return U.hasPermission(s, "tempfly.toggle.self") || U.hasPermission(s, "tempfly.toggle.other");
			case HELP:
				return U.hasPermission(s, "tempfly.help") || U.hasPermission(s, "tempfly.help.admin");
			case INFINITE:
				return U.hasPermission(s, "tempfly.infinite.toggle");
			case PAY:
				return V.payable && U.hasPermission(s, "tempfly.pay");
			case SPEED:
				return U.hasPermission(s, "tempfly.speed.self") || U.hasPermission(s, "tempfly.speed.other");
			case TIME:
				return U.hasPermission(s, "tempfly.time.self") || U.hasPermission(s, "tempfly.time.other");
			case TRAILS:
				return U.hasPermission(s, "tempfly.trails");
			case MIGRATE:
				return !U.isPlayer(s);
			default:
				return false;
			}
		}

		public String getBase() {
			return base;
		}
	}

}
