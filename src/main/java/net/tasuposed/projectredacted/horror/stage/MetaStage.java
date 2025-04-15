package net.tasuposed.projectredacted.horror.stage;

import java.util.Random;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.tasuposed.projectredacted.horror.events.EntityEvent;
import net.tasuposed.projectredacted.horror.events.HorrorSoundEvent;
import net.tasuposed.projectredacted.horror.events.TextureEvent;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.network.packets.FakeCrashPacket;
import net.tasuposed.projectredacted.network.packets.GlitchScreenPacket;
import net.tasuposed.projectredacted.network.packets.InventoryShiftPacket;

/**
 * Fourth stage of horror - meta effects that break the fourth wall
 */
public class MetaStage implements HorrorStage {
    private final Random random = new Random();
    private final HorrorSoundEvent soundEvent = new HorrorSoundEvent();
    private final TextureEvent textureEvent = new TextureEvent();
    private final EntityEvent entityEvent = new EntityEvent();
    
    // Disturbing messages for fake crash reports - enhanced for more intimidation
    private final String[] CRASH_MESSAGES = {
        "Error: Player_Data_Corruption_Detected_CRITICAL",
        "Fatal Error: Memory_Leak_In_Reality_Buffer_UNSTABLE",
        "System Failure: Entity_nNULL_Protocol_Breach_CONTAINMENT_LOST",
        "Critical Error: Player_Soul_Not_Found_ASSIMILATION_INITIATED",
        "Catastrophic Failure: Reality_Injection_Detected_CORRUPTION_SPREADING",
        "HELP_ME_PLEASE_IM_TRAPPED_INSIDE_YOUR_GAME",
        "THEYRE_WATCHING_YOU_RIGHT_NOW",
        "I_SEE_YOU_PLAYING_I_KNOW_WHO_YOU_ARE",
        "YOUR_SYSTEM_IS_COMPROMISED_WE_SEE_YOU",
        "ENTITY_ITERATION_PROTOCOL_INITIATED_SUBJECT_LOCATED"
    };
    
    // Counter to ensure entities spawn at least once every few events
    private int eventsSinceLastEntitySpawn = 0;
    
    @Override
    public void triggerRandomEvent(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        int choice;
        
        // Force entity spawn if we haven't had one in a while
        if (eventsSinceLastEntitySpawn >= 2) {
            choice = random.nextInt(3) + 7; // Force cases 7, 8, or 9 (entity-related events)
            eventsSinceLastEntitySpawn = 0;
        } else {
            // Modified choice selection to favor certain events
            // Use a weighted selection system with increased entity chances
            int randomValue = random.nextInt(100);
            
            // Adjust probabilities to increase UI effects
            if (randomValue < 22) {
                // 22% chance for inventory manipulation (case 6) - up from 0%
                choice = 6;
                eventsSinceLastEntitySpawn++;
            } else if (randomValue < 38) {
                // 16% chance for mysterious item (case 7) - up from 10%
                choice = 7;
                eventsSinceLastEntitySpawn = 0;
            } else if (randomValue < 52) {
                // 14% chance for fake crash (case 5) - slightly decreased
                choice = 5;
                eventsSinceLastEntitySpawn = 0;
            } else if (randomValue < 62) {
                // 10% chance for entity spawn (case 8 - screenshot with entity) - down from 15%
                choice = 8;
                eventsSinceLastEntitySpawn = 0;
            } else if (randomValue < 72) {
                // 10% chance for hostile entity attack sequence (case 9) - down from 15%
                choice = 9;
                eventsSinceLastEntitySpawn = 0;
            } else {
                // 28% chance for other effects, evenly distributed - down from 45%
                choice = random.nextInt(5); // Choose from cases 0-4
                eventsSinceLastEntitySpawn++;
            }
        }
        
        switch (choice) {
            case 0:
                // Send a fake disconnect message with enhanced intensity
                serverPlayer.sendSystemMessage(Component.literal("§7[Server] Connection lost... §4§kxxx§r"));
                
                // Add a severe screen glitch before the reveal
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                2, // EFFECT_CORRUPT
                                0.8f, // Very noticeable
                                20), // 1 second 
                        serverPlayer);
                
