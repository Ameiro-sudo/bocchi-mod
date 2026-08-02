package me.baier.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Random;

public class MathUtil {
    public static final DecimalFormat DF_0 = new DecimalFormat("0");
    public static final DecimalFormat DF_1 = new DecimalFormat("0.0");
    public static final DecimalFormat DF_2 = new DecimalFormat("0.00");
    public static final float PI = (float) Math.PI;
    public static final float TO_RADIANS = PI / 180.0F;
    public static final float TO_DEGREES = 180.0F / PI;

    public static float clamp2(float num, float min, float max) {
        return num < min ? min : Math.min(num, max);
    }
    public static Double clamp(final double num, final double min, final double max) {
        return Math.min(Math.max(num, min), max);
    }
    public static double getRandomInRange(double min, double max) {
        SecureRandom random = new SecureRandom();
        return min == max ? min : random.nextDouble() * (max - min) + min;
    }

    public static int getRandomNumberUsingNextInt(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }

    public static double lerp(double old, double newVal, double amount) {
        return (1.0 - amount) * old + amount * newVal;
    }

    public static Double interpolate(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return interpolate(oldValue, newValue, (float) interpolationValue).floatValue();
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return interpolate(oldValue, newValue, (float) interpolationValue).intValue();
    }

    public static float calculateGaussianValue(float x, float sigma) {
        double output = 1.0 / Math.sqrt(2.0 * Math.PI * (sigma * sigma));
        return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }

    public static double roundToHalf(double d) {
        return Math.round(d * 2) / 2.0;
    }

    public static double round(double num, double increment) {
        BigDecimal bd = new BigDecimal(num);
        bd = (bd.setScale((int) increment, RoundingMode.HALF_UP));
        return bd.doubleValue();
    }

    public static double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static String round(String value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.stripTrailingZeros();
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.toString();
    }

    public static float getRandomFloat(float max, float min) {
        SecureRandom random = new SecureRandom();
        return random.nextFloat() * (max - min) + min;
    }

    public static int getNumberOfDecimalPlace(double value) {
        final BigDecimal bigDecimal = new BigDecimal(value);
        return Math.max(0, bigDecimal.stripTrailingZeros().scale());
    }
    public static Random random = new Random();

    public static boolean equals(float a, float b) {
        return Math.abs(a - b) < 1.0E-5;
    }

    public static int getRandom(final int min, final int max) {
        if (max < min) {
            return 0;
        }
        return min + random.nextInt((max - min) + 1);
    }
}
