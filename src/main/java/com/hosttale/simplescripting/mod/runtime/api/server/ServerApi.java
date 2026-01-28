package com.hosttale.simplescripting.mod.runtime.api.server;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hosttale.simplescripting.mod.runtime.ModRegistrationTracker;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.ShutdownReason;
import com.hypixel.hytale.server.core.task.TaskRegistration;
import com.hypixel.hytale.server.core.task.TaskRegistry;
import org.mozilla.javascript.Function;

import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ServerApi {

    private final String modId;
    private final TaskRegistry taskRegistry;
    private final ModRegistrationTracker registrationTracker;
    private final JsModRuntime runtime;
    private final HytaleLogger logger;
    private final AtomicInteger idSequence = new AtomicInteger();

    public ServerApi(String modId,
                     TaskRegistry taskRegistry,
                     ModRegistrationTracker registrationTracker,
                     JsModRuntime runtime,
                     HytaleLogger logger) {
        this.modId = modId;
        this.taskRegistry = taskRegistry;
        this.registrationTracker = registrationTracker;
        this.runtime = runtime;
        this.logger = logger.getSubLogger("server");
    }

    public String name() {
        return HytaleServer.get().getServerName();
    }

    public JsTaskHandle runLater(int delayMs, Function handler) {
        if (handler == null) {
            throw new IllegalArgumentException("server.runLater requires a function callback.");
        }
        if (delayMs < 0) {
            throw new IllegalArgumentException("delayMs must be zero or positive.");
        }
        String handleId = modId + "-task-" + idSequence.incrementAndGet();
        ScheduledFuture<Void> future = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            try {
                runtime.callFunction(handler, new Object[0]);
            } catch (Exception e) {
                logger.atSevere().log("Task %s failed: %s", handleId, e.getMessage());
            }
            return null;
        }, delayMs, TimeUnit.MILLISECONDS);

        TaskRegistration registration = taskRegistry.registerTask(future);
        registrationTracker.trackRegistration(registration);
        registrationTracker.trackScheduledFuture(future);
        return new JsTaskHandle(handleId, future);
    }

    public JsTaskHandle runRepeating(int initialDelayMs, int periodMs, Function handler) {
        if (handler == null) {
            throw new IllegalArgumentException("server.runRepeating requires a function callback.");
        }
        if (initialDelayMs < 0 || periodMs <= 0) {
            throw new IllegalArgumentException("initialDelayMs must be >= 0 and periodMs must be > 0.");
        }
        String handleId = modId + "-task-" + idSequence.incrementAndGet();
        ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                runtime.callFunction(handler, new Object[0]);
            } catch (Exception e) {
                logger.atSevere().log("Repeating task %s failed: %s", handleId, e.getMessage());
            }
        }, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);

        registrationTracker.trackRegistration(new TaskRegistration(future));
        registrationTracker.trackScheduledFuture(future);
        return new JsTaskHandle(handleId, future);
    }

    public void shutdown(String reason) {
        ShutdownReason shutdownReason = (reason == null || reason.isBlank())
                ? ShutdownReason.SHUTDOWN
                : ShutdownReason.SHUTDOWN.withMessage(reason);
        HytaleServer.get().shutdownServer(shutdownReason);
    }

    public boolean isBooted() {
        return HytaleServer.get().isBooted();
    }

    public record JsTaskHandle(String id, ScheduledFuture<?> future) {
        public boolean cancel() {
            return future.cancel(false);
        }

        public boolean cancelled() {
            return future.isCancelled();
        }
    }
}
