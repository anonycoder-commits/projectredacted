package net.tasuposed.projectredacted.command;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.tasuposed.projectredacted.entity.AngryProtocol37;
import net.tasuposed.projectredacted.entity.DistantStalker;
import net.tasuposed.projectredacted.entity.EntityRegistry;
import net.tasuposed.projectredacted.entity.InvisibleProtocol37;
import net.tasuposed.projectredacted.entity.Iteration;
import net.tasuposed.projectredacted.entity.MiningEntity;
import net.tasuposed.projectredacted.entity.Protocol_37;
import net.tasuposed.projectredacted.horror.HorrorManager;
import net.tasuposed.projectredacted.horror.HorrorStructureSpawner;
import net.tasuposed.projectredacted.horror.PlayerHorrorState;
import net.tasuposed.projectredacted.horror.events.EntityEvent;
import net.tasuposed.projectredacted.horror.events.HorrorSoundEvent;
import net.tasuposed.projectredacted.horror.stage.DisturbanceStage;
import net.tasuposed.projectredacted.horror.stage.FinalStage;
import net.tasuposed.projectredacted.horror.stage.HorrorStage;
import net.tasuposed.projectredacted.horror.stage.MetaStage;
import net.tasuposed.projectredacted.horror.stage.ObviousStage;
import net.tasuposed.projectredacted.horror.stage.SubtleStage;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.network.packets.FakeCrashPacket;
import net.tasuposed.projectredacted.network.packets.GlitchScreenPacket;
import net.tasuposed.projectredacted.network.packets.InventoryShiftPacket;

/**
 * Registers debug commands for testing horror entities and effects
 */
