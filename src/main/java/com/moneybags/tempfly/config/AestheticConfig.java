package com.moneybags.tempfly.config;

import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

public record AestheticConfig(
		boolean actionBar,
		String actionText,
		boolean warningEnabled,
		String warningTitle,
		String warningSubtitle,
		List<Long> warningTimes,
		boolean particles,
		String particleType,
		boolean particleDefault,
		String infinitySymbol,
		String placeholderInfiniteYes,
		String placeholderInfiniteNo,
		String placeholderFlyingYes,
		String placeholderFlyingNo,
		String placeholderUnknownUser,
		String placeholderInvalid,
		String placeholderTimeDays,
		String placeholderTimeHours,
		String placeholderTimeMinutes,
		String placeholderTimeSeconds
) {
	public static AestheticConfig from(FileConfiguration config, FileConfiguration lang) {
		boolean actionBar = config.getBoolean("aesthetic.action_bar.enabled", true);
		String actionText = lang != null && lang.contains("aesthetic.action_bar.text")
				? lang.getString("aesthetic.action_bar.text")
				: config.getString("aesthetic.action_bar.text", "<#4FD2FC>✈ <white>Flight: <#4FD2FC>{FORMATTED_TIME}");

		boolean warningEnabled = config.getBoolean("aesthetic.warning.enabled", true);
		String warningTitle = lang != null && lang.contains("aesthetic.warning.title")
				? lang.getString("aesthetic.warning.title")
				: config.getString("aesthetic.warning.title", "<red><bold>WARNING!</bold></red>");
		String warningSubtitle = lang != null && lang.contains("aesthetic.warning.subtitle")
				? lang.getString("aesthetic.warning.subtitle")
				: config.getString("aesthetic.warning.subtitle", "<yellow>You have <white>{FORMATTED_TIME}</white> of flight remaining!</yellow>");
		List<Long> warningTimes = config.getLongList("aesthetic.warning.seconds");

		boolean particles = config.contains("aesthetic.particles.enabled")
				? config.getBoolean("aesthetic.particles.enabled")
				: config.getBoolean("aesthetic.identifier.particles.enabled", false);
		String particleType = config.contains("aesthetic.particles.type")
				? config.getString("aesthetic.particles.type", "VILLAGER_HAPPY")
				: config.getString("aesthetic.identifier.particles.type", "VILLAGER_HAPPY");
		boolean particleDefault = config.contains("aesthetic.particles.display_by_default")
				? config.getBoolean("aesthetic.particles.display_by_default")
				: config.getBoolean("aesthetic.identifier.particles.display_by_default", false);

		String infinitySymbol = lang != null ? lang.getString("aesthetic.symbols.infinity", "∞") : "∞";

		String placeholderInfiniteYes = lang != null ? lang.getString("aesthetic.placeholders.infinite_yes", infinitySymbol) : infinitySymbol;
		String placeholderInfiniteNo = lang != null ? lang.getString("aesthetic.placeholders.infinite_no", "") : "";
		String placeholderFlyingYes = lang != null ? lang.getString("aesthetic.placeholders.flying_yes", "true") : "true";
		String placeholderFlyingNo = lang != null ? lang.getString("aesthetic.placeholders.flying_no", "false") : "false";
		String placeholderUnknownUser = lang != null ? lang.getString("aesthetic.placeholders.unknown_user", "0s") : "0s";
		String placeholderInvalid = lang != null ? lang.getString("aesthetic.placeholders.invalid", "") : "";

		String placeholderTimeDays = lang != null ? lang.getString("aesthetic.placeholders.time.days",
				lang.getString("aesthetic.featherboard.days", "{DAYS}d ")) : "{DAYS}d ";
		String placeholderTimeHours = lang != null ? lang.getString("aesthetic.placeholders.time.hours",
				lang.getString("aesthetic.featherboard.hours", "{HOURS}h ")) : "{HOURS}h ";
		String placeholderTimeMinutes = lang != null ? lang.getString("aesthetic.placeholders.time.minutes",
				lang.getString("aesthetic.featherboard.minutes", "{MINUTES}m ")) : "{MINUTES}m ";
		String placeholderTimeSeconds = lang != null ? lang.getString("aesthetic.placeholders.time.seconds",
				lang.getString("aesthetic.featherboard.seconds", "{SECONDS}s")) : "{SECONDS}s";

		return new AestheticConfig(
				actionBar, actionText, warningEnabled, warningTitle, warningSubtitle,
				Collections.unmodifiableList(warningTimes),
				particles, particleType, particleDefault, infinitySymbol,
				placeholderInfiniteYes, placeholderInfiniteNo,
				placeholderFlyingYes, placeholderFlyingNo,
				placeholderUnknownUser, placeholderInvalid,
				placeholderTimeDays, placeholderTimeHours,
				placeholderTimeMinutes, placeholderTimeSeconds
		);
	}
}
