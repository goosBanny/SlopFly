package com.moneybags.tempfly.command;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import org.bukkit.command.CommandSender;

import com.moneybags.tempfly.TempFly;

/**
 * Adapter allowing existing TempFlyCommand implementations to function as SubCommands seamlessly.
 */
public class LegacyCommandAdapter implements SubCommand {

	private final TempFly tempfly;
	private final String name;
	private final List<String> aliases;
	private final BiFunction<TempFly, String[], TempFlyCommand> factory;

	public LegacyCommandAdapter(TempFly tempfly, String name, List<String> aliases, BiFunction<TempFly, String[], TempFlyCommand> factory) {
		this.tempfly = Objects.requireNonNull(tempfly, "tempfly cannot be null");
		this.name = Objects.requireNonNull(name, "name cannot be null");
		this.aliases = aliases != null ? aliases : Collections.emptyList();
		this.factory = Objects.requireNonNull(factory, "factory cannot be null");
	}

	public LegacyCommandAdapter(TempFly tempfly, String name, List<String> aliases, Class<? extends TempFlyCommand> commandClass) {
		this(tempfly, name, aliases, (tf, args) -> {
			try {
				return commandClass.getConstructor(TempFly.class, String[].class).newInstance(tf, args);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<String> getAliases() {
		return aliases;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		try {
			// Construct the full argument array [name, arg0, arg1, ...] expected by legacy command implementations
			String[] fullArgs = new String[args.length + 1];
			fullArgs[0] = name;
			System.arraycopy(args, 0, fullArgs, 1, args.length);

			TempFlyCommand instance = factory.apply(tempfly, fullArgs);
			if (instance != null) {
				instance.executeAs(sender);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		try {
			String[] fullArgs = new String[args.length + 1];
			fullArgs[0] = name;
			System.arraycopy(args, 0, fullArgs, 1, args.length);

			TempFlyCommand instance = factory.apply(tempfly, fullArgs);
			if (instance != null) {
				return instance.getPotentialArguments(sender);
			}
		} catch (Exception e) {
			// Ignore tab completion errors
		}
		return Collections.emptyList();
	}
}
