package net.tasuposed.projectredacted.horror.stage;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tasuposed.projectredacted.entity.EntityRegistry;
import net.tasuposed.projectredacted.entity.Iteration;
import net.tasuposed.projectredacted.entity.Protocol_37;
import net.tasuposed.projectredacted.horror.events.EndgameSequence;
import net.tasuposed.projectredacted.horror.events.EntityEvent;
import net.tasuposed.projectredacted.horror.events.HorrorSoundEvent;
import net.tasuposed.projectredacted.horror.events.TextureEvent;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.network.packets.GlitchScreenPacket;

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
    
    // Tracker for endgame trigger attempts
    private boolean endgameTriggered = false;
    private long lastEndgameTrigger = 0;
    
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
        
        // Direct endgame trigger - if this is a direct command call, always trigger endgame
        if (endgameTriggered) {
            // Don't try again if we just tried recently (within 5 seconds)
            long now = System.currentTimeMillis();
            if (now - lastEndgameTrigger < 5000) {
                return;
            }
            
            triggerEndgameSequence(serverPlayer);
            lastEndgameTrigger = now;
            return;
        }
        
        // Check if player is underground
        boolean isUnderground = !serverPlayer.level().canSeeSky(serverPlayer.blockPosition());
        
        // Reduced skipping chance in final stage - they're already at max horror
        if (isUnderground && random.nextInt(100) < 25) {  // Only 25% chance to skip in final stage
            return;
        }
        
        // Increased chance of intense events in the final stage - use a weighted system
        // Prefer events 2-7 which are more intense (70% chance for these)
        int eventType;
        int roll = random.nextInt(100);
        
        if (roll < 10) {
            eventType = 0; // 10% for fog
        } else if (roll < 20) {
            eventType = 1; // 10% for audio
        } else if (roll < 35) {
            eventType = 2; // 15% for Iteration
        } else if (roll < 50) {
            eventType = 3; // 15% for Protocol_37
        } else if (roll < 65) {
            eventType = 4; // 15% for distortion
        } else if (roll < 75) {
            eventType = 5; // 10% for crash message
        } else if (roll < 90) {
            eventType = 6; // 15% for lightning
        } else {
            eventType = 7; // 10% for endgame sequence chance
        }
        
        // Handle crash message cases - increased chance
        if (eventType == 5 && random.nextInt(3) == 0) { // 33% chance when event 5 is rolled
            serverPlayer.connection.disconnect(CRASH_MESSAGE);
            return;
        }
        
        // Explicit handling for final stage - direct endgame triggering with higher chance
        if (eventType == 7) {
            // 50% chance to trigger endgame sequence in final stage
            if (random.nextInt(2) == 0) {
                triggerEndgameSequence(serverPlayer);
                return;
            }
            // Fallback to intense entity encounters
            eventType = random.nextInt(2) == 0 ? 2 : 3; // Either Iteration or Protocol_37
        }
        
        switch (eventType) {
            case 0:
                // Random fog with EXTREME reduced visibility - more intense in final stage
                entityEvent.reduceRenderDistance(serverPlayer, 2, 300, true); // 2 chunks for 15 seconds
                break;
            case 1:
                // Intensified audio hallucination - play multiple sounds
                soundEvent.playDistortedSound(serverPlayer);
                // Schedule a second sound after a short delay
                serverPlayer.level().getServer().tell(new net.minecraft.server.TickTask(
                    serverPlayer.level().getServer().getTickCount() + 20, // 1 second delay
                    () -> soundEvent.playHeartbeat(serverPlayer)
                ));
                break;
            case 2:
                // Spawn multiple Iterations that stalk the player
                spawnIteration(serverPlayer);
                // 30% chance to spawn a second one offset slightly
                if (random.nextInt(10) < 3) {
                    serverPlayer.level().getServer().tell(new net.minecraft.server.TickTask(
                        serverPlayer.level().getServer().getTickCount() + 40, // 2 second delay
                        () -> spawnIteration(serverPlayer)
                    ));
                }
                break;
            case 3:
                // Protocol_37 apparition that disappears when approached - now with distortion
                spawnProtocol37Apparition(serverPlayer);
                // Add extreme screen distortion
                NetworkHandler.sendToPlayer(
                    new GlitchScreenPacket(
                        2, // EFFECT_CORRUPT
                        0.9f, // High intensity
                        30), // 1.5 seconds
                    serverPlayer);
                break;
            case 4:
                // Complete world distortion - significantly enhanced with multiple effects
                applyWorldDistortion(serverPlayer);
                // Add delayed secondary effect
                serverPlayer.level().getServer().tell(new net.minecraft.server.TickTask(
                    serverPlayer.level().getServer().getTickCount() + 60, // 3 second delay
                    () -> {
                        // Add another layer of effects
                        NetworkHandler.sendToPlayer(
                            new GlitchScreenPacket(
                                3, // EFFECT_INVERT
                                0.9f,
                                40), 
                            serverPlayer);
                        soundEvent.playDistortedSound(serverPlayer);
                    }
                ));
                break;
            case 5:
                // Since we didn't disconnect, apply an intense visual corruption
                NetworkHandler.sendToPlayer(
                    new GlitchScreenPacket(
                        2, // EFFECT_CORRUPT
                        1.0f, // Maximum intensity
                        60), // 3 seconds
                    serverPlayer);
                textureEvent.sendCorruptTexturePacket(serverPlayer, 1.0f);
                break;
            case 6:
                // Lightning strike and mysterious entity appearance - now with multiple strikes
                spawnLightningAndEntity(serverPlayer);
                // Add a second strike after a delay
                serverPlayer.level().getServer().tell(new net.minecraft.server.TickTask(
                    serverPlayer.level().getServer().getTickCount() + 20, // 1 second delay
                    () -> spawnLightningAndEntity(serverPlayer)
                ));
                break;
        }
    }
    
    /**
     * Set whether to forcibly trigger the endgame sequence on next event
     */
    public void setEndgameTriggered(boolean triggered) {
        this.endgameTriggered = triggered;
        this.lastEndgameTrigger = System.currentTimeMillis();
    }
    
    /**
     * Trigger the final endgame sequence for a player
     * @param player The player to trigger the sequence for
     */
    private void triggerEndgameSequence(ServerPlayer player) {
        // Apply intense screen effect before endgame
        NetworkHandler.sendToPlayer(
            new GlitchScreenPacket(
                3, // EFFECT_INVERT 
                1.0f, // Maximum intensity
                40), // 2 seconds
            player);
        
        // Send warning message
        player.sendSystemMessage(Component.literal("§f§k||§r §fP̷̩͍̿R̷̛̹͓̆O̶̝̔T̵̜̆͆Ȯ̵̫̮̃C̵̖̦̆O̶̜̪̓L̶̨̼͌̕_̶̛͙̇3̵̨͕̍7̸̲̤̄̇ D̸͋͜Ȩ̶̩̈́̈́T̵͕̿̿Ê̶̗̘C̶̠̱͘T̵̡͔̎E̸̜̹̍D̴̦̹͝§r §f§k||§r"));
        
        // Start the endgame sequence
        EndgameSequence.getInstance().startEndgameSequence(player);
    }
    
    /**
     * Apply world distortion effect - extracted method for better code organization
     */
    private void applyWorldDistortion(ServerPlayer player) {
        // More intense distortion in final stage
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(
                        2, // EFFECT_CORRUPT
                        1.0f, // Maximum intensity
                        180), // 9 seconds - extended duration
                player);
        
        textureEvent.sendCorruptTexturePacket(player, 1.0f);
        
        // Also play heartbeat sound with distorted sound
        soundEvent.playHeartbeat(player);
        
        // Add a delayed distorted sound for more terror
        player.level().getServer().tell(new net.minecraft.server.TickTask(
            player.level().getServer().getTickCount() + 40, // 2 second delay
            () -> soundEvent.playDistortedSound(player)
        ));
    }
    
    /**
     * Spawn lightning and entity - extracted method for better code organization
     */
    private void spawnLightningAndEntity(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        
        // Multiple lightning strikes in a tighter pattern around player
        for (int i = 0; i < 2; i++) {
            BlockPos pos = player.blockPosition().offset(
                    random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            
            // Spawn lightning
            LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
            lightning.moveTo(pos.getX(), pos.getY(), pos.getZ());
            level.addFreshEntity(lightning);
        }
        
        // More dramatic screen flash
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(
                        0, // EFFECT_STATIC
                        1.0f,
                        30), // 1.5 seconds
                player);
        
        // Schedule entity appearance after lightning
        level.getServer().tell(new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + 10, () -> {
            // In final stage, spawn both types of entities for maximum terror
            entityEvent.spawnIterationGlimpse(player);
            
            // 50% chance to also spawn Protocol_37 glimpse
            if (random.nextBoolean()) {
                level.getServer().tell(new net.minecraft.server.TickTask(
                    level.getServer().getTickCount() + 15, () -> {
                        spawnProtocol37Apparition(player);
                    }
                ));
            }
        }));
    }
    
    /**
     * Spawn Iteration entity that follows the player
     */
    private void spawnIteration(ServerPlayer player) {
        // Send scary message with more intensity
        player.sendSystemMessage(Component.literal("§4§k|||§r §4S̷̨̱͔̙͓̞͉̬̣̻̫̱͎̟̿́̍ͅų̵̨̨̺̰̥̯̺̺̳̮̫̭̮̳̭̖̱͚͓̯̠͙͙̦͔͕̟͈̩͙̦̰̭͖̏̑̂̽̏͐̓͒̆͂̑̅̅́̕͜͜ͅͅb̶̨̧̧̨̨͕͙̪͕̱̣̺͈̝̯̘̫̯̯͉̪̪͚̝͚̺͉̞͎͍͈̙̗̮̻̹̬̱͍́̒̓̾̅̈́̇͐̇̋̂̄͝ͅj̵̨̨̛̳͍̰̝̹̠̫̺͑̎̈̀̃̈́̔̀̐̈́̐̓̾͊̏͋̇̚̚ͅe̴̡̠̼̤̖͉̣̦̟͌̋̊̂͛͊͛̊̑̑̃͊̅͂̈̒̊̊̄̀̽̆͑͌̐̾̚̚͝c̶̢̡̧̛̟͇̜̘̜͉̗̫̠̲̝̤̲̗̟̣̳̩̰̪̣̜̯͎̺̖̦̭̫̲̙̙̗̤̼̦̹͖̭̿̄̑̉͊̓͊̓̇͊̋͌̽̎̅̔͂̀̈́̏̑̐̃̆̑̂̋̓̾̃̂͐́͌̐̎͊͘͝ͅͅt̴̢̡̨̡̧̨̨̩̤̣̘̲̘̺̦̬̰̝̣̘̠̣͈̬̘̪͖̯̣̓͒̒͗͋̆̽͒̎̽̈̑̅̓̿̏̊̎̈́̈́̊̊̎̀͒̏̒̌̓͛͑̚͘̚͝͝ͅ ̴̡̧̧̧̧̙͙̗̣̦̥͙̤̮̰̰̭̬̘̠̣͕̼͙͉͓̩͙̤̜͕͙̗̰̼̱̱̱̖̟̔̊̍͂̇̇͛̊̀̑̃̇̐͊̂̍̓̈̚ͅI̷̢̢̛̟̠̹̺͙̺̲͇̲̟̭̯̙̬̯̯̱̦̲̱̞̘̟͑̈́͆̎̒̀͆̊̉̊̈̐̍̑͐̍̔̍͂͐͊͛̕̚͝ͅͅt̸̨̡̡̛̩̟̝̬̮͖̬̗̬̻̥̮̰̯̬͚͔̫͓͎͕͓͓̃̎̎̾̉̐̏ͅͅę̶̢̢̫̗̤̼̜̦̼̹̱̜̙̭̱̩̫̘̪̹̤̤͍̻͎̬̩̼̬͎̯̿̈́̾͋͗̄͗̋̽͐̓̔̀͒̋̐̽͑̎̊͂͐̽̍̀͊͒̑́̄͂̕̚̚͝͝͝ͅr̴̢̧̡̧̨͚̩̭͓̥̗̮͇̺̱̖̫̦̖̬̘̫̠̗̘̗̝͛̀̔͝ä̵̛̛̭̻̫̻͙̼̥̬̠͔̫̹͉̹̰̹̘̦̦͓̠̤́̒͑͆͛͛̄̾̉̀̏̀̉͆̋̎͒̇̉̍̓̌̔͜ͅṱ̵̨̡̨̛̟̯̜̫̥̦̼̮͓͓̫͉̝̫͙͇̼͙̗̹̺̟̦̣̫̥̜̠̞̼̎͋̾̃̒̊͊̋̐̚͜͜͜͠͝i̶̧̛̛̟̠͚͇̝̘̗̣̣̯̖̭̲̯̯͔͚̮̗̞͉͖̭͕̗̔͑̉̓̾̒̌͌̂́́͗̂̽̌̿́̒̍̉͘͘͜ő̷̧̦̤͖̫̲̙̤͖̬̱̗͖̫̖̺̘͙͖̖͖̜͖̟̗̳̤͚̻̈́͂͊̊͐̓̾̋̍̇̔͛́͒̑̋̅̂̾̈͆̀̂̆̚̚͠͝n̴͇̩̮̥̦͙̻̩̮͔̹̮̙̪͎̍́̔̋̌̏̐̑́͜͝ ̴̻̮̦̖̖̘͎̭͇̳̤̖̣͖̩̱̞͍̙́̒͑̔̋̈̆̈̐̄̏̏̉̓͛͆̌̔̽̎͋͂̆̚͠d̶̡̡̡̛̹̱͍̦̗̹̪̺̦̠̯̠̩̯̝̖̭̞̒̀̑́̑̃̾͆̅̂̅̈́̏̐̇̑̈́̐̆́̏͊̆̄́̄̀͗͠͝ē̵̹͓̣̰̞͍̣̲̮̣͋̇͊̃̑͑̀̓̎̅̆̀̓͌͒̅̄͐̚̕̚ţ̴̨̢̧̤̻̰̖̮̯̜͙̜̤̰̜̯̭̹͈͇͙̭͉͕̯̺̲̹͎̭̱͙͙̘̯͌̑͐͒̍͂̄̾̔̄̊͋̃̒̐̈̀̔̓͊̉̐̌̄͊̓̊̏̇̌̊̇̒̇̾̚͝͝͠͝ͅͅê̵̢̨̡̡͉̩̞̖̬͎͚̩̙͙͚̬̝̱̱̥̭̜͍̦̺͙͉̠̝̞̪̩̘̭̭͌́͗̆̉̓̓͑́͊͜͜͜ç̸̨̛̯̰͇̳̬̗̞̝̥̣͆̔͒̓͋͐̈́̂͐̑̈́̅̇̒͐͑̽̊̄͌̾͗͐̉̏́̽͒̾͋̑̓͒̚͝ţ̶̡̮̪͔̦̖̥̯̺̩͚̠̬͚̙̣͓̬̼͓̝̪̘̼͓͉̙̜̼̓̓̿̌̽͑͑̏̔͑̿͑̀̋̌̕̚͜͜ͅͅe̵̡̧̡̡̛̹̹̖̝̼̦̲̘̩͔̗̯̮̩͚̞͉̼̮̘͇̰̬̬̩̗̮̻̠̗͙̠̼̞̝͋́̄̑̔͆̍̏́͛͐̿͌̓̂͒̔̎́̐͑͊͆̚͜͝ͅͅd̶̡̡̢̞̝̺̟̻̭̮̦̳̻̫̠͎̞̯̪̺̰̹̠̬̯̘̗̻̫̫̭̎̊̃̑͑̄ͅ§r §4§k|||§r"));
        
        // Play multiple sounds in sequence
        soundEvent.playHeartbeat(player);
        
        // Add extremely dramatic screen effects - shorter but more intense
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(
                        3, // EFFECT_INVERT
                        1.0f, // Maximum intensity
                        60), // 3 seconds
                player);
        
        // Dramatically reduce render distance to create a claustrophobic atmosphere
        entityEvent.reduceRenderDistance(player, 1, 800, true); // 1 chunk for 40 seconds (extreme)
        
        // Spawn Iteration entity
        ServerLevel level = player.serverLevel();
        Iteration entity = new Iteration(EntityRegistry.ITERATION.get(), level);
        
        // Position the entity in front of the player but closer
        Vec3 lookVec = player.getViewVector(1.0F).normalize().scale(6); // Closer than before
        Vec3 spawnPos = player.position().add(lookVec);
        
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        level.addFreshEntity(entity);
        
        // Schedule a second distorted sound after a delay
        level.getServer().tell(new net.minecraft.server.TickTask(
            level.getServer().getTickCount() + 30, // 1.5 second delay
            () -> soundEvent.playDistortedSound(player)
        ));
    }
    
    /**
     * Spawn a Protocol_37 entity that disappears when approached
     */
    private void spawnProtocol37Apparition(ServerPlayer player) {
        // Send warning message - pre-calculated string for efficiency
        player.sendSystemMessage(Component.literal("§f§k||§r §fP̷̩͍̿R̷̛̹͓̆O̶̝̔T̵̜̆͆Ȯ̵̫̮̃C̵̖̦̆O̶̜̪̓L̶̨̼͌̕_̶̛͙̇3̵̨͕̍7̸̲̤̄̇ D̸͋͜Ȩ̶̩̈́̈́T̵͕̿̿Ê̶̗̘C̶̠̱͘T̵̡͔̎E̸̜̹̍D̴̦̹͝§r §f§k||§r"));
        
        // Add more intense screen effects
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(
                        random.nextInt(4), // Random effect type for unpredictability
                        0.95f, // Increased intensity
                        40), // 2 seconds
                player);
        
        // Severely reduce render distance
        entityEvent.reduceRenderDistance(player, 2, 400, true); // 2 chunks for 20 seconds
        
        // Spawn the Protocol_37 entity
        ServerLevel level = player.serverLevel();
        
        // In final stage, spawn multiple entities surrounding the player
        for (int i = 0; i < 2 + random.nextInt(2); i++) { // 2-3 entities
            Protocol_37 entity = new Protocol_37(EntityRegistry.PROTOCOL_37.get(), level);
            
            // Generate random position around player
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 3 + random.nextDouble() * 3; // 3-6 blocks - closer than before
            
            // Calculate position with a single calculation
            Vec3 spawnPos = player.position().add(
                Math.sin(angle) * distance,
                0, // Same Y level
                Math.cos(angle) * distance
            );
            
            entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            level.addFreshEntity(entity);
            
            // Add small delay between spawns
            if (i < 2) {
                try {
                    Thread.sleep(50);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        
        // Play multiple scary sounds to make the experience more intense
        soundEvent.playDistortedSound(player);
        
        // Schedule additional sound effect
        level.getServer().tell(new net.minecraft.server.TickTask(
            level.getServer().getTickCount() + 20, // 1 second delay
            () -> soundEvent.playHeartbeat(player)
        ));
    }
} 