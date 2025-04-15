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
    
    /**
     * Helper method to register a sound event
     */
    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        // Create sound event with the specified name
        return SOUND_EVENTS.register(name, 
                () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(ProjectRedacted.MODID, name), 16.0F));
    }
    
    /**
     * Register all sounds with the specified event bus
     */
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
} 