package org.fergs.scheduler;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fergs.managers.LoggingManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * A lightweight scheduler for delayed and repeating tasks.
 * Uses a ScheduledExecutorService under the hood with a pool size
 * equal to the number of available processors.
 * Tasks are wrapped in a try/catch to ensure exceptions are logged
 * and don't kill the thread.
 * <p>
 * Usage examples:
 * - Schedule a one-off task after a delay:
 *   SpecterScheduler.schedule(() -> { /* task code *\/ }, 5, TimeUnit.SECONDS);
 * - Schedule a repeating task at fixed rate:
 *   SpecterScheduler.scheduleAtFixedRate(() -> { /* task code *\/ }, 0, 10, TimeUnit.SECONDS);
 * - Schedule a repeating task with fixed delay:
 *   SpecterScheduler.scheduleWithFixedDelay(() -> { /* task code *\/ }, 0, 10, TimeUnit.SECONDS);
 * - Schedule a task to run immediately:
 *   SpecterScheduler.scheduleNow(() -> { /* task code *\/ });
 * - Schedule a repeating task at fixed rate starting immediately:
 *   SpecterScheduler.scheduleAtFixedRateNow(() -> { /* task code *\/ }, 10, TimeUnit.SECONDS);
 * <p>
 * Note: The scheduler runs tasks on daemon threads, so it won't prevent the JVM from exiting.
 * Call SpecterScheduler.shutdown() to cleanly shut down the scheduler when done.
 *
 * @author Fergs32
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SpecterScheduler {
    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final AtomicInteger threadCount = new AtomicInteger(0);
    private static final LoggingManager LOGGER = LoggingManager.getInstance();
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
        LOGGER.log(Level.INFO, "Scheduled task {0}", task);
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
        LOGGER.log(Level.INFO, "Scheduled repeating task {0}", task);
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
        LOGGER.log(Level.INFO, "Scheduled repeating task {0}", task);
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
        LOGGER.log(Level.INFO, "Scheduled immediate task {0}", task);
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
        LOGGER.log(Level.INFO, "Scheduled immediate repeating task {0}", task);
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
            LOGGER.log(Level.SEVERE, "Pool size must be positive, got: {0}", newSize);
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
        LOGGER.log(Level.INFO, "Scheduler restarted with pool size: {0}", newSize);
        LOGGER.log(Level.INFO, "Current pool size: {0}", newSize);
    }
    /**
     * Shut down the scheduler (no new tasks will be accepted).
     * Already‐scheduled tasks will still run.
     */
    public static void shutdown() {
        executor.shutdown();
        LOGGER.log(Level.INFO, "Scheduler shut down");
    }
    /**
     * Wraps your task in a try/catch to ensure exceptions get logged and don't kill the thread.
     * @param task the original task to run
     * @return a wrapped Runnable that catches and logs exceptions
     */
    private static Runnable wrap(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                log.error("Exception in scheduled task: {}", task, t);
                LOGGER.log(Level.SEVERE, "Exception in scheduled task: " + task + " - " + t);
            }
        };
    }
}
