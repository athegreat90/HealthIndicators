package io.github.adytech99.healthindicators.config;

import dev.architectury.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Config {
    public static final String CONFIG_FILE = "healthindicators.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
    private static final ToggleConfigStore STORE = new ToggleConfigStore(Platform.getConfigFolder().resolve(CONFIG_FILE));

    public static boolean getRenderingEnabled(){
        return STORE.getHeartsRenderingEnabled() || STORE.getArmorRenderingEnabled();
    }

    public static boolean getHeartsRenderingEnabled() {
        return STORE.getHeartsRenderingEnabled();
    }

    public static void setHeartsRenderingEnabled(boolean renderingEnabled) {
        if (STORE.getHeartsRenderingEnabled() != renderingEnabled) {
            STORE.setHeartsRenderingEnabled(renderingEnabled);
            save();
        }
    }

    public static boolean getArmorRenderingEnabled() {
        return STORE.getArmorRenderingEnabled();
    }
    public static void setArmorRenderingEnabled(boolean armorRenderingEnabled) {
        if (STORE.getArmorRenderingEnabled() != armorRenderingEnabled) {
            STORE.setArmorRenderingEnabled(armorRenderingEnabled);
            save();
        }
    }

    public static boolean getOverrideAllFiltersEnabled() {
        return STORE.getOverrideAllFiltersEnabled();
    }

    public static void setOverrideAllFiltersEnabled(boolean overrideAllFiltersEnabled) {
        STORE.setOverrideAllFiltersEnabled(overrideAllFiltersEnabled);
    }


    public static void load() {
        try {
            STORE.load();
        } catch (IOException e) {
            LOGGER.error("Could not load {}; the existing file has been preserved", CONFIG_FILE, e);
        }
    }

    public static void save() {
        try {
            STORE.save();
        } catch (IOException e) {
            LOGGER.error("Could not save {}; the existing file has been preserved", CONFIG_FILE, e);
        }
    }
}
