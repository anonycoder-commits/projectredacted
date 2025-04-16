package net.tasuposed.projectredacted.world;

import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tasuposed.projectredacted.ProjectRedacted;

/**
 * Handles special events for The Void dimension
 */
@Mod.EventBusSubscriber(modid = ProjectRedacted.MODID)
public class VoidDimensionEvents {
    
    /**
     * Prevent regular mob spawning in The Void dimension
     */
    @SubscribeEvent
    public static void onCheckMobSpawn(MobSpawnEvent.SpawnPlacementCheck event) {
        // Get the dimension key from the level
        if (event.getLevel() instanceof Level level && 
            level.dimension().equals(DimensionRegistry.THE_VOID)) {
            // Only allow mobs that are specifically spawned by commands or the mod
            if (event.getSpawnType() != MobSpawnType.COMMAND && 
                event.getSpawnType() != MobSpawnType.EVENT &&
                event.getSpawnType() != MobSpawnType.MOB_SUMMONED) {
                event.setResult(Result.DENY);
            }
        }
    }
    
    /**
     * Backup method to prevent any mobs that somehow bypass the first check
     */
    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        // Get the dimension key from the level
        if (event.getLevel() instanceof Level level && 
            level.dimension().equals(DimensionRegistry.THE_VOID)) {
            // Only allow mobs that are specifically spawned by commands or the mod
            if (event.getSpawnType() != MobSpawnType.COMMAND && 
                event.getSpawnType() != MobSpawnType.EVENT &&
                event.getSpawnType() != MobSpawnType.MOB_SUMMONED) {
                event.setResult(Result.DENY);
            }
        }
    }
} 