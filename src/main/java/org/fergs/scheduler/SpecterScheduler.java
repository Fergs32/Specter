package org.fergs.scheduler;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A lightweight scheduler for delayed and repeating tasks.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SpecterScheduler {
    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final AtomicInteger threadCount = new AtomicInteger(0);
    private static final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(
                    POOL_SIZE,
                    r -> {
                        Thread t = new Thread(r, "SpecterScheduler-" + threadCount.incrementAndGet());
                        t.setDaemon(true);
                        return t;
                    }
            );
    /**
     * Schedule a one‐off task to run after the given delay.
     * @return a ScheduledFuture you can cancel if needed.
     */
    public static ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return executor.schedule(wrap(task), delay, unit);
    }

    /**
     * Schedule a repeating task at a fixed rate (period measured from start‑time).
     * @return a ScheduledFuture you can cancel to stop repeats.
     */
    public static ScheduledFuture<?> scheduleAtFixedRate(
            Runnable task, long initialDelay, long period, TimeUnit unit
    ) {
        return executor.scheduleAtFixedRate(wrap(task), initialDelay, period, unit);
    }

    /**
     * Schedule a repeating task with fixed delay between end of one execution and start of next.
     * @return a ScheduledFuture you can cancel to stop repeats.
     */
    public static ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable task, long initialDelay, long delay, TimeUnit unit
    ) {
        return executor.scheduleWithFixedDelay(wrap(task), initialDelay, delay, unit);
    }

    /**
     * Shut down the scheduler (no new tasks will be accepted).
     * Already‐scheduled tasks will still run.
     */
    public static void shutdown() {
        executor.shutdown();
    }

    /**
     * Wraps your task in a try/catch to ensure exceptions get logged and don't kill the thread.
     */
    private static Runnable wrap(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                System.err.println("Error in scheduled task: " + t.getMessage());
            }
        };
    }
}
