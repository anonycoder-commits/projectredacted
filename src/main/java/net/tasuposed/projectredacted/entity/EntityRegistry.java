package net.tasuposed.projectredacted.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tasuposed.projectredacted.ProjectRedacted;

/**
 * Handles registration of custom entities
 */
public class EntityRegistry {
    // Create a Deferred Register for entities
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(
            ForgeRegistries.ENTITY_TYPES, ProjectRedacted.MODID);
    
    // Register the Iteration entity
    public static final RegistryObject<EntityType<Iteration>> ITERATION = ENTITIES.register("iteration",
            () -> EntityType.Builder.of(Iteration::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F) // Same size as a player
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(new ResourceLocation(ProjectRedacted.MODID, "iteration").toString()));
    
    // Register the Protocol_37 entity
    public static final RegistryObject<EntityType<Protocol_37>> PROTOCOL_37 = ENTITIES.register("protocol_37",
            () -> EntityType.Builder.of(Protocol_37::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F) // Same size as a player
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(new ResourceLocation(ProjectRedacted.MODID, "protocol_37").toString()));
    
    // Register the InvisibleProtocol37 entity - Protocol 37's invisible form
    public static final RegistryObject<EntityType<InvisibleProtocol37>> INVISIBLE_PROTOCOL_37 = ENTITIES.register("invisible_protocol_37",
            () -> EntityType.Builder.of(InvisibleProtocol37::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F) // Same size as a player
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(new ResourceLocation(ProjectRedacted.MODID, "invisible_protocol_37").toString()));
    
    // Register the DistantStalker entity - The guy that stalks #2
    public static final RegistryObject<EntityType<DistantStalker>> DISTANT_STALKER = ENTITIES.register("distant_stalker",
            () -> EntityType.Builder.of(DistantStalker::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F) // Same size as a player
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(new ResourceLocation(ProjectRedacted.MODID, "distant_stalker").toString()));
    
    // Register the MiningEntity entity - Mining form in caves
    public static final RegistryObject<EntityType<MiningEntity>> MINING_ENTITY = ENTITIES.register("mining_entity",
            () -> EntityType.Builder.of(MiningEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F) // Same size as a player
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(new ResourceLocation(ProjectRedacted.MODID, "mining_entity").toString()));
    
    // Register the AngryProtocol37 entity - Protocol 37's angry form
    public static final RegistryObject<EntityType<AngryProtocol37>> ANGRY_PROTOCOL_37 = ENTITIES.register("angry_protocol_37",
            () -> EntityType.Builder.of(AngryProtocol37::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F) // Same size as a player
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(new ResourceLocation(ProjectRedacted.MODID, "angry_protocol_37").toString()));
    
    /**
     * Register this registry with the mod event bus
     */
    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
} 