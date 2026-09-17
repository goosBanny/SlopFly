package com.moneybags.tempfly.hook.region.plugins;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.hook.region.CompatRegion;
import com.moneybags.tempfly.hook.region.RegionProvider;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class WorldGuardHook implements RegionProvider {
	
	private static final CompatRegion[] EMPTY_REGIONS = new CompatRegion[0];
	private boolean enabled;
	
    private static Object worldGuard = null;
    private static Object worldGuardPlugin = null;
    private static Object regionContainer = null;
    private static MethodHandle regionContainerGetHandle = null;
    private static MethodHandle worldAdaptHandle = null;
    private static MethodHandle regionManagerGetHandle = null;
    private static MethodHandle vectorConstructorHandle = null;
    private static MethodHandle vectorConstructorMethodHandle = null;

    private static final Map<String, RegionManager> regionManagerCache = new ConcurrentHashMap<>();

    public WorldGuardHook(TempFly tempfly) {
        regionManagerCache.clear();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Method getInstanceMethod = worldGuardClass.getMethod("getInstance");
            worldGuard = getInstanceMethod.invoke(null);
        } catch (Exception ex) {
    		Plugin plugin = tempfly.getServer().getPluginManager().getPlugin("WorldGuard");
    		if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
    			return;
    		}
    		worldGuardPlugin = (WorldGuardPlugin) plugin;
        }
        if (worldGuard != null) {
            try {
                Method getPlatFormMethod = worldGuard.getClass().getMethod("getPlatform");
                Object platform = getPlatFormMethod.invoke(worldGuard);
                Method getRegionContainerMethod = platform.getClass().getMethod("getRegionContainer");
                regionContainer = getRegionContainerMethod.invoke(platform);
                
                Class<?> worldEditWorldClass = Class.forName("com.sk89q.worldedit.world.World");
                Class<?> worldEditAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                Method worldAdaptMethod = worldEditAdapterClass.getMethod("adapt", World.class);
                worldAdaptHandle = lookup.unreflect(worldAdaptMethod);

                Method regionContainerGetMethod = regionContainer.getClass().getMethod("get", worldEditWorldClass);
                regionContainerGetHandle = lookup.unreflect(regionContainerGetMethod);
            } catch (Exception ex) {
                regionContainer = null;
                return;
            }
        } else {
            try {
				regionContainer = ((WorldGuardPlugin) worldGuardPlugin).getClass().getMethod("getRegionContainer").invoke(((WorldGuardPlugin) worldGuardPlugin));
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
            try {
                Method regionContainerGetMethod = regionContainer.getClass().getMethod("get", World.class);
                regionContainerGetHandle = lookup.unreflect(regionContainerGetMethod);
            } catch (Exception ex) {
                regionContainer = null;
                return;
            }
        }
        try {
            Class<?> vectorClass = Class.forName("com.sk89q.worldedit.Vector");
            Constructor<?> vectorConstructor = vectorClass.getConstructor(Double.TYPE, Double.TYPE, Double.TYPE);
            vectorConstructorHandle = lookup.unreflectConstructor(vectorConstructor);

            Method regionManagerGetMethod = RegionManager.class.getMethod("getApplicableRegions", vectorClass);
            regionManagerGetHandle = lookup.unreflect(regionManagerGetMethod);
        } catch (Exception ex) {
            try {
                Class<?> vectorClass = Class.forName("com.sk89q.worldedit.math.BlockVector3");
                Method vectorConstructorMethod = vectorClass.getMethod("at", Double.TYPE, Double.TYPE, Double.TYPE);
                vectorConstructorMethodHandle = lookup.unreflect(vectorConstructorMethod);

                Method regionManagerGetMethod = RegionManager.class.getMethod("getApplicableRegions", vectorClass);
                regionManagerGetHandle = lookup.unreflect(regionManagerGetMethod);
            } catch (Exception sodonewiththis) {
                regionContainer = null;
                return;
            }
        }
        enabled = worldGuardPlugin != null || worldGuard != null;
    }

    public static void clearCache() {
        regionManagerCache.clear();
    }

    public RegionManager getRegionManager(World world) {
        if (world == null || regionContainer == null || regionContainerGetHandle == null) return null;
        return regionManagerCache.computeIfAbsent(world.getName(), wName -> {
            try {
                if (worldAdaptHandle != null) {
                    Object worldEditWorld = worldAdaptHandle.invoke(world);
                    return (RegionManager) regionContainerGetHandle.invoke(regionContainer, worldEditWorld);
                } else {
                    return (RegionManager) regionContainerGetHandle.invoke(regionContainer, world);
                }
            } catch (Throwable e) {
                return null;
            }
        });
    }

    public ApplicableRegionSet getRegionSet(Location location) {
        if (location == null || location.getWorld() == null) return null;
        RegionManager regionManager = getRegionManager(location.getWorld());
        if (regionManager == null || regionManagerGetHandle == null) return null;
        try {
            Object vector = vectorConstructorMethodHandle == null
                    ? vectorConstructorHandle.invoke(location.getX(), location.getY(), location.getZ())
                    : vectorConstructorMethodHandle.invoke(location.getX(), location.getY(), location.getZ());
            return (ApplicableRegionSet) regionManagerGetHandle.invoke(regionManager, vector);
        } catch (Throwable ex) {
            return null;
        }
    }
    
    @Override
    public CompatRegion[] getApplicableRegions(Location loc) {
    	ApplicableRegionSet set = getRegionSet(loc);
    	if (set == null || set.size() == 0) {
    		return EMPTY_REGIONS;
    	}
    	List<CompatRegion> list = new ArrayList<>(set.size());
    	for (ProtectedRegion r: set) {
    		list.add(new CompatRegion(r.getId()));
    	}
    	return list.toArray(new CompatRegion[list.size()]);
    }

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