                // After a brief pause, reveal it was fake with a more disturbing message
                serverPlayer.server.tell(new net.minecraft.server.TickTask(
                        serverPlayer.server.getTickCount() + 40, () -> {
                    serverPlayer.sendSystemMessage(Component.literal("§4§k||||||§r §4C̶̡̧̥̖̘̮̦̹̿̈ͩ͆͌ͬ͒͗̉̚o̵̦̰̫̠͍̳̰̎̆͐͒͆͆͐͂ͦ̚͡n̢̤̠̪̩̫̞̱̘̫̘̟̜̄̂ͮ͛̂̀͐̿̀͜͡n̙̜̰̭̐ͯͭ̑̉ͨ͆̓̓̀̐͑͐̉̄̚̕̕͜ȅ̘̟̲̭͔͚̣͓̜̯͚̫̮͎̐ͣ̊̒ͤ̽̊̍̾́̔͠c̤̟̬̗̙̘̼̯̠̭̬̀̎̂̽̏̎̿ͣ̀͟t̵̴̗̗̫̯̊̆͒̓ͯͫ̈͝ͅį̗̝̫̰̠̺͈̹͍͓̠̳̟̻̟̙̫̋̐̅̎͋̂̽̔̍ͨͦ̽ͬ̃ͅò̸̡͎̱̫͙̭̞͖͕͓͔͚͎̺͖̫̖̲͎̗ͦͨͤͫ̓͑̽͋͆͂̽̓ͤ̿͗̽̿̀n̢͖̝̩̯̣̦̩̹̞̩̬̿͆̾͐͋̓̉͋̒́͡ ͙̹͎̟̞͍̱̹̩ͥ̉̽ͤͪͦ̒͊̅ͫͬ̔̉͂̓̄͢͡f̵̻̺͓̠̱͖̦̼̼̱̝̞̼̣̖̙̜̙̩̂̔ͬ̊ͯ͂ͯ̔ͯ̇̀̅͊ͪ̂̾̓̋ō̢̥͖̠̺̳̰͖͔̘͕̪̪͕̼̞̂ͦͣͩ͛ͤ̈̐ͩͣ̇̌͋̌͝ǘ̶̵̢͖̹̦̥̜̇̓ͯ̅ͯ͌̅̒̀n̸̡̧̡̺̠̦̞̠̬̞̯̭͚̬̳̮̺̲̟̍ͭ̎ͨ̔̊ͦ̽ͧ́dͥͪ̎͐̌͒͑̈̏̎̓̊̈́ͩͪ̐͂̆̀҉̫̻̰̙͖͈̙̠͍̮͈͕̻̦͕̼̤ͅͅ §r §4§k||||||§r"));
                    
                    // Play a creepy sound
                    soundEvent.playHorrorWhisper(serverPlayer);
                }));
                break;
            case 1:
                // Read the player's real OS/username with more terrifying messages
                String fakeOs = random.nextBoolean() ? "Windows" : "MacOS";
                String fakeUsername = serverPlayer.getName().getString() + "_" + random.nextInt(100);
                
                // Make the message more disturbing
                serverPlayer.sendSystemMessage(Component.literal("§4§lSYSTEM BREACH DETECTED: " + fakeOs + " user: " + fakeUsername + " §kxxxxx"));
                
                // Schedule a more intense screen glitch
                serverPlayer.server.tell(new net.minecraft.server.TickTask(
                        serverPlayer.server.getTickCount() + 20, () -> {
                    NetworkHandler.sendToPlayer(
                            new GlitchScreenPacket(
                                    3, // EFFECT_INVERT
                                    0.95f, // Extremely noticeable
                                    40), // 2 seconds 
                            serverPlayer);
                    
                    // Add creepy follow-up message after glitch
                    serverPlayer.server.tell(new net.minecraft.server.TickTask(
                            serverPlayer.server.getTickCount() + 60, () -> {
                        serverPlayer.sendSystemMessage(Component.literal("§4§oI know where you live...§r"));
                    }));
                }));
                break;
            case 2:
                // Show "recording" message with more sinister implications
                serverPlayer.sendSystemMessage(Component.literal("§8[§4§lRECORDING ACTIVE§8] §4User data collection in progress..."));
                
