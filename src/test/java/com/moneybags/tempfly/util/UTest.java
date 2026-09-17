package com.moneybags.tempfly.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UTest {

    @Test
    public void testColorCodeTranslation() {
        String input = "&aHello &bWorld &c!";
        String output = U.cc(input);

        assertNotNull(output);
        assertTrue(output.contains("§aHello"));
        assertTrue(output.contains("§bWorld"));
        assertTrue(output.contains("§c!"));
    }

    @Test
    public void testHexColorCodeTranslation() {
        String input = "&#FF5555Hello &#00ffaaWorld &a!";
        String output = U.cc(input);

        assertNotNull(output);
        assertTrue(output.contains("§x§F§F§5§5§5§5Hello"));
        assertTrue(output.contains("§x§0§0§f§f§a§aWorld"));
        assertTrue(output.contains("§a!"));
    }

    @Test
    public void testStripColorCodes() {
        String input = "&#FF5555Hello &bWorld &c!";
        String stripped = U.strip(input);

        assertEquals("Hello World !", stripped);
    }

    @Test
    public void testArrayToString() {
        String[] arr = new String[]{"apple", "banana", "cherry"};
        String result = U.arrayToString(arr, ", ");

        assertEquals("apple, banana, cherry", result);
        assertNull(U.arrayToString(null, ", "));
    }

    @Test
    public void testSkipArray() {
        String[] arr = new String[]{"first", "second", "third", "fourth"};

        String[] skipped2 = U.skipArray(arr, 2);
        assertArrayEquals(new String[]{"third", "fourth"}, skipped2);

        String[] skippedAll = U.skipArray(arr, 4);
        assertEquals(0, skippedAll.length);

        String[] skippedExcess = U.skipArray(arr, 10);
        assertEquals(0, skippedExcess.length);
    }
}
