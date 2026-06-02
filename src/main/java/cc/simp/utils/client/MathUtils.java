package cc.simp.utils.client;

import cc.simp.utils.Util;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.concurrent.ThreadLocalRandom;

public class MathUtils extends Util {

    public static final float PI = (float) Math.PI;
    public static final float TO_RADIANS = PI / 180.0F;
    public static final float TO_DEGREES = 180.0F / PI;

    public static double wrappedDifference(double number1, double number2) {
        return Math.min(Math.abs(number1 - number2), Math.min(Math.abs(number1 - 360) - Math.abs(number2 - 0), Math.abs(number2 - 360) - Math.abs(number1 - 0)));
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

    public static double getRandom(double min, double max) {
        if (min == max) {
            return min;
        } else if (min > max) {
            final double d = min;
            min = max;
            max = d;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static float calculateGaussianValue(float x, float sigma) {
        double PI = Math.PI;
        double output = 1.0 / Math.sqrt(2.0 * PI * (sigma * sigma));
        return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }

    public static double lerp(double pct, double start, double end) {
        return start + pct * (end - start);
    }

    public static float lerp(float min, float max, float delta) {
        return min + (max - min) * delta;
    }
    public static double incValue(double val, double inc) {
        double one = 1.0 / inc;
        return Math.round(val * one) / one;
    }

    public static float interpolate(float current, float target) {
        return interpolate(current, target, mc.timer.renderPartialTicks);
    }

    public static float interpolate(float current, float target, float multiple) {
        if (multiple == mc.timer.renderPartialTicks) {
            return current + (target - current) * multiple;
        }

        return current;
    }

    public static double interpolate(double current, double target) {
        return interpolate(current, target, mc.timer.renderPartialTicks);
    }

    public static double interpolate(double current, double target, float multiple) {
        return interpolate((float) current, (float) target, multiple);
    }

    public static Vec3 interpolate(Vec3 current, Vec3 target) {
        return interpolate(current, target, mc.timer.renderPartialTicks);
    }

    public static Vec3 interpolate(Vec3 current, Vec3 target, float multiple) {
        if (multiple == mc.timer.renderPartialTicks) {
            return new Vec3(
                    interpolate(current.xCoord, target.xCoord, multiple),
                    interpolate(current.yCoord, target.yCoord, multiple),
                    interpolate(current.zCoord, target.zCoord, multiple));
        }

        return current;
    }

    public static AxisAlignedBB interpolate(AxisAlignedBB current, AxisAlignedBB target) {
        return interpolate(current, target, mc.timer.renderPartialTicks);
    }

    public static AxisAlignedBB interpolate(AxisAlignedBB current, AxisAlignedBB target, float multiple) {
        if (multiple == mc.timer.renderPartialTicks) {
            return new AxisAlignedBB(
                    interpolate(current.minX, target.minX, multiple),
                    interpolate(current.minY, target.minY, multiple),
                    interpolate(current.minZ, target.minZ, multiple),
                    interpolate(current.maxX, target.maxX, multiple),
                    interpolate(current.maxY, target.maxY, multiple),
                    interpolate(current.maxZ, target.maxZ, multiple)
            );
        }

        return current;
    }
}
