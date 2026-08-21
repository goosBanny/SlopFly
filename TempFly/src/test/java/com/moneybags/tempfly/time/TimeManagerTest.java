package com.moneybags.tempfly.time;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.util.DailyDate;
import com.moneybags.tempfly.util.V;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TimeManagerTest {

    private TimeManager timeManager;

    @BeforeEach
    public void setUp() {
        // Initialize default static V values for formatting tests
        V.timeFormat = "{QUANTITY}{UNIT}";
        V.unitDays = "d";
        V.unitHours = "h";
        V.unitMinutes = "m";
        V.unitSeconds = "s";

        timeManager = new TimeManager(null);
    }

    @Test
    public void testFormatTimeConversions() {
        double totalSeconds = 90061; // 1 day (86400), 1 hour (3600), 1 minute (60), 1 second (1)

        assertEquals(1L, timeManager.formatTime(TimeUnit.DAYS, totalSeconds));
        assertEquals(1L, timeManager.formatTime(TimeUnit.HOURS, totalSeconds));
        assertEquals(1L, timeManager.formatTime(TimeUnit.MINUTES, totalSeconds));
        assertEquals(1L, timeManager.formatTime(TimeUnit.SECONDS, totalSeconds));
    }

    @Test
    public void testFormatTimeZeroAndBoundary() {
        assertEquals(0L, timeManager.formatTime(TimeUnit.DAYS, 0));
        assertEquals(0L, timeManager.formatTime(TimeUnit.HOURS, 0));
        assertEquals(0L, timeManager.formatTime(TimeUnit.MINUTES, 0));
        assertEquals(0L, timeManager.formatTime(TimeUnit.SECONDS, 0));

        assertEquals(0L, timeManager.formatTime(TimeUnit.DAYS, 86399));
        assertEquals(23L, timeManager.formatTime(TimeUnit.HOURS, 86399));
        assertEquals(59L, timeManager.formatTime(TimeUnit.MINUTES, 86399));
        assertEquals(59L, timeManager.formatTime(TimeUnit.SECONDS, 86399));
    }

    @Test
    public void testRegexStringFormattedTime() {
        String template = "Flight: {FORMATTED_TIME}";
        double seconds = 90061; // 1d 1h 1m 1s

        String result = timeManager.regexString(template, seconds);
        assertEquals("Flight: 1d 1h 1m 1s", result);
    }

    @Test
    public void testRegexStringTokens() {
        String template = "{DAYS}d:{HOURS}h:{MINUTES}m:{SECONDS}s";
        double seconds = 3665; // 0d 1h 1m 5s

        String result = timeManager.regexString(template, seconds);
        assertEquals("0d:1h:1m:5s", result);
    }

    @Test
    public void testRegexStringNullSafety() {
        assertEquals("", timeManager.regexString(null, 100));
    }

    @Test
    public void testDailyDateComparisonSameDay() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2026, Calendar.AUGUST, 21, 10, 0, 0);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2026, Calendar.AUGUST, 21, 22, 30, 0);

        DailyDate d1 = new DailyDate(cal1.getTimeInMillis());
        DailyDate d2 = new DailyDate(cal2.getTimeInMillis());

        assertEquals(d1, d2);
    }

    @Test
    public void testDailyDateComparisonDifferentDays() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2026, Calendar.AUGUST, 21, 23, 59, 59);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2026, Calendar.AUGUST, 22, 0, 0, 1);

        DailyDate d1 = new DailyDate(cal1.getTimeInMillis());
        DailyDate d2 = new DailyDate(cal2.getTimeInMillis());

        assertNotEquals(d1, d2);
    }
}
