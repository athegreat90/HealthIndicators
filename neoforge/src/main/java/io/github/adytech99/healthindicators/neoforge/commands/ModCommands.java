package io.github.adytech99.healthindicators.neoforge.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.adytech99.healthindicators.HealthIndicatorsCommon;
import io.github.adytech99.healthindicators.RenderTracker;
import io.github.adytech99.healthindicators.enums.HealthDisplayTypeEnum;
import io.github.adytech99.healthindicators.config.ModConfig;
import io.github.adytech99.healthindicators.util.ConfigUtils;
import io.github.adytech99.healthindicators.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;


public class ModCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        registerConfigCommands(event.getDispatcher());
        registerOpenConfigCommand(event.getDispatcher());
    }
    public static void registerConfigCommands(@UnknownNullability CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("healthindicators")
                .then(Commands.literal("offset")
                        .then(Commands.argument("offset", DoubleArgumentType.doubleArg())
                                .executes(context -> {
                                    ModConfig.HANDLER.instance().display_offset = DoubleArgumentType.getDouble(context, "offset");
                                    ModConfig.HANDLER.save();
                                    ConfigUtils.sendMessage(Minecraft.getInstance().player, Component.literal("Set heart offset to " + Util.truncate(ModConfig.HANDLER.instance().display_offset, 2)));
                                    return 1;
                                })))

                .then(Commands.literal("indicator-type")
                        .then(Commands.argument("indicator_type", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    builder.suggest("heart");
                                    builder.suggest("number");
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    HealthDisplayTypeEnum displayTypeEnum;
                                    String type = StringArgumentType.getString(context, "indicator_type");
                                    if ("heart".equals(type)) {
                                        displayTypeEnum = HealthDisplayTypeEnum.HEARTS;
                                    } else if ("number".equals(type)) {
                                        displayTypeEnum = HealthDisplayTypeEnum.NUMBER;
                                    } else {
                                        ConfigUtils.sendMessage(Minecraft.getInstance().player, Component.literal("Unknown argument, please try again."));
                                        return 1;
                                    }
                                    ModConfig.HANDLER.instance().indicator_type = displayTypeEnum;
                                    ModConfig.HANDLER.save();
                                    ConfigUtils.sendMessage(Minecraft.getInstance().player, Component.literal("Set display type to " + ModConfig.HANDLER.instance().indicator_type));
                                    return 1;
                                })))

                .then(Commands.literal("monitor")
                        .then(Commands.argument("entity_name", StringArgumentType.string())
                                .suggests((context, builder) -> {
                            if (Minecraft.getInstance().level == null) return builder.buildFuture();
                            for(Entity entity : Minecraft.getInstance().level.entitiesForRendering()){
                                if (!(entity instanceof LivingEntity)) continue;
                                if(entity.hasCustomName()) builder.suggest(Objects.requireNonNull(entity.getCustomName()).getString());
                                if(entity instanceof Player) builder.suggest(Objects.requireNonNull(entity.getDisplayName()).getString());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            Minecraft client = Minecraft.getInstance();
                            if (client.level == null || client.player == null) return 0;
                            Entity entity = Util.getEntityFromName(client.level, StringArgumentType.getString(context, "entity_name"));
                            if(entity instanceof LivingEntity livingEntity) {
                                ConfigUtils.sendMessage(Minecraft.getInstance().player, Component.literal("Now monitoring " + StringArgumentType.getString(context, "entity_name")));
                                RenderTracker.setTrackedEntity(livingEntity);
                                return 1;
                            }
                            else ConfigUtils.sendMessage(Minecraft.getInstance().player, Component.literal("There is no entity named " + StringArgumentType.getString(context, "entity_name") + " in the world. It may have died or gone out of render distance."));
                            return 0;
                        })))

                .then(Commands.literal("stop-monitoring")
                        .executes(context -> {
                            RenderTracker.setTrackedEntity(null);
                            ConfigUtils.sendMessage(Minecraft.getInstance().player, Component.literal("Stopped monitoring "));
                            return 0;
                        }))
        );
    }

    public static void registerOpenConfigCommand(@UnknownNullability CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("healthindicators")
                .executes(context -> {
                    HealthIndicatorsCommon.openConfig();
                    return 1;
                })
        );
    }
}
