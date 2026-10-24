package io.github.adytech99.healthindicators.util;

/** Calculations shared by world indicators and the HUD, independent of Minecraft or a graphics backend. */
public final class IndicatorMath {
    private IndicatorMath() {
    }

    public static float finiteNonNegative(float value) {
        return Float.isFinite(value) && value > 0.0F ? value : 0.0F;
    }

    public static int healthPoints(float value) {
        return boundedCeil(finiteNonNegative(value));
    }

    public static int heartCount(int points) {
        // Adding one before dividing would overflow for Integer.MAX_VALUE.
        return points <= 0 ? 0 : points / 2 + points % 2;
    }

    public static double percentage(float health, float absorption, float maximum) {
        if (!Float.isFinite(maximum) || maximum <= 0.0F) {
            return 0.0;
        }
        // Add in double precision: two finite float values can overflow a float sum.
        return ((double) finiteNonNegative(health) + finiteNonNegative(absorption)) / maximum * 100.0;
    }

    public static int scaledHealth(float health, float maximum, int displayMaximum) {
        int maximumPoints = healthPoints(maximum);
        if (maximumPoints == 0 || displayMaximum <= 0) {
            return 0;
        }
        // Preserve the original order: round health and maximum before scaling the heart display.
        return boundedCeil((double) healthPoints(health) / maximumPoints * displayMaximum);
    }

    public static int decimalPlaces(int places) {
        // The existing YACL slider supports 0..12; retain that entire range.
        return Math.clamp(places, 0, 12);
    }

    public static int iconsPerRow(int value) {
        return value > 0 ? value : 10;
    }

    public static String formatHealth(float health, float absorption, float maximum, int places,
                                      boolean asPercentage) {
        String format = "%." + decimalPlaces(places) + "f";
        if (asPercentage) {
            return String.format(format, percentage(health, absorption, maximum)) + " %";
        }
        double current = (double) finiteNonNegative(health) + finiteNonNegative(absorption);
        // Keep the default locale used by the original health text formatter.
        return String.format(format, current) + " / " + String.format(format, (double) finiteNonNegative(maximum));
    }

    public static int damageAlpha(int elapsedTicks, int visibilitySeconds, int fadeSeconds, boolean fade) {
        // Seconds come from configuration and may be edited beyond the GUI bounds.
        long duration = Math.max(0L, (long) visibilitySeconds) * 20L;
        long elapsed = Math.max(0L, (long) elapsedTicks);
        if (duration == 0 || elapsed >= duration) {
            return 0;
        }
        long fadeDuration = Math.min(duration, Math.max(0L, (long) fadeSeconds) * 20L);
        if (!fade || fadeDuration == 0) {
            return 255;
        }
        long fadeStart = duration - fadeDuration;
        if (elapsed <= fadeStart) {
            return 255;
        }
        return 255 - (int) (255.0 * (elapsed - fadeStart) / fadeDuration);
    }

    private static int boundedCeil(double value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(value));
    }
}
