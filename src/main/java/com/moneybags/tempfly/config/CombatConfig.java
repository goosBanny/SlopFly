package com.moneybags.tempfly.config;

import org.bukkit.configuration.file.FileConfiguration;

public record CombatConfig(
		int tagPvp,
		int tagPve,
		boolean attackMob,
		boolean attackPlayer,
		boolean attackedByMob,
		boolean attackedByPlayer,
		boolean attackedBySelf
) {
	public int tagPvpTicks() {
		return tagPvp * 20;
	}

	public int tagPveTicks() {
		return tagPve * 20;
	}

	public static CombatConfig from(FileConfiguration config) {
		int tagPvp = config.contains("general.combat.pvp_tag")
				? config.getInt("general.combat.pvp_tag")
				: config.getInt("general.combat.tag.pvp", 10);
		int tagPve = config.contains("general.combat.pve_tag")
				? config.getInt("general.combat.pve_tag")
				: config.getInt("general.combat.tag.pve", 10);
		boolean attackMob = config.contains("general.combat.attack_mob")
				? config.getBoolean("general.combat.attack_mob")
				: config.getBoolean("general.combat.disable.attack_mob", true);
		boolean attackPlayer = config.contains("general.combat.attack_player")
				? config.getBoolean("general.combat.attack_player")
				: config.getBoolean("general.combat.disable.attack_player", true);
		boolean attackedByMob = config.contains("general.combat.attacked_by_mob")
				? config.getBoolean("general.combat.attacked_by_mob")
				: config.getBoolean("general.combat.disable.attacked_by_mob", true);
		boolean attackedByPlayer = config.contains("general.combat.attacked_by_player")
				? config.getBoolean("general.combat.attacked_by_player")
				: config.getBoolean("general.combat.disable.attacked_by_player", true);
		boolean attackedBySelf = config.contains("general.combat.self_inflicted")
				? config.getBoolean("general.combat.self_inflicted")
				: config.getBoolean("general.combat.disable.attacked_by_self", true);

		return new CombatConfig(
				tagPvp, tagPve, attackMob, attackPlayer,
				attackedByMob, attackedByPlayer, attackedBySelf
		);
	}
}
