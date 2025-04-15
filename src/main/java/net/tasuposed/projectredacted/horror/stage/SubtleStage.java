package net.tasuposed.projectredacted.horror.stage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tasuposed.projectredacted.entity.Protocol_37;
import net.tasuposed.projectredacted.entity.EntityRegistry;
import net.tasuposed.projectredacted.horror.events.EntityEvent;
import net.tasuposed.projectredacted.horror.events.HorrorSoundEvent;
import net.tasuposed.projectredacted.horror.events.TextureEvent;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.network.packets.GlitchScreenPacket;

import java.util.Random;

/**
 * First stage of horror - subtle environmental changes
 */
public class SubtleStage implements HorrorStage {
    private final Random random = new Random();
    private final HorrorSoundEvent soundEvent = new HorrorSoundEvent();
    private final TextureEvent textureEvent = new TextureEvent();
    private final EntityEvent entityEvent = new EntityEvent();
    
    @Override
    public void triggerRandomEvent(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Use weighted selection to ensure a fair distribution
        int randomValue = random.nextInt(100);
        int choice;
        
        if (randomValue < 22) {
            // 22% chance for distant mysterious sound (most common for subtle stage)
            choice = 0;
        } else if (randomValue < 42) {
            // 20% chance for subtle screen glitch
            choice = 1;
        } else if (randomValue < 62) {
            // 20% chance for minor texture glitch
            choice = 2;
        } else if (randomValue < 77) {
            // 15% chance for random environmental sound
            choice = 3;
        } else if (randomValue < 87) {
            // 10% chance for whispers
            choice = 4;
        } else if (randomValue < 91) {
            // 4% chance for shadow glimpse (rare in subtle stage)
            choice = 5;
        } else if (randomValue < 94) {
            // 3% chance for heartbeat (very rare in subtle stage)
            choice = 6;
        } else if (randomValue < 98) {
            // 4% chance for very distant Protocol_37 sighting (very rare in subtle stage)
            choice = 8;
        } else {
            // 2% chance for doing nothing
            choice = 7;
        }
        
        switch (choice) {
            case 0:
                // Distant mysterious sound
                soundEvent.playDistantSound(serverPlayer);
                break;
            case 1:
                // Subtle screen glitch
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                0, // Static effect
                                0.3f, // Low intensity
                                20), // 1 second
                        serverPlayer);
                break;
            case 2:
                // Minor texture glitch
                textureEvent.sendTextureGlitchPacket(serverPlayer);
                break;
            case 3:
                // Random environmental sound
                soundEvent.playDistortedSound(serverPlayer);
                break;
            case 4:
                // Early game whispers - rare in subtle stage
                soundEvent.playWhisper(serverPlayer);
                break;
            case 5:
                // Very rare shadow glimpse - making entities appear earlier
                entityEvent.spawnTemporaryShadowFigure(serverPlayer);
                break;
            case 6:
                // Heartbeat sound - early appearance
                soundEvent.playHeartbeat(serverPlayer);
                break;
            case 7:
                // Do nothing - makes effects feel more random
                break;
            case 8:
                // Extremely distant Protocol_37 sighting - barely visible
                spawnVeryDistantProtocol37(serverPlayer);
                break;
        }
    }
    
    /**
     * Spawn a very distant Protocol_37 that is barely visible
     * This is for the subtle stage, so it should be almost imperceptible
     */
    private void spawnVeryDistantProtocol37(ServerPlayer player) {
        // Use the customizable method with parameters for the Subtle stage
        // Reduced distance for better visibility in caves (20-25 blocks)
        entityEvent.spawnCustomProtocol37(
            player,                       // player
            20 + random.nextInt(6),       // distance: 20-25 blocks (reduced from 30-35)
            60,                           // duration: 3 seconds (60 ticks) before auto-removal
            0.2f,                         // glitchIntensity: very low
            random.nextFloat() < 0.3f     // sendMessage: only 30% chance
        );
    }
} 