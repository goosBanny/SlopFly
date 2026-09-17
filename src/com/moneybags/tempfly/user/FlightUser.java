package com.moneybags.tempfly.user;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.moneybags.tempfly.aesthetic.particle.Particles;
import com.moneybags.tempfly.environment.FlightEnvironment;
import com.moneybags.tempfly.environment.RelativeTimeRegion;
import com.moneybags.tempfly.fly.FlightEngine;
import com.moneybags.tempfly.fly.FlightManager;
import com.moneybags.tempfly.fly.FlightState;
import com.moneybags.tempfly.fly.RequirementProvider;
import com.moneybags.tempfly.fly.RequirementProvider.InquiryType;
import com.moneybags.tempfly.fly.result.FlightResult;
import com.moneybags.tempfly.hook.region.CompatRegion;
import com.moneybags.tempfly.storage.UserFlightData;
import com.moneybags.tempfly.storage.UserRepository;
import com.moneybags.tempfly.time.TimeManager;
import com.moneybags.tempfly.util.Console;
import com.moneybags.tempfly.util.U;
import com.moneybags.tempfly.util.V;
import com.moneybags.tempfly.util.data.DataBridge;
import com.moneybags.tempfly.util.data.DataPointer;
import com.moneybags.tempfly.util.data.DataBridge.DataValue;


public class FlightUser {
	
	
	private final FlightManager manager;
	private final TimeManager timeManager;
	private final Player p;
	private final UserEnvironment environment;
	private final FlightState state;
	private final FlightEngine engine;
	
	private BukkitTask initialTask, enforceTask;

	public FlightUser(Player p, FlightManager manager, UserFlightData data) {
		this(p, manager, data.getTime(), data.getTrail(), data.isInfinite(), data.isBypass(),
				data.isLoggedInFlight(), data.isCompatLoggedInFlight(), data.getSpeed());
	}

	public FlightUser(Player p, FlightManager manager,
			double time, String particle, boolean infinite, boolean bypass, boolean logged, boolean compatLogged,
			double selectedSpeed) {
		this.manager = manager;
		this.timeManager = manager.getTempFly().getTimeManager();
		
		this.p = p;
		this.state = new FlightState(p.getUniqueId(), time, particle, infinite, bypass, selectedSpeed);
		this.engine = new FlightEngine();
		
		this.environment = new UserEnvironment(this, p);
		
		manager.updateLocation(this, p.getLocation(), p.getLocation(), true, true);
		
		initialTask = Bukkit.getScheduler().runTaskLater(manager.getTempFly(), new InitialTask(logged, compatLogged), 1);
	}

	public FlightState getState() {
		return state;
	}

	public FlightEngine getEngine() {
		return engine;
	}
	
	private class InitialTask implements Runnable {
		
		boolean
		logged,
		compatLogged;
		
		public InitialTask(boolean logged, boolean compatLogged) {
			this.logged = logged;
			this.compatLogged = compatLogged;
		}
		
		@Override
		public void run() {
			if (logged && (hasInfiniteFlight() || state.getTime() > 0) && V.autoFly) {
				Console.debug("--| Player is flight logged");
				if (!enableFlight()) {
					sendRequirementMessage();
					enforce(1);
				} else {
					applySpeedCorrect(true, 0);
				}
				
				
			} else if (!compatLogged) {
				// Compat flight log is when the player logs out while flying but not with tempfly.
				// We want to save this value so tempfly doesnt break other plugins flight features.
				Console.debug("--| Player is not compat flight logged");
				enforce(1);
			}
			manager.getTempFly().getDataBridge().stageChange(DataPointer.of(DataValue.PLAYER_FLIGHT_LOG, p.getUniqueId().toString()), false);
			manager.getTempFly().getDataBridge().stageChange(DataPointer.of(DataValue.PLAYER_COMPAT_FLIGHT_LOG, p.getUniqueId().toString()), false);
		}
		
	}
	
