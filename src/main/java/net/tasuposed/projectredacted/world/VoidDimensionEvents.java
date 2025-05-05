package net.tasuposed.projectredacted.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tasuposed.projectredacted.ProjectRedacted;
import net.tasuposed.projectredacted.horror.events.EndgameSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles special events for The Void dimension
 */
@Mod.EventBusSubscriber(modid = ProjectRedacted.MODID)
public class VoidDimensionEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoidDimensionEvents.class);
    
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
    
    /**
     * Force players to respawn in The Void if the world has been erased
     * This prevents them from escaping through death
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // Only run on server
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        // Check if world has been erased
        if (EndgameSequence.getInstance().isWorldErased()) {
            // Get the void dimension
            ServerLevel voidLevel = player.server.getLevel(DimensionRegistry.THE_VOID);
            if (voidLevel != null) {
                LOGGER.info("Player {} attempted to respawn after world erasure, forcing respawn in The Void", 
                        player.getName().getString());
                
                // If not already in The Void, schedule teleportation after respawn
                if (player.level().dimension() != DimensionRegistry.THE_VOID) {
                    player.server.tell(new net.minecraft.server.TickTask(
                            player.server.getTickCount() + 5, () -> {
                        // Find safe position in void
                        TheVoidPortalHandler.teleportPlayerToVoid(player, player.blockPosition());
                    }));
                }
            }
        }
    }
} 