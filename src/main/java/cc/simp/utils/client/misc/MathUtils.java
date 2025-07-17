package cc.simp.utils.client.misc;

import cc.simp.utils.client.Util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.security.SecureRandom;

public class MathUtils extends Util {

    public static float lerp(final float from, final float to, final float speed) {
        return from + (to - from) * speed;
    }

    public static double roundToDecimalPlace(double value, double inc) {
        final double halfOfInc = inc / 2.0D;
        final double floored = StrictMath.floor(value / inc) * inc;
        if (value >= floored + halfOfInc)
            return new BigDecimal(StrictMath.ceil(value / inc) * inc, MathContext.DECIMAL64).
                    stripTrailingZeros()
                    .doubleValue();
        else
            return new BigDecimal(floored, MathContext.DECIMAL64)
                    .stripTrailingZeros()
                    .doubleValue();
    }

    public static int getRandomNumberUsingNextInt(int min, int max) {
        java.util.Random random = new java.util.Random();
        return random.nextInt(max - min) + min;
    }

    public static double nextSecureInt(final int origin, final int bound) {
        if (origin == bound) {
            return origin;
        }
        final SecureRandom secureRandom = new SecureRandom();
        final int difference = bound - origin;
        return origin + secureRandom.nextInt(difference);
    }

    public static float getAngleDifference(final float a, final float b) {
        return ((a - b) % 360.0f + 540.0f) % 360.0f - 180.0f;
    }

}
