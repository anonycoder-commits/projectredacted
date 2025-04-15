package net.tasuposed.projectredacted.horror.stage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.tasuposed.projectredacted.horror.events.EntityEvent;
import net.tasuposed.projectredacted.horror.events.HorrorSoundEvent;
import net.tasuposed.projectredacted.horror.events.TextureEvent;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.network.packets.GlitchScreenPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.tasuposed.projectredacted.network.packets.PlaySoundPacket;

import java.util.Random;

/**
 * Third stage of horror - obvious disturbances that can't be ignored
 */
public class ObviousStage implements HorrorStage {
    private final Random random = new Random();
    private final HorrorSoundEvent soundEvent = new HorrorSoundEvent();
    private final TextureEvent textureEvent = new TextureEvent();
    private final EntityEvent entityEvent = new EntityEvent();
    
    @Override
    public void triggerRandomEvent(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Use weighted selection for consistent balance
        int randomValue = random.nextInt(100);
        int choice;
        
        if (randomValue < 15) {
            // 15% chance for whispers
            choice = 0;
        } else if (randomValue < 30) {
            // 15% chance for shadow figure
            choice = 1;
        } else if (randomValue < 45) {
            // 15% chance for glitch screen
            choice = 2;
        } else if (randomValue < 60) {
            // 15% chance for texture corruption
            choice = 3;
        } else if (randomValue < 75) {
            // 15% chance for threatening message
            choice = 4;
        } else {
            // 25% chance for Protocol_37 sighting - much more common in obvious stage
            choice = 5;
        }
        
        switch (choice) {
            case 0:
                // Play a whisper directly to the player
                soundEvent.playWhisper(serverPlayer);
                break;
            case 1:
                // Spawn a temporary shadow figure in peripheral vision
                entityEvent.spawnTemporaryShadowFigure(serverPlayer);
                break;
            case 2:
                // Send a glitch screen effect
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                2, // EFFECT_CORRUPT
                                0.7f, // More noticeable
                                40), // 2 seconds 
                        serverPlayer);
                break;
            case 3:
                // More intense texture corruption
                textureEvent.sendCorruptTexturePacket(serverPlayer, 0.7f);
                break;
            case 4:
                // Player sees a message from "unknown entity"
                serverPlayer.sendSystemMessage(Component.literal("§4§k|||§r §4W̶̢̛͖͈̞̘͍͈̦̲͚̹̔̂͋̎͆a̴̧̹̖̜̖̫̱̹̎̑̉͑̔͐ť̵̯̥̤̝̦̪̲̐ͅc̸̥͈̱̙̞̋̓̅͗̂̚͠h̶̨̭̳̹̰̳̬̾̎̏̄̓̃̃͟͜͝͝ͅi̴͖̓̇̎̊̕̕͟n̷̻̣̖̪͍̮̏͊̇̀̀͂̍g̶̢̦̩̪̮̩̮̼̀̆̂̚͝ͅ ỷ̵̹̗̬̙̺̣̄̓̏ṓ̸̙̤̲͈̞̔̔͠ű̸̦̠̤̘̼̣̈̓̄̽̃͢͠§r §4§k|||§r"));
                break;
            case 5:
                // More intimidating Protocol_37 sighting for Obvious stage
                spawnIntimidatingProtocol37(serverPlayer);
                break;
        }
    }
    
    /**
     * Creates a more intimidating Protocol_37 appearance for the Obvious stage
     * Closer distance, more effects, and higher impact
     */
    private void spawnIntimidatingProtocol37(ServerPlayer player) {
        // First create a tense atmosphere with sounds
        soundEvent.playHeartbeat(player);
        
        // Use a custom spawn with parameters for the Obvious stage
        // Close-medium distance (20-25 blocks), longer visibility, higher glitch intensity
        player.server.tell(new TickTask(
                player.server.getTickCount() + 40, () -> {
            entityEvent.spawnCustomProtocol37(
                player,                       // player
                12 + random.nextInt(6),       // distance: 12-17 blocks (reduced from 20-25)
                200,                          // duration: 10 seconds (200 ticks) before auto-removal
                0.7f,                         // glitchIntensity: high
                true                          // sendMessage: always
            );
            
            // Add second effect after the entity appears
            player.server.tell(new TickTask(
                    player.server.getTickCount() + 60, () -> {
                // Screen corruption
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                2, // EFFECT_CORRUPT
                                0.5f, // Medium intensity
                                30), // 1.5 seconds
                        player);
                
                // Send a more ominous message
                player.sendSystemMessage(Component.literal("§4§kx§r §4Protocol_37 detected§r §4§kx§r"));
            }));
        }));
    }
} 