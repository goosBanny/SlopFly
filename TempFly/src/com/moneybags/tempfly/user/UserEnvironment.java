package com.moneybags.tempfly.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;

import com.moneybags.tempfly.environment.FlightEnvironment;
import com.moneybags.tempfly.environment.RelativeTimeRegion;
import com.moneybags.tempfly.hook.region.CompatRegion;
import com.moneybags.tempfly.util.Console;

public class UserEnvironment {

	private static final CompatRegion[] EMPTY_REGIONS = new CompatRegion[0];
	private static final RelativeTimeRegion[] EMPTY_RT_REGIONS = new RelativeTimeRegion[0];

	private final FlightUser user;
	private final FlightEnvironment environment;
	
	private boolean freeFlight;
	
	private final Set<CompatRegion> encompassing = new LinkedHashSet<>();
	
	private final List<RelativeTimeRegion> rtRegions = new ArrayList<>();
	private RelativeTimeRegion rtWorld;
	private RelativeTimeRegion[] cachedRtArray = EMPTY_RT_REGIONS;

	public UserEnvironment(FlightUser user, FlightEnvironment environment, CompatRegion[] initialRegions) {
		this.user = user;
		this.environment = environment;
		if (initialRegions != null) {
			encompassing.addAll(Arrays.asList(initialRegions));
		}
		if (environment != null) {
			asessRtRegions();
			asessRtWorld();
			asessInfiniteFlight();
		}
	}
	
	public UserEnvironment(FlightUser user, Player p) {
		Console.debug("--| Loading user environment...");
		this.user = user;
		this.environment = user.getFlightManager().getFlightEnvironment();
		
		encompassing.addAll(Arrays.asList(
				user.getFlightManager().getTempFly().getHookManager().hasRegionProvider()
				? user.getFlightManager().getTempFly().getHookManager().getRegionProvider().getApplicableRegions(user.getPlayer().getLocation())
				: EMPTY_REGIONS));
		
		StringBuilder builder = new StringBuilder();
		encompassing.stream().forEach(rg -> builder.append(rg.getId() + ", "));
		Console.debug("--| Current regions: " + builder);
		asessRtRegions();
		asessRtWorld();
		asessInfiniteFlight();
	}
	
	public FlightUser getUser() {
		return user;
	}
	
	
	/**
	 * 
	 * --=-----------------=--
	 *    Real Time Regions
	 * --=-----------------=--
	 * 
	 */
	
	
	
	public CompatRegion[] getCurrentRegionSet() {
		return encompassing.toArray(EMPTY_REGIONS);
	}
	
	public void updateCurrentRegionSet(CompatRegion[] regions) {
		this.encompassing.clear();
		if (regions != null) {
			this.encompassing.addAll(Arrays.asList(regions));
		}
		asessRtRegions();
		asessInfiniteFlight();
	}
	
	public boolean isInside(CompatRegion region) {
		return region != null && encompassing.contains(region);
	}
	
	
	/**
	 * 
	 * --=------------=--
	 *    RelativeTime
	 * --=------------=--
	 * 
	 */
	
	
	public RelativeTimeRegion[] getRelativeTimeRegions() {
		return cachedRtArray;
	}

	private void rebuildRtRegionsCache() {
		if (rtWorld == null) {
			cachedRtArray = rtRegions.toArray(EMPTY_RT_REGIONS);
			return;
		}
		RelativeTimeRegion[] array = new RelativeTimeRegion[rtRegions.size() + 1];
		for (int i = 0; i < rtRegions.size(); i++) {
			array[i] = rtRegions.get(i);
		}
		array[rtRegions.size()] = rtWorld;
		cachedRtArray = array;
	}

	public void asessRtWorld() {
		if (environment != null && user != null && user.getPlayer() != null) {
			rtWorld = environment.getRelativeTime(user.getPlayer().getWorld());
		} else {
			rtWorld = null;
		}
		rebuildRtRegionsCache();
	}
	
	public void asessRtRegions() {
		if (environment == null) {
			rtRegions.clear();
			rebuildRtRegionsCache();
			return;
		}
		RelativeTimeRegion[] rtArray = environment.getRelativeTimeRegions();
		if (rtArray == null || rtArray.length == 0 || encompassing.isEmpty()) {
			rtRegions.clear();
			rebuildRtRegionsCache();
			return;
		}
		Set<String> regionIds = new HashSet<>(encompassing.size());
		for (CompatRegion r : encompassing) {
			regionIds.add(r.getId());
		}
		rtRegions.clear();
		for (RelativeTimeRegion rt : rtArray) {
			if (regionIds.contains(rt.getName())) {
				rtRegions.add(rt);
			}
		}
		rebuildRtRegionsCache();
	}
	
	public void asessInfiniteFlight() {
		if (environment == null) {
			freeFlight = false;
			return;
		}
		if (user != null && user.getPlayer() != null && environment.isInfinite(user.getPlayer().getWorld())) {
			freeFlight = true;
			return;
		}
		
		for (CompatRegion r: encompassing) {
			if (environment.isInfinite(r)) {
				freeFlight = true;
				return;
			}
		}
		freeFlight = false;
	}
	
	public boolean hasInfiniteFlight() {
		return freeFlight;
	}
	

	/**
	 * @param regions The list of regions to check
	 * @return True if the list is the same.
	 */
	public boolean checkIdenticalRegions(List<CompatRegion> regions) {
		if (regions == null || regions.size() != encompassing.size()) {
			return false;
		}
		return encompassing.containsAll(regions);
	}
}

