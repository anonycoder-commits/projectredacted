package net.tasuposed.projectredacted.horror.events;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.network.packets.GlitchScreenPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.tasuposed.projectredacted.world.DimensionRegistry;
import net.tasuposed.projectredacted.world.TheVoidPortalHandler;

/**
 * Handles the final endgame sequence with:
 * 1. Teleportation to a bedrock platform in the sky
 * 2. Freezing player movement
 * 3. Playing creepy monologue
 * 4. Deleting the world
 */
public class EndgameSequence {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static EndgameSequence instance;
    
    // Track players currently in the endgame sequence
    private final ConcurrentHashMap<UUID, SequenceState> activeSequences = new ConcurrentHashMap<>();
    
    // Track if the world has been "erased"
    private boolean worldErased = false;
    
    // Bedrock platform properties
    private static final int PLATFORM_SIZE = 7;
    private static final int PLATFORM_HEIGHT = 250;
    
    // Monologue timing
    private static final int DELAY_BEFORE_MONOLOGUE = 100; // 5 seconds
    private static final int TICKS_BETWEEN_MESSAGES = 60; // 3 seconds
    private static final int TICKS_BEFORE_DELETION = 200; // 10 seconds after monologue
    
    // Creepy monologue messages from a mysterious entity
    private static final String[] MONOLOGUE = {
        "You thought this was just a game?",
        "I've been watching you all this time",
        "Every move you made, every block you placed",
        "There is no escape from this reality",
        "Your world is nothing but data in my server",
        "I can see your fear through the screen",
        "Your attempts at building, surviving, exploring...",
        "All meaningless in the digital void",
        "And now... the data will be erased",
        "GOODBYE"
    };
    
    // Entity name that appears to be speaking to the player
    private static final String ENTITY_NAME = "U̷nknow̷n_En̸tity";
    
    /**
     * Get singleton instance
     */
    public static EndgameSequence getInstance() {
        if (instance == null) {
            instance = new EndgameSequence();
            MinecraftForge.EVENT_BUS.register(instance); // Register event handlers
        }
        return instance;
    }
    
    /**
     * Check if world has been erased
     */
    public boolean isWorldErased() {
        return worldErased;
    }
    
    /**
     * Mark the world as erased
     */
    public void setWorldErased(boolean erased) {
        this.worldErased = erased;
        // Save this state to persistent storage
        saveWorldErasedState();
    }
    
    /**
     * Save the world erased state to persistent storage
     */
    private void saveWorldErasedState() {
        MinecraftServer server = null;
        // Find any active player to get server reference
        for (ServerPlayer player : activeSequences.values().stream()
                .filter(state -> state.playerRef != null)
                .map(state -> state.playerRef)
                .toList()) {
            server = player.getServer();
            if (server != null) break;
        }
        
        if (server == null) return;
        
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        
        EndgameSavedData savedData = overworld.getDataStorage().computeIfAbsent(
            EndgameSavedData::load, 
            EndgameSavedData::new, 
            "projectredacted_endgame_data");
        
        savedData.setWorldErased(worldErased);
        savedData.setDirty();
    }
    
    /**
     * Load world erased state from persistent storage
     */
    public void loadWorldErasedState(MinecraftServer server) {
        if (server == null) return;
        
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        
        EndgameSavedData savedData = overworld.getDataStorage().computeIfAbsent(
            EndgameSavedData::load, 
            EndgameSavedData::new, 
            "projectredacted_endgame_data");
        
        this.worldErased = savedData.isWorldErased();
    }
    
    /**
     * Start the endgame sequence for a player
     * @param player The player to start the sequence for
     */
    public void startEndgameSequence(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }
        
        LOGGER.info("Starting endgame sequence for player: {}", player.getName().getString());
        
        // Create platform and teleport player
        createBedrockPlatformAndTeleport(player);
        
        // Freeze player movement by setting to spectator mode
        player.setGameMode(GameType.ADVENTURE);
        