	public void save() {
		UUID uuid = (p != null) ? p.getUniqueId() : state.getUuid();
		Console.debug("", "-----< Save FlightUser: (" + uuid.toString() + ") >-----");
		if (manager.getTempFly() != null) {
			UserRepository repo = manager.getTempFly().getUserRepository();
			if (repo != null) {
				boolean loggedInFlight = hasFlightEnabled() || hasAutoFlyQueued();
				boolean compatLoggedInFlight = !loggedInFlight && p != null && p.isFlying();
				boolean damageProt = manager.getTempFly().getFallSafetyService() != null && manager.getTempFly().getFallSafetyService().isProtected(uuid);
				repo.saveUser(new UserFlightData(
						uuid,
						state.getTime(),
						loggedInFlight,
						compatLoggedInFlight,
						damageProt,
						0L,
						state.getTrail(),
						state.isInfinite(),
						state.isBypass(),
						state.getSelectedSpeed()
				));
			}
			DataBridge bridge = manager.getTempFly().getDataBridge();
			if (bridge != null) {
				String u = uuid.toString();
				bridge.manualCommit(
						DataPointer.of(DataValue.PLAYER_TIME, u),
						DataPointer.of(DataValue.PLAYER_DAILY_BONUS, u),
						DataPointer.of(DataValue.PLAYER_DAMAGE_PROTECTION, u),
						DataPointer.of(DataValue.PLAYER_FLIGHT_LOG, u),
						DataPointer.of(DataValue.PLAYER_COMPAT_FLIGHT_LOG, u),
						DataPointer.of(DataValue.PLAYER_TRAIL, u),
						DataPointer.of(DataValue.PLAYER_INFINITE, u),
						DataPointer.of(DataValue.PLAYER_BYPASS, u),
						DataPointer.of(DataValue.PLAYER_SPEED, u));
			}
		}
	}
	
	
	public FlightManager getFlightManager() {
		return manager;
	}
	
	public double getTime() {
		return state.getTime();
	}
	
	public Player getPlayer() {
		return p;
	}
	
	public void setTime(double time) {
		if (time <= 0) {
			time = 0;
		}
		double oldTime = state.getTime();
		state.setTime(time);
		manager.getTempFly().getDataBridge().stageChange(DataPointer.of(DataValue.PLAYER_TIME, p.getUniqueId().toString()), time);
		if (!hasInfiniteFlight() && p.isFlying()) {
			if (V.actionBar) {doActionBar();}
		}
		if (time > 0 && hasAutoFlyQueued() && !hasFlightEnabled()) {
			enableFlight();
		} else if (time == 0) {
			disableFlight(0, !V.damageTime);
			setAutoFly(true);
		} else if (oldTime == 0 && time > 0 && !hasFlightEnabled() && V.autoFlyTimeReceived) {
			enableFlight();
		}
	}
	
	public void resetIdleTimer() {
		state.resetIdle();
	}
	
	public boolean isIdle() {
		return V.idleThreshold > -1 && state.isIdle(V.idleThreshold * 20L);
	}
	
	public boolean hasFlightEnabled() {
		return state.isEnabled();
	}
	
	public boolean hasAutoFlyQueued() {
		return state.isAutoEnable();
	}
	
	public void setAutoFly(boolean auto) {
		state.setAutoEnable(auto);
	}
	
	public boolean isOpenForSubmission() {
		return hasAutoFlyQueued() || hasFlightEnabled();
	}
	
	public UserEnvironment getEnvironment() {
		return environment;
	}
	
	/**
	 * @return true if the user has infinite flight and it is enabled.
	 */
	public boolean hasInfiniteFlight() {
		return (p != null && p.hasPermission("tempfly.infinite")) || (environment != null && environment.hasInfiniteFlight());
	}
	
	/**
	 * Set whether the user has infinite flight enabled.
	 * @param enable enable infinite flight?
	 */
	public void setInfiniteFlight(boolean enable) {
		manager.getTempFly().getDataBridge().stageChange(DataPointer.of(DataValue.PLAYER_INFINITE, p.getUniqueId().toString()), enable);
		state.setInfinite(enable);
		this.cachedActionBarText = null;
		this.cachedActionBarSecond = -1;
		if (!enable && V.actionBar && state.getTime() > 0) {
			doActionBar();
		} else if (!enable && state.getTime() <= 0) {
			disableFlight(0, !V.damageCommand);
			setAutoFly(true);
		} else if (enable && hasAutoFlyQueued()) {
			enableFlight();
		} else if (enable && V.actionBar && p.isFlying()) {
			doActionBar();
		}
	} 
	
	/**
	 * @return true if the user has requirement bypass and it is enabled.
	 */
	public boolean hasRequirementBypass() {
		return p.hasPermission("tempfly.bypass") && state.hasFlightBypass();
	}
	
