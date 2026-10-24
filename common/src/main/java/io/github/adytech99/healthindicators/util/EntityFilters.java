package io.github.adytech99.healthindicators.util;

import java.util.Collection;

/** Entity-list matching shared by whitelist and blacklist modes. */
public final class EntityFilters {
    private EntityFilters() {}

    public static boolean matches(boolean blacklist, Collection<String> entries,
                                  String entityType, boolean player, String playerName) {
        // An empty list has historically left all entity types enabled in either mode.
        if (entries == null || entries.isEmpty()) return true;
        boolean listed = false;
        for (String entry : entries) {
            if (entry == null) continue;
            if (entry.equals(entityType) || (player && entry.equals(playerName))) {
                listed = true;
                break;
            }
        }
        return blacklist ? !listed : listed;
    }
}
