package io.github.adytech99.healthindicators.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class IndicatorMathTest {
    @Test
    void roundsHealthIntoFullAndHalfHeartsWithoutOverflow() {
        assertEquals(0, IndicatorMath.healthPoints(0));
        assertEquals(1, IndicatorMath.healthPoints(0.1F));
        assertEquals(3, IndicatorMath.healthPoints(2.1F));
        assertEquals(0, IndicatorMath.heartCount(0));
        assertEquals(1, IndicatorMath.heartCount(1));
        assertEquals(1, IndicatorMath.heartCount(2));
        assertEquals(2, IndicatorMath.heartCount(3));
        assertEquals(10, IndicatorMath.heartCount(20));
        assertEquals(Integer.MAX_VALUE, IndicatorMath.healthPoints(Float.MAX_VALUE));
        assertEquals(1_073_741_824, IndicatorMath.heartCount(Integer.MAX_VALUE));
    }

    @Test
    void sanitizesInvalidHealthWithoutDiscardingFinitePositiveValues() {
        for (float invalid : new float[]{-1, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertEquals(0.0F, IndicatorMath.finiteNonNegative(invalid));
            assertEquals(0, IndicatorMath.healthPoints(invalid));
        }
        assertEquals(0, IndicatorMath.heartCount(-1));
        assertEquals(Float.MAX_VALUE, IndicatorMath.finiteNonNegative(Float.MAX_VALUE));
        assertEquals(1.5F, IndicatorMath.finiteNonNegative(1.5F));
    }

    @Test
    void includesAbsorptionInPercentagesAndDoesNotCapValidOverheal() {
        assertEquals(75.0, IndicatorMath.percentage(10, 5, 20));
        assertEquals(150.0, IndicatorMath.percentage(20, 10, 20));
        assertEquals(200.0, IndicatorMath.percentage(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE));
        assertTrue(Double.isFinite(IndicatorMath.percentage(Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE)));
        for (float invalidMaximum : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertEquals(0.0, IndicatorMath.percentage(10, 5, invalidMaximum));
        }
        assertEquals(25.0, IndicatorMath.percentage(Float.NaN, 5, 20));
    }

    @Test
    void percentageHeartsRoundHealthAndMaximumBeforeScaling() {
        assertEquals(11, IndicatorMath.scaledHealth(10.1F, 20, 20));
        assertEquals(11, IndicatorMath.scaledHealth(10.1F, 20.1F, 20));
        assertEquals(10, IndicatorMath.scaledHealth(10.1F, 21.1F, 20));
        assertEquals(7, IndicatorMath.scaledHealth(1, 3, 20));
        assertEquals(30, IndicatorMath.scaledHealth(30, 20, 20));
        assertEquals(0, IndicatorMath.scaledHealth(10, 0, 20));
        assertEquals(0, IndicatorMath.scaledHealth(10, Float.NaN, 20));
        assertEquals(0, IndicatorMath.scaledHealth(10, 20, -1));
        assertEquals(Integer.MAX_VALUE, IndicatorMath.scaledHealth(Float.MAX_VALUE, 1, Integer.MAX_VALUE));
    }

    @Test
    void keepsNormalFormattingAndBoundsEditedConfigurationValues() {
        assertEquals(String.format("%.1f / %.1f", 12.5, 20.0), IndicatorMath.formatHealth(10, 2.5F, 20, 1, false));
        assertEquals(String.format("%.0f %%", 75.0), IndicatorMath.formatHealth(10, 5, 20, 0, true));
        assertEquals(String.format("%.1f / %.1f", 0.0, 0.0), IndicatorMath.formatHealth(Float.NaN, -2, Float.POSITIVE_INFINITY, 1, false));
        assertEquals(String.format("%.1f %%", 0.0), IndicatorMath.formatHealth(10, 5, 0, 1, true));
        assertFalse(IndicatorMath.formatHealth(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, 1, false).contains("Infinity"));
        assertEquals(0, IndicatorMath.decimalPlaces(-1));
        assertEquals(12, IndicatorMath.decimalPlaces(12));
        assertEquals(12, IndicatorMath.decimalPlaces(Integer.MAX_VALUE));
        assertEquals(10, IndicatorMath.iconsPerRow(0));
        assertEquals(10, IndicatorMath.iconsPerRow(-20));
        assertEquals(40, IndicatorMath.iconsPerRow(40));
    }

    @Test
    void followsTheUsersDefaultNumberLocale() {
        Locale previous = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
            assertEquals("12,5 / 20,0", IndicatorMath.formatHealth(10, 2.5F, 20, 1, false));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous);
        }
    }

    @Test
    void fadesAtTheConfiguredBoundaryAndExpires() {
        assertEquals(255, IndicatorMath.damageAlpha(0, 4, 3, true));
        assertEquals(255, IndicatorMath.damageAlpha(20, 4, 3, true));
        assertEquals(128, IndicatorMath.damageAlpha(50, 4, 3, true));
        assertEquals(5, IndicatorMath.damageAlpha(79, 4, 3, true));
        assertEquals(0, IndicatorMath.damageAlpha(80, 4, 3, true));
        assertEquals(0, IndicatorMath.damageAlpha(81, 4, 3, true));
    }

    @Test
    void handlesDisabledAndInvalidFadeDurationsWithoutDivisionByZero() {
        assertEquals(255, IndicatorMath.damageAlpha(79, 4, 0, true));
        assertEquals(0, IndicatorMath.damageAlpha(80, 4, 0, true));
        assertEquals(255, IndicatorMath.damageAlpha(79, 4, 3, false));
        assertEquals(128, IndicatorMath.damageAlpha(40, 4, 9, true));
        assertEquals(0, IndicatorMath.damageAlpha(0, 0, 0, true));
        assertEquals(0, IndicatorMath.damageAlpha(0, -1, 3, true));
        assertEquals(255, IndicatorMath.damageAlpha(-1, 4, -2, true));
        assertEquals(255, IndicatorMath.damageAlpha(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, true));
    }
}
