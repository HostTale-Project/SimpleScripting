package com.hosttale.simplescripting.mod.runtime.api.database;

import com.hosttale.simplescripting.mod.runtime.JsModRuntime;
import com.hosttale.simplescripting.mod.runtime.db.ModDatabase;
import com.hypixel.hytale.logger.HytaleLogger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.typedarrays.NativeArrayBuffer;
import org.mozilla.javascript.typedarrays.NativeArrayBufferView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

public final class DatabaseApi implements AutoCloseable {

    private final JsModRuntime runtime;
    private final ModDatabase database;

    public DatabaseApi(String modId, JsModRuntime runtime, HytaleLogger logger) {
        this.runtime = runtime;
        this.database = new ModDatabase(modId, logger);
    }

    public Scriptable execute(String sql) {
        return execute(sql, null);
    }

    public Scriptable execute(String sql, Object params) {
        List<Object> normalized = normalizeParams(params);
        ModDatabase.UpdateResult result = database.execute(sql, normalized);
        return withContext(cx -> {
            NativeObject obj = (NativeObject) cx.newObject(runtime.getScope());
            ScriptableObject.putProperty(obj, "changes", result.changes());
            OptionalLong lastInsert = result.lastInsertRowId();
            if (lastInsert.isPresent()) {
                ScriptableObject.putProperty(obj, "lastInsertRowid", lastInsert.getAsLong());
            }
            return obj;
        });
    }

    public NativeArray query(String sql) {
        return query(sql, null);
    }

    public NativeArray query(String sql, Object params) {
        List<Object> normalized = normalizeParams(params);
        List<Map<String, Object>> rows = database.query(sql, normalized, 0);
        return withContext(cx -> rowsToJs(rows, cx));
    }

    public Object queryOne(String sql) {
        return queryOne(sql, null);
    }

    public Object queryOne(String sql, Object params) {
        List<Object> normalized = normalizeParams(params);
        List<Map<String, Object>> rows = database.query(sql, normalized, 1);
        if (rows.isEmpty()) {
            return null;
        }
        return withContext(cx -> toJsRow(rows.get(0), cx));
    }

    public Object transaction(Function handler) {
        if (handler == null) {
            throw new IllegalArgumentException("db.transaction requires a function callback.");
        }
        return database.inTransaction(() -> runtime.callFunction(handler, Context.emptyArgs));
    }

    @Override
    public void close() {
        database.close();
    }

    private List<Object> normalizeParams(Object params) {
        if (params == null || params == Undefined.instance) {
            return List.of();
        }
        if (params instanceof NativeArray arr) {
            return normalizeArrayParams(arr);
        }
        if (params instanceof Object[] array) {
            return normalizeArrayParams(array);
        }
        if (params instanceof List<?> list) {
            return normalizeArrayParams(list.toArray());
        }
        return List.of(toSqlValue(params));
    }

    private List<Object> normalizeArrayParams(NativeArray arr) {
        long length = arr.getLength();
        Object[] copy = new Object[(int) length];
        for (int i = 0; i < length; i++) {
            copy[i] = arr.get(i, arr);
        }
        return normalizeArrayParams(copy);
    }

    private List<Object> normalizeArrayParams(Object[] values) {
        List<Object> normalized = new ArrayList<>(values.length);
        for (Object value : values) {
            normalized.add(toSqlValue(value));
        }
        return normalized;
    }

    private Object toSqlValue(Object value) {
        if (value == null || value == Undefined.instance) {
            return null;
        }
        if (value instanceof NativeJavaObject wrapped) {
            return toSqlValue(wrapped.unwrap());
        }
        if (value instanceof NativeArray array) {
            return toBlob(array);
        }
        if (value instanceof NativeArrayBufferView view) {
            return sliceBuffer(view.getBuffer(), view.getByteOffset(), view.getByteLength());
        }
        if (value instanceof NativeArrayBuffer buffer) {
            return sliceBuffer(buffer, 0, buffer.getLength());
        }
        if (value instanceof byte[] bytes) {
            return bytes;
        }
        if (value instanceof Boolean || value instanceof Number || value instanceof String) {
            return value;
        }
        if (value instanceof CharSequence sequence) {
            return sequence.toString();
        }
        throw new IllegalArgumentException("Unsupported parameter type for db: " + value.getClass().getSimpleName());
    }

    private byte[] toBlob(NativeArray array) {
        long length = array.getLength();
        byte[] bytes = new byte[(int) length];
        for (int i = 0; i < length; i++) {
            Object element = array.get(i, array);
            if (!(element instanceof Number number)) {
                throw new IllegalArgumentException("BLOB parameters must be byte arrays (numbers 0-255).");
            }
            int b = number.intValue();
            if (b < 0 || b > 255) {
                throw new IllegalArgumentException("BLOB parameters must contain numbers between 0 and 255.");
            }
            bytes[i] = (byte) b;
        }
        return bytes;
    }

    private byte[] sliceBuffer(NativeArrayBuffer buffer, int offset, int length) {
        byte[] raw = buffer.getBuffer();
        int start = Math.max(0, offset);
        int end = Math.min(raw.length, start + Math.max(0, length));
        return Arrays.copyOfRange(raw, start, end);
    }

    private NativeArray rowsToJs(List<Map<String, Object>> rows, Context cx) {
        Object[] jsRows = new Object[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            jsRows[i] = toJsRow(rows.get(i), cx);
        }
        return (NativeArray) cx.newArray(runtime.getScope(), jsRows);
    }

    private ScriptableObject toJsRow(Map<String, Object> row, Context cx) {
        ScriptableObject jsRow = (ScriptableObject) cx.newObject(runtime.getScope());
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            Object jsValue = Context.javaToJS(entry.getValue(), runtime.getScope());
            ScriptableObject.putProperty(jsRow, entry.getKey(), jsValue);
        }
        return jsRow;
    }

    private <T> T withContext(java.util.function.Function<Context, T> fn) {
        Context cx = Context.getCurrentContext();
        boolean entered = false;
        if (cx == null) {
            cx = runtime.enterContext();
            entered = true;
        }
        try {
            return fn.apply(cx);
        } finally {
            if (entered) {
                Context.exit();
            }
        }
    }
}
