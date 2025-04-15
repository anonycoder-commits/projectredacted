package net.tasuposed.projectredacted.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tasuposed.projectredacted.ProjectRedacted;

/**
 * Configures entity spawn placements and attributes
 */
@Mod.EventBusSubscriber(modid = ProjectRedacted.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntitySpawnConfig {

    /**
     * Register spawn placements for our custom entities
     */
    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        // Register Iteration entity spawn placement
        event.register(
            EntityRegistry.ITERATION.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Iteration::checkIterationSpawnRules,
            SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        
        // Register Protocol_37 entity spawn placement
        event.register(
            EntityRegistry.PROTOCOL_37.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Protocol_37::checkProtocol37SpawnRules,
            SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        
        // Register InvisibleProtocol37 entity spawn placement
        event.register(
            EntityRegistry.INVISIBLE_PROTOCOL_37.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            InvisibleProtocol37::checkSpawnRules,
            SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        
        // Register DistantStalker entity spawn placement
        event.register(
            EntityRegistry.DISTANT_STALKER.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            DistantStalker::checkSpawnRules,
            SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        
        // Register MiningEntity entity spawn placement
        event.register(
            EntityRegistry.MINING_ENTITY.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            MiningEntity::checkSpawnRules,
            SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        
        // Register AngryProtocol37 entity spawn placement
        event.register(
            EntityRegistry.ANGRY_PROTOCOL_37.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            AngryProtocol37::checkSpawnRules,
            SpawnPlacementRegisterEvent.Operation.REPLACE
        );
    }
    
    /**
     * Register entity attributes for our custom entities
     */
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // Register attributes for Iteration entity
        event.put(EntityRegistry.ITERATION.get(), Iteration.createAttributes().build());
        
        // Register attributes for Protocol_37 entity
        event.put(EntityRegistry.PROTOCOL_37.get(), Protocol_37.createAttributes().build());
        
        // Register attributes for InvisibleProtocol37 entity
        event.put(EntityRegistry.INVISIBLE_PROTOCOL_37.get(), InvisibleProtocol37.createAttributes().build());
        
        // Register attributes for DistantStalker entity
        event.put(EntityRegistry.DISTANT_STALKER.get(), DistantStalker.createAttributes().build());
        
        // Register attributes for MiningEntity entity
        event.put(EntityRegistry.MINING_ENTITY.get(), MiningEntity.createAttributes().build());
        
        // Register attributes for AngryProtocol37 entity
        event.put(EntityRegistry.ANGRY_PROTOCOL_37.get(), AngryProtocol37.createAttributes().build());
    }
} 