	/**
	 * Set whether the user has requirement bypass enabled. This has no effect if they do not have the permission tempfly.bypass
	 * @param enable enable requirement bypass?
	 */
	public void setRequirementBypass(boolean enable) {
		manager.getTempFly().getDataBridge().stageChange(DataPointer.of(DataValue.PLAYER_BYPASS, p.getUniqueId().toString()), enable);
		state.setBypass(enable);
		if (enable && hasAutoFlyQueued()) {
			enableFlight();
		} else if (!enable && hasFlightEnabled() && hasFlightRequirements()) {
			FlightResult result = getCurrentRequirement();
			if (result != null) {
				U.m(p, result.getMessage());
				disableFlight(0, result.hasDamageProtection());
			}
			setAutoFly(true);
		}
	}
	
	
	/**
	 * 
	 * --=--------------=--
	 *    Flight Control
	 * --=--------------=--
	 * 
	 */
	
	
	
	/**
	 * Internal clean up method called when the player quits or server is reloading.
	 */
	public void onQuit(boolean reload) {
		if (hasFlightEnabled() || hasAutoFlyQueued()) {
			manager.getTempFly().getDataBridge().stageChange(DataPointer.of(DataValue.PLAYER_FLIGHT_LOG, p.getUniqueId().toString()), true);
			if (!reload) {disableFlight(-1, false);}
		} else if (p.isFlying()) {
			manager.getTempFly().getDataBridge().stageChange(DataPointer.of(DataValue.PLAYER_COMPAT_FLIGHT_LOG, p.getUniqueId().toString()), true);
		}
		save();
		if (initialTask != null) {initialTask.cancel();}
		if (enforceTask != null) {enforceTask.cancel();}
		removeDamageProtection();
		clearSpeedPermCache();
	}
	
	/**
	 * This is a safety method to make sure a players flight is disabled.
	 * This will only try to remove flight if the user has flight mode disabled but are flying anyway.
	 * 
	 * @param delay The delay in ticks to enforce removal of flight. 1 should suffice.
	 */
	public void enforce(int delay) {
		Console.debug("enforcing disabled flight");
		if (enforceTask != null) {
			enforceTask.cancel();
		}
		enforceTask = Bukkit.getScheduler().runTaskLater(manager.getTempFly(), new EnforceTask(), delay);
	}
	
	/**
	 * TODO
	 * if players take fall damage when flight is lost and they are not supposed to
	 * there is an infinite flight bug somewhere to track down because the enforcement task is
	 * removing the flight after is was supposed to be disabled without adding the proper damage protec1tion. 
	 * @author Kevin
	 *
	 */
	public class EnforceTask implements Runnable {

		@Override
		public void run() {
			// If the users flight is enabled again when the task runs we will return.
			if (hasFlightEnabled()) return;
			GameMode m = p.getGameMode();
			if (m == GameMode.CREATIVE && V.creativeTimer) {
				Console.debug("--- Enforcing disabled flight A ----");
				p.setFlying(false);
				p.setAllowFlight(false);
			} else if (m != GameMode.CREATIVE && m != GameMode.SPECTATOR) {
				Console.debug("--- Enforcing disabled flight B ----");
				p.setFlying(false);
				p.setAllowFlight(false);
			}
			
		}
		
	}
	
	/**
	 * Turn off players flight with a safety delay that will enforce proper removal of flight.
	 * @param delay The delay in ticks to enforce removal of flight. 1 should suffice. -1 for no enforcement
	 */
	public void disableFlight(int delay, boolean fallSafely) {
		Console.debug("------ disable flight -------");
		boolean wasEnabled = state.isEnabled();
		state.setEnabled(false);
		if (!wasEnabled && !p.isFlying() && !p.getAllowFlight()) {
			return;
		}
		Runnable action = () -> {
			if (!p.isOnline()) return;
			GameMode m = p.getGameMode();
			// Fixes a weird bug where fall damage accumulates through flight and damages even when 1 block off the ground.
			if (p.isFlying()) {p.setFallDistance(0);}
			if (m == GameMode.CREATIVE && V.creativeTimer) {
				Console.debug("--> set flying false 1");
				p.setFlying(false);
				p.setAllowFlight(false);
			} else if (m != GameMode.CREATIVE && m != GameMode.SPECTATOR) {
				Console.debug("--> set flying false 2");
				p.setFlying(false);
				p.setAllowFlight(false);
				if (fallSafely) {addDamageProtection();}
			}
			if (delay > -1) {enforce(delay);}
		};
		if (Bukkit.isPrimaryThread()) {
			action.run();
		} else {
			Bukkit.getScheduler().runTask(manager.getTempFly(), action);
		}
	}
	
