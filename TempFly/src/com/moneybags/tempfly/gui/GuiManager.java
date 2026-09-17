package com.moneybags.tempfly.gui;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.gui.abstraction.Page;

public class GuiManager implements Listener {

	private final TempFly tempfly;
	private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<>();
	
	public GuiManager(TempFly tempfly) {
		this.tempfly = tempfly;
		tempfly.getServer().getPluginManager().registerEvents(this, tempfly);
	}
	
	public TempFly getTempFly() {
		return tempfly;
	}
	
	public Collection<GuiSession> getSessions() {
		return sessions.values();
	}
	
	public void endAllSessions() {
		for (GuiSession session : sessions.values()) {
			session.endSession();
		}
		sessions.clear();
	}
	
	public GuiSession getSession(Player p) {
		if (p == null) return null;
		return sessions.get(p.getUniqueId());
	}
	
	public GuiSession createSession(Player p) {
		if (p == null) return null;
		UUID u = p.getUniqueId();
		GuiSession old = sessions.remove(u);
		if (old != null) {
			old.endSession();
		}
		GuiSession session = new GuiSession(p);
		sessions.put(u, session);
		return session;
	}
	
	@EventHandler (priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent e) {
		GuiSession session = sessions.remove(e.getPlayer().getUniqueId());
		if (session != null) {
			session.endSession();
		}
	}

	@EventHandler (priority = EventPriority.MONITOR)
	public void onKick(org.bukkit.event.player.PlayerKickEvent e) {
		GuiSession session = sessions.remove(e.getPlayer().getUniqueId());
		if (session != null) {
			session.endSession();
		}
	}
	
	@EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on(InventoryCloseEvent e) {
		if (!(e.getPlayer() instanceof Player)) {
			return;
		}
		Player p = (Player) e.getPlayer();
		GuiSession session = sessions.get(p.getUniqueId());
		if (session != null) {
			Page page = session.getPage();
			if (page != null) {
				page.onClose(e);	
			}
			if (!session.saveSession()) {
				sessions.remove(p.getUniqueId());	
			}
		}
	}
	
	@EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on(InventoryClickEvent e) {
		if (!(e.getWhoClicked() instanceof Player)) {
			return;
		}
		Player p = (Player) e.getWhoClicked();
		GuiSession session = sessions.get(p.getUniqueId());
		if (session == null) {
			return;
		}
		e.setCancelled(true);
		if (e.getClickedInventory() == null) {
			return;
		}
		int slot = e.getRawSlot();
		if (session.getPage() != null) {
			session.getPage().runPage(slot, e);
		}
	}
	
	@EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on(InventoryDragEvent e) {
		if (!(e.getWhoClicked() instanceof Player)) {
			return;
		}
		Player p = (Player) e.getWhoClicked();
		if (sessions.containsKey(p.getUniqueId())) {
			e.setCancelled(true);
		}
	}	

}
