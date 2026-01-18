package com.hosttale.simplescripting.task;

import com.hosttale.simplescripting.util.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scheduler for delayed and repeating tasks.
 * Provides setTimeout/setInterval-like functionality for JavaScript.
 */
public class Scheduler {
    private static final int TICK_DURATION_MS = 50; // 20 ticks per second
    
    private final ScheduledExecutorService executor;
    private final Map<Long, ScheduledFuture<?>> tasks;
    private final AtomicLong nextTaskId;
    private final Scriptable scope;
    private final Logger logger;

    public Scheduler(Scriptable scope, Logger logger) {
        this.executor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "SimpleScripting-Scheduler");
            t.setDaemon(true);
            return t;
        });
        this.tasks = new ConcurrentHashMap<>();
        this.nextTaskId = new AtomicLong(1);
        this.scope = scope;
        this.logger = logger;
    }

    /**
     * Schedules a task to run after a delay.
     * @param callback The JavaScript function to execute
     * @param delayTicks Delay in game ticks (20 ticks = 1 second)
     * @return Task ID that can be used to cancel the task
     */
    public long runLater(@Nonnull Function callback, int delayTicks) {
        long taskId = nextTaskId.getAndIncrement();
        long delayMs = delayTicks * TICK_DURATION_MS;

        ScheduledFuture<?> future = executor.schedule(() -> {
            executeCallback(callback, taskId);
            tasks.remove(taskId);
        }, delayMs, TimeUnit.MILLISECONDS);

        tasks.put(taskId, future);
        return taskId;
    }

    /**
     * Schedules a task to run after a delay in milliseconds.
     * @param callback The JavaScript function to execute
     * @param delayMs Delay in milliseconds
     * @return Task ID that can be used to cancel the task
     */
    public long runLaterMs(@Nonnull Function callback, long delayMs) {
        long taskId = nextTaskId.getAndIncrement();

        ScheduledFuture<?> future = executor.schedule(() -> {
            executeCallback(callback, taskId);
            tasks.remove(taskId);
        }, delayMs, TimeUnit.MILLISECONDS);

        tasks.put(taskId, future);
        return taskId;
    }

    /**
     * Schedules a repeating task.
     * @param callback The JavaScript function to execute
     * @param delayTicks Initial delay in game ticks
     * @param periodTicks Period between executions in game ticks
     * @return Task ID that can be used to cancel the task
     */
    public long runRepeating(@Nonnull Function callback, int delayTicks, int periodTicks) {
        long taskId = nextTaskId.getAndIncrement();
        long delayMs = delayTicks * TICK_DURATION_MS;
        long periodMs = periodTicks * TICK_DURATION_MS;

        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            executeCallback(callback, taskId);
        }, delayMs, periodMs, TimeUnit.MILLISECONDS);

        tasks.put(taskId, future);
        return taskId;
    }

    /**
     * Schedules a repeating task with millisecond precision.
     * @param callback The JavaScript function to execute
     * @param delayMs Initial delay in milliseconds
     * @param periodMs Period between executions in milliseconds
     * @return Task ID that can be used to cancel the task
     */
    public long runRepeatingMs(@Nonnull Function callback, long delayMs, long periodMs) {
        long taskId = nextTaskId.getAndIncrement();

        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            executeCallback(callback, taskId);
        }, delayMs, periodMs, TimeUnit.MILLISECONDS);

        tasks.put(taskId, future);
        return taskId;
    }

    // ========================================================================
    // JAVA RUNNABLE OVERLOADS (for internal use)
    // ========================================================================

    /**
     * Schedules a Java Runnable to run after a delay.
     * @param runnable The Java Runnable to execute
     * @param delayMs Delay in milliseconds
     * @return Task ID that can be used to cancel the task
     */
    public long runLaterMs(@Nonnull Runnable runnable, long delayMs) {
        long taskId = nextTaskId.getAndIncrement();

        ScheduledFuture<?> future = executor.schedule(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.severe("Error in scheduled task: " + e.getMessage());
            }
            tasks.remove(taskId);
        }, delayMs, TimeUnit.MILLISECONDS);

        tasks.put(taskId, future);
        return taskId;
    }

    /**
     * Schedules a Java Runnable to run repeatedly.
     * @param runnable The Java Runnable to execute
     * @param periodMs Period between executions in milliseconds
     * @return Task ID that can be used to cancel the task
     */
    public long runRepeatingMs(@Nonnull Runnable runnable, long periodMs) {
        long taskId = nextTaskId.getAndIncrement();

        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.severe("Error in repeating task: " + e.getMessage());
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);

        tasks.put(taskId, future);
        return taskId;
    }

    /**
     * Cancels a scheduled task.
     * @param taskId The task ID returned by runLater or runRepeating
     * @return true if the task was cancelled, false if not found
     */
    public boolean cancel(long taskId) {
        ScheduledFuture<?> future = tasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
            return true;
        }
        return false;
    }

    /**
     * Cancels all scheduled tasks.
     */
    public void cancelAll() {
        for (ScheduledFuture<?> future : tasks.values()) {
            future.cancel(false);
        }
        tasks.clear();
    }

    /**
     * Checks if a task is still scheduled.
     * @param taskId The task ID
     * @return true if the task exists and hasn't completed
     */
    public boolean isScheduled(long taskId) {
        ScheduledFuture<?> future = tasks.get(taskId);
        return future != null && !future.isDone() && !future.isCancelled();
    }

    /**
     * Gets the number of active tasks.
     * @return Count of scheduled tasks
     */
    public int getActiveTaskCount() {
        return tasks.size();
    }

    /**
     * Converts seconds to ticks.
     * @param seconds Number of seconds
     * @return Equivalent number of ticks
     */
    public static int secondsToTicks(double seconds) {
        return (int) (seconds * 20);
    }

    /**
     * Converts ticks to seconds.
     * @param ticks Number of ticks
     * @return Equivalent number of seconds
     */
    public static double ticksToSeconds(int ticks) {
        return ticks / 20.0;
    }

    /**
     * Executes a JavaScript callback function.
     */
    private void executeCallback(Function callback, long taskId) {
        try {
            Context cx = Context.enter();
            try {
                callback.call(cx, scope, scope, new Object[]{taskId});
            } finally {
                Context.exit();
            }
        } catch (Exception e) {
            logger.severe("Error executing scheduled task " + taskId + ": " + e.getMessage());
        }
    }

    /**
     * Shuts down the scheduler.
     * Should be called when the plugin is disabled.
     */
    public void shutdown() {
        cancelAll();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
