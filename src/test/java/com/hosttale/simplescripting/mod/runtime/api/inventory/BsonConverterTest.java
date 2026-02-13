package com.hosttale.simplescripting.mod.runtime.api.inventory;

import org.bson.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class BsonConverterTest {

    private Context context;
    private Scriptable scope;

    @BeforeEach
    void setUp() {
        context = Context.enter();
        context.setLanguageVersion(Context.VERSION_ES6);
        scope = context.initStandardObjects();
    }

    @Test
    void toScriptableConvertsStringValue() {
        BsonDocument doc = new BsonDocument("name", new BsonString("TestItem"));
        
        Object result = BsonConverter.toScriptable(doc, scope);
        
        assertInstanceOf(Scriptable.class, result);
        Scriptable obj = (Scriptable) result;
        assertEquals("TestItem", obj.get("name", obj));
    }

    @Test
    void toScriptableConvertsInt32Value() {
        BsonDocument doc = new BsonDocument("count", new BsonInt32(42));
        
        Object result = BsonConverter.toScriptable(doc, scope);
        
        Scriptable obj = (Scriptable) result;
        assertEquals(42, obj.get("count", obj));
    }

    @Test
    void toScriptableConvertsInt64Value() {
        BsonDocument doc = new BsonDocument("bigNum", new BsonInt64(999999999999L));
        
        Object result = BsonConverter.toScriptable(doc, scope);
        
        Scriptable obj = (Scriptable) result;
        assertEquals(999999999999.0, ((Number) obj.get("bigNum", obj)).doubleValue());
    }

    @Test
    void toScriptableConvertsDoubleValue() {
        BsonDocument doc = new BsonDocument("price", new BsonDouble(12.99));
        
        Object result = BsonConverter.toScriptable(doc, scope);
        
        Scriptable obj = (Scriptable) result;
        assertEquals(12.99, ((Number) obj.get("price", obj)).doubleValue(), 0.001);
    }

    @Test
    void toScriptableConvertsBooleanValue() {
        BsonDocument doc = new BsonDocument("active", new BsonBoolean(true));
        
        Object result = BsonConverter.toScriptable(doc, scope);
        
        Scriptable obj = (Scriptable) result;
        assertEquals(true, obj.get("active", obj));
    }

    @Test
    void toScriptableConvertsArrayValue() {
        BsonArray arr = new BsonArray();
        arr.add(new BsonInt32(1));
        arr.add(new BsonInt32(2));
        arr.add(new BsonInt32(3));
        BsonDocument doc = new BsonDocument("numbers", arr);
        
        Object result = BsonConverter.toScriptable(doc, scope);
        
        Scriptable obj = (Scriptable) result;
        Object numbersObj = obj.get("numbers", obj);
        assertInstanceOf(NativeArray.class, numbersObj);
        NativeArray numbers = (NativeArray) numbersObj;
        assertEquals(3, numbers.getLength());
        assertEquals(1, numbers.get(0, numbers));
        assertEquals(2, numbers.get(1, numbers));
        assertEquals(3, numbers.get(2, numbers));
    }

    @Test
    void toScriptableConvertsNestedDocument() {
        BsonDocument inner = new BsonDocument("x", new BsonInt32(10))
                .append("y", new BsonInt32(20));
        BsonDocument doc = new BsonDocument("position", inner);
        
        Object result = BsonConverter.toScriptable(doc, scope);
        
        Scriptable obj = (Scriptable) result;
        Object posObj = obj.get("position", obj);
        assertInstanceOf(Scriptable.class, posObj);
        Scriptable pos = (Scriptable) posObj;
        assertEquals(10, pos.get("x", pos));
        assertEquals(20, pos.get("y", pos));
    }

    @Test
    void toScriptableConvertsBinaryToBase64() {
        byte[] data = {1, 2, 3, 4, 5};
        BsonDocument doc = new BsonDocument("data", new BsonBinary(data));
        
        Object result = BsonConverter.toScriptable(doc, scope);
        
        Scriptable obj = (Scriptable) result;
        String base64 = (String) obj.get("data", obj);
        assertArrayEquals(data, Base64.getDecoder().decode(base64));
    }

    @Test
    void toScriptableConvertsNullValue() {
        BsonDocument doc = new BsonDocument("empty", BsonNull.VALUE);
        
        Object result = BsonConverter.toScriptable(doc, scope);
        
        Scriptable obj = (Scriptable) result;
        assertNull(obj.get("empty", obj));
    }

    @Test
    void toScriptableReturnsNullForNullDocument() {
        assertNull(BsonConverter.toScriptable(null, scope));
    }

    @Test
    void toScriptableReturnsNullForEmptyDocument() {
        assertNull(BsonConverter.toScriptable(new BsonDocument(), scope));
    }

    @Test
    void toBsonConvertsSimpleObject() {
        ScriptableObject obj = (ScriptableObject) context.newObject(scope);
        obj.put("name", obj, "TestItem");
        obj.put("count", obj, 42);
        obj.put("active", obj, true);
        
        BsonDocument result = BsonConverter.toBson(obj);
        
        assertEquals("TestItem", result.getString("name").getValue());
        assertEquals(42, result.getInt32("count").getValue());
        assertTrue(result.getBoolean("active").getValue());
    }

    @Test
    void toBsonConvertsNestedObject() {
        ScriptableObject inner = (ScriptableObject) context.newObject(scope);
        inner.put("x", inner, 10);
        inner.put("y", inner, 20);
        
        ScriptableObject obj = (ScriptableObject) context.newObject(scope);
        obj.put("position", obj, inner);
        
        BsonDocument result = BsonConverter.toBson(obj);
        
        BsonDocument pos = result.getDocument("position");
        assertEquals(10, pos.getInt32("x").getValue());
        assertEquals(20, pos.getInt32("y").getValue());
    }

    @Test
    void toBsonConvertsArray() {
        NativeArray arr = new NativeArray(new Object[]{1, 2, 3});
        ScriptableObject obj = (ScriptableObject) context.newObject(scope);
        obj.put("numbers", obj, arr);
        
        BsonDocument result = BsonConverter.toBson(obj);
        
        BsonArray numbers = result.getArray("numbers");
        assertEquals(3, numbers.size());
        assertEquals(1, numbers.get(0).asInt32().getValue());
        assertEquals(2, numbers.get(1).asInt32().getValue());
        assertEquals(3, numbers.get(2).asInt32().getValue());
    }

    @Test
    void toBsonPreservesIntegerTypes() {
        ScriptableObject obj = (ScriptableObject) context.newObject(scope);
        obj.put("small", obj, 100);  // Should be Int32
        obj.put("large", obj, 9999999999L);  // Should be Int64
        obj.put("decimal", obj, 12.5);  // Should be Double
        
        BsonDocument result = BsonConverter.toBson(obj);
        
        assertEquals(BsonType.INT32, result.get("small").getBsonType());
        assertEquals(BsonType.INT64, result.get("large").getBsonType());
        assertEquals(BsonType.DOUBLE, result.get("decimal").getBsonType());
    }

    @Test
    void toBsonReturnsEmptyDocumentForNull() {
        BsonDocument result = BsonConverter.toBson(null);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void getMetadataValueReturnsCorrectValue() {
        BsonDocument doc = new BsonDocument("name", new BsonString("TestItem"));
        
        Object result = BsonConverter.getMetadataValue(doc, "name", scope);
        
        assertEquals("TestItem", result);
    }

    @Test
    void getMetadataValueReturnsNullForMissingKey() {
        BsonDocument doc = new BsonDocument("name", new BsonString("TestItem"));
        
        Object result = BsonConverter.getMetadataValue(doc, "missing", scope);
        
        assertNull(result);
    }

    @Test
    void hasMetadataReturnsTrueForExistingKey() {
        BsonDocument doc = new BsonDocument("name", new BsonString("TestItem"));
        
        assertTrue(BsonConverter.hasMetadata(doc, "name"));
    }

    @Test
    void hasMetadataReturnsFalseForMissingKey() {
        BsonDocument doc = new BsonDocument("name", new BsonString("TestItem"));
        
        assertFalse(BsonConverter.hasMetadata(doc, "missing"));
    }

    @Test
    void withMetadataAddsNewKey() {
        BsonDocument doc = new BsonDocument("name", new BsonString("TestItem"));
        
        BsonDocument result = BsonConverter.withMetadata(doc, "count", 42);
        
        assertEquals("TestItem", result.getString("name").getValue());
        assertEquals(42, result.getInt32("count").getValue());
    }

    @Test
    void withMetadataDoesNotModifyOriginal() {
        BsonDocument doc = new BsonDocument("name", new BsonString("TestItem"));
        
        BsonDocument result = BsonConverter.withMetadata(doc, "count", 42);
        
        assertFalse(doc.containsKey("count"));
        assertTrue(result.containsKey("count"));
    }

    @Test
    void withMetadataWorksWithNullDocument() {
        BsonDocument result = BsonConverter.withMetadata(null, "name", "TestItem");
        
        assertEquals("TestItem", result.getString("name").getValue());
    }

    @Test
    void roundTripConversionPreservesData() {
        // Create JavaScript object
        ScriptableObject obj = (ScriptableObject) context.newObject(scope);
        obj.put("name", obj, "TestItem");
        obj.put("count", obj, 64);
        obj.put("price", obj, 12.99);
        obj.put("active", obj, true);
        
        // Convert to BSON
        BsonDocument bson = BsonConverter.toBson(obj);
        
        // Convert back to JavaScript
        Object result = BsonConverter.toScriptable(bson, scope);
        
        // Verify data preserved
        Scriptable resultObj = (Scriptable) result;
        assertEquals("TestItem", resultObj.get("name", resultObj));
        assertEquals(64, resultObj.get("count", resultObj));
        assertEquals(12.99, ((Number) resultObj.get("price", resultObj)).doubleValue(), 0.001);
        assertEquals(true, resultObj.get("active", resultObj));
    }
}
