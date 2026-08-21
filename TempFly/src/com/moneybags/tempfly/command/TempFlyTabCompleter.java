package com.moneybags.tempfly.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import com.moneybags.tempfly.util.U;
import com.moneybags.tempfly.util.V;

public class TempFlyTabCompleter implements TabCompleter, Listener {
	
	private final CommandManager manager;
	
	public TempFlyTabCompleter(CommandManager manager) {
		this.manager = manager;
		try {
			Class.forName("org.bukkit.event.server.TabCompleteEvent");
			Bukkit.getServer().getPluginManager().registerEvents(this, manager.getTempFly());
		} catch (ClassNotFoundException ignored) {}
	}
	
	@Override
	public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
		if (V.disableTab && !U.hasPermission(s, "tempfly.disable_tab.bypass")) {
			return new ArrayList<>();
		}
		if (args.length == 0 || args.length == 1) {
			List<String> matches = new ArrayList<>();
			String partial = args.length == 1 ? args[0] : "";
			if (U.hasPermission(s, "tempfly.toggle.self") || U.hasPermission(s, "tempfly.toggle.other")) {
				for (String toggle : manager.getToggleCompletions(false)) {
					if (toggle.toLowerCase().startsWith(partial.toLowerCase())) {
						matches.add(toggle);
					}
				}
			}
			return matches;
		} else {
			TempFlyCommand command = manager.getCommand(args);
			return command == null || !command.hasPermission(s) ? new ArrayList<>() : command.getPotentialArguments(s);
		}
	}
	
	@EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on(TabCompleteEvent e) {
		String[] args = e.getBuffer().split(" ");
		if (args.length == 0) {
			return;
		}
		if (!args[0].equalsIgnoreCase("/fly")) {
			return;
		}
		
		List<String> completions = new ArrayList<>();
		Arrays.asList(U.skipArray(args, 1)).forEach(completions::add);
		if (e.getBuffer().endsWith(" ")) {
			completions.add("");
		}
		e.setCompletions(onTabComplete(e.getSender(), null, "", completions.toArray(new String[0])));
	}
}
