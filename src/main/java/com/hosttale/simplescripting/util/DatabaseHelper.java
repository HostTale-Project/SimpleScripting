package com.hosttale.simplescripting.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Helper class for persisting data in JSON files from JavaScript.
 * Provides simple database-like functionality using JSON files.
 */
public class DatabaseHelper {
    private final Path dbPath;
    private final Gson gson;

    public DatabaseHelper() {
        // Create db folder in SimpleScripting directory
        this.dbPath = Constants.UNIVERSE_PATH
                .resolve("SimpleScripting")
                .resolve("db");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Ensure db directory exists
        try {
            if (!Files.exists(dbPath)) {
                Files.createDirectories(dbPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create database directory", e);
        }
    }

    /**
     * Saves data to a JSON file.
     * @param fileName The name of the file (without .json extension)
     * @param key The key to store the data under
     * @param value The value to store (can be String, Number, Boolean, or JSON string)
     * @return true if successful, false otherwise
     */
    public boolean save(String fileName, String key, Object value) {
        try {
            Path filePath = dbPath.resolve(fileName + ".json");
            
            // Read existing data or create new object
            JsonObject data;
            if (Files.exists(filePath)) {
                String content = Files.readString(filePath);
                data = JsonParser.parseString(content).getAsJsonObject();
            } else {
                data = new JsonObject();
            }
            
            // Add or update the key-value pair
            if (value instanceof String) {
                String strValue = (String) value;
                // Try to parse as JSON if it looks like JSON
                if (strValue.trim().startsWith("{") || strValue.trim().startsWith("[")) {
                    try {
                        JsonElement jsonValue = JsonParser.parseString(strValue);
                        data.add(key, jsonValue);
                    } catch (Exception e) {
                        // If not valid JSON, store as string
                        data.addProperty(key, strValue);
                    }
                } else {
                    data.addProperty(key, strValue);
                }
            } else if (value instanceof Number) {
                data.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                data.addProperty(key, (Boolean) value);
            } else {
                // Try to convert to JSON
                String jsonStr = gson.toJson(value);
                JsonElement jsonValue = JsonParser.parseString(jsonStr);
                data.add(key, jsonValue);
            }
            
            // Write to file
            String json = gson.toJson(data);
            Files.writeString(filePath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reads data from a JSON file.
     * @param fileName The name of the file (without .json extension)
     * @param key The key to read
     * @return The value as a string, or null if not found
     */
    public String get(String fileName, String key) {
        try {
            Path filePath = dbPath.resolve(fileName + ".json");
            
            if (!Files.exists(filePath)) {
                return null;
            }
            
            String content = Files.readString(filePath);
            JsonObject data = JsonParser.parseString(content).getAsJsonObject();
            
            if (!data.has(key)) {
                return null;
            }
            
            JsonElement element = data.get(key);
            
            // Return the element as string
            if (element.isJsonPrimitive()) {
                return element.getAsString();
            } else {
                // Return JSON object/array as string
                return gson.toJson(element);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if a key exists in a file.
     * @param fileName The name of the file (without .json extension)
     * @param key The key to check
     * @return true if the key exists, false otherwise
     */
    public boolean has(String fileName, String key) {
        try {
            Path filePath = dbPath.resolve(fileName + ".json");
            
            if (!Files.exists(filePath)) {
                return false;
            }
            
            String content = Files.readString(filePath);
            JsonObject data = JsonParser.parseString(content).getAsJsonObject();
            
            return data.has(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a key from a file.
     * @param fileName The name of the file (without .json extension)
     * @param key The key to delete
     * @return true if successful, false otherwise
     */
    public boolean delete(String fileName, String key) {
        try {
            Path filePath = dbPath.resolve(fileName + ".json");
            
            if (!Files.exists(filePath)) {
                return false;
            }
            
            String content = Files.readString(filePath);
            JsonObject data = JsonParser.parseString(content).getAsJsonObject();
            
            if (!data.has(key)) {
                return false;
            }
            
            data.remove(key);
            
            // Write back to file
            String json = gson.toJson(data);
            Files.writeString(filePath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets all data from a file.
     * @param fileName The name of the file (without .json extension)
     * @return JSON string of all data, or null if file doesn't exist
     */
    public String getAll(String fileName) {
        try {
            Path filePath = dbPath.resolve(fileName + ".json");
            
            if (!Files.exists(filePath)) {
                return null;
            }
            
            return Files.readString(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deletes an entire file.
     * @param fileName The name of the file (without .json extension)
     * @return true if successful, false otherwise
     */
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = dbPath.resolve(fileName + ".json");
            
            if (!Files.exists(filePath)) {
                return false;
            }
            
            Files.delete(filePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the database directory path.
     * @return The path to the db directory
     */
    public String getDbPath() {
        return dbPath.toString();
    }

    /**
     * Cast String to UUID
     */
    public UUID castStringToUUID(String str) {
        return UUID.fromString(str);
    }
}
