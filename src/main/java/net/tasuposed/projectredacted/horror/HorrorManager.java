package net.tasuposed.projectredacted.horror;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.tasuposed.projectredacted.ProjectRedacted;
import net.tasuposed.projectredacted.config.HorrorConfig;
import net.tasuposed.projectredacted.horror.stage.*;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.MinecraftServer;
import java.util.UUID;

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import net.tasuposed.projectredacted.horror.events.EntityEvent;

/**
 * Main manager for all horror elements in the mod.
 * Controls progression, timing, and activation of horror features.
 */
public class HorrorManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static HorrorManager instance;
    
    // Map of players to their horror progression state
    private final Map<UUID, PlayerHorrorState> playerStates = new HashMap<>();
    
    // List of all horror stages in order of progression
    private final List<HorrorStage> stages = new ArrayList<>();
    
    // Flag to track if config is loaded
    private static boolean configLoaded = false;
    
    // Debug tracking fields
    private boolean hadEventLastTick = false;
    private String lastEventType = null;
    private List<String> recentEvents = new ArrayList<>();
    private int eventCounter = 0;
    private long lastResetTime = 0;
    
    // Debug mode for easier testing
    private static boolean debugMode = false;
    
    private HorrorManager() {
        initStages();
    }
    
    /**
     * Initialize all horror stages in progression order
     */
    private void initStages() {
        // Stage 1: Subtle oddities
        stages.add(new SubtleStage());
        
        // Stage 2: Noticeable disturbances
        stages.add(new DisturbanceStage());
        
        // Stage 3: Obvious horror
        stages.add(new ObviousStage());
        
        // Stage 4: Fake glitches and crashes
        stages.add(new FakeGlitchStage());
        
        // Stage 5: Fourth wall breaking
        stages.add(new MetaStage());
        
        // Stage 6: Final horror
        stages.add(new FinalStage());
    }
    
    public static HorrorManager getInstance() {
        if (instance == null) {
            instance = new HorrorManager();
        }
        return instance;
    }
    
    /**
     * Initialize the horror system
     */
    public static void init() {
        getInstance(); // Ensure instance is created
        HorrorStructureSpawner.init();
        LOGGER.info("Horror system initialized");
    }
    
    /**
     * Check if debug mode is enabled
     */
    public static boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Set debug mode on or off
     */
    public static void setDebugMode(boolean debug) {
        debugMode = debug;
        LOGGER.info("Horror debug mode set to: {}", debug);
    }
    
    /**
     * Get or create horror state for a player
     */
    public PlayerHorrorState getPlayerState(Player player) {
        return playerStates.computeIfAbsent(player.getUUID(), uuid -> new PlayerHorrorState());
    }
    
    /**
     * Progress the horror stage for a player if enough time has passed
     */
    public void progressHorrorIfReady(Player player) {
        // Skip if configs aren't loaded yet
        if (!configLoaded) {
            return;
        }
        
        // Get player state
        PlayerHorrorState state = getPlayerState(player);
        
        // Don't progress if horror is disabled
        if (!HorrorConfig.HORROR_ENABLED.get()) {
            return;
        }
        
        // Check if enough time has passed for progression
        if (state.isReadyForNextStage()) {
            int oldStage = state.getStage();
            state.advanceStage();
            int newStage = state.getStage();
            
            LOGGER.info("Player {} has advanced from horror stage {} to {}", 
                    player.getName().getString(), oldStage, newStage);
                    
            // Trigger a stage-advancement specific event
            if (newStage >= 0 && newStage < stages.size()) {
                HorrorStage newStageObj = stages.get(newStage);
                try {
                    LOGGER.info("Triggering stage advancement event: {}", newStageObj.getClass().getSimpleName());
                    newStageObj.triggerRandomEvent(player);
                    
                    // Record this progression event
                    hadEventLastTick = true;
                    lastEventType = "STAGE ADVANCE: " + newStageObj.getClass().getSimpleName();
                    recentEvents.add(0, System.currentTimeMillis() + ": " + lastEventType + 
                            " for " + player.getName().getString());
                } catch (Exception e) {
                    // Log any exceptions that occur during event triggering
                    LOGGER.error("Error triggering stage advancement event: ", e);
                }
            } else {
                LOGGER.warn("Invalid new horror stage {} for player {}", newStage, player.getName().getString());
            }
        }
    }
    
    /**
     * Mark configs as loaded - called from config load events
     */
    public static void markConfigLoaded() {
        configLoaded = true;
        LOGGER.debug("Horror config marked as loaded");
    }
    
    /**
     * Check if config is loaded
     */
    public static boolean isConfigLoaded() {
        return configLoaded;
    }
    
    /**
     * Trigger random horror events based on player's current stage
     */
    public void triggerRandomEvent(Player player) {
        // Skip if configs aren't loaded yet
        if (!configLoaded) {
            LOGGER.debug("Config not loaded, skipping event trigger");
            return;
        }
        
        PlayerHorrorState state = getPlayerState(player);
        int currentStage = state.getStage();
        
        // Don't trigger events if horror is disabled
        if (!HorrorConfig.HORROR_ENABLED.get()) {
            LOGGER.debug("Horror disabled, skipping event trigger");
            return;
        }
        
        // Get appropriate stage and trigger a random event
        if (currentStage >= 0 && currentStage < stages.size()) {
            HorrorStage stage = stages.get(currentStage);
            LOGGER.info("Triggering horror event for player {} at stage {}: {}", 
                player.getName().getString(), currentStage, stage.getClass().getSimpleName());
            try {
                // Check if multiplayer synchronization is enabled
                if (player.level().getServer() != null && player.level().getServer().getPlayerCount() > 1 
                    && HorrorConfig.SYNC_MULTIPLAYER_EVENTS.get()) {
                    // Synchronize the event across all players in multiplayer
                    triggerSynchronizedEvent(player, stage);
                } else {
                    // Single player or sync disabled, trigger normally
                    stage.triggerRandomEvent(player);
                }
                
                // Record this successful event
                hadEventLastTick = true;
                lastEventType = stage.getClass().getSimpleName();
                
                // Add to recent events list (max 10 events)
                String eventInfo = System.currentTimeMillis() + ": " + lastEventType + " for " + player.getName().getString();
                recentEvents.add(0, eventInfo); // Add at beginning of list
                if (recentEvents.size() > 10) {
                    recentEvents.remove(recentEvents.size() - 1); // Remove oldest
                }
            } catch (Exception e) {
                // Log any exceptions that occur during event triggering
                LOGGER.error("Error triggering horror event: ", e);
            }
        } else {
            LOGGER.warn("Invalid horror stage {} for player {}", currentStage, player.getName().getString());
        }
    }
    
    /**
     * Synchronize a horror event across all players in multiplayer
     * This ensures everyone experiences the same event at the same time
     */
    private void triggerSynchronizedEvent(Player triggerPlayer, HorrorStage stage) {
        if (!(triggerPlayer instanceof ServerPlayer)) {
            return;
        }
        
        LOGGER.info("Synchronizing horror event for all players from trigger player: {}", 
                triggerPlayer.getName().getString());
        
        // Get the server for player list
        net.minecraft.server.MinecraftServer server = triggerPlayer.level().getServer();
        if (server == null) return;
        
        // Get all players
        java.util.List<ServerPlayer> allPlayers = server.getPlayerList().getPlayers();
        
        // Generate random seed based on trigger player for consistent randomization
        long seed = triggerPlayer.getUUID().getLeastSignificantBits() + System.currentTimeMillis();
        Random sharedRandom = new Random(seed);
        
        // Store the current stage of the trigger player
        int triggerStage = getPlayerState(triggerPlayer).getStage();
        
        // For each player on the server, trigger a synchronized event at the same stage
        for (ServerPlayer player : allPlayers) {
            // Temporarily override the player's state with the trigger player's stage
            // to ensure consistent experience
            PlayerHorrorState originalState = getPlayerState(player);
            int originalStage = originalState.getStage();
            
            try {
                // Only synchronize players that are at least in subtle stage
                if (originalStage >= 0) {
                    // Temporarily set to trigger player's stage for event
                    originalState.setCurrentStage(triggerStage);
                    
                    // Trigger the event with the shared random seed
                    triggerRandomEventWithSeed(player, stage, sharedRandom);
                    
                    LOGGER.debug("Synchronized event for player: {}", player.getName().getString());
                }
            } finally {
                // Always restore original stage after event
                originalState.setCurrentStage(originalStage);
            }
        }
    }
    
    /**
     * Trigger a random event with a specific random seed to ensure consistency
     */
    private void triggerRandomEventWithSeed(Player player, HorrorStage stage, Random random) {
        if (player instanceof ServerPlayer serverPlayer) {
            // Use reflection to temporarily set the Random object in the stage
            try {
                // Get the random field from the stage class
                java.lang.reflect.Field randomField = stage.getClass().getDeclaredField("random");
                randomField.setAccessible(true);
                
                // Store the original random
                Random originalRandom = (Random) randomField.get(stage);
                
                try {
                    // Set our synchronized random
                    randomField.set(stage, random);
                    
                    // Trigger the event with our synchronized random
                    stage.triggerRandomEvent(player);
                } finally {
                    // Always restore the original random
                    randomField.set(stage, originalRandom);
                }
            } catch (Exception e) {
                // If reflection fails, fall back to normal triggering
                LOGGER.error("Failed to synchronize random for horror event: ", e);
                stage.triggerRandomEvent(player);
            }
        }
    }
    
    /**
     * Event handler for player ticks - chance to trigger horror events
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Skip if configs aren't loaded yet
        if (!configLoaded) {
            return;
        }
        
        // Only process on server side, at the END phase, and only for server players
        if (event.phase != TickEvent.Phase.END || 
            !(event.player instanceof ServerPlayer) ||
            event.player.level().isClientSide()) {
            return;
        }
        
        ServerPlayer player = (ServerPlayer) event.player;
        
        // Every 5 minutes per player, check if we should progress their horror stage
        if (player.level().getGameTime() % 6000 == player.getId() % 100) {
            // Progress the player's horror stage if they've spent enough time
            progressHorrorIfReady(player);
            
            // Log for debugging
            LOGGER.debug("Checking horror progression for {}", player.getName().getString());
        }
        
        // Every second, with a higher probability, trigger a random horror event
        if (player.level().getGameTime() % 20 == 0 && player.level().getRandom().nextInt(120) == 0) { // Increased frequency more (150 -> 120)
            // Get player's personal frequency modifier
            PlayerHorrorState state = getPlayerState(player);
            float frequencyModifier = state.getFrequencyModifier();
            
            // Skip events entirely if modifier is 0
            if (frequencyModifier <= 0.0f) {
                return;
            }
            
            // Apply the player's personal frequency modifier and config value
            float baseChance = HorrorConfig.EVENT_FREQUENCY.get().floatValue();
            
            // Increase chance in higher stages to get more entity spawns naturally
            int currentStage = state.getCurrentStage();
            
            // Boost frequency in all stages, but more in later stages
            if (currentStage == 0) {
                baseChance *= 1.15f; // 15% boost even in subtle stage
            } else if (currentStage == 1) {
                baseChance *= 1.35f; // 35% boost in disturbance stage
            } else {
                // Stronger boost in later stages (was only applying here before)
                baseChance *= (1.0f + (currentStage * 0.3f)); // 60% boost at stage 2, 90% at stage 3, 120% at stage 4
            }
            
            float adjustedChance = baseChance * frequencyModifier;
            
            // Random chance to trigger an event with higher probability
            if (player.level().getRandom().nextFloat() < adjustedChance * 1.35f) { // Increased boost from 25% to 35%
                LOGGER.debug("Triggering random horror event for {}", player.getName().getString());
                triggerRandomEvent(player);
            }
        }
    }
    
    /**
     * Reset horror state when player logs in (optional, based on config)
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Skip if configs aren't loaded yet
        if (!configLoaded) {
            return;
        }
        
        if (HorrorConfig.RESET_ON_LOGIN.get()) {
            playerStates.remove(event.getEntity().getUUID());
        }
    }
    
    /**
     * Trigger a random event based on the current stage
     */
    private void triggerRandomEvent(ServerPlayer player) {
        HorrorStage currentStage = getCurrentStage(player);
        if (currentStage == null) {
            return;
        }
        
        // Trigger the event
        currentStage.triggerRandomEvent(player);
        
        // Log the event for debug purposes
        hadEventLastTick = true;
        lastEventType = currentStage.getClass().getSimpleName();
        
        // Add to recent events list (max 10 events)
        String eventInfo = System.currentTimeMillis() + ": " + lastEventType + " for " + player.getName().getString();
        recentEvents.add(0, eventInfo); // Add at beginning of list
        if (recentEvents.size() > 10) {
            recentEvents.remove(recentEvents.size() - 1); // Remove oldest
        }
        
        // Count events in the last period
        eventCounter++;
    }
    
    /**
     * Get the current horror stage for a player
     */
    private HorrorStage getCurrentStage(Player player) {
        PlayerHorrorState state = getPlayerState(player);
        int stageIndex = state.getStage();
        
        if (stageIndex >= 0 && stageIndex < stages.size()) {
            return stages.get(stageIndex);
        }
        
        return null;
    }
    
    /**
     * Check if an event happened in the last tick
     * For debugging purposes
     */
    public boolean hadEventThisTick() {
        boolean result = hadEventLastTick;
        hadEventLastTick = false; // Reset after reading
        return result;
    }
    
    /**
     * Get information about the last event that occurred
     * For debugging purposes
     */
    public String getLastEventInfo() {
        return lastEventType != null ? lastEventType : "Unknown";
    }
    
    /**
     * Get the count of events in the last 10 minutes
     * For debugging purposes
     */
    public int getEventCountLastPeriod() {
        // Reset counter every 10 minutes
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResetTime > 600000) { // 10 minutes in milliseconds
            eventCounter = 0;
            lastResetTime = currentTime;
        }
        
        return eventCounter;
    }
    
    /**
     * Get an array of recent events
     * For debugging purposes
     */
    public String[] getRecentEvents() {
        return recentEvents.toArray(new String[0]);
    }
    
    /**
     * Save all player horror states
     * Called when the server is shutting down
     */
    public void saveAllPlayerStates(MinecraftServer server) {
        if (server == null) return;
        
        ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld == null) return;
        
        HorrorSavedData savedData = overworld.getDataStorage().computeIfAbsent(
            HorrorSavedData::load, 
            HorrorSavedData::new, 
            "projectredacted_horror_data");
        
        savedData.setPlayerStates(playerStates);
        savedData.setDirty();
    }
    
    /**
     * Load all player horror states
     * Called when the server is starting up
     */
    public void loadAllPlayerStates(MinecraftServer server) {
        if (server == null) return;
        
        ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld == null) return;
        
        HorrorSavedData savedData = overworld.getDataStorage().computeIfAbsent(
            HorrorSavedData::load, 
            HorrorSavedData::new, 
            "projectredacted_horror_data");
        
        playerStates.clear();
        playerStates.putAll(savedData.getPlayerStates());
    }
    
    /**
     * SavedData class for storing horror states between server sessions
     */
    public static class HorrorSavedData extends SavedData {
        private final Map<UUID, PlayerHorrorState> playerStates = new HashMap<>();
        
        public HorrorSavedData() {
        }
        
        public static HorrorSavedData load(CompoundTag tag) {
            HorrorSavedData data = new HorrorSavedData();
            
            if (tag.contains("PlayerStates", Tag.TAG_LIST)) {
                ListTag playerList = tag.getList("PlayerStates", Tag.TAG_COMPOUND);
                
                for (int i = 0; i < playerList.size(); i++) {
                    CompoundTag playerTag = playerList.getCompound(i);
                    UUID uuid = playerTag.getUUID("UUID");
                    
                    PlayerHorrorState state = new PlayerHorrorState();
                    state.load(playerTag.getCompound("State"));
                    
                    data.playerStates.put(uuid, state);
                }
            }
            
            return data;
        }
        
        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag playerList = new ListTag();
            
            for (Map.Entry<UUID, PlayerHorrorState> entry : playerStates.entrySet()) {
                CompoundTag playerTag = new CompoundTag();
                playerTag.putUUID("UUID", entry.getKey());
                playerTag.put("State", entry.getValue().save());
                
                playerList.add(playerTag);
            }
            
            tag.put("PlayerStates", playerList);
            return tag;
        }
        
        public Map<UUID, PlayerHorrorState> getPlayerStates() {
            return playerStates;
        }
        
        public void setPlayerStates(Map<UUID, PlayerHorrorState> states) {
            playerStates.clear();
            playerStates.putAll(states);
            setDirty();
        }
    }
} 