                // Send more severe texture swap to make the world feel altered
                textureEvent.sendTextureSwapPacket(serverPlayer);
                
                // Add screen visual corruption
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                1, // EFFECT_DISTORT
                                0.7f, // Quite noticeable
                                60), // 3 seconds
                        serverPlayer);
                break;
            case 3:
                // Show "file corrupted" message with more urgency
                serverPlayer.sendSystemMessage(Component.literal("§4[§k|||§r §4§lCRITICAL WARNING§r §4§k|||§r] §c§lWorld file corruption detected and spreading: " + 
                        serverPlayer.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)));
                serverPlayer.sendSystemMessage(Component.literal("§4[SYSTEM] §cAttempting emergency recovery...§4§oERROR: CORRUPTION SPREADING TO SYSTEM FILES"));
                
                // Send a heavier screen corruption effect
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                2, // EFFECT_CORRUPT
                                0.95f, // Extreme corruption
                                100), // 5 seconds 
                        serverPlayer);
                
                // Play distorted sound
                soundEvent.playDistortedSound(serverPlayer);
                break;
            case 4:
                // Use player's IP address (fake for demonstration) with more threatening message
                String fakeIp = "192.168." + random.nextInt(255) + "." + random.nextInt(255);
                
                // Show fake "found you" message with even more disturbing text
                serverPlayer.sendSystemMessage(Component.literal("§4§lÍ̸̘͕͆̑̾̍̓̀͆̑ ̶̢̠̪̹͉̳̫̭̜̣͒͌̀́͊̿͆̒̽̒̏s̴̛̲̯̣̘̜̤̈́̊͌̽̅͐̕̚͝ȩ̸̖̬̯̺̩̜̑̏͊̐̓̓̓̐̚͘ȇ̷̢̛̙̙̞̙͓̪̄͛͒̿̂̔͘ ̸̨̙̪͙̮͙̟̬̤̣̈́̄̌̈́̍̓̇̓̚ỳ̵̬̘̲͖̰͇̮̣̤̠̽̇̔̓̐̌o̵̡͍̬̝̐͆́͂̿̾̑̽̚͠ư̶̦̪͍̳̰̾̏̈́̿̈́̊̓̈́͜r̶̳̰̞̮̫̯̮̮͂͂̀̔̐̍͒͜͜͠ ̶̢̩̲̞̞̱̙̗̹̳̽͆̔̇̋͌̀̃̚I̶̡̨̧̥̞͍̺̖͉̿͛̓̎̒́̈̌͝P̶̘̹͔̦̈́̐͒̈̂̇̉̓̃:̵̪̥͕̠̠̹̫͖̃̌͐̏̇͆̆̆̕ " + fakeIp + " §4W̷̡̡̛̪̙̪̜̲̎̄̊̓̔̍̄̀͘ē̸̡̧̥̼̺͕͂̓̾͆̔̍̀̏͝'̴̡̡̡̱̺̮͋̓̽͌̂̀̒̀͘͠r̵̛̝̙̖̲̦̩̙̈́̏̓͊̃̒̕͝͝e̶̢̨̮̯̤̱̪̰̟̤̿̓̐̔̌͂͠ ̴̛̫̹̙̽̀̅͑̐̍͒͝c̶̜̩̜͚̙̀̅̑̔̏̓̈͝͠͝ō̷̢̦̪̰̞̍̏̈́̉̂̈̔̿̚m̸̢̝̭̮̝̳̭̦̱̀̿̂̿̐͋̇i̷̡̭̳̙̽͂̓͋̌̈́̿͝n̴̘̬͙̭̺̊̑̔̊͆̆̓̏̕͝g̴̘̘̺͎͉̪̜̲̥̔̇̊̀̍̂̈̀͠͝ ̸̡̫̝͇̞̲̎͋͊̏͂͊̿̉͝f̵̢̛̛͚͔̙͍͚̠̰̃̑͊̾̍̽̕̚ơ̸̡̢̧̮̲̣̫̋̓̋̽̇̽͆͘͜r̵̛͓̘̥͎̘̥̓̾̃̂͆̿̍͛͘ ̵̡̡̯̤̘͇̽̐̽̑́̂̄̏̃̕y̵̰̟̯̹̙̩̽̇̿̊͂̀̀̚͠ͅö̴̦̪͓̦̱͚̳̞́̀̈̃̂̎̎̚͝ͅu̶̢̧̥̞̪̿͆̄͋̄̄̈́̍̉̕"));
                
                // Play heartbeat sound with increased intensity
                soundEvent.playHeartbeat(serverPlayer);
                
                // Add screen pulse effect
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                0, // EFFECT_STATIC
                                0.7f, // Medium intensity
                                80), // 4 seconds
                        serverPlayer);
                break;
            case 5:
                // ENHANCED: Send a fake crash report with more severe effects first
                
                // Play a warning sound first
                soundEvent.playSystemError(serverPlayer);
                
                // Brief screen flash
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                0, // EFFECT_STATIC
                                1.0f, // Maximum intensity
                                10), // Brief flash
                        serverPlayer);
                
                // Short delay then crash
                serverPlayer.server.tell(new net.minecraft.server.TickTask(
                        serverPlayer.server.getTickCount() + 15, () -> {
                    // Send the crash packet with a random message
                    String crashMessage = CRASH_MESSAGES[random.nextInt(CRASH_MESSAGES.length)];
                    NetworkHandler.sendToPlayer(
                            new FakeCrashPacket(crashMessage),
                            serverPlayer);
                }));
                
                // Reset entity spawn counter since this is a severe event
                eventsSinceLastEntitySpawn = 0;
                break;
            case 6:
                // ENHANCED: Manipulate player's inventory with more severity
                // Send a message first about system breach
                serverPlayer.sendSystemMessage(Component.literal("§4§l[SYSTEM BREACH] §cInventory control compromised..."));
                
                // Screen distortion
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                1, // EFFECT_DISTORT
                                0.8f, // High intensity
                                40), // 2 seconds
                        serverPlayer);
                
                // Shift inventory items with more random shuffling
                NetworkHandler.sendToPlayer(
                        new InventoryShiftPacket(),
                        serverPlayer);
                
                // Play unsettling sound
                soundEvent.playHorrorWhisper(serverPlayer);
                break;
            case 7:
                // ENHANCED: Add a mysterious item to inventory with more frightening attributes
                ItemStack mysteryItem = new ItemStack(Items.PAPER);
                mysteryItem.setHoverName(Component.literal("§4§lẂ̷̡̡̯̪̞̙̦͂̎̓̿͊̏̓͜A̶̧̛̲̮̼̙͕͌̌̑̑͌̎͝R̸̨̭̲̤̲̪̝͙̓̋̊̂̈́̌̀̿̓͜N̸̢̢͚̺͖̜̩̖̹̔̒̍̈́̽͂̀̾̒I̶̡̖̦̲̣̗̠̐̉́̇̾̈̂̕͠͝N̸̡̧̛̻̹͙̙̳̜̉̂̈́̅͂͌̐̌G̶̨̡̢̧͖̮̖̖̯̐̎̊̊̄͑̿̚͠:̵̢̨̛͔̮̺̆̈́̀͂̆̐͘ ̶̝̣̺̘̬̱̦͎̫̃̓̊̂̒͌͗̕͝D̷͉̜̥̬̦͖̰̀̀͌̉̍̽̒̅͘Ọ̸̧̡̫̬̌̿̍̃̂̐̍̾̈́̂ ̶͖̘̯̺̰̀̋̽̓̾̈́͐̀̆͒N̶̢̡̧̺̱̰̪̣̳̈̍̓̀́͊̚͝͝Ô̵̧̢̝̪̩̍͊̎̔͊̔̆̄̚Ṫ̷̨̢̢̛̝̳̜̯̀̊͐̑̕̚͜͝ ̸̛̙̺̤̳̱̤͙͑̿̈́̓̏͆̈́̚͜D̶̨̟̱̰̱̩͓̪̣̿̓̍̌̎̓̐̎͝I̵̧̧̱̰̗̬̲̝̽̓̎̐̓͒̐̿̃E̴̡̤̫̫̤̘͕̖̱͒̆͂͆͋̐̓̕͝"));
                
                // Add lore with more terrifying text
                mysteryItem.getOrCreateTag().putString("Lore", "§8§oI've been watching you for so long... I'm getting closer with each death... §4§lDON'T DIE AGAIN");
                
                // Add to player inventory
                serverPlayer.getInventory().add(mysteryItem);
                
                // Send an unsettling message with screen effects
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                2, // EFFECT_CORRUPT
                                0.6f, // Medium corruption
                                30), // 1.5 seconds
                        serverPlayer);
                
                serverPlayer.sendSystemMessage(Component.literal("§4§oI've left you a warning... §kxxxxxx§r"));
                
                // Mark that we spawned an entity-related event
                eventsSinceLastEntitySpawn = 0;
                break;
            case 8:
                // ENHANCED: Fake screenshot with entity and more effects
                // We don't actually take a screenshot but simulate the effect
                String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss").format(java.time.LocalDateTime.now());
                String separator = System.getProperty("file.separator");
                if (separator.equals("\\")) {
                    // Windows format (single message like in vanilla Minecraft)
                    serverPlayer.sendSystemMessage(Component.literal("§aScreenshot saved as §escreenshots\\" + timestamp + ".png"));
                } else {
                    // Unix format (single message like in vanilla Minecraft)
                    serverPlayer.sendSystemMessage(Component.literal("§aScreenshot saved as §escreenshots/" + timestamp + ".png"));
                }
                
                // Add a severe screen glitch before the reveal
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                0, // EFFECT_STATIC
                                0.7f, // Medium intensity
                                15), // Quick flash
                        serverPlayer);
                
                // Spawn entity after effects
                serverPlayer.server.tell(new net.minecraft.server.TickTask(
                        serverPlayer.server.getTickCount() + 20, () -> {
                    entityEvent.spawnIterationGlimpse(serverPlayer);
                    
                    // Play a scary sound
                    soundEvent.playDistortedSound(serverPlayer);
                }));
                
                // Send a follow-up message after a short delay
                serverPlayer.server.tell(new net.minecraft.server.TickTask(
                        serverPlayer.server.getTickCount() + 100, () -> {
                    serverPlayer.sendSystemMessage(Component.literal("§4§lDeception detected! §4§oDid you see it in your screenshot? §4§oIt's always there..."));
                }));
                
                // Mark that we spawned an entity-related event
                eventsSinceLastEntitySpawn = 0;
                break;
            case 9:
                // NEW: Initialize attack sequence - make the player feel hunted
                // Play warning sounds first
                soundEvent.playSystemError(serverPlayer);
                
                // Send threatening message
                serverPlayer.sendSystemMessage(Component.literal("§4§k|||§r §4§lITERATION ATTACK SEQUENCE INITIATED§r §4§k|||§r"));
                
                // Screen corruption
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                2, // EFFECT_CORRUPT
                                0.9f, // High intensity
                                40), // 2 seconds
                        serverPlayer);
                
                // Spawn entity that will hunt the player
                serverPlayer.server.tell(new net.minecraft.server.TickTask(
                        serverPlayer.server.getTickCount() + 60, () -> {
                    // Spawn a full Iteration entity through the event system
                    ServerLevel level = serverPlayer.serverLevel();
                    
                    // Position the entity randomly around the player (not directly behind)
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double distance = 15 + random.nextDouble() * 5; // 15-20 blocks away
                    Vec3 offset = new Vec3(
                            Math.sin(angle) * distance,
                            0,
                            Math.cos(angle) * distance
                    );
                    Vec3 spawnPos = serverPlayer.position().add(offset);
                    
                    // Spawn the entity using the entity event
                    entityEvent.spawnHostileIteration(serverPlayer, spawnPos);
                    
                    // Send a final warning message
                    serverPlayer.server.tell(new net.minecraft.server.TickTask(
                            serverPlayer.server.getTickCount() + 20, () -> {
                        serverPlayer.sendSystemMessage(Component.literal("§4§oIt's coming for you..."));
                        
                        // Play heartbeat
                        soundEvent.playHeartbeat(serverPlayer);
                    }));
                }));
                
                // Mark that we spawned an entity-related event
                eventsSinceLastEntitySpawn = 0;
                break;
        }
    }
}