	/**
	 * Enable the users flight
	 * @return false if the users flight can not be enabled due to flight requirements.
	 */
	@SuppressWarnings("deprecation")
	public boolean enableFlight() {
		Console.debug("------ enable flight -------");
		if (hasFlightRequirements() && !hasRequirementBypass()) {
			setAutoFly(true);
			return false;
		}
		if (state.getTime() <= 0 && !hasInfiniteFlight()) {
			setAutoFly(true);
			return false;
		}
		Console.debug("--> set flying true");
		state.setEnabled(true);
		Runnable action = () -> {
			if (p.isOnline() && state.isEnabled()) {
				p.setAllowFlight(true);
				p.setFlying(!p.isOnGround());
				applySpeedCorrect(true, 0);
			}
		};
		if (Bukkit.isPrimaryThread()) {
			action.run();
		} else {
			Bukkit.getScheduler().runTask(manager.getTempFly(), action);
		}
		return true;
	}
	
	/**
	 * Method to make sure a player can fly when they are supposed to.
	 */
	public void applyFlightCorrect() {
		Console.debug("------ apply flight correct -------");
		Bukkit.getScheduler().runTaskLater(manager.getTempFly(), () -> {
			if (p.isOnline() && hasFlightEnabled()) {
				p.setAllowFlight(true);
				p.setFlying(true);
			}
		}, 1);
	}
	
	/**
	 * 
	 * --=-----------=--
	 *    Requirement
	 * --=-----------=--
	 * 
	 */
	
	
	
	public RequirementProvider[] getFlightRequirements() {
		return state.getRequirements().keySet().toArray(new RequirementProvider[0]);
	}
	
	public boolean hasFlightRequirement(RequirementProvider requirement) {
		return state.hasRequirement(requirement);
	}
	
	public boolean hasFlightRequirement(RequirementProvider requirement, InquiryType type) {
		return state.hasRequirement(requirement, type);
	}
	
	public boolean hasFlightRequirements() {
		return state.hasFlightRequirements();
	}
	
	public void submitFlightRequirement(RequirementProvider requirement, FlightResult failedResult) {
		if (V.debug) {Console.debug("", "---- Submitting failed requirement to user (" + p.getName() + ") ----", "--| Requirement: " + requirement.getClass().toGenericString(), "--| Requirements: " + state.getRequirements());}
		state.submitRequirement(requirement, failedResult);
		if (hasFlightEnabled()) {
			setAutoFly(true);
		}
	}
	
	/**
	 * 
	 * @param requirement
	 * @param type
	 * @return true if there are no more requirements
	 */
	public boolean removeFlightRequirement(RequirementProvider requirement, InquiryType type) {
		if (V.debug) {Console.debug("", "---- Removing flight requirement from user ----", "--| Requirement: " + requirement.getClass().toGenericString(), "--| Requirements: " + state.getRequirements());}
		state.removeRequirement(requirement, type);
		return !hasFlightRequirements();
	}
	
	/**
	 * 
	 * @param requirement
	 * @return true if there are no more requirements
	 */
	public boolean removeFlightRequirement(RequirementProvider requirement) {
		if (V.debug) {Console.debug("", "---- Removing flight requirement from user ----", "--| Requirement: " + requirement.getClass().toGenericString(), "--| Requirements: " + state.getRequirements());}
		state.removeRequirements(requirement);
		return !hasFlightRequirements();
	}
	
	public void removeFlightRequirements() {
		state.clearRequirements();
	}
	
	public void sendRequirementMessage() {
		FlightResult req = getCurrentRequirement();
		if (req != null) {
			U.m(p, req.getMessage());
		}
	}
	
	public FlightResult getCurrentRequirement() {
		return state.getCurrentRequirement();
	}
	
