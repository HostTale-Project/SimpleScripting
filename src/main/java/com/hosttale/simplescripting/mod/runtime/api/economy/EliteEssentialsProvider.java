package com.hosttale.simplescripting.mod.runtime.api.economy;

import com.hypixel.hytale.logger.HytaleLogger;

import java.util.UUID;

/**
 * Economy provider for EliteEssentials.
 * Uses reflection to access the EliteEssentials EconomyAPI.
 */
public class EliteEssentialsProvider extends BaseEconomyProvider {
    
    private Class<?> economyApiClass;
    
    public EliteEssentialsProvider(HytaleLogger logger) {
        super(logger);
    }
    
    @Override
    protected String getPluginIdentifier() {
        return "com.eliteessentials:EliteEssentials";
    }
    
    @Override
    protected boolean isReady() {
        return economyApiClass != null;
    }
    
    @Override
    protected boolean initializeProvider() throws Exception {
        // Load the EconomyAPI class
        economyApiClass = Class.forName("com.eliteessentials.api.EconomyAPI");
        
        // Check if economy is enabled
        boolean enabled = (boolean) economyApiClass.getMethod("isEnabled").invoke(null);
        
        if (!enabled) {
            logger.atInfo().log("EliteEssentials economy is disabled in config");
            economyApiClass = null;
            return false;
        }
        
        logger.atInfo().log("EliteEssentials economy integration enabled");
        return true;
    }
    
    @Override
    public String getName() {
        return "EliteEssentials";
    }
    
    @Override
    public double getBalance(String playerUuid) {
        return executeOperation("getBalance", playerUuid, () -> {
            UUID uuid = parseUuid(playerUuid);
            Object result = economyApiClass.getMethod("getBalance", UUID.class).invoke(null, uuid);
            return result instanceof Double ? (Double) result : 0.0;
        }, 0.0);
    }
    
    @Override
    public boolean deposit(String playerUuid, double amount) {
        return executeOperation("deposit", playerUuid, () -> {
            UUID uuid = parseUuid(playerUuid);
            Object result = economyApiClass.getMethod("deposit", UUID.class, double.class).invoke(null, uuid, amount);
            return Boolean.TRUE.equals(result);
        }, false);
    }
    
    @Override
    public boolean withdraw(String playerUuid, double amount) {
        return executeOperation("withdraw", playerUuid, () -> {
            UUID uuid = parseUuid(playerUuid);
            Object result = economyApiClass.getMethod("withdraw", UUID.class, double.class).invoke(null, uuid, amount);
            return Boolean.TRUE.equals(result);
        }, false);
    }
    
    @Override
    public boolean has(String playerUuid, double amount) {
        return executeOperation("has", playerUuid, () -> {
            UUID uuid = parseUuid(playerUuid);
            Object result = economyApiClass.getMethod("has", UUID.class, double.class).invoke(null, uuid, amount);
            return Boolean.TRUE.equals(result);
        }, false);
    }
}