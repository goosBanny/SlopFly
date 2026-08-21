package com.moneybags.tempfly.util;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.primitives.Doubles;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class U {

	private static final Pattern LOCATION_STRING_PATTERN = Pattern.compile("~");
	private static final String PREFIX = "{PREFIX}";

	public static void command(String s) {
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), s);
	}
	
	public static boolean isPlayer(CommandSender s){
		return s instanceof Player;
	}
	
	public static String locationToString(Location loc) {
		if (loc == null) {
			return null;
		}

		return String.format(
				"%s~%d~%d~%d",
				loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()
		);
	}
	
	public static Location locationFromString(String loc) {
		String[] args = LOCATION_STRING_PATTERN.split(loc);

		if (args.length < 4) {
			return null;
		}

		World world = Bukkit.getWorld(args[0]);

		if (world == null) {
			return null;
		}

		Double x = Doubles.tryParse(args[1]);
		Double y = Doubles.tryParse(args[2]);
		Double z = Doubles.tryParse(args[3]);

		return (x == null || y == null || z == null) ? null : new Location(world, x, y , z);
	}
	
	private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

	public static String cc(String m) {
		if (m == null || m.isEmpty()) {
			return "";
		}

		if (m.contains("&#")) {
			java.util.regex.Matcher matcher = HEX_PATTERN.matcher(m);
			StringBuilder sb = new StringBuilder(m.length() + 32);
			while (matcher.find()) {
				String hex = matcher.group(1);
				StringBuilder replacement = new StringBuilder(14);
				replacement.append(ChatColor.COLOR_CHAR).append('x');
				for (int i = 0; i < 6; i++) {
					replacement.append(ChatColor.COLOR_CHAR).append(hex.charAt(i));
				}
				matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement.toString()));
			}
			matcher.appendTail(sb);
			m = sb.toString();
		}

		return ChatColor.translateAlternateColorCodes('&', m);
	}
	
	public static String strip(String m) {
		return ChatColor.stripColor(U.cc(m));
	}
	
	public static void m(CommandSender p, String s) {
		if (s == null || s.equals(PREFIX) || s.length() == 0) {
			return;
		}

		String pfx = V.prefix != null ? V.prefix : "";
		p.sendMessage(s.replace(PREFIX, pfx));
	}
	
	public static void m(OfflinePlayer p, String s) {
		if (p.isOnline()) {
			m((Player) p, s);
		}
	}
	
	public static void m(Player p, String s) {
		if (s == null || s.isEmpty() || (V.prefix != null && s.equals(V.prefix)) || s.equals(PREFIX)) {
			return;
		}

		String pfx = V.prefix != null ? V.prefix : "";
		p.sendMessage(s.replace(PREFIX, pfx));
	}
	
	public static boolean hasPermission(CommandSender s, String perm){
		return !isPlayer(s) || s.hasPermission(perm);
	}
	
	public static String locToString(Location loc) {
		return locationToString(loc);
	}
	
	public static Location locFromString(String loc) {
		return locationFromString(loc);
	}
	
	public static ItemStack getConfigItem(FileConfiguration config, String path) {
		ConfigurationSection section = config.getConfigurationSection(path);

		if (section == null) {
			return new ItemStack(Material.STONE);
		}

		ItemStack item = new ItemStack(Material.STONE, Math.max(1, section.getInt("amount", 1)));
		ItemMeta meta = item.getItemMeta();
		
		meta.setDisplayName(cc(ChatColor.RESET + section.getString("name", "&cThis item is broken. :'(")));
		meta.setLore(
				section.getStringList("lore").stream()
						.map(it -> cc(ChatColor.RESET + it))
						.collect(Collectors.toList())
		);

		item.setItemMeta(meta);
		return item;
	}
	
	public static String arrayToString(Object[] array, String divider) {
		if (array == null) {
			return null;
		}

		return Joiner.on(Strings.nullToEmpty(divider)).join(array);
	}
	
	public static String[] skipArray(String[] array, int skip) {
		if (array.length <= skip) {
			return new String[0];
		}

		return Arrays.copyOfRange(array, skip, array.length);
	}

}
