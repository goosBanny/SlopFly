package com.moneybags.tempfly.util.data;

import com.moneybags.tempfly.util.data.DataBridge.DataValue;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DataPointerTest {

    @Test
    public void testDataPointerEquality() {
        String uuid = UUID.randomUUID().toString();
        DataPointer p1 = DataPointer.of(DataValue.PLAYER_TIME, uuid);
        DataPointer p2 = DataPointer.of(DataValue.PLAYER_TIME, uuid);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(DataValue.PLAYER_TIME, p1.getValue());
        assertArrayEquals(new String[]{uuid}, p1.getPath());
    }

    @Test
    public void testDataPointerInequalityDifferentValues() {
        String uuid = UUID.randomUUID().toString();
        DataPointer p1 = DataPointer.of(DataValue.PLAYER_TIME, uuid);
        DataPointer p2 = DataPointer.of(DataValue.PLAYER_SPEED, uuid);

        assertNotEquals(p1, p2);
    }

    @Test
    public void testDataPointerInequalityDifferentPaths() {
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        DataPointer p1 = DataPointer.of(DataValue.PLAYER_TIME, uuid1);
        DataPointer p2 = DataPointer.of(DataValue.PLAYER_TIME, uuid2);

        assertNotEquals(p1, p2);
    }

    @Test
    public void testDataPointerCompoundPath() {
        DataPointer p1 = DataPointer.of(DataValue.PLAYER_DAILY_BONUS, "user-123", "bonus");
        DataPointer p2 = DataPointer.of(DataValue.PLAYER_DAILY_BONUS, "user-123", "bonus");

        assertEquals(p1, p2);
        assertArrayEquals(new String[]{"user-123", "bonus"}, p1.getPath());
    }
}
