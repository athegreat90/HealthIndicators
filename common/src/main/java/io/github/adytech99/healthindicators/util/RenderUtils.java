package io.github.adytech99.healthindicators.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.adytech99.healthindicators.config.ModConfig;
import io.github.adytech99.healthindicators.enums.ArmorTypeEnum;
import io.github.adytech99.healthindicators.enums.HeartTypeEnum;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

public class RenderUtils {
    public static void drawHeart(Matrix4f model, VertexConsumer vertexConsumer, float x, HeartTypeEnum type, LivingEntity livingEntity) {
        drawHeart(model, vertexConsumer, x, type, livingEntity, 1.0F, 0.0);
    }

    public static void drawHeart(Matrix4f model, VertexConsumer vertexConsumer, float x, HeartTypeEnum type, LivingEntity livingEntity, float opacity) {
        drawHeart(model, vertexConsumer, x, type, livingEntity, opacity, 0.0);
    }

    public static void drawHeart(Matrix4f model, VertexConsumer vertexConsumer, float x, HeartTypeEnum type, LivingEntity livingEntity, float opacity, double squaredDistance) {
        drawHeart(model, vertexConsumer, x, type, livingEntity, opacity, squaredDistance, false);
    }
    
    public static void drawHeart(Matrix4f model, VertexConsumer vertexConsumer, float x, HeartTypeEnum type, LivingEntity livingEntity, float opacity, double squaredDistance, boolean isObstructed) {
        drawHeart(model, vertexConsumer, x, type, opacity, squaredDistance, isObstructed);
    }

    public static void drawHeart(Matrix4f model, VertexConsumer vertexConsumer, float x, HeartTypeEnum type,
                                 float opacity, double squaredDistance, boolean isObstructed) {
        float minU = 0F;
        float maxU = 1F;
        float minV = 0F;
        float maxV = 1F;

        float heartSize = 9F;
        
        // Z offset only when non-see through, might change later.
        float z = 0F;
        if (!isObstructed) {
            float baseOffset = 0.01F;
            //float distanceScale = 1.0F + (float)(Math.sqrt(squaredDistance) * 0.001F);
            float distanceScale = 1.0F + (float)(Math.sqrt(squaredDistance) * 1F);
            float scaledOffset = baseOffset * distanceScale;
            z = (type == HeartTypeEnum.EMPTY) ? -scaledOffset : scaledOffset;
        }

        vertexConsumer.addVertex(model, x, 0F - heartSize, z).setUv(minU, maxV).setLight(15728880).setColor(1.0F, 1.0F, 1.0F, opacity);
        vertexConsumer.addVertex(model, x - heartSize, 0F - heartSize, z).setUv(maxU, maxV).setLight(15728880).setColor(1.0F, 1.0F, 1.0F, opacity);
        vertexConsumer.addVertex(model, x - heartSize, 0F, z).setUv(maxU, minV).setLight(15728880).setColor(1.0F, 1.0F, 1.0F, opacity);
        vertexConsumer.addVertex(model, x, 0F, z).setUv(minU, minV).setLight(15728880).setColor(1.0F, 1.0F, 1.0F, opacity);
    }

    public static void drawArmor(Matrix4f model, VertexConsumer vertexConsumer, float x, ArmorTypeEnum type) {
        drawArmor(model, vertexConsumer, x, type, 1.0F, 0.0);
    }

    public static void drawArmor(Matrix4f model, VertexConsumer vertexConsumer, float x, ArmorTypeEnum type, float opacity) {
        drawArmor(model, vertexConsumer, x, type, opacity, 0.0);
    }

    public static void drawArmor(Matrix4f model, VertexConsumer vertexConsumer, float x, ArmorTypeEnum type, float opacity, double squaredDistance) {
        drawArmor(model, vertexConsumer, x, type, opacity, squaredDistance, false);
    }
    
    public static void drawArmor(Matrix4f model, VertexConsumer vertexConsumer, float x, ArmorTypeEnum type, float opacity, double squaredDistance, boolean isObstructed) {
        float minU = 0F;
        float maxU = 1F;
        float minV = 0F;
        float maxV = 1F;

        float armorSize = 9F;
        
        float z = 0F;
        if (!isObstructed) {
            float baseOffset = 0.01F;
            float distanceScale = 1.0F + (float)(Math.sqrt(squaredDistance) * 0.001F);
            float scaledOffset = baseOffset * distanceScale;
            z = (type == ArmorTypeEnum.EMPTY) ? -scaledOffset : scaledOffset;
        }

        vertexConsumer.addVertex(model, x, 0F - armorSize, z).setUv(minU, maxV).setLight(15728880).setColor(1.0F, 1.0F, 1.0F, opacity);
        vertexConsumer.addVertex(model, x - armorSize, 0F - armorSize, z).setUv(maxU, maxV).setLight(15728880).setColor(1.0F, 1.0F, 1.0F, opacity);
        vertexConsumer.addVertex(model, x - armorSize, 0F, z).setUv(maxU, minV).setLight(15728880).setColor(1.0F, 1.0F, 1.0F, opacity);
        vertexConsumer.addVertex(model, x, 0F, z).setUv(minU, minV).setLight(15728880).setColor(1.0F, 1.0F, 1.0F, opacity);
    }

    public static String getHealthText(LivingEntity livingEntity) {
        return IndicatorMath.formatHealth(livingEntity.getHealth(), livingEntity.getAbsorptionAmount(),
                livingEntity.getMaxHealth(), ModConfig.HANDLER.instance().decimal_places,
                ModConfig.HANDLER.instance().percentage_based_health);
    }
}
