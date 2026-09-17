package com.moneybags.tempfly.aesthetic;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.DecimalFormat;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.time.TimeManager.Placeholder;
import com.moneybags.tempfly.user.FlightUser;
import com.moneybags.tempfly.util.Console;
import com.moneybags.tempfly.util.V;

/**
 * Optional reflection-based integration with MVdWPlaceholderAPI.
 * This avoids hard dependencies or local checked-in binaries while fully supporting
 * placeholder registration at runtime if MVdWPlaceholderAPI is installed.
 */
public class MvdWAPI {

	public static void initialize(TempFly tempfly) {
		if (!Bukkit.getPluginManager().isPluginEnabled("MVdWPlaceholderAPI")) {
			return;
		}

		try {
			Class<?> apiClass = Class.forName("be.maximvdw.placeholderapi.PlaceholderAPI");
			Class<?> replacerClass = Class.forName("be.maximvdw.placeholderapi.PlaceholderReplacer");
			Method registerMethod = apiClass.getMethod("registerPlaceholder", org.bukkit.plugin.Plugin.class, String.class, replacerClass);

			register(tempfly, registerMethod, replacerClass, "tempfly_time_formatted", p ->
				p != null ? tempfly.getTimeManager().getPlaceHolder(p, Placeholder.TIME_FORMATTED) : null
			);

			register(tempfly, registerMethod, replacerClass, "tempfly_time_days", p ->
				p != null ? tempfly.getTimeManager().getPlaceHolder(p, Placeholder.TIME_DAYS) : null
			);

			register(tempfly, registerMethod, replacerClass, "tempfly_time_hours", p ->
				p != null ? tempfly.getTimeManager().getPlaceHolder(p, Placeholder.TIME_HOURS) : null
			);

			register(tempfly, registerMethod, replacerClass, "tempfly_time_minutes", p ->
				p != null ? tempfly.getTimeManager().getPlaceHolder(p, Placeholder.TIME_MINUTES) : null
			);

			register(tempfly, registerMethod, replacerClass, "tempfly_time_seconds", p ->
				p != null ? tempfly.getTimeManager().getPlaceHolder(p, Placeholder.TIME_SECONDS) : null
			);

			register(tempfly, registerMethod, replacerClass, "tempfly_time_seconds_total", p ->
				p != null ? tempfly.getTimeManager().getPlaceHolder(p, Placeholder.TIME_SECONDS_TOTAL) : null
			);

			register(tempfly, registerMethod, replacerClass, "tempfly_list_name", p -> {
				if (p != null && p.isOnline()) {
					FlightUser u = tempfly.getFlightManager().getUser(p);
					return u != null ? u.getListPlaceholder() : "";
				}
				return null;
			});

			register(tempfly, registerMethod, replacerClass, "tempfly_name_tag", p -> {
				if (p != null && p.isOnline()) {
					FlightUser u = tempfly.getFlightManager().getUser(p);
					return u != null ? u.getTagPlaceholder() : "";
				}
				return null;
			});

			register(tempfly, registerMethod, replacerClass, "tempfly_infinite", p -> {
				if (p != null && p.isOnline()) {
					FlightUser u = tempfly.getFlightManager().getUser(p);
					String yes = V.placeholderInfiniteYes != null ? V.placeholderInfiniteYes : (V.infinity != null ? V.infinity : "∞");
					String no = V.placeholderInfiniteNo != null ? V.placeholderInfiniteNo : "";
					return u != null && u.hasInfiniteFlight() ? yes : no;
				}
				return null;
			});

			register(tempfly, registerMethod, replacerClass, "tempfly_is_flying", p -> {
				if (p != null && p.isOnline()) {
					FlightUser u = tempfly.getFlightManager().getUser(p);
					String yes = V.placeholderFlyingYes != null ? V.placeholderFlyingYes : "true";
					String no = V.placeholderFlyingNo != null ? V.placeholderFlyingNo : "false";
					return u != null && u.hasFlightEnabled() ? yes : no;
				}
				return null;
			});

			register(tempfly, registerMethod, replacerClass, "tempfly_speed", p -> {
				if (p != null && p.isOnline()) {
					return new DecimalFormat("#.##").format(p.getFlySpeed() * 10);
				}
				return null;
			});

			Console.info("Successfully registered MVdWPlaceholderAPI placeholders via reflection hook.");
		} catch (Throwable t) {
			Console.warn("Failed to hook into MVdWPlaceholderAPI: " + t.getMessage());
		}
	}

	private static void register(TempFly plugin, Method registerMethod, Class<?> replacerInterface, String placeholder, Function<Player, String> resolver) throws Exception {
		Object proxy = Proxy.newProxyInstance(
			replacerInterface.getClassLoader(),
			new Class<?>[] { replacerInterface },
			new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					if ("onPlaceholderReplace".equals(method.getName()) && args != null && args.length == 1) {
						Object event = args[0];
						Method getPlayerMethod = event.getClass().getMethod("getPlayer");
						Player player = (Player) getPlayerMethod.invoke(event);
						return resolver.apply(player);
					}
					return null;
				}
			}
		);
		registerMethod.invoke(null, plugin, placeholder, proxy);
	}
}
