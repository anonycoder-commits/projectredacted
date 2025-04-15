package net.tasuposed.projectredacted.horror.stage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * Second stage of horror - more noticeable disturbances
 */
public class DisturbanceStage implements HorrorStage {
    private final Random random = new Random();
    private final HorrorSoundEvent soundEvent = new HorrorSoundEvent();
    private final TextureEvent textureEvent = new TextureEvent();
    private final EntityEvent entityEvent = new EntityEvent();
    
    @Override
    public void triggerRandomEvent(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Use weighted selection for more balanced distribution
        int randomValue = random.nextInt(100);
        int choice;
        
        if (randomValue < 15) {
            // 15% chance for whispers
            choice = 0;
        } else if (randomValue < 30) {
            // 15% chance for cryptic message (with potential entity spawn)
            choice = 1;
        } else if (randomValue < 45) {
            // 15% chance for texture disruption
            choice = 2;
        } else if (randomValue < 60) {
            // 15% chance for shadow figure
            choice = 3;
        } else if (randomValue < 75) {
            // 15% chance for screen glitch
            choice = 4;
        } else if (randomValue < 85) {
            // 10% chance for Iteration glimpse
            choice = 5;
        } else {
            // 15% chance for Protocol_37 appearance (moderately common in disturbance stage)
            choice = 6;
        }
        
        switch (choice) {
            case 0:
                // Play whispers
                soundEvent.playWhisper(serverPlayer);
                break;
            case 1:
                // Show mysterious message
                serverPlayer.sendSystemMessage(Component.literal("§7You feel like you're being watched..."));
                
                // Small chance to actually show a glimpse to reinforce the message
                if (random.nextFloat() < 0.4f) {
                    serverPlayer.server.tell(new net.minecraft.server.TickTask(
                        serverPlayer.server.getTickCount() + 20, () -> {
                            entityEvent.spawnTemporaryShadowFigure(serverPlayer);
                    }));
                }
                break;
            case 2:
                // Texture disruption
                textureEvent.sendTextureGlitchPacket(serverPlayer);
                break;
            case 3:
                // Shadow figure (now more common as it's an explicit option)
                entityEvent.spawnTemporaryShadowFigure(serverPlayer);
                break;
            case 4:
                // Screen glitch
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                1, // EFFECT_DISTORTION
                                0.5f, // Medium intensity
                                30), // 1.5 seconds
                        serverPlayer);
                break;
            case 5:
                // Add Iteration glimpse - previously only in later stages
                entityEvent.spawnIterationGlimpse(serverPlayer);
                break;
            case 6:
                // Add Protocol_37 sighting at medium distance
                spawnMediumDistanceProtocol37(serverPlayer);
                break;
        }
    }
    
    /**
     * Spawn Protocol_37 at a medium distance
     * For the disturbance stage, more noticeable than in subtle stage
     */
    private void spawnMediumDistanceProtocol37(ServerPlayer player) {
        // Use the customizable method with parameters for the Disturbance stage
        // Reduced distance for better visibility in caves (10-15 blocks)
        entityEvent.spawnCustomProtocol37(
            player,                        // player
            10 + random.nextInt(6),        // distance: 10-15 blocks (reduced from 15-20)
            120,                           // duration: 6 seconds (120 ticks) before auto-removal
            0.3f,                          // glitchIntensity: medium-low
            true                           // sendMessage: always
        );
        
        // Also play a subtle sound
        soundEvent.playDistantSound(player);
    }
} 