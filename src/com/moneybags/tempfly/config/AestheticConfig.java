package com.moneybags.tempfly.config;

import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

public record AestheticConfig(
		boolean actionBar,
		String actionText,
		String warningTitle,
		String warningSubtitle,
		List<Long> warningTimes,
		boolean particles,
		String particleType,
		boolean particleDefault,
		String infinitySymbol,
		boolean listDynamic,
		String listName,
		boolean tagDynamic,
		String tagName,
		String placeholderInfiniteYes,
		String placeholderInfiniteNo,
		String placeholderFlyingYes,
		String placeholderFlyingNo,
		String placeholderUnknownUser,
		String placeholderInvalid
) {
	public static AestheticConfig from(FileConfiguration config, FileConfiguration lang) {
		boolean actionBar = config.getBoolean("aesthetic.action_bar.enabled", true);
		String actionText = config.getString("aesthetic.action_bar.text", "&6Flight meter&7: {FORMATTED_TIME}");

		boolean warningEnabled = config.getBoolean("aesthetic.warning.enabled", true);
		String warningTitle = config.getString("aesthetic.warning.title", "&cWarning!");
		String warningSubtitle = config.getString("aesthetic.warning.subtitle", "&eYou have {FORMATTED_TIME} remaining!");
		List<Long> warningTimes = config.getLongList("aesthetic.warning.seconds");

		boolean particles = config.contains("aesthetic.identifier.particles.enabled")
				? config.getBoolean("aesthetic.identifier.particles.enabled")
				: config.getBoolean("aesthetic.particles.appearance.enabled", true);
		String particleType = config.contains("aesthetic.identifier.particles.type")
				? config.getString("aesthetic.identifier.particles.type", "VILLAGER_HAPPY")
				: config.getString("aesthetic.particles.appearance.type", "VILLAGER_HAPPY");
		boolean particleDefault = config.contains("aesthetic.identifier.particles.display_by_default")
				? config.getBoolean("aesthetic.identifier.particles.display_by_default")
				: config.getBoolean("aesthetic.particles.appearance.default", false);

		String infinitySymbol = lang != null ? lang.getString("aesthetic.symbols.infinity", "∞") : "∞";

		boolean listDynamic = config.contains("aesthetic.identifier.tab_list.enabled")
				? config.getBoolean("aesthetic.identifier.tab_list.enabled")
				: config.getBoolean("aesthetic.identifier.tab_list.dynamic", false);
		String listName = config.getString("aesthetic.identifier.tab_list.name", "{PLAYER}");

		boolean tagDynamic = config.contains("aesthetic.identifier.name_tag.enabled")
				? config.getBoolean("aesthetic.identifier.name_tag.enabled")
				: config.getBoolean("aesthetic.identifier.name_tag.dynamic", false);
		String tagName = config.getString("aesthetic.identifier.name_tag.name", "{PLAYER}");

		String placeholderInfiniteYes = lang != null ? lang.getString("aesthetic.placeholders.infinite_yes", infinitySymbol) : infinitySymbol;
		String placeholderInfiniteNo = lang != null ? lang.getString("aesthetic.placeholders.infinite_no", "") : "";
		String placeholderFlyingYes = lang != null ? lang.getString("aesthetic.placeholders.flying_yes", "true") : "true";
		String placeholderFlyingNo = lang != null ? lang.getString("aesthetic.placeholders.flying_no", "false") : "false";
		String placeholderUnknownUser = lang != null ? lang.getString("aesthetic.placeholders.unknown_user", "0s") : "0s";
		String placeholderInvalid = lang != null ? lang.getString("aesthetic.placeholders.invalid", "") : "";

		return new AestheticConfig(
				actionBar, actionText, warningTitle, warningSubtitle,
				Collections.unmodifiableList(warningTimes),
				particles, particleType, particleDefault, infinitySymbol,
				listDynamic, listName, tagDynamic, tagName,
				placeholderInfiniteYes, placeholderInfiniteNo,
				placeholderFlyingYes, placeholderFlyingNo,
				placeholderUnknownUser, placeholderInvalid
		);
	}
}