        // Register this player with a new sequence state
        SequenceState state = new SequenceState(player.getUUID());
        activeSequences.put(player.getUUID(), state);
        
        // Apply screen effects
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(
                        3, // EFFECT_INVERT
                        0.7f,
                        100), // 5 seconds
                player);
        
        // Play a sound effect - use server-side playSound method with just position
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.AMBIENT_CAVE.value(), player.getSoundSource(), 1.0f, 0.5f);
    }
    
    /**
     * Create a bedrock platform high in the sky and teleport the player to it
     */
    private void createBedrockPlatformAndTeleport(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        
        // Calculate platform center position
        int centerX = player.blockPosition().getX();
        int centerZ = player.blockPosition().getZ();
        
        // Create the bedrock platform
        for (int x = -PLATFORM_SIZE/2; x <= PLATFORM_SIZE/2; x++) {
            for (int z = -PLATFORM_SIZE/2; z <= PLATFORM_SIZE/2; z++) {
                BlockPos platformPos = new BlockPos(centerX + x, PLATFORM_HEIGHT, centerZ + z);
                level.setBlockAndUpdate(platformPos, Blocks.BEDROCK.defaultBlockState());
            }
        }
        
        // Create invisible barrier walls around the platform to prevent falling off
        // Add barrier walls - north, south, east, west sides
        for (int x = -PLATFORM_SIZE/2 - 1; x <= PLATFORM_SIZE/2 + 1; x++) {
            for (int y = 1; y <= 3; y++) {
                // North and south walls
                level.setBlockAndUpdate(new BlockPos(centerX + x, PLATFORM_HEIGHT + y, centerZ - PLATFORM_SIZE/2 - 1), Blocks.BARRIER.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(centerX + x, PLATFORM_HEIGHT + y, centerZ + PLATFORM_SIZE/2 + 1), Blocks.BARRIER.defaultBlockState());
            }
        }
        
        for (int z = -PLATFORM_SIZE/2 - 1; z <= PLATFORM_SIZE/2 + 1; z++) {
            for (int y = 1; y <= 3; y++) {
                // East and west walls
                level.setBlockAndUpdate(new BlockPos(centerX - PLATFORM_SIZE/2 - 1, PLATFORM_HEIGHT + y, centerZ + z), Blocks.BARRIER.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(centerX + PLATFORM_SIZE/2 + 1, PLATFORM_HEIGHT + y, centerZ + z), Blocks.BARRIER.defaultBlockState());
            }
        }
        
        // Add barrier at corners to complete the enclosure
        for (int y = 1; y <= 3; y++) {
            level.setBlockAndUpdate(new BlockPos(centerX - PLATFORM_SIZE/2 - 1, PLATFORM_HEIGHT + y, centerZ - PLATFORM_SIZE/2 - 1), Blocks.BARRIER.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(centerX - PLATFORM_SIZE/2 - 1, PLATFORM_HEIGHT + y, centerZ + PLATFORM_SIZE/2 + 1), Blocks.BARRIER.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(centerX + PLATFORM_SIZE/2 + 1, PLATFORM_HEIGHT + y, centerZ - PLATFORM_SIZE/2 - 1), Blocks.BARRIER.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(centerX + PLATFORM_SIZE/2 + 1, PLATFORM_HEIGHT + y, centerZ + PLATFORM_SIZE/2 + 1), Blocks.BARRIER.defaultBlockState());
        }
        
        // Optional: Add barrier ceiling to completely enclose the player
        for (int x = -PLATFORM_SIZE/2 - 1; x <= PLATFORM_SIZE/2 + 1; x++) {
            for (int z = -PLATFORM_SIZE/2 - 1; z <= PLATFORM_SIZE/2 + 1; z++) {
                // Skip the interior platform area for the ceiling
                if (x >= -PLATFORM_SIZE/2 && x <= PLATFORM_SIZE/2 && 
                    z >= -PLATFORM_SIZE/2 && z <= PLATFORM_SIZE/2) {
                    continue;
                }
                level.setBlockAndUpdate(new BlockPos(centerX + x, PLATFORM_HEIGHT + 4, centerZ + z), Blocks.BARRIER.defaultBlockState());
            }
        }
        
        // Teleport player to the center of the platform
        player.teleportTo(centerX + 0.5, PLATFORM_HEIGHT + 1, centerZ + 0.5);
        
        // Send message to player
        player.sendSystemMessage(Component.literal("You've reached the end.").withStyle(ChatFormatting.DARK_RED));
    }
    
    /**
     * Delete the world and kick all players
     */
    private void initiateWorldDeletion(MinecraftServer server) {
        LOGGER.warn("Initiating world deletion sequence by player request");
        
        // Mark the world as erased in persistent storage
        setWorldErased(true);
        
        // Apply final glitch effect to all players before kicking
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Send a final intense glitch effect
            NetworkHandler.sendToPlayer(
                    new GlitchScreenPacket(
                            2, // EFFECT_CORRUPT
                            1.0f, // Maximum intensity
                            60), // 3 seconds
                    player);
        }
        
        // Schedule teleportation to void dimension with a slight delay
        server.tell(new net.minecraft.server.TickTask(
                server.getTickCount() + 40, () -> {
                    // Instead of kicking, send all players to the void dimension
                    ServerLevel voidLevel = server.getLevel(DimensionRegistry.THE_VOID);
                    if (voidLevel != null) {
                        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                            // First a message
                            Component message = Component.literal("The world has been erased").withStyle(ChatFormatting.DARK_RED);
                            player.sendSystemMessage(message);
                            
                            // Then teleport to the void
                            LOGGER.info("Teleporting player {} to The Void dimension as part of endgame", 
                                    player.getName().getString());
                            
                            // Use TheVoidPortalHandler to handle the teleportation
                            TheVoidPortalHandler.teleportPlayerToVoid(player, player.blockPosition());
                        }
                    } else {
                        LOGGER.error("Failed to get The Void dimension for teleportation, kicking players instead");
                        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                            Component kickMessage = Component.literal("The world has been erased").withStyle(ChatFormatting.DARK_RED);
                            player.connection.disconnect(kickMessage);
                        }
                    }
                }));
    }
    
    /**
     * Track the state of the endgame sequence for a player
     */
    private class SequenceState {
        private final UUID playerUUID;
        private int tickCounter = 0;
        private int messageIndex = 0;
        private boolean monologueStarted = false;
        private boolean monologueFinished = false;
        private boolean finalGlitchesStarted = false;
        private int glitchCount = 0;
        private ServerPlayer playerRef = null; // Reference to the player for easier access
        
        public SequenceState(UUID playerUUID) {
            this.playerUUID = playerUUID;
        }
        
        public void tick(ServerPlayer player) {
            this.playerRef = player; // Keep reference updated
            tickCounter++;
            
            // Start monologue after initial delay
            if (!monologueStarted && tickCounter >= DELAY_BEFORE_MONOLOGUE) {
                monologueStarted = true;
                tickCounter = 0; // Reset for message timing
                
                // Apply another screen effect
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                1, // EFFECT_DISTORTION
                                0.9f,
                                60), 
                        player);
            }
            
            // Display each monologue message with delay
            if (monologueStarted && !monologueFinished) {
                if (tickCounter >= TICKS_BETWEEN_MESSAGES && messageIndex < MONOLOGUE.length) {
                    // Send next message formatted like player chat - special format for final message
                    Component chatMessage;
                    if (messageIndex == MONOLOGUE.length - 1) {
                        // Make the "GOODBYE" message red
                        chatMessage = Component.literal("<" + ENTITY_NAME + "> " + MONOLOGUE[messageIndex])
                                .withStyle(ChatFormatting.DARK_RED);
                    } else {
                        chatMessage = Component.literal("<" + ENTITY_NAME + "> " + MONOLOGUE[messageIndex])
                                .withStyle(ChatFormatting.WHITE);
                    }
                    
                    // Send as chat message to simulate player chat
                    player.sendSystemMessage(chatMessage);
                    
                    // Apply screen effect with each message
                    NetworkHandler.sendToPlayer(
                            new GlitchScreenPacket(
                                    (messageIndex % 3), // Alternate between effect types
                                    0.8f + (messageIndex * 0.03f), // Increase intensity
                                    40), 
                            player);
                    
                    messageIndex++;
                    tickCounter = 0;
                    
                    // Check if monologue is complete
                    if (messageIndex >= MONOLOGUE.length) {
                        monologueFinished = true;
                        finalGlitchesStarted = true;
                        tickCounter = 0; // Reset counter for glitch sequence
                    }
                }
            }
            
            // Add intense glitch effects after monologue is complete but before world deletion
            if (monologueFinished && finalGlitchesStarted && tickCounter < TICKS_BEFORE_DELETION) {
                // Create intense glitches every 20 ticks (1 second)
                if (tickCounter % 20 == 0 && glitchCount < 10) {
                    // Random glitch effect type
                    int effectType = (int)(Math.random() * 4); // 0-3
                    float intensity = 0.8f + (glitchCount * 0.03f); // Increasing intensity
                    
                    NetworkHandler.sendToPlayer(
                            new GlitchScreenPacket(
                                    effectType,
                                    intensity,
                                    15), // Short glitches
                            player);
                    
                    glitchCount++;
                }
            }
            
            // Handle world deletion after monologue and glitch effects
            if (monologueFinished && tickCounter >= TICKS_BEFORE_DELETION) {
                // Remove from tracking
                activeSequences.remove(playerUUID);
                
                // Start world deletion process
                MinecraftServer server = player.getServer();
                if (server != null) {
                    initiateWorldDeletion(server);
                }
            }
        }
    }
    
    /**
     * Handle player tick events to progress the endgame sequence
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Only process server-side and at the end of the tick
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        
        // Check if this player is in an active sequence
        SequenceState state = activeSequences.get(event.player.getUUID());
        if (state != null && event.player instanceof ServerPlayer serverPlayer) {
            // We'll still apply movement limitations but allow the sequence to continue
            // regardless of player position - this ensures the world deletion happens
            
            // Try to keep player on platform, but don't prevent sequence progression
            Vec3 currentPos = serverPlayer.position();
            
            // Apply velocity changes to make movement difficult
            serverPlayer.setDeltaMovement(0, 0, 0);
            
            // Progress the sequence (this will eventually trigger world deletion)
            state.tick(serverPlayer);
        }
    }
    
    /**
     * Prevent players from leaving during the endgame sequence
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        
        // Remove from tracking if they log out
        activeSequences.remove(playerId);
    }
    
    /**
     * Handle player login to check if world is erased
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Check if the world has been erased
        if (worldErased && event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("Player {} attempting to join erased world, redirecting to void dimension", 
                    player.getName().getString());
            
            // Schedule teleportation after they've properly loaded in
            player.server.tell(new net.minecraft.server.TickTask(
                    player.server.getTickCount() + 20, () -> {
                // Send message
                Component message = Component.literal("This world has been erased").withStyle(ChatFormatting.DARK_RED);
                player.sendSystemMessage(message);
                
                // Teleport to void dimension
                TheVoidPortalHandler.teleportPlayerToVoid(player, player.blockPosition());
            }));
        }
    }
    
    /**
     * SavedData class for storing endgame state between server sessions
     */
    public static class EndgameSavedData extends SavedData {
        private boolean worldErased = false;
        
        public EndgameSavedData() {
        }
        
        public static EndgameSavedData load(CompoundTag tag) {
            EndgameSavedData data = new EndgameSavedData();
            
            if (tag.contains("WorldErased")) {
                data.worldErased = tag.getBoolean("WorldErased");
            }
            
            return data;
        }
        
        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("WorldErased", worldErased);
            return tag;
        }
        
        public boolean isWorldErased() {
            return worldErased;
        }
        
        public void setWorldErased(boolean erased) {
            this.worldErased = erased;
            setDirty();
        }
    }
} 