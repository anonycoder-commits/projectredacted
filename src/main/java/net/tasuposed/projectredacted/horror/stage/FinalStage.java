package net.tasuposed.projectredacted.horror.stage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tasuposed.projectredacted.ProjectRedacted;
import net.tasuposed.projectredacted.entity.Iteration;
import net.tasuposed.projectredacted.entity.Protocol_37;
import net.tasuposed.projectredacted.entity.EntityRegistry;
import net.tasuposed.projectredacted.horror.events.EntityEvent;
import net.tasuposed.projectredacted.horror.events.HorrorSoundEvent;
import net.tasuposed.projectredacted.horror.events.TextureEvent;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.network.packets.GlitchScreenPacket;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Final stage of horror - ultimate terror effects
 */
public class FinalStage implements HorrorStage {
    private final Random random = new Random();
    private final HorrorSoundEvent soundEvent = new HorrorSoundEvent();
    private final TextureEvent textureEvent = new TextureEvent();
    private final EntityEvent entityEvent = new EntityEvent();
    
    // Cache the crash message to avoid repeated string creation
    private static final Component CRASH_MESSAGE = Component.literal(
            "§4C̷̡͖̺̭̦̗̖̓͋̽̐̊͋͢ơ̢̧̝̗͓̻̼̊͒̎̾͘̚r̢̨̘̘̮͈̞̦̱̒͆́̃͌̓͝r̶̡̮̗͔̹̱̞̓̊̅̐͘͞ͅȗ̡̡̻̩̬̺͉̼̏̆̽̃̐͆̉͘͢p̷̼̘̦̳̻̥̊̊̌̓̃̈́͝t̬̥̺̲͓̆̊̉̇̆̌͘͘ḙ̴̡̻̘̝̟̉̃͐͊̀̇̕͞͡ͅd̨̡̗̝̼͙̜̭̠̏͋̂̿̓͋̉͡ D̷̢̪͖̱̬̜̹̓̃̈́̎̾̀̀̌̈́a̸̠̖̜̜̐̄̐͒̅̽̇̏͢͜͞ẗ̞͙̪͇̫͚̙̂̾̐̈́͘͜͜͞á̷̡̨̦̰̤̫̱͋̇͒̏̃̒̕͢͠:̶̡̲̞̩͍̮͈̻̤̓͋̽̃̔̈̕ M̵̢̳̤̯̦̻̭̗̜̏̿̈́̀̆͘i̶̛̙͓̻̫̞̦͌̃̾̾̌̇̀̀͡n̸̛̦̮̘̰͖̟̦̪̣̭͑͊̌̋͌̊̈͗͝e̷̯̮͙̟̗͇̥̤͇͌͛͐͋̊̋̋̽͗͢c̶̨̞͔̱̘̖̹̺̤̆̓̆̋̊̽̍͂̓̿r̮̦̥̫̠̖͐̀̽̓̆̿͟͜͟͞a̸͙̘̦̩̻̔̃̊̒̌̍͘͘͟f̶̧̨̧̜̙̤͖̥̈̆̇̒̏͊̽̽̕͜͝ţ͖̬̭̝̣̘̝̿̀̏̓̃̈́̚͞ W̵͉̯̙͖̗̤̣̮̋͑̓͒̓̎̚͘͜͠o̭̠̤̞̣̲̮̱̯̔̀̅͗̊͗̀͂̚ŗ̸̬̟̬̱̰͈͙͒͑̊͑̿͌̍͂̚͞l̷̢̢̯͇̱̻̄̓̑̂̓̔̕̕ḓ̢̗͖̗̺͓̿̂̐̂̚͢ͅ");
    
    // Define handlers for each event type
    private final Runnable[] eventHandlers = new Runnable[8];
    
    public FinalStage() {
        // Initialize event handlers for each type
        initEventHandlers();
    }
    
    /**
     * Initialize the event handler array for better performance
     */
    private void initEventHandlers() {
        // 0: Random fog with reduced visibility
        eventHandlers[0] = () -> {};
        
        // 1: Audio hallucination  
        eventHandlers[1] = () -> {};
        
        // 2: Spawn Iteration that stalks the player
        eventHandlers[2] = () -> {};
        
        // 3: Protocol_37 apparition that disappears when approached
        eventHandlers[3] = () -> {};
        
        // 4: Complete world distortion
        eventHandlers[4] = () -> {};
        
        // 5: Game appears to crash with scary message
        eventHandlers[5] = () -> {};
        
        // 6: Lightning strike and mysterious entity appearance
        eventHandlers[6] = () -> {};
        
        // 7: Protocol37 in final stage  
        eventHandlers[7] = () -> {};
    }
    
