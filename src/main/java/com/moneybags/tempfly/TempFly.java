package com.moneybags.tempfly;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.moneybags.tempfly.aesthetic.ClipAPI;
import com.moneybags.tempfly.aesthetic.MvdWAPI;
import com.moneybags.tempfly.aesthetic.particle.Particles;
import com.moneybags.tempfly.command.CommandManager;
import com.moneybags.tempfly.fly.FlightManager;

import com.moneybags.tempfly.hook.HookManager;
import com.moneybags.tempfly.hook.TempFlyHook;
import com.moneybags.tempfly.message.MessageService;
import com.moneybags.tempfly.safety.FallSafetyService;
import com.moneybags.tempfly.storage.UserRepository;
import com.moneybags.tempfly.time.AsyncTimeParameters;
import com.moneybags.tempfly.time.TimeManager;
import com.moneybags.tempfly.user.FlightUser;
import com.moneybags.tempfly.util.Console;
import com.moneybags.tempfly.util.ParticleTask;
import com.moneybags.tempfly.util.U;
import com.moneybags.tempfly.util.V;
import com.moneybags.tempfly.util.AutoSave;
import com.moneybags.tempfly.util.data.DataBridge;
import com.moneybags.tempfly.util.data.Files;

public class TempFly extends JavaPlugin {
	
	private static TempFly instance;
	public static TempFly getInstance() {
		return instance;
	}

	// static abusers unite
	private static TempFlyAPI tfApi;
	public static TempFlyAPI getAPI() {
		return tfApi;
	}

	
	private MessageService messageService;
	private FallSafetyService fallSafetyService;
	private HookManager hooks;
	private DataBridge bridge;
	private UserRepository userRepository;
	private FlightManager flight;
	private TimeManager time;
	private CommandManager commands;

	private BukkitTask autosave;
	private BukkitTask particleTask;
	
	public MessageService getMessageService() {
		return messageService;
	}

	public FallSafetyService getFallSafetyService() {
		return fallSafetyService;
	}

	public HookManager getHookManager() {
		return hooks;
	}
	
	public DataBridge getDataBridge() {
		return bridge;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}
	
	public FlightManager getFlightManager() {
		return flight;
	}
	
	public TimeManager getTimeManager() {
		return time;
	}
	
	public CommandManager getCommandManager() {
		return commands;
	}


	
	@Override
	public void onEnable() {
		instance = this;
		this.messageService = new MessageService();
		this.fallSafetyService = new FallSafetyService(this);
		Console.setLogger(this.getLogger());
		
		Files.createFiles(this);
		V.loadValues();
		
		try {
			this.bridge = new DataBridge(this);
			this.userRepository = UserRepository.create(this.bridge);
		} catch (IOException | SQLException e1) {
			e1.printStackTrace();
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		tfApi = new TempFlyAPI(this);
		this.flight   = new FlightManager(this);
		this.time     = new TimeManager(this);
		this.hooks    = new HookManager(this);
		this.commands = new CommandManager(this);
		hooks.loadInternalGenres();
		initializeAesthetics();


		autosave = new AutoSave(bridge).runTaskTimerAsynchronously(this, V.save * 20 * 60, V.save * 20 * 60);
		
		// Support "/reload"
		for (Player p: Bukkit.getOnlinePlayers()) {
			flight.addUser(p);
		}
	}
	
	private void initializeAesthetics() {
		Particles.initialize(this);
		
		if (particleTask != null) {
			particleTask.cancel();
			particleTask = null;
		}
		if (V.particles) {
			particleTask = new ParticleTask(this).runTaskTimer(this, 0, 5);
		}

		if (Bukkit.getPluginManager().isPluginEnabled("MVdWPlaceholderAPI")) {
			Console.info("Initializing MvdwAPI");
			MvdWAPI.initialize(this);
		}
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			Console.info("Initializing ClipAPI");
			ClipAPI.initialize(this);
		}
	}
	

	
	@Override
	public void onDisable() {
		if (particleTask != null) {
			particleTask.cancel();
			particleTask = null;
		}
		if (autosave != null) {
			autosave.cancel();
			autosave = null;
		}
		if (fallSafetyService != null) {
			fallSafetyService.clearAll();
		}
		if (messageService != null) {
			messageService.clearCache();
		}
		if (flight != null) {
			flight.onDisable();
		}

		if (userRepository != null) {
			userRepository.close();
		}
		if (bridge != null) {
			bridge.onDisable();
		}
		instance = null;
	}
	
	/*
	 * Reload the plugin, this is the method called upon command /tempfly reload
	 */
	//TODO reload hooks
	public void reloadTempfly() {
		if (messageService != null) {
			messageService.clearCache();
		}
		
		if (userRepository != null) {
			userRepository.flush();
		}
		bridge.commitAll();
		Files.createFiles(this);
		V.loadValues();
		initializeAesthetics();
		
		flight.onTempflyReload();
		hooks.onTempflyReload();
		
		if (autosave != null) {
			autosave.cancel();
			autosave = new AutoSave(bridge).runTaskTimerAsynchronously(this, 0, V.save * 20 * 60);
		}
	}
	
}
