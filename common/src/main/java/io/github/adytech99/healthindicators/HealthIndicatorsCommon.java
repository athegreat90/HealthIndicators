package io.github.adytech99.healthindicators;

import dev.architectury.event.events.client.ClientGuiEvent;
import io.github.adytech99.healthindicators.config.Config;
import io.github.adytech99.healthindicators.config.ModConfig;
import io.github.adytech99.healthindicators.util.ConfigUtils;
import io.github.adytech99.healthindicators.util.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HealthIndicatorsCommon {
    public static final String MOD_ID = "healthindicators";
    public static Minecraft client = Minecraft.getInstance();
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final KeyMapping.Category HEALTH_INDICATORS_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    private static boolean changed = false;
    private static boolean openConfig = false;
    private static ClientLevel previousLevel;
    private static LocalPlayer previousPlayer;


    public static void init() {
        ModConfig.HANDLER.load();
        ModConfig.HANDLER.instance().validate();
        Config.load();
        ClientGuiEvent.RENDER_HUD.register(HealthIndicatorsCommon::onHudRender);
        client = Minecraft.getInstance();
        LOGGER.info("Never be heartless!");
    }

    public static void tick(){
        client = Minecraft.getInstance();
        if (client == null) return;
        if (client.level != previousLevel || client.player != previousPlayer) {
            // Save pending offset edits before a disconnect, respawn or dimension change.
            savePendingChanges();
            RenderTracker.clear();
            DamageDirectionIndicatorRenderer.clear();
            if (Config.getOverrideAllFiltersEnabled()) disableOverrideFilters();
            openConfig = false;
            previousLevel = client.level;
            previousPlayer = client.player;
        }
        if (client.level == null || client.player == null) {
            savePendingChanges();
            return;
        }
        if(openConfig){
            Screen configScreen = ModConfig.createScreen(client.gui.screen());
            client.gui.setScreen(configScreen);
            openConfig = false;
        }
        if(client.level.getGameTime() % 200 == 0) savePendingChanges();
        RenderTracker.tick(client);
        DamageDirectionIndicatorRenderer.tick();
    }

    private static void savePendingChanges() {
        if (changed) {
            ModConfig.HANDLER.save();
            changed = false;
        }
    }

    public static void onHudRender(GuiGraphicsExtractor drawContext1, DeltaTracker renderTickCounter1) {
        if (client == null || client.level == null || client.player == null) return;
        LivingEntity trackedEntity = RenderTracker.getTrackedEntity();
        if (trackedEntity != null) {
            if (trackedEntity.level() != client.level || trackedEntity.isDeadOrDying() || trackedEntity.isRemoved()) {
                RenderTracker.setTrackedEntity(null);
            } else {
                HudRenderer.onHudRender(drawContext1, renderTickCounter1);
            }
        }
        if(ModConfig.HANDLER.instance().enable_damage_direction_indicators && client.isLocalServer()) DamageDirectionIndicatorRenderer.render(drawContext1, renderTickCounter1.getRealtimeDeltaTicks());
    }

    public static void openConfig(){
        openConfig = client != null && client.level != null && client.player != null;
    }

    public static void enableHeartsRendering(){
        Config.setHeartsRenderingEnabled(!Config.getHeartsRenderingEnabled());
        if (client != null && client.player != null) {
            int formatting;
            if(ModConfig.HANDLER.instance().colored_messages) formatting = Config.getHeartsRenderingEnabled() ? 0x55FF55 : 0xFF5555;
            else formatting = 0xFFFFFF;
            ConfigUtils.sendMessage(client.player, Component.literal((Config.getHeartsRenderingEnabled() ? "Enabled" : "Disabled") + " Health Indicators").withStyle(style -> style.withColor(formatting)));
        }
    }

    public static void enableArmorRendering(){
        Config.setArmorRenderingEnabled(!Config.getArmorRenderingEnabled());
        if (client != null && client.player != null) {
            int formatting;
            if(ModConfig.HANDLER.instance().colored_messages) formatting = Config.getArmorRenderingEnabled() ? 0x55FF55 : 0xFF5555;
            else formatting = 0xFFFFFF;
            ConfigUtils.sendMessage(client.player, Component.literal((Config.getArmorRenderingEnabled() ? "Enabled" : "Disabled") + " Armor Indicators").withStyle(style -> style.withColor(formatting)));
        }
    }

    public static void increaseOffset(){
        ModConfig.HANDLER.instance().display_offset = (ModConfig.HANDLER.instance().display_offset + ModConfig.HANDLER.instance().offset_step_size);
        changed = true;
        if (client != null && client.player != null) {
            ConfigUtils.sendMessage(client.player, Component.literal("Set heart offset to " + Util.truncate(ModConfig.HANDLER.instance().display_offset,2)));
        }
    }
    public static void decreaseOffset(){
        ModConfig.HANDLER.instance().display_offset = (ModConfig.HANDLER.instance().display_offset - ModConfig.HANDLER.instance().offset_step_size);
        changed = true;
        if (client != null && client.player != null) {
            ConfigUtils.sendMessage(client.player, Component.literal("Set heart offset to " + Util.truncate(ModConfig.HANDLER.instance().display_offset,2)));
        }
    }

    public static void overrideFilters(){
        Config.setOverrideAllFiltersEnabled(true);
        if (client != null && client.player != null) {
            ConfigUtils.sendOverlayMessage(client.player, Component.literal( " Config Criteria " + (Config.getOverrideAllFiltersEnabled() ? "Temporarily Overridden" : "Re-implemented")));
        }
    }

    public static void disableOverrideFilters(){
        Config.setOverrideAllFiltersEnabled(false);
        if (client != null) client.gui.hud.setOverlayMessage(Component.literal(""), false);
    }

    public static void openConfigScreen(){
        openConfig();
    }
}
