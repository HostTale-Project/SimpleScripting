package com.hosttale.simplescripting.util;

import com.hypixel.hytale.logger.HytaleLogger;

/**
 * Simplified logger wrapper for easier JavaScript usage.
 * Provides direct methods instead of requiring chained calls.
 */
public class Logger {
    private final HytaleLogger logger;

    /**
     * Creates a Logger instance with the specified HytaleLogger.
     */
    public Logger(HytaleLogger hytaleLogger) {
        this.logger = hytaleLogger;
    }

    /**
     * Logs an informational message.
     */
    public void info(String message) {
        if (logger != null) {
            logger.atInfo().log(message);
        }
    }

    /**
     * Logs a severe error message.
     */
    public void severe(String message) {
        if (logger != null) {
            logger.atSevere().log(message);
        }
    }

    /**
     * Logs a warning message.
     */
    public void warning(String message) {
        if (logger != null) {
            logger.atWarning().log(message);
        }
    }

    /**
     * Logs a configuration message.
     */
    public void config(String message) {
        if (logger != null) {
            logger.atConfig().log(message);
        }
    }

    /**
     * Logs a fine-level debug message.
     */
    public void fine(String message) {
        if (logger != null) {
            logger.atFine().log(message);
        }
    }

    /**
     * Logs a finer-level debug message.
     */
    public void finer(String message) {
        if (logger != null) {
            logger.atFiner().log(message);
        }
    }

    /**
     * Logs a finest-level debug message.
     */
    public void finest(String message) {
        if (logger != null) {
            logger.atFinest().log(message);
        }
    }

    /**
     * Gets the underlying logger instance.
     */
    public HytaleLogger getLogger() {
        return logger;
    }
}
