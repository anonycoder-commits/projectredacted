package net.tasuposed.projectredacted.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.tasuposed.projectredacted.ProjectRedacted;

/**
 * Configuration settings for the horror system
 */
@Mod.EventBusSubscriber(modid = ProjectRedacted.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HorrorConfig {
    public static final ForgeConfigSpec SPEC;
    
    public static final BooleanValue HORROR_ENABLED;
    public static final BooleanValue RESET_ON_LOGIN;
    public static final IntValue TIME_BETWEEN_STAGES;
    public static final IntValue STAGE_DURATION;
    public static final DoubleValue EVENT_FREQUENCY;
    public static final BooleanValue SYNC_MULTIPLAYER_EVENTS;
    
    // New config values for community-inspired entities
    public static final IntValue INVISIBLE_PROTOCOL_37_SPAWN_CHANCE;
    public static final IntValue DISTANT_STALKER_SPAWN_CHANCE;
    public static final IntValue MINING_ENTITY_SPAWN_CHANCE;
    public static final IntValue PROTOCOL_37_TRANSFORM_CHANCE;
    public static final BooleanValue ENABLE_COMMUNITY_ENTITIES;
    
    // Add new multiplayer scaling configuration
    public static final BooleanValue DYNAMIC_MULTIPLAYER_SCALING;
    public static final DoubleValue MULTIPLAYER_SCALING_FACTOR;
    public static final IntValue GLOBAL_EVENT_COOLDOWN;
    public static final IntValue MINIMUM_EVENTS_PER_STAGE;
    
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        
        builder.comment("Horror System Settings")
               .push("horror");
        
        HORROR_ENABLED = builder
                .comment("Enable horror effects")
                .define("horrorEnabled", true);
        
        RESET_ON_LOGIN = builder
                .comment("Reset horror progression on player login")
                .define("resetOnLogin", false);
        
        TIME_BETWEEN_STAGES = builder
                .comment("Time in minutes between horror stages. Lower values = faster progression.")
                .defineInRange("timeBetweenStages", 5, 1, 1440);
                
        STAGE_DURATION = builder
                .comment("Duration of each stage in minutes before progressing to next stage")
                .defineInRange("stageDuration", 15, 1, 1440);
        
        EVENT_FREQUENCY = builder
                .comment("Base frequency of horror events (0.0-1.0). Higher values = more frequent events.")
                .defineInRange("eventFrequency", 0.25D, 0.0D, 1.0D);
        
        SYNC_MULTIPLAYER_EVENTS = builder
                .comment("Synchronize horror events across all players in multiplayer to ensure a shared experience. IMPORTANT: Required for Protocol_37 and Iteration entities to spawn visibly in multiplayer.")
                .define("syncMultiplayerEvents", true);
        
        // Add new multiplayer scaling configuration
        DYNAMIC_MULTIPLAYER_SCALING = builder
                .comment("Dynamically scale event frequencies based on player count in multiplayer. When enabled, the frequency of per-player events will be reduced as more players join to prevent event overload.")
                .define("dynamicMultiplayerScaling", true);
        
        MULTIPLAYER_SCALING_FACTOR = builder
                .comment("Controls how aggressively the event frequency scales down with more players. Lower values = more aggressive scaling (0.5-1.0)")
                .defineInRange("multiplayerScalingFactor", 0.7D, 0.5D, 1.0D);
        
        GLOBAL_EVENT_COOLDOWN = builder
                .comment("Minimum time in seconds between horror events for a single player. Prevents event spam.")
                .defineInRange("globalEventCooldown", 60, 0, 600);
        
        MINIMUM_EVENTS_PER_STAGE = builder
                .comment("Minimum number of horror events a player should experience during each stage before progression. Set to 0 to disable.")
                .defineInRange("minimumEventsPerStage", 3, 0, 30);
        
        // Configuration for community-inspired entities
        ENABLE_COMMUNITY_ENTITIES = builder
                .comment("Enable community-inspired entities. These include Protocol 37's invisible form, the distant stalker, mining entity, and angry Protocol 37.")
                .define("enableCommunityEntities", true);
                
        INVISIBLE_PROTOCOL_37_SPAWN_CHANCE = builder
                .comment("Chance (1 in X) for Protocol 37's invisible form to spawn")
                .defineInRange("invisibleProtocol37SpawnChance", 1200, 1, 10000);
                
        DISTANT_STALKER_SPAWN_CHANCE = builder
                .comment("Chance (1 in X) for the distant stalker entity to spawn")
                .defineInRange("distantStalkerSpawnChance", 1800, 1, 10000);
                
        MINING_ENTITY_SPAWN_CHANCE = builder
                .comment("Chance (1 in X) for the mining entity to spawn in caves")
                .defineInRange("miningEntitySpawnChance", 800, 1, 10000);
                
        PROTOCOL_37_TRANSFORM_CHANCE = builder
                .comment("Chance (1 in X) for Protocol 37 to transform into its angry form")
                .defineInRange("protocol37TransformChance", 8, 1, 100);
        
        builder.pop();
        
        SPEC = builder.build();
    }
    
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        // Only mark as loaded if this is the right config file
        if (event.getConfig().getSpec() == SPEC) {
            // Mark config as loaded
            net.tasuposed.projectredacted.horror.HorrorManager.markConfigLoaded();
        }
    }
} 