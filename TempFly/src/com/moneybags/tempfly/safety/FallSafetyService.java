package com.moneybags.tempfly.safety;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Dedicated fall-safety service providing robust, leak-free damage negation
 * on involuntary flight revocations (combat, region border exit, expiration).
 */
public class FallSafetyService implements Listener {

	private static final long DEFAULT_TIMEOUT_TICKS = 120L; // 6 seconds

	private static final BukkitTask DUMMY_TASK = new BukkitTask() {
		@Override
		public int getTaskId() {
			return -1;
		}

		@Override
		public Plugin getOwner() {
			return null;
		}

		@Override
		public boolean isSync() {
			return false;
		}

		@Override
		public void cancel() {}

		@Override
		public boolean isCancelled() {
			return false;
		}
	};

	private final Plugin plugin;
	private final Map<UUID, BukkitTask> activeProtections;

	public FallSafetyService(Plugin plugin) {
		this.plugin = plugin;
		this.activeProtections = new ConcurrentHashMap<>();
		if (plugin != null && plugin.getServer() != null && plugin.getServer().getPluginManager() != null) {
			plugin.getServer().getPluginManager().registerEvents(this, plugin);
		}
	}

	/**
	 * Adds fall protection to a player with default timeout (120 ticks).
	 */
	public void addProtection(Player player) {
		if (player == null) return;
		addProtection(player.getUniqueId(), DEFAULT_TIMEOUT_TICKS);
	}

	/**
	 * Adds fall protection to a player with custom timeout.
	 */
	public void addProtection(UUID uuid, long durationTicks) {
		if (uuid == null) return;
		removeProtection(uuid);

		BukkitTask task = DUMMY_TASK;
		if (plugin != null && Bukkit.getServer() != null) {
			task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
				activeProtections.remove(uuid);
			}, Math.max(1L, durationTicks));
		}

		activeProtections.put(uuid, task != null ? task : DUMMY_TASK);
	}

	/**
	 * Removes fall protection for a player.
	 */
	public void removeProtection(UUID uuid) {
		if (uuid == null) return;
		BukkitTask existing = activeProtections.remove(uuid);
		if (existing != null && existing != DUMMY_TASK) {
			existing.cancel();
		}
	}

	/**
	 * Checks if a player currently has active fall protection.
	 */
	public boolean hasProtection(UUID uuid) {
		return uuid != null && activeProtections.containsKey(uuid);
	}

	/**
	 * Clears all active fall protections (e.g. on plugin disable).
	 */
	public void clearAll() {
		for (BukkitTask task : activeProtections.values()) {
			if (task != null) {
				task.cancel();
			}
		}
		activeProtections.clear();
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getCause() != DamageCause.FALL) {
			return;
		}
		Entity victim = event.getEntity();
		if (!(victim instanceof Player player)) {
			return;
		}

		if (hasProtection(player.getUniqueId())) {
			event.setCancelled(true);
			removeProtection(player.getUniqueId());
			player.setFallDistance(0);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		removeProtection(event.getPlayer().getUniqueId());
	}
}
