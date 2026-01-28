package com.hosttale.simplescripting.mod.runtime;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.registry.Registration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Tracks per-mod registrations so they can be torn down on disable/reload.
 */
public final class ModRegistrationTracker {

    private final List<Registration> registrations = new ArrayList<>();
    private final List<ScheduledFuture<?>> scheduledFutures = new ArrayList<>();

    public synchronized void trackRegistration(Registration registration) {
        if (registration != null) {
            registrations.add(registration);
        }
    }

    public synchronized void trackScheduledFuture(ScheduledFuture<?> future) {
        if (future != null) {
            scheduledFutures.add(future);
        }
    }

    public synchronized void clearAll(HytaleLogger logger) {
        registrations.forEach(registration -> {
            try {
                registration.unregister();
            } catch (Exception e) {
                logger.atWarning().log("Failed to unregister resource: %s", e.getMessage());
            }
        });
        registrations.clear();

        scheduledFutures.forEach(future -> {
            try {
                future.cancel(false);
            } catch (Exception e) {
                logger.atWarning().log("Failed to cancel scheduled task: %s", e.getMessage());
            }
        });
        scheduledFutures.clear();
    }
}