	/**
	 * Quality of life method.
	 * Evaluate the overall flight status of the user, checks all flight requirements present on the server.
	 * Used mainly when a player first joins or for some reason is not being tracked and we need to re-check everything.
	 * Flight will be disabled if a requirement fails.
	 * The requirement will then be submitted to the user for the auto flight enable feature.
	 * 
	 * @return false if the user fails.
	 */
	public boolean evaluateFlightRequirements(Location loc, boolean failMessage) {
		List<FlightResult> results = new ArrayList<>();
		results.addAll(manager.inquireFlight(this, loc.getWorld()));
		results.addAll(manager.inquireFlight(this, loc));
		if (manager.getTempFly().getHookManager().hasRegionProvider()) {
			results.addAll(manager.inquireFlight(this, manager.getTempFly().getHookManager().getRegionProvider().getApplicableRegions(p.getLocation())));
		}
		results.addAll(manager.inquireFlightBeyondScope(this));
		submitFlightResults(results, false);
		if (hasFlightRequirements()) {
			if (!hasRequirementBypass() && failMessage) {
				sendRequirementMessage();
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Evaluate the flight status of the user for the requirements introduced by the provider.
	 * Flight will be disabled if a requirement fails.
	 * The requirement will then be submitted to the user for the auto flight enable feature.
	 *
	 * @param failMessage Do you want the fail message to be sent to the user if they cannot fly? 
	 * @return false if the user fails.
	 */
	
	public boolean evaluateFlightRequirement(RequirementProvider requirement, Location loc) {
		List<FlightResult> results = new ArrayList<>();
		if (!requirement.handles(InquiryType.WORLD)) {
			results.add(requirement.handleFlightInquiry(this, loc.getWorld()));
		}
		if (!requirement.handles(InquiryType.LOCATION)) {
			results.add(requirement.handleFlightInquiry(this, loc));
		}
		if (!requirement.handles(InquiryType.REGION)
				&& manager.getTempFly().getHookManager().hasRegionProvider()) {
			results.add(requirement.handleFlightInquiry(this, environment.getCurrentRegionSet()));
		}
		return submitFlightResults(results, hasFlightEnabled()) && !hasFlightRequirement(requirement);
	}
	
	
	/**
	 * Submit a batch of flight results to the FlightUser.
	 * @param results The results to submit
	 * @return false if the results disabled the users flight aka user failed.
	 */
	public boolean submitFlightResult(FlightResult result) {
		RequirementProvider provider = result.getRequirement();
		InquiryType type = result.getInquiryType();
		if (!result.isAllowed()) {
			submitFlightRequirement(provider, result);
			if (!hasRequirementBypass()) {
				if (hasFlightEnabled()) {
					U.m(p, result.getMessage());
				}
				disableFlight(1, result.hasDamageProtection());	
			}
			return false;
		} else {
			if (hasFlightRequirement(provider, type) && removeFlightRequirement(provider, type)) {
				updateRequirements(result.getMessage());
			}	
		}
		return true;
	}
	
	/**
	 * Submit a batch of flight results.
	 * @param results
	 * @return
	 */
	public boolean submitFlightResults(List<FlightResult> results, boolean failMessage) {
		// The result that actually disabled the flight. first come first serve.
		FlightResult disabled = null;
		// The final result that enabled the flight.
		FlightResult enable = null;
		
		for (FlightResult result: results) {
			RequirementProvider provider = result.getRequirement();
			InquiryType type = result.getInquiryType();
			if (!result.isAllowed()) {
				if (disabled == null) {
					disabled = result;
				}
				if (!hasFlightRequirement(provider, type)) {
					submitFlightRequirement(provider, result);
				}
			} else {
				if (hasFlightRequirement(provider, type) && removeFlightRequirement(provider, type)) {
					enable = result;
				}	
			}
		}
		if (disabled != null) {
			if (!hasRequirementBypass()) {
				if (failMessage) {
					U.m(p, disabled.getMessage());
				}
				disableFlight(1, disabled.hasDamageProtection());
			}
			return false;
		} else if (enable != null) {
			updateRequirements(enable.getMessage());
		}
		return true;
	}
	
	/**
	 * Update the flight requirements for the user. Automatically auto enables flight if applicable. 
	 * @return True if there are no more requirements.
	 */
	public boolean updateRequirements(String enableMessage) {
		Console.debug("", "--- updating requirements ---", "--| requirements: " + state.getRequirements().toString(),
				"--| flight enabled: " + hasFlightEnabled(), "--| auto flight: " + hasAutoFlyQueued(), "--| time: " + state.getTime());
		
		if (!hasFlightRequirements() && !hasFlightEnabled() && (state.getTime() > 0 || hasInfiniteFlight())) {
			if (hasAutoFlyQueued()) {
				setAutoFly(false);
				if (!hasRequirementBypass()) {
					Console.debug("--|> AutoFly engaged!");
					U.m(p, enableMessage);
					enableFlight();
				} else {
					Console.debug("--|> Autofly will not be invoked, User has requirement bypass mode...");
				}
			}
			return true;
		}
		return !hasFlightRequirements();
	}
	
	
	
	/**
	 * 
	 * --=-----------=--
	 *    Fall Damage
	 * --=-----------=--
	 * 
	 */
	
	
	public void addDamageProtection() {
		if (manager != null && manager.getTempFly() != null && manager.getTempFly().getFallSafetyService() != null && p != null) {
			manager.getTempFly().getFallSafetyService().addProtection(p);
		}
	}
	
	public void removeDamageProtection() {
		if (manager != null && manager.getTempFly() != null && manager.getTempFly().getFallSafetyService() != null && p != null) {
			manager.getTempFly().getFallSafetyService().removeProtection(p.getUniqueId());
		}
	}
	
	public boolean hasDamageProtection() {
		if (manager != null && manager.getTempFly() != null && manager.getTempFly().getFallSafetyService() != null && p != null) {
			return manager.getTempFly().getFallSafetyService().hasProtection(p.getUniqueId());
		}
		return false;
	}
	
	
	
	/**
	 * 
	 * --=----------=--
	 *    Aesthetics
	 * --=----------=--
	 * 
	 */
	
	
	
	/**
	 * This method returns a string to keep the plugin compatible through versions.
	 * @return The enum string representation of the particle
	 */
	public String getTrail() {
		return state.getTrail();
	}
	
	/**
	 *  This method requires a string to keep the plugin compatible through versions.
	 *  The enum value of the particle as a string
	 * @param particle
	 */
	public void setTrail(String particle) {
		state.setTrail(particle);
		manager.getTempFly().getDataBridge().stageChange(DataPointer.of(DataValue.PLAYER_TRAIL, p.getUniqueId().toString()), particle);
	}
	
	public void playTrail() {
		String particle = state.getTrail();
		if (particle == null || particle.length() == 0) {return;}
		
		if (p.getGameMode() == GameMode.SPECTATOR) {
			return;
		}
		
		if (V.hideVanish) {
			for (MetadataValue meta : p.getMetadata("vanished")) {
				if (meta.asBoolean()) {
					return;
				}
			}
		}
		
		
		Particles.play(p.getLocation(), particle);
	}
	
	public String getListPlaceholder() {
		return (p.isFlying() && hasFlightEnabled()) ? "<#00f878>[Fly]</#00f878>" : "";
	}
	
	public String getTagPlaceholder() {
		return (p.isFlying() && hasFlightEnabled()) ? "<#00f878>[Fly]</#00f878>" : "";
	}
	
	private String cachedActionBarText;
	private long cachedActionBarSecond = -1;

	public void doActionBar() {
		if (p == null || !p.isOnline() || hasInfiniteFlight()) return;
		long roundedSec = (long) Math.ceil(state.getTime());
		if (cachedActionBarText == null || cachedActionBarSecond != roundedSec) {
			cachedActionBarSecond = roundedSec;
			cachedActionBarText = timeManager.regexString(V.actionText, getTime(), false);
		}
		p.sendActionBar(cachedActionBarText);
	}
	
	
	/**
	 * 
	 * --=-----------=--
	 *   Speed control
	 * --=-----------=--
	 * 
	 */
	
	public double getSpeedPreference() {
		return state.getSelectedSpeed();
	}
	
	public void setSpeedPreference(double speed) {
		state.setSelectedSpeed(speed);
		manager.getTempFly().getDataBridge().stageChange(DataPointer.of(DataValue.PLAYER_SPEED, p.getUniqueId().toString()), speed);
	}
	
	public boolean hasSpeedPreference() {
		return state.getSelectedSpeed() > 0 && manager.getFlightEnvironment().allowSpeedPreference();
	}
	
	/**
	 * Correct the users flight speed. Takes into account permissions and max world / region speeds.
	 * @return The resulting speed of the user.
	 */
	public float applySpeedCorrect(boolean message, int delay) {
		float maxSpeed = getMaxSpeed();
		Console.debug("--| Max speed: " + String.valueOf(maxSpeed));
		Console.debug("--| Preferred speed: " + String.valueOf(state.getSelectedSpeed()));
		if (hasSpeedPreference() && maxSpeed > state.getSelectedSpeed() && manager.getFlightEnvironment().allowSpeedPreference()) {
			maxSpeed = (float) state.getSelectedSpeed();
		}
		
		final float val = maxSpeed / 10;
		final float def = manager.getFlightEnvironment().getDefaultSpeed() / 10;
		Console.debug("--| final speed value: " + val);
		if (p.getFlySpeed() > val
				|| (p.getFlySpeed() != val && !manager.getFlightEnvironment().allowSpeedPreference()) 
				|| (p.getFlySpeed() < val && hasSpeedPreference())) {
			Console.debug("--| Players speed needs to be changed.");
			Runnable updateAction = () -> {
				Console.debug("-----> | changing player speed");
				if (p.isOnline()) {
					Console.debug("player speed: " + p.getFlySpeed(), "value: " + val);
					Console.debug("is speed prefernce allowed? " + manager.getFlightEnvironment().allowSpeedPreference(),
							p.getFlySpeed() != val && !manager.getFlightEnvironment().allowSpeedPreference());
					if (p.getFlySpeed() > val && message) {
						U.m(p, V.flySpeedLimitSelf.replaceAll("\\{SPEED}", new DecimalFormat("#.##").format(val * 10)));
					}
					p.setFlySpeed(val);
				}
			};
			if (delay <= 0 && Bukkit.isPrimaryThread()) {
				updateAction.run();
			} else {
				Bukkit.getScheduler().runTaskLater(manager.getTempFly(), updateAction, Math.max(1, delay));
			}
			
		} else if (p.getFlySpeed() != def && !hasSpeedPreference()) {
			float fin = Math.min(def, val);
			Console.debug("--| Players speed needs to be fixed it is stuck under default speed.");
			Runnable defaultAction = () -> {
				Console.debug("-----> | changing player speed");
				if (p.isOnline()) {
					Console.debug("player speed: " + p.getFlySpeed(), "value: " + val);
					p.setFlySpeed(fin);
				}
			};
			if (delay <= 0 && Bukkit.isPrimaryThread()) {
				defaultAction.run();
			} else {
				Bukkit.getScheduler().runTaskLater(manager.getTempFly(), defaultAction, Math.max(1, delay));
			}
		}
		return val;
	}
	
	public float getMaxSpeed() {
		Console.debug("get max speed 1");
		CompatRegion[] regions = environment.getCurrentRegionSet();
		FlightEnvironment env = manager.getFlightEnvironment();
		
		// Permissions for region speed take priority
		float finSpeed = getMaxSpeed(regions);
		if (finSpeed > 0) {
			Console.debug("2: " + finSpeed);
			return finSpeed;
		} else if (env.hasMaxSpeed(regions)) {
			float rSpeed = env.getMaxSpeed(regions);
			if (rSpeed > 0) {
				Console.debug("4: " + rSpeed);
				return rSpeed;
			}
		}
		
		// Permissions for world speed go next
		finSpeed = getMaxSpeed(p.getWorld());
		if (finSpeed > 0) {
			Console.debug("3: " + finSpeed);
			return finSpeed;
		} else if (env.hasMaxSpeed(p.getWorld())) {
			float wSpeed = env.getMaxSpeed(p.getWorld());
			if (wSpeed > 0) {
				Console.debug("4: " + wSpeed);
				return wSpeed;
			}
		}
		
		// return default environment speed indicator
		float defSpeed = env.getDefaultSpeed();
		return defSpeed > 0 ? defSpeed : 1f;
	}
	
	public float getMaxSpeed(World world) {
		return this.calculatePermissionSpeed("world." + world.getName(), "world.*");
	}
	
	public float getMaxSpeed(CompatRegion[] regions) {
		float permSpeed = -999;
		for (CompatRegion region: regions) {
			permSpeed = Math.max(calculatePermissionSpeed("region." + region.getId(), "region.*"), permSpeed);
			Console.debug("--| Region: " + region.getId(), "--| Permission speed for this region is: " + permSpeed);
		}
		return permSpeed;
	}
	
	private final Map<String, Float> speedPermCache = new HashMap<>();

	public void clearSpeedPermCache() {
		speedPermCache.clear();
	}

	float calculatePermissionSpeed(String permission, String wildcard) {
		Float cached = speedPermCache.get(permission);
		if (cached != null) {
			return cached;
		}
		float speed = calculatePermissionSpeed(p != null ? p.getEffectivePermissions() : Collections.emptySet(), permission, wildcard);
		speedPermCache.put(permission, speed);
		return speed;
	}

	public static float calculatePermissionSpeed(Set<PermissionAttachmentInfo> permissions, String permission, String wildcard) {
		String prefix1 = "tempfly.speed." + permission + ".";
		String prefix2 = "tempfly.speed." + wildcard + ".";
		float maxFound = 0;
		if (permissions == null) {
			return -999;
		}
		for (PermissionAttachmentInfo info : permissions) {
			if (!info.getValue()) continue;
			String perm = info.getPermission();
			String rawNum = null;
			if (perm.startsWith(prefix1)) {
				rawNum = perm.substring(prefix1.length());
			} else if (perm.startsWith(prefix2)) {
				rawNum = perm.substring(prefix2.length());
			}
			if (rawNum != null && !rawNum.isEmpty()) {
				rawNum = rawNum.replace("[", "").replace("]", "");
				try {
					float found = Float.parseFloat(rawNum);
					maxFound = Math.max(found, maxFound);
				} catch (NumberFormatException ignored) {}
			}
		}
		return maxFound > 0 ? maxFound : -999;
	}
	
	
	/**
	 * 
	 * --=---------=--
	 *     Ticking & Time Processing
	 * --=---------=--
	 * 
	 */
	
	private boolean previouslyFlying;
	private boolean messaged;

	public boolean hasTimer() {
		return doFlightTimer();
	}

	public void tick(int deltaTicks) {
		if (p == null || !p.isValid()) {
			return;
		}
		state.addIdleTicks(deltaTicks);
		
		if (hasInfiniteFlight()) {
			return;
		}
		
		if (!doFlightTimer()) {
			return;
		}
		
		state.addAccumulativeCycleMillis(deltaTicks * 50L);
		while (state.getAccumulativeCycleMillis() >= 1000L) {
			state.setAccumulativeCycleMillis(state.getAccumulativeCycleMillis() - 1000L);
			executeTimer();
		}
	}

	private void executeTimer() {
		if (state.getTime() > 0) {
			FlightEngine.TickOutcome outcome = engine.processOneSecondCycle(state, environment.toContext());
			double currentTime = state.getTime();
			manager.getTempFly().getDataBridge().stageChange(DataPointer.of(DataValue.PLAYER_TIME, p.getUniqueId().toString()), currentTime);	
			
			if (V.warningTimes != null && V.warningTimes.contains((long) currentTime)) {
				p.sendTitle(timeManager.regexString(V.warningTitle, currentTime),
					timeManager.regexString(V.warningSubtitle, currentTime), 15, 30, 15);
			}
			if (V.actionBar) {
				doActionBar();
			}
			
			if (outcome == FlightEngine.TickOutcome.EXPIRED || currentTime <= 0) {
				timeExpired();
			}
		} else if (hasFlightEnabled()) {
			timeExpired();
		}
	}

	private void timeExpired() {
		disableFlight(-1, !V.damageTime);
		U.m(p, V.invalidTimeSelf);
		setAutoFly(true);
	}

	private boolean doFlightTimer() {
		FlightEngine.TimerEligibility eligibility = engine.evaluateEligibility(
				state,
				p.isFlying(),
				p.isOnGround(),
				isIdle(),
				p.getGameMode(),
				p.getVehicle() != null,
				V.getGeneral()
		);
		if (eligibility != FlightEngine.TimerEligibility.ELIGIBLE) {
			if (eligibility == FlightEngine.TimerEligibility.IDLE_RESTRICTED) {
				doIdleDropAndMessage();
			}
			return false;
		}
		return doIdleCheck();
	}

	private boolean doIdleCheck() {
		if (isIdle()) {
			doIdleDropAndMessage();
			return V.idleTimer;
		} else {
			this.messaged = false;
		}
		
		return true;
	}

	private void doIdleDropAndMessage() {
		if (V.idleDrop) {
			disableFlight(0, !V.damageIdle);
		}
		if (!this.messaged) {
			U.m(p, V.idleDrop ? V.disabledIdle : V.consideredIdle);
			this.messaged = true;
		}
	}
}