public class HorrorCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(HorrorCommands.class);
    
    // Screen effect constants
    private static final int STATIC = 0;
    private static final int DISTORTION = 1;
    private static final int CORRUPT = 2;
    private static final int INVERT = 3;
    
    // Sound effect constants
    private static final String AMBIENCE = "ambience";
    private static final String WHISPER = "whisper";
    private static final String HEARTBEAT = "heartbeat";
    private static final String GROWL = "growl";
    
    // Entity type constants
    private static final String ITERATION = "iteration";
    private static final String PROTOCOL_37 = "protocol_37";
    private static final String NATURAL_SPAWN = "natural";
    
    // New community entity constants
    private static final String INVISIBLE_PROTOCOL_37 = "invisible_protocol_37";
    private static final String DISTANT_STALKER = "distant_stalker";
    private static final String MINING_ENTITY = "mining_entity";
    private static final String ANGRY_PROTOCOL_37 = "angry_protocol_37";
    
    // Meta effect constants
    private static final String CRASH = "crash";
    private static final String INVENTORY_SHIFT = "inventory";
    private static final String MYSTERY_ITEM = "mystery_item";
    private static final String FAKE_SCREENSHOT = "screenshot";
    
    // Crash message constants
    private static final String[] CRASH_MESSAGES = {
        "Error: Player_Data_Corruption_Detected",
        "Fatal Error: Memory_Leak_In_Reality_Buffer",
        "System Failure: Entity_303_Protocol_Breach",
        "Critical Error: Player_Soul_Not_Found",
        "Catastrophic Failure: Reality_Injection_Detected",
        "HELP_ME_PLEASE_IM_TRAPPED",
        "THEYRE_WATCHING_YOU",
        "I_SEE_YOU_PLAYING"
    };
    
    // Track debug mode status
    private static boolean debugModeEnabled = false;
    private static boolean isDebugListenerRegistered = false;
    
    /**
     * Register all commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Create a simple test command
        LiteralArgumentBuilder<CommandSourceStack> testCommand =
                Commands.literal("projectREDACTEDtest")
                    .requires(source -> true)  // Everyone can use this one
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> Component.literal("§aHorror test command is working!"), false);
                        context.getSource().sendSuccess(() -> Component.literal("§aTry §6/projectREDACTED entity <entity_type>§a to spawn entities"), false);
                        context.getSource().sendSuccess(() -> Component.literal("§aAvailable community entities: §6invisible_protocol_37, distant_stalker, mining_entity, angry_protocol_37"), false);
                        context.getSource().sendSuccess(() -> Component.literal("§aTry §6/projectREDACTED entity transform_protocol_37§a to see Protocol 37 transform into angry form"), false);
                        return 1;
                    });
        
        // Register the test command
        dispatcher.register(testCommand);
        
        // Debug command
        LiteralArgumentBuilder<CommandSourceStack> debugCommand = Commands.literal("projectREDACTEDdebug")
            .requires(source -> source.hasPermission(2)) // Require permission level 2 (ops)
            .then(Commands.literal("monitor")
                .executes(context -> {
                    toggleDebugMode(context.getSource());
                    return 1;
                })
            )
            .then(Commands.literal("status")
                .executes(context -> {
                    showDebugStatus(context.getSource());
                    return 1;
                })
            )
            .then(Commands.literal("events")
                .executes(context -> {
                    checkRecentEvents(context.getSource());
                    return 1;
                })
            );
        
        // Register the debug command
        dispatcher.register(debugCommand);
        
        // Stage events command
        LiteralArgumentBuilder<CommandSourceStack> stageCommand = Commands.literal("stage")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("subtle").executes(context -> runStageEvent(context.getSource(), new SubtleStage())))
            .then(Commands.literal("disturbance").executes(context -> runStageEvent(context.getSource(), new DisturbanceStage())))
            .then(Commands.literal("obvious").executes(context -> runStageEvent(context.getSource(), new ObviousStage())))
            .then(Commands.literal("meta").executes(context -> runStageEvent(context.getSource(), new MetaStage())))
            .then(Commands.literal("final").executes(context -> runStageEvent(context.getSource(), new FinalStage())))
            .then(Commands.literal("set")
                .then(Commands.argument("stage", IntegerArgumentType.integer(0, 4))
                    .executes(context -> setPlayerStage(context.getSource(), IntegerArgumentType.getInteger(context, "stage")))));
        
        // Screen effects command
        LiteralArgumentBuilder<CommandSourceStack> effectCommand = Commands.literal("effect")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("static").executes(context -> triggerScreenEffect(context.getSource(), STATIC, null)))
            .then(Commands.literal("distortion").executes(context -> triggerScreenEffect(context.getSource(), DISTORTION, null)))
            .then(Commands.literal("corrupt").executes(context -> triggerScreenEffect(context.getSource(), CORRUPT, null)))
            .then(Commands.literal("invert").executes(context -> triggerScreenEffect(context.getSource(), INVERT, null)));
        
        // Sound command
        LiteralArgumentBuilder<CommandSourceStack> soundCommand = Commands.literal("sound")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("ambience").executes(context -> playHorrorSound(context.getSource(), AMBIENCE)))
            .then(Commands.literal("whisper").executes(context -> playHorrorSound(context.getSource(), WHISPER)))
            .then(Commands.literal("heartbeat").executes(context -> playHorrorSound(context.getSource(), HEARTBEAT)))
            .then(Commands.literal("growl").executes(context -> playHorrorSound(context.getSource(), GROWL)));
        
        // Entity command
        LiteralArgumentBuilder<CommandSourceStack> entityCommand = Commands.literal("entity")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("iteration").executes(context -> spawnEntity(context.getSource(), ITERATION)))
            .then(Commands.literal("protocol_37").executes(context -> spawnEntity(context.getSource(), PROTOCOL_37)))
            .then(Commands.literal("invisible_protocol_37").executes(context -> spawnEntity(context.getSource(), INVISIBLE_PROTOCOL_37)))
            .then(Commands.literal("distant_stalker").executes(context -> spawnEntity(context.getSource(), DISTANT_STALKER)))
            .then(Commands.literal("mining_entity").executes(context -> spawnEntity(context.getSource(), MINING_ENTITY)))
            .then(Commands.literal("angry_protocol_37").executes(context -> spawnEntity(context.getSource(), ANGRY_PROTOCOL_37)))
            .then(Commands.literal("transform_protocol_37").executes(context -> transformProtocol37(context.getSource())))
            .then(Commands.literal("naturalspawn")
                .then(Commands.literal("iteration").executes(context -> naturalSpawnEntity(context.getSource(), ITERATION)))
                .then(Commands.literal("protocol_37").executes(context -> naturalSpawnEntity(context.getSource(), PROTOCOL_37)))
                .then(Commands.literal("invisible_protocol_37").executes(context -> naturalSpawnEntity(context.getSource(), INVISIBLE_PROTOCOL_37)))
                .then(Commands.literal("distant_stalker").executes(context -> naturalSpawnEntity(context.getSource(), DISTANT_STALKER)))
                .then(Commands.literal("mining_entity").executes(context -> naturalSpawnEntity(context.getSource(), MINING_ENTITY)))
                .then(Commands.literal("angry_protocol_37").executes(context -> naturalSpawnEntity(context.getSource(), ANGRY_PROTOCOL_37))));
        
        // Meta effects command (new)
        LiteralArgumentBuilder<CommandSourceStack> metaCommand = Commands.literal("meta")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("crash").executes(context -> triggerMetaEffect(context.getSource(), CRASH)))
            .then(Commands.literal("inventory").executes(context -> triggerMetaEffect(context.getSource(), INVENTORY_SHIFT)))
            .then(Commands.literal("mystery_item").executes(context -> triggerMetaEffect(context.getSource(), MYSTERY_ITEM)))
            .then(Commands.literal("screenshot").executes(context -> triggerMetaEffect(context.getSource(), FAKE_SCREENSHOT)));
        
        // Main command with all subcommands
        LiteralArgumentBuilder<CommandSourceStack> rootCommand = Commands.literal("projectREDACTED")
            .requires(source -> source.hasPermission(2))
            .then(stageCommand)
            .then(effectCommand)
            .then(soundCommand)
            .then(entityCommand)
            .then(metaCommand)
            .then(Commands.literal("reset")
                .executes(context -> resetPlayerState(context.getSource())))
            .then(Commands.literal("spawn_structure")
                .requires(source -> source.hasPermission(2))
                .executes(context -> spawnTestStructure(context.getSource(), -1))
                .then(Commands.argument("type", IntegerArgumentType.integer(0, 39))
                    .executes(context -> spawnTestStructure(context.getSource(), 
                        IntegerArgumentType.getInteger(context, "type")))));
        
        dispatcher.register(rootCommand);
    }
    
    /**
     * Toggle debug mode for monitoring horror events
     */
    private static void toggleDebugMode(CommandSourceStack source) {
        debugModeEnabled = !debugModeEnabled;
        
        if (debugModeEnabled) {
            source.sendSuccess(() -> Component.literal("§aHorror debug mode §2ENABLED§a. Natural events will be logged in chat."), true);
            
            // Register event listener if not already registered
            if (!isDebugListenerRegistered) {
                registerDebugListener();
                isDebugListenerRegistered = true;
            }
        } else {
            source.sendSuccess(() -> Component.literal("§cHorror debug mode §4DISABLED§c."), true);
        }
    }
    
    /**
     * Show current debug status
     */
    private static void showDebugStatus(CommandSourceStack source) {
        if (debugModeEnabled) {
            source.sendSuccess(() -> Component.literal("§aHorror debug mode is currently §2ENABLED§a."), false);
        } else {
            source.sendSuccess(() -> Component.literal("§cHorror debug mode is currently §4DISABLED§c."), false);
        }
        
        // Get event stats from HorrorManager
        HorrorManager manager = HorrorManager.getInstance();
        if (manager != null) {
            source.sendSuccess(() -> Component.literal("§dLast 10 minutes: §r" + manager.getEventCountLastPeriod() + " events"), false);
        }
    }
    
    /**
     * Register the debug event listener
     */
    private static void registerDebugListener() {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
                if (!debugModeEnabled || event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
                    return;
                }
                
                // Check for events every second (20 ticks)
                if (event.getServer().getTickCount() % 20 == 0) {
                    HorrorManager manager = HorrorManager.getInstance();
                    if (manager != null && manager.hadEventThisTick()) {
                        notifyOpsOfEvent(event.getServer(), manager.getLastEventInfo());
                    }
                }
            }
        });
    }
    
    /**
     * Check for recent horror events
     */
    private static int checkRecentEvents(CommandSourceStack source) {
        HorrorManager manager = HorrorManager.getInstance();
        if (manager != null) {
            String[] recentEvents = manager.getRecentEvents();
            
            if (recentEvents.length == 0) {
                source.sendSuccess(() -> Component.literal("§dNo recent horror events recorded."), false);
            } else {
                source.sendSuccess(() -> Component.literal("§dRecent horror events:"), false);
                for (String event : recentEvents) {
                    source.sendSuccess(() -> Component.literal("§5- §r" + event), false);
                }
            }
        } else {
            source.sendFailure(Component.literal("§cHorror manager not available."));
        }
        
        return 1;
    }
    
    /**
     * Notify server operators of horror events
     */
    private static void notifyOpsOfEvent(net.minecraft.server.MinecraftServer server, String eventInfo) {
        if (eventInfo == null) {
            eventInfo = "Unknown event";
        }
        
        final String finalEventInfo = eventInfo;
        
        // Send notification to all operators
        server.getPlayerList().getPlayers().forEach(player -> {
            if (player.hasPermissions(2)) {
                player.sendSystemMessage(Component.literal("§5[Horror Debug]§r §d" + finalEventInfo));
            }
        });
    }
    
    /**
     * Summon Iteration entity
     */
    private static int summonIteration(CommandSourceStack source, Collection<ServerPlayer> players) {
        try {
            ServerLevel level = source.getLevel();
            Vec3 pos = source.getPosition();
            
            // Use our registered entity type to spawn a real Iteration entity
            Iteration entity = new Iteration(EntityRegistry.ITERATION.get(), level);
            entity.setPos(pos.x(), pos.y(), pos.z());
            level.addFreshEntity(entity);
            
            source.sendSuccess(() -> Component.literal("§aSpawned Iteration entity"), true);
            
            // If players specified, notify them
            if (players != null && !players.isEmpty()) {
                players.forEach(player -> source.sendSuccess(() -> 
                    Component.literal("§aIteration entity summoned near " + player.getName().getString()), true));
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError spawning Iteration entity: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Summon Protocol_37 entity
     */
    private static int summonProtocol37(CommandSourceStack source, Collection<ServerPlayer> players) {
        try {
            ServerLevel level = source.getLevel();
            Vec3 pos = source.getPosition();
            
            // Use our registered entity type to spawn a real Protocol_37 entity
            Protocol_37 entity = new Protocol_37(EntityRegistry.PROTOCOL_37.get(), level);
            entity.setPos(pos.x(), pos.y(), pos.z());
            level.addFreshEntity(entity);
            
            source.sendSuccess(() -> Component.literal("§aSpawned Protocol_37 entity"), true);
            
            // If players specified, notify them
            if (players != null && !players.isEmpty()) {
                players.forEach(player -> source.sendSuccess(() -> 
                    Component.literal("§aProtocol_37 entity summoned near " + player.getName().getString()), true));
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError spawning Protocol_37 entity: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Trigger a stage event
     */
    private static int runStageEvent(CommandSourceStack source, HorrorStage stage) {
        try {
            // Try to get the player who executed the command
            if (source.getEntity() instanceof ServerPlayer player) {
                stage.triggerRandomEvent(player);
                source.sendSuccess(() -> Component.literal("§aTriggered " + stage.getClass().getSimpleName() + 
                    " for you"), true);
            } else {
                source.sendFailure(Component.literal("§cMust execute as player"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError: " + e.getMessage()));
            LOGGER.error("Error triggering stage event", e);
            return 0;
        }
        return 1;
    }
    
    /**
     * Trigger a screen effect
     */
    private static int triggerScreenEffect(CommandSourceStack source, int type, 
            Collection<ServerPlayer> players) {
        try {
            // If players specified, apply to them
            if (players != null && !players.isEmpty()) {
                for (ServerPlayer player : players) {
                    NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(type, 1.0f, 300), player);
                    source.sendSuccess(() -> Component.literal("§aSent screen effect type " + type + 
                        " to " + player.getName().getString()), true);
                }
            } else {
                // Try to get the player who executed the command
                if (source.getEntity() instanceof ServerPlayer player) {
                    NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(type, 1.0f, 300), player);
                    source.sendSuccess(() -> Component.literal("§aSent screen effect type " + type + " to you"), true);
                } else {
                    source.sendFailure(Component.literal("§cMust specify players or execute as player"));
                    return 0;
                }
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError triggering screen effect: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Play a horror sound
     */
    private static int playHorrorSound(CommandSourceStack source, String soundType) {
        return playSoundEffect(source, soundType, null);
    }
    
    /**
     * Spawn a horror entity
     */
    private static int spawnEntity(CommandSourceStack source, String entityType) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }
        
        ServerPlayer player = (ServerPlayer) source.getEntity();
        ServerLevel level = player.serverLevel();
        
        // Spawn position - in front of the player
        Vec3 lookVec = player.getLookAngle();
        double x = player.getX() + lookVec.x * 5;
        double y = player.getY();
        double z = player.getZ() + lookVec.z * 5;
        
        // Identify the entity type and spawn it
        boolean success = false;
        
        switch(entityType) {
            case ITERATION:
                Iteration iteration = EntityRegistry.ITERATION.get().create(level);
                if (iteration != null) {
                    iteration.moveTo(x, y, z, 0, 0);
                    level.addFreshEntity(iteration);
                    success = true;
                }
                break;
                
            case PROTOCOL_37:
                Protocol_37 protocol37 = EntityRegistry.PROTOCOL_37.get().create(level);
                if (protocol37 != null) {
                    protocol37.moveTo(x, y, z, 0, 0);
                    level.addFreshEntity(protocol37);
                    success = true;
                }
                break;
                
            case INVISIBLE_PROTOCOL_37:
                InvisibleProtocol37 invisProto = EntityRegistry.INVISIBLE_PROTOCOL_37.get().create(level);
                if (invisProto != null) {
                    invisProto.moveTo(x, y, z, 0, 0);
                    invisProto.setInvisible(true); // Ensure it's invisible immediately
                    level.addFreshEntity(invisProto);
                    
                    // Add particle effect to indicate where the invisible entity spawned
                    level.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.SMOKE,
                        x, y + 1, z,
                        20, 0.5, 0.5, 0.5, 0.02
                    );
                    success = true;
                }
                break;
                
            case DISTANT_STALKER:
                DistantStalker stalker = EntityRegistry.DISTANT_STALKER.get().create(level);
                if (stalker != null) {
                    stalker.moveTo(x, y, z, 0, 0);
                    level.addFreshEntity(stalker);
                    success = true;
                }
                break;
                
            case MINING_ENTITY:
                MiningEntity miner = EntityRegistry.MINING_ENTITY.get().create(level);
                if (miner != null) {
                    miner.moveTo(x, y, z, 0, 0);
                    level.addFreshEntity(miner);
                    success = true;
                }
                break;
                
            case ANGRY_PROTOCOL_37:
                AngryProtocol37 angryProto = EntityRegistry.ANGRY_PROTOCOL_37.get().create(level);
                if (angryProto != null) {
                    angryProto.moveTo(x, y, z, 0, 0);
                    angryProto.setGlowingTag(true); // Ensure glowing effect immediately
                    level.addFreshEntity(angryProto);
                    
                    // Add sound effect for the angry form spawning
                    level.playSound(null, 
                        new BlockPos((int)x, (int)y, (int)z), 
                        SoundEvents.ENDERMAN_STARE, 
                        SoundSource.HOSTILE, 
                        1.0F, 0.5F
                    );
                    success = true;
                }
                break;
        }
        
        if (success) {
            source.sendSuccess(() -> Component.literal("§aSpawned " + entityType + " entity"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to spawn " + entityType));
            return 0;
        }
    }
    
    /**
     * Play a sound effect
     */
    private static int playSoundEffect(CommandSourceStack source, String soundType, Collection<ServerPlayer> players) {
        try {
            HorrorSoundEvent soundEvent = new HorrorSoundEvent();
            
            // If players specified, apply to them
            if (players != null && !players.isEmpty()) {
                for (ServerPlayer player : players) {
                    applySoundToPlayer(soundEvent, soundType, player);
                    source.sendSuccess(() -> Component.literal("§aPlayed " + soundType + " sound to " + 
                        player.getName().getString()), true);
                }
            } else {
                // Try to get the player who executed the command
                if (source.getEntity() instanceof ServerPlayer player) {
                    applySoundToPlayer(soundEvent, soundType, player);
                    source.sendSuccess(() -> Component.literal("§aPlayed " + soundType + " sound to you"), true);
                } else {
                    source.sendFailure(Component.literal("§cMust specify players or execute as player"));
                    return 0;
                }
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError playing sound effect: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Apply a sound to a player
     */
    private static void applySoundToPlayer(HorrorSoundEvent soundEvent, String soundType, ServerPlayer player) {
        switch (soundType) {
            case WHISPER:
                soundEvent.playWhisper(player);
                break;
            case AMBIENCE:
                soundEvent.playDistantSound(player);
                break;
            case HEARTBEAT:
                soundEvent.playHeartbeat(player);
                break;
            case GROWL:
                soundEvent.playGrowl(player);
                break;
            default:
                // Handle any other sound types that might be added in the future
                LOGGER.warn("Unknown sound type: {}", soundType);
                break;
        }
    }
    
    /**
     * Trigger an entity effect
     */
    private static int triggerEntityEffect(CommandSourceStack source, String effectType, 
            Collection<ServerPlayer> players) {
        try {
            EntityEvent entityEvent = new EntityEvent();
            
            // If players specified, apply to them
            if (players != null && !players.isEmpty()) {
                for (ServerPlayer player : players) {
                    applyEntityEffectToPlayer(entityEvent, effectType, player);
                    source.sendSuccess(() -> Component.literal("§aApplied " + effectType + " entity effect to " + 
                        player.getName().getString()), true);
                }
            } else {
                // Try to get the player who executed the command
                if (source.getEntity() instanceof ServerPlayer player) {
                    applyEntityEffectToPlayer(entityEvent, effectType, player);
                    source.sendSuccess(() -> Component.literal("§aApplied " + effectType + " entity effect to you"), true);
                } else {
                    source.sendFailure(Component.literal("§cMust specify players or execute as player"));
                    return 0;
                }
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError triggering entity effect: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Apply an entity effect to a player
     */
    private static void applyEntityEffectToPlayer(EntityEvent entityEvent, String effectType, ServerPlayer player) {
        switch (effectType) {
            case "shadow":
                entityEvent.spawnTemporaryShadowFigure(player);
                break;
            case "glimpse":
                entityEvent.spawnIterationGlimpse(player);
                break;
            case "protocol37":
                entityEvent.spawnDistantProtocol37(player);
                break;
        }
    }
    
    /**
     * Set a player's horror event frequency
     */
    private static int setPlayerFrequency(CommandSourceStack source, float frequency) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("§cThis command can only be used by players."));
            return 0;
        }
        
        // Get the player's horror state and update frequency
        HorrorManager manager = HorrorManager.getInstance();
        PlayerHorrorState state = manager.getPlayerState(player);
        state.setFrequencyModifier(frequency);
        
        // Notify the player
        String formattedPercent = String.format("%.0f", frequency * 100);
        if (frequency <= 0.0f) {
            source.sendSuccess(() -> Component.literal("§aHorror events §4disabled§a for you."), false);
        } else if (frequency < 1.0f) {
            source.sendSuccess(() -> Component.literal("§aHorror event frequency set to §6" + formattedPercent + "%§a of normal."), false);
        } else if (frequency == 1.0f) {
            source.sendSuccess(() -> Component.literal("§aHorror event frequency set to §6normal§a."), false);
        } else {
            source.sendSuccess(() -> Component.literal("§aHorror event frequency set to §c" + formattedPercent + "%§a of normal. §cSpooky!"), false);
        }
        
        return 1;
    }
    
    /**
     * Show a player's current horror event frequency
     */
    private static int showPlayerFrequency(CommandSourceStack source) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("§cThis command can only be used by players."));
            return 0;
        }
        
        // Get the player's horror state
        HorrorManager manager = HorrorManager.getInstance();
        PlayerHorrorState state = manager.getPlayerState(player);
        float frequency = state.getFrequencyModifier();
        
        // Show current setting
        String formattedPercent = String.format("%.0f", frequency * 100);
        if (frequency <= 0.0f) {
            source.sendSuccess(() -> Component.literal("§aYour horror events are currently §4disabled§a."), false);
        } else if (frequency < 1.0f) {
            source.sendSuccess(() -> Component.literal("§aYour horror event frequency is set to §6" + formattedPercent + "%§a of normal."), false);
        } else if (frequency == 1.0f) {
            source.sendSuccess(() -> Component.literal("§aYour horror event frequency is set to §6normal§a."), false);
        } else {
            source.sendSuccess(() -> Component.literal("§aYour horror event frequency is set to §c" + formattedPercent + "%§a of normal."), false);
        }
        
        return 1;
    }

    private static int setPlayerStage(CommandSourceStack source, int stageNumber) {
        if (source.getEntity() instanceof ServerPlayer player) {
            PlayerHorrorState playerState = HorrorManager.getInstance().getPlayerState(player);
            
            String stageName;
            switch (stageNumber) {
                case 0:
                    stageName = "Subtle";
                    break;
                case 1:
                    stageName = "Disturbance";
                    break;
                case 2:
                    stageName = "Obvious";
                    break;
                case 3:
                    stageName = "Meta";
                    break;
                case 4:
                    stageName = "Final";
                    break;
                default:
                    stageName = "Unknown";
                    break;
            }
            
            playerState.setCurrentStage(stageNumber);
            playerState.setLastStageAdvance(System.currentTimeMillis());
            
            source.sendSuccess(() -> Component.literal("Set horror stage to " + stageName + " (Stage " + stageNumber + ")"), true);
            return Command.SINGLE_SUCCESS;
        }
        
        source.sendFailure(Component.literal("Command must be executed by a player"));
        return 0;
    }

    /**
     * Reset a player's horror state to initial values
     */
    private static int resetPlayerState(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            PlayerHorrorState playerState = HorrorManager.getInstance().getPlayerState(player);
            playerState.reset();
            
            source.sendSuccess(() -> Component.literal("§aReset horror state to initial values. You are now at Subtle Stage (Stage 0)."), true);
            return Command.SINGLE_SUCCESS;
        }
        
        source.sendFailure(Component.literal("§cCommand must be executed by a player"));
        return 0;
    }

    /**
     * Trigger a meta horror effect for testing
     */
    private static int triggerMetaEffect(CommandSourceStack source, String effectType) {
        Collection<ServerPlayer> players;
        
        // Get the player to apply the effect to
        if (source.getEntity() instanceof ServerPlayer) {
            players = Collections.singleton((ServerPlayer) source.getEntity());
        } else {
            source.sendFailure(Component.literal("§cMust be run by a player"));
            return 0;
        }
        
        if (players.isEmpty()) {
            source.sendFailure(Component.literal("§cNo players to apply effect to"));
            return 0;
        }
        
        // Apply the specified meta effect to each player
        for (ServerPlayer player : players) {
            switch (effectType) {
                case CRASH:
                    // Send a random crash message
                    String crashMessage = CRASH_MESSAGES[player.level().getRandom().nextInt(CRASH_MESSAGES.length)];
                    NetworkHandler.sendToPlayer(new FakeCrashPacket(crashMessage), player);
                    source.sendSuccess(() -> Component.literal("§aSent fake crash screen to " + player.getName().getString()), true);
                    break;
                    
                case INVENTORY_SHIFT:
                    // Shift inventory items
                    NetworkHandler.sendToPlayer(new InventoryShiftPacket(), player);
                    source.sendSuccess(() -> Component.literal("§aShifted inventory items for " + player.getName().getString()), true);
                    break;
                    
                case MYSTERY_ITEM:
                    // Add mysterious item
                    ItemStack mysteryItem = new ItemStack(Items.PAPER);
                    mysteryItem.setHoverName(Component.literal("§4D̸̢̦̱̥͙̘̩͑̊͐̓̂̌͜o̷̢̗̼̬͔̤̱̠̓̿͊̇͊̽̕͜͡n̵̨̧̝̯̞̪̥̪͑͑́͐̆̃̋͆̚̕'̵̬̹̝͕͕̬̙̟̓̈́̓̿̈̌̚͟t̷̨̪͇̙̹̯͛́̓̅̀͌̄̍͟͢ R̢̡̫̮̗̅͊̏̌̌̚͝ȩ̝̯̪̜͙͐̓̍̄̄̊̄̀̚͝a̷̛̘̤̘̗̘̥̙̥͒̐̄̄̈̀͒̽d̴̪̱͉̬̰̭̤̏̇͑̽͑̽̂̿͋͟ T̷̢̗̗̗̦͚͌̇̈́̀̃̚͢͞ḩ̢͙̪̳̱̬̯̓̐̏̾͌̎̑͢͝͝į̸̟̳̮̥̪̮͔̱͙̓́͗͐̽̀̓͌̕͡s̨̫̻̫̥̳̿̓̐̄͆͛̓"));
                    mysteryItem.getOrCreateTag().putString("Lore", "§8§oI'm watching you play...");
                    player.getInventory().add(mysteryItem);
                    player.sendSystemMessage(Component.literal("§4§oI left you a gift..."));
                    source.sendSuccess(() -> Component.literal("§aAdded mystery item to " + player.getName().getString() + "'s inventory"), true);
                    break;
                    
                case FAKE_SCREENSHOT:
                    // Fake screenshot effect
                    String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss").format(java.time.LocalDateTime.now());
                    String separator = System.getProperty("file.separator");
                    
                    if (separator.equals("\\")) {
                        // Windows format (single message)
                        player.sendSystemMessage(Component.literal("§aScreenshot saved as §escreenshots\\" + timestamp + ".png"));
                    } else {
                        // Unix format (single message)
                        player.sendSystemMessage(Component.literal("§aScreenshot saved as §escreenshots/" + timestamp + ".png"));
                    }
                    
                    // Show entity glimpse after a short delay
                    player.server.tell(new net.minecraft.server.TickTask(
                            player.server.getTickCount() + 20, () -> {
                        EntityEvent entityEvent = new EntityEvent();
                        entityEvent.spawnIterationGlimpse(player);
                        
                        // Send a follow-up message
                        player.server.tell(new net.minecraft.server.TickTask(
                                player.server.getTickCount() + 60, () -> {
                            player.sendSystemMessage(Component.literal("§4§lDeception detected! §4§oCheck your screenshots..."));
                        }));
                    }));
                    
                    source.sendSuccess(() -> Component.literal("§aTriggered fake screenshot effect for " + player.getName().getString()), true);
                    break;
                    
                default:
                    source.sendFailure(Component.literal("§cUnknown meta effect type: " + effectType));
                    return 0;
            }
        }
        
        return 1;
    }

    private static int spawnTestStructure(CommandSourceStack source, int type) {
        if (source.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = source.getLevel();
            HorrorStructureSpawner.debugSpawnStructure(player, level, type);
            return Command.SINGLE_SUCCESS;
        }
        
        source.sendFailure(Component.literal("§cCommand must be executed by a player"));
        return 0;
    }

    /**
     * Try to naturally spawn entities in a way that mimics the normal spawn mechanism
     */
    private static int naturalSpawnEntity(CommandSourceStack source, String entityType) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }
        
        ServerPlayer player = (ServerPlayer) source.getEntity();
        ServerLevel level = player.serverLevel();
        
        // Count of successful spawns
        int spawnCount = 0;
        
        // Choose what to spawn based on entity type
        if (ITERATION.equals(entityType)) {
            // Spawn just iteration
            spawnCount = attemptNaturalSpawn(level, player, EntityRegistry.ITERATION.get(), "Iteration");
        } else if (PROTOCOL_37.equals(entityType)) {
            // Spawn just protocol37
            spawnCount = attemptNaturalSpawn(level, player, EntityRegistry.PROTOCOL_37.get(), "Protocol_37");
        } else if (INVISIBLE_PROTOCOL_37.equals(entityType)) {
            // Spawn invisible protocol37
            spawnCount = attemptNaturalSpawn(level, player, EntityRegistry.INVISIBLE_PROTOCOL_37.get(), "InvisibleProtocol37");
        } else if (DISTANT_STALKER.equals(entityType)) {
            // Spawn distant stalker
            spawnCount = attemptNaturalSpawn(level, player, EntityRegistry.DISTANT_STALKER.get(), "DistantStalker");
        } else if (MINING_ENTITY.equals(entityType)) {
            // Spawn mining entity
            spawnCount = attemptNaturalSpawn(level, player, EntityRegistry.MINING_ENTITY.get(), "MiningEntity");
        } else if (ANGRY_PROTOCOL_37.equals(entityType)) {
            // Spawn angry protocol37
            spawnCount = attemptNaturalSpawn(level, player, EntityRegistry.ANGRY_PROTOCOL_37.get(), "AngryProtocol37");
        }
        
        // Save the final spawn count for use in lambda
        final int finalSpawnCount = spawnCount;
        
        // Report results
        if (finalSpawnCount > 0) {
            source.sendSuccess(() -> Component.literal("§aSuccessfully spawned §6" + finalSpawnCount + "§a " + entityType + " entities"), true);
            return finalSpawnCount;
        } else {
            // If we couldn't find spawn locations, spawn directly near the player instead
            source.sendSuccess(() -> Component.literal("§eCouldn't find natural spawn locations, spawning near player instead"), false);
            return spawnEntity(source, entityType);
        }
    }
    
    /**
     * Attempt to naturally spawn a horror entity following spawn rules
     */
    private static int attemptNaturalSpawn(ServerLevel level, ServerPlayer player, EntityType<?> entityType, String entityName) {
        int spawnCount = 0;
        Random random = new Random();
        
        // Make multiple attempts to find valid spawn locations
        for (int attempt = 0; attempt < 30; attempt++) {
            // Calculate a randomized position
            double distance = 20.0 + random.nextDouble() * 60.0; // Between 20-80 blocks away
            double angle = random.nextDouble() * Math.PI * 2.0;
            double offsetX = Math.sin(angle) * distance;
            double offsetZ = Math.cos(angle) * distance;
            
            // Player position as starting point
            BlockPos playerPos = player.blockPosition();
            int x = playerPos.getX() + (int)offsetX;
            int z = playerPos.getZ() + (int)offsetZ;
            
            // Try various Y positions
            for (int yOffset = -10; yOffset <= 10; yOffset += 2) {
                int y = playerPos.getY() + yOffset;
                
                // Don't go below bedrock
                if (y < 1) continue;
                
                BlockPos spawnPos = new BlockPos(x, y, z);
                
                // Simple check for suitable spawn position - solid block below, air at position
                if (level.getBlockState(spawnPos.below()).isSolid() && 
                    level.getBlockState(spawnPos).isAir() && 
                    level.getBlockState(spawnPos.above()).isAir()) {
                    
                    // For MiningEntity, prefer underground
                    if (entityName.equals("MiningEntity") && level.canSeeSky(spawnPos)) {
                        continue; // Skip if not underground and trying to spawn MiningEntity
                    }
                    
                    // Try to spawn
                    if (entityType.spawn(level, spawnPos, MobSpawnType.COMMAND) != null) {
                        spawnCount++;
                        LOGGER.debug("Successfully naturally spawned {} at {}", entityName, spawnPos);
                        break; // Break from y-loop after successful spawn
                    }
                }
            }
            
            // Stop after spawning one entity successfully
            if (spawnCount > 0) {
                break;
            }
        }
        
        return spawnCount;
    }

    /**
     * Test the transformation of Protocol_37 to AngryProtocol37
     */
    private static int transformProtocol37(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }
        
        ServerPlayer player = (ServerPlayer) source.getEntity();
        ServerLevel level = player.serverLevel();
        
        // Spawn position - in front of the player
        Vec3 lookVec = player.getLookAngle();
        double x = player.getX() + lookVec.x * 5;
        double y = player.getY();
        double z = player.getZ() + lookVec.z * 5;
        
        // First spawn a normal Protocol_37
        Protocol_37 protocol37 = EntityRegistry.PROTOCOL_37.get().create(level);
        if (protocol37 != null) {
            protocol37.moveTo(x, y, z, 0, 0);
            level.addFreshEntity(protocol37);
            source.sendSuccess(() -> Component.literal("§aSpawned Protocol_37 that will transform in 3 seconds"), true);
            
            // Schedule the transformation to happen after a delay
            level.getServer().tell(new net.minecraft.server.TickTask(level.getServer().getTickCount() + 60, () -> {
                if (!protocol37.isRemoved()) {
                    protocol37.tryTransformToAngryForm(player);
                    level.playSound(null, 
                        new BlockPos((int)x, (int)y, (int)z), 
                        SoundEvents.ENDERMAN_TELEPORT, 
                        SoundSource.HOSTILE, 
                        1.0F, 0.5F
                    );
                    source.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("§cProtocol_37 transformed into its angry form!"), 
                        false
                    );
                }
            }));
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to spawn Protocol_37 for transformation test"));
            return 0;
        }
    }
} 