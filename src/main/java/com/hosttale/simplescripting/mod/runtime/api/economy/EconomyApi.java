package com.hosttale.simplescripting.mod.runtime.api.economy;

import com.hypixel.hytale.logger.HytaleLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * JS API facade for economy integration.
 * Supports multiple economy providers (VaultUnlocked, EliteEssentials, etc.).
 * Uses lazy initialization and provider priority to find an available economy system.
 */
public final class EconomyApi {

    private final HytaleLogger logger;
    private final List<EconomyProvider> providers;
    private EconomyProvider activeProvider;
    private boolean initialized = false;

    public EconomyApi(HytaleLogger logger) {
        this.logger = logger.getSubLogger("economy");
        this.providers = new ArrayList<>();
        
        // Register providers in priority order (VaultUnlocked first, then EliteEssentials)
        providers.add(new VaultUnlockedProvider(logger.getSubLogger("vault")));
        providers.add(new EliteEssentialsProvider(logger.getSubLogger("eliteessentials")));
    }

    private synchronized EconomyProvider getProvider() {
        if (initialized) {
            return activeProvider;
        }
        
        initialized = true;
        
        // Try each provider in order until one is available
        for (EconomyProvider provider : providers) {
            if (provider.isAvailable()) {
                activeProvider = provider;
                logger.atInfo().log("Using economy provider: " + provider.getName());
                return activeProvider;
            }
        }
        
        logger.atInfo().log("No economy provider available");
        return null;
    }

    public boolean isAvailable() {
        return getProvider() != null;
    }

    /**
     * Get player's balance in default currency/world.
     */
    public double balance(String playerUuid) {
        EconomyProvider provider = getProvider();
        return provider != null ? provider.getBalance(playerUuid) : 0.0;
    }

    /**
     * Deposit amount to player's account.
     */
    public boolean deposit(String playerUuid, double amount) {
        EconomyProvider provider = getProvider();
        return provider != null && provider.deposit(playerUuid, amount);
    }

    /**
     * Withdraw amount from player's account.
     */
    public boolean withdraw(String playerUuid, double amount) {
        EconomyProvider provider = getProvider();
        return provider != null && provider.withdraw(playerUuid, amount);
    }

    /**
     * Check if player has sufficient balance.
     */
    public boolean has(String playerUuid, double amount) {
        EconomyProvider provider = getProvider();
        return provider != null && provider.has(playerUuid, amount);
    }

    /**
     * Get economy provider name.
     */
    public String getName() {
        EconomyProvider provider = getProvider();
        return provider != null ? provider.getName() : "None";
    }
}