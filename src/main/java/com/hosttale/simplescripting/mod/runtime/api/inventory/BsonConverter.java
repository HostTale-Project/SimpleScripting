package com.hosttale.simplescripting.mod.runtime.api.inventory;

import org.bson.*;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for converting between Hytale's BSON documents and JavaScript objects.
 * 
 * <p>Handles bidirectional conversion while maintaining type fidelity where possible.
 * JavaScript numbers map to BSON Int32/Int64/Double depending on value range.</p>
 * 
 * <h3>Type Mapping</h3>
 * <ul>
 *   <li>BSON String ↔ JS string</li>
 *   <li>BSON Int32/Int64/Double ↔ JS number</li>
 *   <li>BSON Boolean ↔ JS boolean</li>
 *   <li>BSON Array ↔ JS array</li>
 *   <li>BSON Document ↔ JS object</li>
 *   <li>BSON Binary ↔ JS string (base64)</li>
 *   <li>BSON Null ↔ JS null</li>
 * </ul>
 */
public final class BsonConverter {

    private BsonConverter() {
        // Utility class
    }

    /**
     * Convert a BSON document to a JavaScript object.
     * Returns null if the document is null or empty.
     * 
     * @param doc The BSON document to convert
     * @param scope The JavaScript scope for creating objects
     * @return A JavaScript object, or null if doc is null
     */
    public static Object toScriptable(BsonDocument doc, Scriptable scope) {
        if (doc == null || doc.isEmpty()) {
            return null;
        }
        
        var obj = Context.getCurrentContext().newObject(scope);
        
        for (var entry : doc.entrySet()) {
            String key = entry.getKey();
            BsonValue value = entry.getValue();
            obj.put(key, obj, toScriptableValue(value, scope));
        }
        
        return obj;
    }

    /**
     * Convert a BSON value to its JavaScript equivalent.
     * 
     * @param value The BSON value to convert
     * @param scope The JavaScript scope for creating objects/arrays
     * @return The JavaScript equivalent value
     */
    private static Object toScriptableValue(BsonValue value, Scriptable scope) {
        if (value == null || value.isNull()) {
            return null;
        }

        return switch (value.getBsonType()) {
            case STRING -> value.asString().getValue();
            case INT32 -> value.asInt32().getValue();
            case INT64 -> (double) value.asInt64().getValue(); // JS only has number type
            case DOUBLE -> value.asDouble().getValue();
            case BOOLEAN -> value.asBoolean().getValue();
            case ARRAY -> {
                BsonArray arr = value.asArray();
                NativeArray jsArray = new NativeArray(arr.size());
                Context.getCurrentContext().initStandardObjects(); // Ensure scope
                for (int i = 0; i < arr.size(); i++) {
                    jsArray.put(i, jsArray, toScriptableValue(arr.get(i), scope));
                }
                yield jsArray;
            }
            case DOCUMENT -> toScriptable(value.asDocument(), scope);
            case BINARY -> {
                // Encode binary data as base64 string
                byte[] data = value.asBinary().getData();
                yield Base64.getEncoder().encodeToString(data);
            }
            case NULL -> null;
            default -> value.toString(); // Fallback for unsupported types
        };
    }

    /**
     * Convert a JavaScript object to a BSON document.
     * Returns an empty document if the object is null or not convertible.
     * 
     * @param obj The JavaScript object to convert
     * @return A BSON document
     * @throws IllegalArgumentException if circular reference detected
     */
    public static BsonDocument toBson(Object obj) {
        if (obj == null || !(obj instanceof Scriptable scriptable)) {
            return new BsonDocument();
        }
        
        return toBsonDocument(scriptable, new HashSet<>());
    }

