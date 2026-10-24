package io.github.adytech99.healthindicators;

import io.github.adytech99.healthindicators.config.Config;
import io.github.adytech99.healthindicators.config.ModConfig;
import io.github.adytech99.healthindicators.util.EntityFilters;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RenderTracker {
    private static final Minecraft client = Minecraft.getInstance();
    private static final ConcurrentHashMap<UUID, Integer> UUIDS = new ConcurrentHashMap<>();
    public static boolean after_attack = ModConfig.HANDLER.instance().after_attack;

    private static LivingEntity trackedEntity;

    public static LivingEntity getTrackedEntity() {
        return trackedEntity;
    }

    public static void setTrackedEntity(LivingEntity trackedEntity) {
        RenderTracker.trackedEntity = trackedEntity;
    }

    public static void clear() {
        UUIDS.clear();
        trackedEntity = null;
        after_attack = ModConfig.HANDLER.instance().after_attack;
    }

    public static void tick(Minecraft client){
        if(client.player == null || client.level == null) {
            clear();
            return;
        }
        if(Config.getRenderingEnabled()) {
            Set<UUID> validEntities = new HashSet<>();
            for (Entity entity : client.level.entitiesForRendering()) {
                if (!isInvalid(entity)) validEntities.add(entity.getUUID());
                if (entity instanceof LivingEntity livingEntity && satisfiesAdvancedCriteria(client.player, livingEntity) && satisfiesList(client.player, livingEntity)) {
                    addToUUIDS(livingEntity);
                } else removeFromUUIDS(entity.getUUID());
            }
            trimEntities(validEntities);
        } else {
            UUIDS.clear();
        }
        if(ModConfig.HANDLER.instance().after_attack != after_attack) {
            UUIDS.clear();
            after_attack = ModConfig.HANDLER.instance().after_attack;
        }
        if(trackedEntity != null && (trackedEntity.level() != client.level || trackedEntity.isDeadOrDying() || trackedEntity.isRemoved())) setTrackedEntity(null);
    }

    public static void onDamage(DamageSource damageSource, LivingEntity livingEntity) {
        if (client.level == null || client.player == null || livingEntity == null
                || livingEntity.level() != client.level || damageSource == null) return;
        if(damageSource.getEntity() instanceof Player){
            if(ModConfig.HANDLER.instance().after_attack && RenderTracker.isEntityTypeAllowed(livingEntity, client.player) && satisfiesList(client.player, livingEntity)) {
                //setTrackedEntity(livingEntity);
                if (!addToUUIDS(livingEntity)) {
                    UUIDS.replace(livingEntity.getUUID(), (ModConfig.HANDLER.instance().time_after_hit * 20));
                }
            }
        }
    }


    public static void trimEntities(ClientLevel world) {
        if (world == null || world != client.level || !Config.getRenderingEnabled()) {
            UUIDS.clear();
            return;
        }
        Set<UUID> validEntities = new HashSet<>();
        for (Entity entity : world.entitiesForRendering()) {
            if (!isInvalid(entity)) validEntities.add(entity.getUUID());
        }
        trimEntities(validEntities);
    }

    private static void trimEntities(Set<UUID> validEntities) {
        Iterator<Map.Entry<UUID, Integer>> iterator = UUIDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            entry.setValue(entry.getValue() - 1);
            if (entry.getValue() <= 0 || !validEntities.contains(entry.getKey())) {
                iterator.remove(); // Safe removal during iteration
            }
        }
    }


    public static void removeFromUUIDS(Entity entity){
        UUIDS.remove(entity.getUUID());
    }
    public static void removeFromUUIDS(UUID uuid){
        UUIDS.remove(uuid);
    }

    public static boolean addToUUIDS(LivingEntity livingEntity){
        if(!UUIDS.containsKey(livingEntity.getUUID())){
            UUIDS.put(livingEntity.getUUID(), ModConfig.HANDLER.instance().after_attack ? (ModConfig.HANDLER.instance().time_after_hit * 20) : 2400);
            return true;
        }
        else return false;
    }

    public static boolean isInUUIDS(LivingEntity livingEntity){
        return UUIDS.containsKey(livingEntity.getUUID());
    }

    public static boolean overridePlayers(LocalPlayer playerEntity, LivingEntity livingEntity){
        return (ModConfig.HANDLER.instance().override_players && livingEntity instanceof Player && livingEntity != client.player)
                || (livingEntity == client.player && ModConfig.HANDLER.instance().self);
    }

    public static boolean isEntityTypeAllowed(LivingEntity livingEntity, Player self){
        if(!ModConfig.HANDLER.instance().passive_mobs && livingEntity instanceof AgeableMob) return false;
        if(!ModConfig.HANDLER.instance().hostile_mobs && livingEntity instanceof Monster) return false;
        if(!ModConfig.HANDLER.instance().players && livingEntity instanceof Player) return false;
        if(!ModConfig.HANDLER.instance().self && livingEntity == self) return false;
        return true;
    }

    public static boolean satisfiesAdvancedCriteria(LocalPlayer player, LivingEntity livingEntity){
        if(overridePlayers(player, livingEntity)) return true;

        if(!isEntityTypeAllowed(livingEntity, player)) return false; //Entity Types
        if(ModConfig.HANDLER.instance().after_attack && !UUIDS.containsKey(livingEntity.getUUID())) return false; //Damaged by Player, key should have been added by separate means. Necessary because removal check is done by this method.
        if(ModConfig.HANDLER.instance().damaged_only && (livingEntity.getHealth() == livingEntity.getMaxHealth() || livingEntity.getHealth() > livingEntity.getMaxHealth()*((float) ModConfig.HANDLER.instance().max_health_percentage / 100)) && livingEntity.getAbsorptionAmount() <= 0) return false; //Damaged by Any Reason
        if(ModConfig.HANDLER.instance().looking_at && !isTargeted(livingEntity)) return false;
        if(ModConfig.HANDLER.instance().within_distance && livingEntity.distanceTo(player) > ModConfig.HANDLER.instance().distance) return false;

        return !isInvalid(livingEntity);
    }

    public static boolean satisfiesList(LocalPlayer player, LivingEntity livingEntity){
        if (livingEntity == null) return false;
        return EntityFilters.matches(ModConfig.HANDLER.instance().blacklistOrWhitelist,
                ModConfig.HANDLER.instance().list,
                EntityType.getKey(livingEntity.getType()).toString(),
                livingEntity instanceof Player, livingEntity.getName().getString());
    }

    public static boolean isTargeted(LivingEntity livingEntity){
        Entity camera = client.getCameraEntity();
        if (camera == null || livingEntity == null || client.player == null || client.level == null
                || livingEntity.level() != client.level) return false;
        double d = ModConfig.HANDLER.instance().reach;
        if (d <= 0) return false;
        double e = Mth.square(d);
        Vec3 vec3d = camera.getEyePosition(0);
        HitResult hitResult = camera.pick(d, 0, false);
        double f = hitResult.getLocation().distanceToSqr(vec3d);
        if (hitResult.getType() != HitResult.Type.MISS) {
            e = f;
            d = Math.sqrt(e);
        }
        Vec3 vec3d2 = camera.getViewVector(0);
        Vec3 vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
        AABB box = camera.getBoundingBox().expandTowards(vec3d2.scale(d)).inflate(1.0, 1.0, 1.0);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(camera, vec3d, vec3d3, box, entity -> !entity.isSpectator() && entity.isPickable(), e);

        if (entityHitResult != null && entityHitResult.getEntity() instanceof LivingEntity livingEntity1){
            return livingEntity1 == livingEntity;
        }
        return false;
    }

    public static boolean isOkayToRenderThroughWalls(LivingEntity livingEntity){
        return isTargeted(livingEntity) && !livingEntity.isAlwaysTicking();
    }

    public static boolean isInvalid(Entity entity){
        return (entity == null
                || client.level == null
                || entity.level() != client.level
                || !entity.isAlive()
                || !entity.showVehicleHealth()
                || entity.touchingUnloadedChunk()
                || !(entity instanceof LivingEntity)
                || client.player == null
                || client.player.getVehicle() == entity
                || entity.isInvisibleTo(client.player));
    }
}

