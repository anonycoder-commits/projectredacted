package net.tasuposed.projectredacted.horror.stage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.TickTask;
import net.tasuposed.projectredacted.entity.Protocol_37;
import net.tasuposed.projectredacted.entity.EntityRegistry;
import net.tasuposed.projectredacted.horror.events.EntityEvent;
import net.tasuposed.projectredacted.horror.events.HorrorSoundEvent;
import net.tasuposed.projectredacted.horror.events.TextureEvent;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.network.packets.FakeCrashPacket;
import net.tasuposed.projectredacted.network.packets.GlitchScreenPacket;

import java.util.Random;

/**
 * Specialized horror stage that focuses on creating a sense that the game
 * is becoming unstable with frequent glitches, fake crashes, and Protocol_37 sightings
 */
public class FakeGlitchStage implements HorrorStage {
    private final Random random = new Random();
    private final HorrorSoundEvent soundEvent = new HorrorSoundEvent();
    private final TextureEvent textureEvent = new TextureEvent();
    private final EntityEvent entityEvent = new EntityEvent();
    
    // Disturbing messages for fake crash reports
    private final String[] CRASH_MESSAGES = {
        "FATAL ERROR: PROTOCOL_37_MEMORY_CORRUPTION",
        "CRITICAL FAILURE: REALITY_BUFFER_OVERFLOW",
        "SYSTEM CRASH: ENTITY_DATA_BREACH_DETECTED",
        "FATAL EXCEPTION: PLAYER_SOUL_COMPROMISED",
        "PROTOCOL_37_IS_WATCHING_YOUR_EVERY_MOVE",
        "GAME_PROCESS_TERMINATED_BY_UNKNOWN_ENTITY",
        "SECURITY_BREACH: PROTOCOL_37_HAS_ESCAPED_CONTAINMENT",
        "FATAL ERROR: GAME_REALITY_FABRIC_TORN",
        "SYSTEM FAILURE: YOUR_COORDINATES_HAVE_BEEN_LOGGED",
        "EMERGENCY_SHUTDOWN: PROTOCOL_37_HAS_FOUND_YOU"
    };
    
    @Override
    public void triggerRandomEvent(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Use weighted selection with higher focus on crashes and Protocol_37
        int randomValue = random.nextInt(100);
        int choice;
        
        if (randomValue < 35) {
            // 35% chance for fake crash screen
            choice = 0;
        } else if (randomValue < 70) {
            // 35% chance for Protocol_37 sighting with glitch effects
            choice = 1;
        } else if (randomValue < 85) {
            // 15% chance for severe texture corruption
            choice = 2;
        } else {
            // 15% chance for fake game freeze
            choice = 3;
        }
        
        switch (choice) {
            case 0:
                // Fake crash screen with system error sound
                triggerFakeCrash(serverPlayer);
                break;
            case 1:
                // Protocol_37 sighting with glitches
                spawnGlitchyProtocol37(serverPlayer);
                break;
            case 2:
                // Severe texture corruption
                textureEvent.sendCorruptTexturePacket(serverPlayer, 0.95f);
                // Also send a screen glitch for extra effect
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                2, // EFFECT_CORRUPT
                                0.8f, // High intensity
                                35), // 1.75 seconds
                        serverPlayer);
                break;
            case 3:
                // Fake game freeze - send a long-duration low-intensity static
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                1, // EFFECT_FREEZE
                                0.3f, // Low intensity but noticeable
                                80), // 4 seconds of "freeze"
                        serverPlayer);
                
                // After the "freeze", play error sound and show brief corruption
                serverPlayer.server.tell(new TickTask(
                        serverPlayer.server.getTickCount() + 85, () -> {
                    soundEvent.playSystemError(serverPlayer);
                    
                    // Brief corruption after the freeze
                    NetworkHandler.sendToPlayer(
                            new GlitchScreenPacket(
                                    2, // EFFECT_CORRUPT
                                    0.5f, // Medium intensity
                                    15), // Quick flash
                            serverPlayer);
                }));
                break;
        }
    }
    
    /**
     * Trigger a fake crash screen with custom error message
     */
    private void triggerFakeCrash(ServerPlayer player) {
        // Play system error sound first
        soundEvent.playSystemError(player);
        
        // Brief screen flash
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(
                        0, // EFFECT_STATIC
                        1.0f, // Maximum intensity
                        10), // Brief flash
                player);
        
        // Short delay then crash
        player.server.tell(new TickTask(
                player.server.getTickCount() + 15, () -> {
            // Send the crash packet with a random message
            String crashMessage = CRASH_MESSAGES[random.nextInt(CRASH_MESSAGES.length)];
            NetworkHandler.sendToPlayer(
                    new FakeCrashPacket(crashMessage),
                    player);
        }));
    }
    
    /**
     * Spawn a Protocol_37 entity with glitchy effects
     */
    private void spawnGlitchyProtocol37(ServerPlayer player) {
        // First create screen static to build tension
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(
                        0, // EFFECT_STATIC
                        0.6f, // Medium-high intensity
                        20), // 1 second
                player);
        
        // Corrupted system message
        player.sendSystemMessage(Component.literal("§4§kP§r §4WARNING: Protocol_37 §kdete§r§4cted§r §4§kP§r"));
        
        // Play distorted sound
        soundEvent.playDistortedSound(player);
        
        // After effects, spawn multiple glimpses of Protocol_37 in different locations
        player.server.tell(new TickTask(
                player.server.getTickCount() + 30, () -> {
            // First sighting - medium distance
            entityEvent.spawnCustomProtocol37(
                player,                      // player
                15 + random.nextInt(10),     // distance: 15-25 blocks 
                80,                          // duration: 4 seconds (80 ticks)
                0.5f,                        // glitchIntensity: medium
                false                        // sendMessage: false (we already sent one)
            );
            
            // Schedule second sighting at different location
            player.server.tell(new TickTask(
                    player.server.getTickCount() + 100, () -> {
                // Second sighting - closer
                entityEvent.spawnCustomProtocol37(
                    player,                  // player
                    10 + random.nextInt(5),  // distance: 10-15 blocks
                    60,                      // duration: 3 seconds (60 ticks)
                    0.7f,                    // glitchIntensity: higher
                    true                     // sendMessage: true
                );
                
                // Play additional distorted sound
                soundEvent.playWhisper(player);
            }));
        }));
    }
} 