package com.hosttale.simplescripting.mod.runtime.api.ecs;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

final class VectorConverters {

    private VectorConverters() {
    }

    static Vector3d toVector3d(Object raw, String fieldName) {
        if (raw instanceof Vector3d vec) {
            return vec;
        }
        if (raw instanceof Number number) {
            double v = number.doubleValue();
            return new Vector3d(v, v, v);
        }
        if (raw instanceof Scriptable scriptable) {
            if (ScriptableObject.hasProperty(scriptable, "length")
                    && ScriptableObject.getProperty(scriptable, "length") instanceof Number length
                    && length.intValue() >= 3) {
                double x = toDouble(ScriptableObject.getProperty(scriptable, 0), fieldName + "[0]");
                double y = toDouble(ScriptableObject.getProperty(scriptable, 1), fieldName + "[1]");
                double z = toDouble(ScriptableObject.getProperty(scriptable, 2), fieldName + "[2]");
                return new Vector3d(x, y, z);
            }
            double x = toDouble(ScriptableObject.getProperty(scriptable, "x"), fieldName + ".x");
            double y = toDouble(ScriptableObject.getProperty(scriptable, "y"), fieldName + ".y");
            double z = toDouble(ScriptableObject.getProperty(scriptable, "z"), fieldName + ".z");
            return new Vector3d(x, y, z);
        }
        throw new IllegalArgumentException("Expected " + fieldName + " to be a Vector3d-like object.");
    }

    static Vector3f toVector3f(Object raw, String fieldName) {
        if (raw instanceof Vector3f vec) {
            return vec;
        }
        if (raw instanceof Number number) {
            float v = number.floatValue();
            return new Vector3f(v, v, v);
        }
        if (raw instanceof Scriptable scriptable) {
            if (ScriptableObject.hasProperty(scriptable, "length")
                    && ScriptableObject.getProperty(scriptable, "length") instanceof Number length
                    && length.intValue() >= 3) {
                float x = (float) toDouble(ScriptableObject.getProperty(scriptable, 0), fieldName + "[0]");
                float y = (float) toDouble(ScriptableObject.getProperty(scriptable, 1), fieldName + "[1]");
                float z = (float) toDouble(ScriptableObject.getProperty(scriptable, 2), fieldName + "[2]");
                return new Vector3f(x, y, z);
            }
            float x = (float) toDouble(ScriptableObject.getProperty(scriptable, "x"), fieldName + ".x");
            float y = (float) toDouble(ScriptableObject.getProperty(scriptable, "y"), fieldName + ".y");
            float z = (float) toDouble(ScriptableObject.getProperty(scriptable, "z"), fieldName + ".z");
            return new Vector3f(x, y, z);
        }
        throw new IllegalArgumentException("Expected " + fieldName + " to be a Vector3f-like object.");
    }

    private static double toDouble(Object value, String fieldName) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalArgumentException("Expected numeric value for " + fieldName + ".");
    }
}
