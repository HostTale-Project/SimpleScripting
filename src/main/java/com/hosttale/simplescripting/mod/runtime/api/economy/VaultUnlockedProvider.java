package com.hosttale.simplescripting.mod.runtime.api.economy;

import com.hypixel.hytale.logger.HytaleLogger;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Economy provider for VaultUnlocked.
 * Uses reflection to avoid compile-time dependency.
 */
public class VaultUnlockedProvider extends BaseEconomyProvider {
    
    private Object economy;
    private Class<?> economyClass;
    private Class<?> economyResponseClass;
    
    public VaultUnlockedProvider(HytaleLogger logger) {
        super(logger);
    }
    
    @Override
    protected String getPluginIdentifier() {
        return "TheNewEconomy:VaultUnlocked";
    }
    
    @Override
    protected boolean isReady() {
        return economy != null;
    }
    
    @Override
    protected boolean initializeProvider() throws Exception {
        // Load VaultUnlocked classes
        economyClass = Class.forName("net.milkbowl.vault2.economy.Economy");
        economyResponseClass = Class.forName("net.milkbowl.vault2.economy.EconomyResponse");
        
        // Access VaultUnlocked's services directly via VaultUnlockedServicesManager
        Class<?> servicesManagerClass = Class.forName("net.cfh.vault.VaultUnlockedServicesManager");
        Object services = servicesManagerClass.getMethod("get").invoke(null);
        
        if (services == null) {
            logger.atInfo().log("VaultUnlockedServicesManager returned null");
            return false;
        }
        
        Object econ = services.getClass().getMethod("economyObj").invoke(services);
        
        if (econ == null) {
            logger.atInfo().log("Economy service is null - no provider registered yet");
            return false;
        }
        
        boolean enabled = (boolean) economyClass.getMethod("isEnabled").invoke(econ);
        if (!enabled) {
            logger.atInfo().log("Economy found but not enabled");
            return false;
        }
        
        this.economy = econ;
        String name = (String) economyClass.getMethod("getName").invoke(econ);
        logger.atInfo().log("VaultUnlocked economy integration enabled: " + name);
        return true;
    }
    
    @Override
    public String getName() {
        if (economy != null) {
            try {
                return (String) economyClass.getMethod("getName").invoke(economy);
            } catch (Exception e) {
                // Ignore
            }
        }
        return "VaultUnlocked";
    }
    
    @Override
    public double getBalance(String playerUuid) {
        return executeOperation("getBalance", playerUuid, () -> {
            UUID uuid = parseUuid(playerUuid);
            Object bal = economyClass.getMethod("balance", String.class, UUID.class)
                    .invoke(economy, "SimpleScripting", uuid);
            return bal instanceof BigDecimal ? ((BigDecimal) bal).doubleValue() : 0.0;
        }, 0.0);
    }
    
    @Override
    public boolean deposit(String playerUuid, double amount) {
        return executeOperation("deposit", playerUuid, () -> {
            UUID uuid = parseUuid(playerUuid);
            Object resp = economyClass.getMethod("deposit", String.class, UUID.class, BigDecimal.class)
                    .invoke(economy, "SimpleScripting", uuid, BigDecimal.valueOf(amount));
            
            Object responseType = economyResponseClass.getField("type").get(resp);
            return "SUCCESS".equals(responseType.toString());
        }, false);
    }
    
    @Override
    public boolean withdraw(String playerUuid, double amount) {
        return executeOperation("withdraw", playerUuid, () -> {
            UUID uuid = parseUuid(playerUuid);
            Object resp = economyClass.getMethod("withdraw", String.class, UUID.class, BigDecimal.class)
                    .invoke(economy, "SimpleScripting", uuid, BigDecimal.valueOf(amount));
            
            Object responseType = economyResponseClass.getField("type").get(resp);
            return "SUCCESS".equals(responseType.toString());
        }, false);
    }
    
    @Override
    public boolean has(String playerUuid, double amount) {
        return executeOperation("has", playerUuid, () -> {
            UUID uuid = parseUuid(playerUuid);
            Object result = economyClass.getMethod("has", String.class, UUID.class, BigDecimal.class)
                    .invoke(economy, "SimpleScripting", uuid, BigDecimal.valueOf(amount));
            return Boolean.TRUE.equals(result);
        }, false);
    }
}
