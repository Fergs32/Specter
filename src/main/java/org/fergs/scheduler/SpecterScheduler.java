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
    private static ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(
                    POOL_SIZE,
                    r -> {
                        Thread t = new Thread(r, "SpecterScheduler-" + threadCount.incrementAndGet());
                        t.setDaemon(true);
                        return t;
                    }
            );

    /**
     * Schedules a one‑off task to run after a delay, returning a ScheduledFuture.
     * This future can be used to cancel the task if needed, but it will run anyway
     * if not cancelled before the delay expires.
     *
     * @param task  the task to run, wrapped in a try/catch to handle exceptions.
     * @param delay the delay before running the task, in the specified time unit.
     * @param unit  the time unit for the delay (e.g., TimeUnit.SECONDS).
     */
    @SuppressWarnings("unused")
    public static void schedule(Runnable task, long delay, TimeUnit unit) {
        executor.schedule(wrap(task), delay, unit);
    }
    /**
     * Schedules a repeating task with fixed rate, where the next execution starts
     * after a fixed period from the start of the previous execution.
     *
     * @param task the task to run, wrapped in a try/catch to handle exceptions.
     * @param initialDelay the initial delay before the first execution, in the specified time unit.
     * @param period the period between successive executions, in the specified time unit.
     * @param unit the time unit for the initial delay and period (e.g., TimeUnit.SECONDS).
     * @return a ScheduledFuture that can be used to cancel the task if needed.
     */
    @SuppressWarnings("unused")
    public static ScheduledFuture<?> scheduleAtFixedRate(
            Runnable task, long initialDelay, long period, TimeUnit unit
    ) {
        return executor.scheduleAtFixedRate(wrap(task), initialDelay, period, unit);
    }
    /**
     * Schedules a repeating task with fixed delay, where the next execution starts
     * after a fixed period from the end of the previous execution.
     *
     * @param task the task to run, wrapped in a try/catch to handle exceptions.
     * @param initialDelay the initial delay before the first execution, in the specified time unit.
     * @param delay the delay between the end of one execution and the start of the next, in the specified time unit.
     * @param unit the time unit for the initial delay and delay (e.g., TimeUnit.SECONDS).
     * @return a ScheduledFuture that can be used to cancel the task if needed.
     */
    @SuppressWarnings("unused")
    public static ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable task, long initialDelay, long delay, TimeUnit unit
    ) {
        return executor.scheduleWithFixedDelay(wrap(task), initialDelay, delay, unit);
    }
    /**
     * Schedules a one‑off task to run immediately, returning a ScheduledFuture.
     * This future can be used to cancel the task if needed, but it will run anyway
     * if not cancelled before the delay expires.
     *
     * @param task the task to run, wrapped in a try/catch to handle exceptions.
     * @return a ScheduledFuture that can be used to cancel the task if needed.
     */
    @SuppressWarnings("unused")
    public static ScheduledFuture<?> scheduleNow(Runnable task) {
        return executor.schedule(wrap(task), 0, TimeUnit.MILLISECONDS);
    }
    /**
     * Schedules a repeating task with fixed rate, starting immediately.
     *
     * @param task the task to run, wrapped in a try/catch to handle exceptions.
     * @param period the period between successive executions, in the specified time unit.
     * @param unit the time unit for the period (e.g., TimeUnit.SECONDS).
     * @return a ScheduledFuture that can be used to cancel the task if needed.
     */
    @SuppressWarnings("unused")
    public static ScheduledFuture<?> scheduleAtFixedRateNow(Runnable task, long period, TimeUnit unit) {
        return executor.scheduleAtFixedRate(wrap(task), 0, period, unit);
    }
    /**
     * Sets the pool size for the scheduler and restarts it.
     * This will shut down the current executor and create a new one with the specified size.
     *
     * @param newSize the new size of the thread pool, must be positive.
     * @throws IllegalArgumentException if newSize is not positive.
     */
    @SuppressWarnings("unused")
    public void setPoolSizeAndRestart(int newSize) {
        if (newSize <= 0) {
            throw new IllegalArgumentException("Pool size must be positive");
        }
        shutdown();
        threadCount.set(0);
        executor.shutdownNow();
        executor = Executors.newScheduledThreadPool(
                newSize,
                r -> {
                    Thread t = new Thread(r, "SpecterScheduler-" + threadCount.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
        );
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
