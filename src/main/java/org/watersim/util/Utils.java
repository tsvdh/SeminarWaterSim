package org.watersim.util;

public class Utils {

    public static float lerp(float a, float b, float w) {
        return a * (1 - w) + b * w;
    }
}
