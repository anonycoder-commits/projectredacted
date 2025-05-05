package net.tasuposed.projectredacted.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tasuposed.projectredacted.ProjectRedacted;

/**
 * Registry for custom sound events
 */
public class SoundRegistry {
    // Create a deferred register for sound events
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = 
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ProjectRedacted.MODID);
    
    // Register each sound from our sounds.json
    public static final RegistryObject<SoundEvent> HEARTBEAT = registerSoundEvent("entity.heartbeat");
    public static final RegistryObject<SoundEvent> WHISPER = registerSoundEvent("entity.whisper");
    public static final RegistryObject<SoundEvent> GROWL = registerSoundEvent("entity.growl");
    
    // Enhanced cinematic sounds for better YouTube presentation
    public static final RegistryObject<SoundEvent> ITERATION_AMBIENT = registerSoundEvent("entity.iteration.ambient");
    public static final RegistryObject<SoundEvent> ITERATION_TELEPORT = registerSoundEvent("entity.iteration.teleport");
    public static final RegistryObject<SoundEvent> ITERATION_ATTACK = registerSoundEvent("entity.iteration.attack");
    public static final RegistryObject<SoundEvent> DISTANT_MUSIC = registerSoundEvent("music.distant");
    public static final RegistryObject<SoundEvent> REALITY_WARP = registerSoundEvent("ambient.reality_warp");
    public static final RegistryObject<SoundEvent> SCREAM = registerSoundEvent("entity.scream");
    
    /**
     * Helper method to register a sound event
     */
    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        // For ambient sounds, use wider range
        float range = name.contains("ambient") || name.contains("music") ? 32.0F : 16.0F;
        
        // Create sound event with the specified name
        return SOUND_EVENTS.register(name, 
                () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(ProjectRedacted.MODID, name), range));
    }
    
    /**
     * Register all sounds with the specified event bus
     */
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
} 