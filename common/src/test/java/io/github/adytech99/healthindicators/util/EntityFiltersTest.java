package io.github.adytech99.healthindicators.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityFiltersTest {
    @Test
    void emptyListsPreserveUnrestrictedBehavior() {
        assertTrue(EntityFilters.matches(true, List.of(), "minecraft:cow", false, "Cow"));
        assertTrue(EntityFilters.matches(false, List.of(), "minecraft:cow", false, "Cow"));
    }

    @Test
    void blacklistMatchesAllPlayersUsingRegistryId() {
        assertFalse(EntityFilters.matches(true, List.of("minecraft:player"), "minecraft:player", true, "Alex"));
        assertTrue(EntityFilters.matches(true, List.of("minecraft:player"), "minecraft:cow", false, "Cow"));
    }

    @Test
    void whitelistMatchesAllPlayersUsingRegistryId() {
        assertTrue(EntityFilters.matches(false, List.of("minecraft:player"), "minecraft:player", true, "Alex"));
        assertFalse(EntityFilters.matches(false, List.of("minecraft:player"), "minecraft:cow", false, "Cow"));
    }

    @Test
    void playerNamesUseExactMatchingInBothModes() {
        assertTrue(EntityFilters.matches(false, List.of("Alex"), "minecraft:player", true, "Alex"));
        assertFalse(EntityFilters.matches(true, List.of("Alex"), "minecraft:player", true, "Alex"));
        assertFalse(EntityFilters.matches(false, List.of("alex"), "minecraft:player", true, "Alex"));
    }

    @Test
    void mobDisplayNamesDoNotMatchPlayerNameEntries() {
        assertFalse(EntityFilters.matches(false, List.of("Alex"), "minecraft:cow", false, "Alex"));
        assertTrue(EntityFilters.matches(true, List.of("Alex"), "minecraft:cow", false, "Alex"));
    }

    @Test
    void entityRegistryIdsMatchInBothModes() {
        assertTrue(EntityFilters.matches(false, List.of("minecraft:cow"), "minecraft:cow", false, "Cow"));
        assertFalse(EntityFilters.matches(true, List.of("minecraft:cow"), "minecraft:cow", false, "Cow"));
    }

    @Test
    void nullEntriesDoNotCrashMatching() {
        assertTrue(EntityFilters.matches(false, Arrays.asList(null, "Alex"), "minecraft:player", true, "Alex"));
        assertTrue(EntityFilters.matches(true, Arrays.asList((String) null), "minecraft:player", true, "Alex"));
        assertFalse(EntityFilters.matches(false, Arrays.asList((String) null), "minecraft:player", true, "Alex"));
    }
}
