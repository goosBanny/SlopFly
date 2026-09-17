package com.moneybags.tempfly.user;

import com.moneybags.tempfly.environment.FlightEnvironment;
import com.moneybags.tempfly.environment.RelativeTimeRegion;
import com.moneybags.tempfly.hook.region.CompatRegion;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserEnvironmentTest {

    @Test
    public void testCheckIdenticalRegions() {
        CompatRegion r1 = new CompatRegion("spawn");
        CompatRegion r2 = new CompatRegion("pvp");

        UserEnvironment env = new UserEnvironment(null, null, new CompatRegion[]{r1, r2});

        List<CompatRegion> same = Arrays.asList(r1, r2);
        assertTrue(env.checkIdenticalRegions(same));

        List<CompatRegion> diff = Arrays.asList(r1);
        assertFalse(env.checkIdenticalRegions(diff));

        List<CompatRegion> diffElements = Arrays.asList(r1, new CompatRegion("market"));
        assertFalse(env.checkIdenticalRegions(diffElements));

        assertFalse(env.checkIdenticalRegions(null));
    }

    @Test
    public void testRelativeTimeFactorAccumulation() {
        FlightEnvironment flightEnv = new FlightEnvironment();
        RelativeTimeRegion rtr1 = new RelativeTimeRegion(2.0, false, "nether_region");
        RelativeTimeRegion rtr2 = new RelativeTimeRegion(1.5, false, "boss_arena");

        flightEnv.addRelativeTimeRegion(rtr1);
        flightEnv.addRelativeTimeRegion(rtr2);

        CompatRegion r1 = new CompatRegion("nether_region");
        CompatRegion r2 = new CompatRegion("boss_arena");

        UserEnvironment env = new UserEnvironment(null, flightEnv, new CompatRegion[]{r1, r2});

        RelativeTimeRegion[] activeRegions = env.getRelativeTimeRegions();
        assertEquals(2, activeRegions.length);

        double totalCostFactor = 1.0;
        for (RelativeTimeRegion rtr : activeRegions) {
            totalCostFactor *= rtr.getFactor();
        }
        assertEquals(3.0, totalCostFactor, 0.001);
    }

    @Test
    public void testInfiniteFlightDetectionOnRegion() {
        FlightEnvironment flightEnv = new FlightEnvironment();
        flightEnv.addInfiniteRegion("lobby");
        CompatRegion infiniteRegion = new CompatRegion("lobby");

        UserEnvironment env = new UserEnvironment(null, flightEnv, new CompatRegion[]{infiniteRegion});
        assertTrue(env.hasInfiniteFlight());
    }

    @Test
    public void testNonInfiniteFlight() {
        FlightEnvironment flightEnv = new FlightEnvironment();
        CompatRegion standardRegion = new CompatRegion("wilderness");

        UserEnvironment env = new UserEnvironment(null, flightEnv, new CompatRegion[]{standardRegion});
        assertFalse(env.hasInfiniteFlight());
    }
}
