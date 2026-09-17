package com.moneybags.tempfly.util.data;

import com.moneybags.tempfly.util.data.DataBridge.DataValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class DataBridgeConcurrencyTest {

    private DataBridge dataBridge;

    @BeforeEach
    public void setUp() {
        dataBridge = new DataBridge();
    }

    @AfterEach
    public void tearDown() {
        dataBridge.onDisable();
    }

    @Test
    public void testGetOrDefaultWithStagedValue() {
        String uuid = UUID.randomUUID().toString();
        DataPointer pointer = DataPointer.of(DataValue.PLAYER_TIME, uuid);

        assertNull(dataBridge.getOrDefault(pointer, null));

        dataBridge.stageChange(pointer, 123.45);
        assertTrue(dataBridge.isStaged(pointer));

        Object value = dataBridge.getOrDefault(pointer, 0.0);
        assertEquals(123.45, (Double) value, 0.001);
    }

    @Test
    public void testConcurrentStaging() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String uuid = "user-" + threadId + "-" + j;
                        DataPointer pointer = DataPointer.of(DataValue.PLAYER_TIME, uuid);
                        dataBridge.stageChange(pointer, (double) j);

                        Object retrieved = dataBridge.getOrDefault(pointer, null);
                        if (retrieved == null || (Double) retrieved != (double) j) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(0, errorCount.get());
    }

    @Test
    public void testConcurrentCommitAndStaging() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Stage initial changes
        for (int i = 0; i < 50; i++) {
            dataBridge.stageChange(DataPointer.of(DataValue.PLAYER_TIME, "init-" + i), (double) i);
        }

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        dataBridge.stageChange(DataPointer.of(DataValue.PLAYER_TIME, "thread-" + threadId + "-" + j), (double) j);
                        if (j % 10 == 0) {
                            dataBridge.commitAllSynchronously();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Flush remaining
        dataBridge.commitAllSynchronously();
    }
}
