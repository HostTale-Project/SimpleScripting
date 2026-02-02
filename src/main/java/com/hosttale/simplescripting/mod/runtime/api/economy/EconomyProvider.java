package com.hosttale.simplescripting.mod.runtime.api.economy;

/**
 * Abstraction for economy service providers.
 * Allows supporting multiple economy plugins with a unified interface.
 */
public interface EconomyProvider {
    
    /**
     * Get the provider name.
     */
    String getName();
    
    /**
     * Check if this provider is available and ready.
     */
    boolean isAvailable();
    
    /**
     * Get player's balance.
     */
    double getBalance(String playerUuid);
    
    /**
     * Deposit amount to player's account.
     */
    boolean deposit(String playerUuid, double amount);
    
    /**
     * Withdraw amount from player's account.
     */
    boolean withdraw(String playerUuid, double amount);
    
    /**
     * Check if player has sufficient balance.
     */
    boolean has(String playerUuid, double amount);
}