    /**
     * Convert a Scriptable object to a BSON document with circular reference detection.
     * 
     * @param scriptable The JavaScript object
     * @param visited Set of already visited objects (for circular reference detection)
     * @return A BSON document
     * @throws IllegalArgumentException if circular reference detected
     */
    private static BsonDocument toBsonDocument(Scriptable scriptable, Set<Object> visited) {
        if (visited.contains(scriptable)) {
            throw new IllegalArgumentException("Circular reference detected in object conversion");
        }
        visited.add(scriptable);
        
        BsonDocument doc = new BsonDocument();
        
        // Get all property IDs (handles both indexed and named properties)
        Object[] ids = scriptable.getIds();
        
        for (Object id : ids) {
            String key = id.toString();
            Object value = scriptable.get(key, scriptable);
            
            if (value != Scriptable.NOT_FOUND) {
                BsonValue bsonValue = toBsonValue(value, visited);
                if (bsonValue != null) {
                    doc.append(key, bsonValue);
                }
            }
        }
        
        visited.remove(scriptable);
        return doc;
    }

    /**
     * Convert a JavaScript value to its BSON equivalent.
     * 
     * @param value The JavaScript value
     * @param visited Set of visited objects for circular reference detection
     * @return The BSON value, or null if not convertible
     */
    private static BsonValue toBsonValue(Object value, Set<Object> visited) {
        if (value == null) {
            return BsonNull.VALUE;
        }

        // Handle primitive types
        if (value instanceof String str) {
            return new BsonString(str);
        }
        if (value instanceof Boolean bool) {
            return new BsonBoolean(bool);
        }
        if (value instanceof Number num) {
            double d = num.doubleValue();
            
            // Preserve integer types when possible
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                long l = num.longValue();
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                    return new BsonInt32((int) l);
                } else {
                    return new BsonInt64(l);
                }
            }
            return new BsonDouble(d);
        }

        // Handle arrays
        if (value instanceof NativeArray arr) {
            BsonArray bsonArray = new BsonArray();
            long length = arr.getLength();
            for (int i = 0; i < length; i++) {
                Object element = arr.get(i, arr);
                if (element != Scriptable.NOT_FOUND) {
                    BsonValue bsonValue = toBsonValue(element, visited);
                    if (bsonValue != null) {
                        bsonArray.add(bsonValue);
                    }
                }
            }
            return bsonArray;
        }

        // Handle objects (convert to BSON document)
        if (value instanceof Scriptable scriptable && !(value instanceof NativeArray)) {
            return toBsonDocument(scriptable, visited);
        }

        // Unsupported type - return null
        return null;
    }

    /**
     * Get a metadata value from a BSON document and convert it to JavaScript.
     * Returns null if the key doesn't exist.
     * 
     * @param doc The BSON document
     * @param key The metadata key
     * @param scope The JavaScript scope
     * @return The JavaScript value, or null if not found
     */
    public static Object getMetadataValue(BsonDocument doc, String key, Scriptable scope) {
        if (doc == null || !doc.containsKey(key)) {
            return null;
        }
        
        BsonValue value = doc.get(key);
        return toScriptableValue(value, scope);
    }

    /**
     * Check if a BSON document has a specific key.
     * 
     * @param doc The BSON document
     * @param key The metadata key
     * @return true if the key exists, false otherwise
     */
    public static boolean hasMetadata(BsonDocument doc, String key) {
        return doc != null && doc.containsKey(key);
    }

    /**
     * Create a new BSON document with an additional key-value pair.
     * Original document is not modified.
     * 
     * @param doc The original document (may be null)
     * @param key The key to add
     * @param value The JavaScript value
     * @return A new BSON document with the added value
     */
    public static BsonDocument withMetadata(BsonDocument doc, String key, Object value) {
        BsonDocument newDoc = new BsonDocument();
        
        // Copy existing entries if doc is not null
        if (doc != null) {
            for (var entry : doc.entrySet()) {
                newDoc.append(entry.getKey(), entry.getValue());
            }
        }
        
        // Add new value
        BsonValue bsonValue = toBsonValue(value, new HashSet<>());
        if (bsonValue != null) {
            newDoc.append(key, bsonValue);
        }
        
        return newDoc;
    }
}