    @Override
    public void triggerRandomEvent(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // Higher chance of intense events in the final stage
        int eventType = random.nextInt(10);
        
        // Map events 8 and 9 to default case (Protocol37)
        if (eventType >= 8) {
            eventType = 3; // Map to Protocol37 apparition
        }
        
        // Handle crash message cases specially
        if (eventType == 5 || eventType == 6) {
            serverPlayer.connection.disconnect(CRASH_MESSAGE);
            return;
        }
        
        switch (eventType) {
            case 0:
                // Random fog with reduced visibility
                entityEvent.reduceRenderDistance(serverPlayer, 3, 200, true); // 3 chunks for 10 seconds
                break;
            case 1:
                // Audio hallucination
                soundEvent.playDistortedSound(serverPlayer);
                break;
            case 2:
                // Spawn Iteration that stalks the player
                spawnIteration(serverPlayer);
                break;
            case 3:
                // Protocol_37 apparition that disappears when approached
                spawnProtocol37Apparition(serverPlayer);
                break;
            case 4:
                // Complete world distortion - combine screen and texture effects with sounds
                applyWorldDistortion(serverPlayer);
                break;
            case 7:
                // Lightning strike and mysterious entity appearance
                spawnLightningAndEntity(serverPlayer);
                break;
        }
    }
    
    /**
     * Apply world distortion effect - extracted method for better code organization
     */
    private void applyWorldDistortion(ServerPlayer player) {
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(
                        2, // EFFECT_CORRUPT
                        1.0f, // Maximum intensity
                        120), // 6 seconds
                player);
        
        textureEvent.sendCorruptTexturePacket(player, 1.0f);
        
        // Also play heartbeat sound
        soundEvent.playHeartbeat(player);
    }
    
    /**
     * Spawn lightning and entity - extracted method for better code organization
     */
    private void spawnLightningAndEntity(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition().offset(
                random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
        
        // Spawn lightning
        LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        lightning.moveTo(pos.getX(), pos.getY(), pos.getZ());
        level.addFreshEntity(lightning);
        
        // Send screen flash
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(
                        0, // EFFECT_STATIC
                        1.0f,
                        20),
                player);
        
        // Schedule entity appearance after lightning
        level.getServer().tell(new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + 10, () -> {
            entityEvent.spawnIterationGlimpse(player);
        }));
    }
    
    /**
     * Spawn Iteration entity that follows the player
     */
    private void spawnIteration(ServerPlayer player) {
        // Send scary message
        player.sendSystemMessage(Component.literal("§4§k|||§r §4Subject Iteration detected§r §4§k|||§r"));
        
        // Then play heartbeat sound
        soundEvent.playHeartbeat(player);
        
        // Send screen glitch
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(
                        3, // EFFECT_INVERT
                        0.8f,
                        40),
                player);
        
        // Dramatically reduce render distance to create a claustrophobic atmosphere
        entityEvent.reduceRenderDistance(player, 2, 600, true); // 2 chunks for 30 seconds (600 ticks)
        
        // Spawn Iteration entity
        ServerLevel level = player.serverLevel();
        Iteration entity = new Iteration(EntityRegistry.ITERATION.get(), level);
        
        // Position the entity in front of the player
        Vec3 lookVec = player.getViewVector(1.0F).normalize().scale(10);
        Vec3 spawnPos = player.position().add(lookVec);
        
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        level.addFreshEntity(entity);
    }
    
    /**
     * Spawn a Protocol_37 entity that disappears when approached
     */
    private void spawnProtocol37Apparition(ServerPlayer player) {
        // Send warning message - pre-calculated string for efficiency
        player.sendSystemMessage(Component.literal("§f§k||§r §fP̷̩͍̿R̷̛̹͓̆O̶̝̔T̵̜̆͆Ȯ̵̫̮̃C̵̖̦̆O̶̜̪̓L̶̨̼͌̕_̶̛͙̇3̵̨͕̍7̸̲̤̄̇ D̸͋͜Ȩ̶̩̈́̈́T̵͕̿̿Ê̶̗̘C̶̠̱͘T̵̡͔̎E̸̜̹̍D̴̦̹͝§r §f§k||§r"));
        
        // Send screen glitch with more intensity
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(
                        1, // EFFECT_DISTORTION
                        0.85f, // Increased intensity
                        30),
                player);
        
        // Moderately reduce render distance
        entityEvent.reduceRenderDistance(player, 4, 300, true); // 4 chunks for 15 seconds (300 ticks)
        
        // Spawn the Protocol_37 entity
        ServerLevel level = player.serverLevel();
        Protocol_37 entity = new Protocol_37(EntityRegistry.PROTOCOL_37.get(), level);
        
        // Generate a random position closer to the player - optimize with one random check
        boolean inLineOfSight = random.nextBoolean();
        double distance;
        Vec3 spawnPos;
        
        if (inLineOfSight) {
            // Spawn directly in player's view with a single random call
            distance = 6 + random.nextInt(4); // 6-9 blocks
            Vec3 lookVec = player.getViewVector(1.0F).normalize().scale(distance);
            spawnPos = player.position().add(lookVec);
        } else {
            // Spawn in a flanking position with fewer calculations
            double angle = random.nextDouble() * Math.PI * 2;
            distance = 4 + random.nextDouble() * 3; // 4-7 blocks
            
            // Calculate position with a single calculation
            spawnPos = player.position().add(
                Math.sin(angle) * distance,
                0, // Same Y level
                Math.cos(angle) * distance
            );
        }
        
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        level.addFreshEntity(entity);
        
        // Play a scary sound to make the experience more intense
        soundEvent.playDistortedSound(player);
    }
} 