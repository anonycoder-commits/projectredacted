package net.tasuposed.projectredacted.horror;

import java.util.UUID;

import net.minecraft.world.entity.player.Player;
import net.tasuposed.projectredacted.config.HorrorConfig;
import net.minecraft.nbt.CompoundTag;

/**
 * Tracks horror-related state for a specific player
 */
public class PlayerHorrorState {
    private UUID playerUUID;
    private String playerName;
    
    // Stage tracking
    private int currentStage = 0; // 0=subtle, 1=disturbance, 2=obvious, 3=fake_glitch, 4=meta, 5=final
    private long lastStageAdvance = 0;
    
    // Event frequency control
    private float frequencyModifier = 1.0f; // Normal frequency
    
    // Default values to use if config is not loaded yet
    private static final long DEFAULT_STAGE_DURATION_MS = 30 * 60 * 1000; // 30 minutes in ms
    
    /**
     * Default constructor for server loading
     */
    public PlayerHorrorState() {
        this.playerUUID = new UUID(0, 0);
        this.playerName = "unknown";
        this.lastStageAdvance = System.currentTimeMillis();
    }
    
    /**
     * Create a new horror state for a player
     */
    public PlayerHorrorState(Player player) {
        this.playerUUID = player.getUUID();
        this.playerName = player.getName().getString();
        this.lastStageAdvance = System.currentTimeMillis();
    }
    
    /**
     * Get player UUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    /**
     * Get player name
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * Update player name if changed
     */
    public void updatePlayerName(String newName) {
        this.playerName = newName;
    }
    
    /**
     * Get current horror stage (0-5)
     */
    public int getCurrentStage() {
        return currentStage;
    }
    
    /**
     * Alias for getCurrentStage() for compatibility
     */
    public int getStage() {
        return getCurrentStage();
    }
    
    /**
     * Advance to the next stage
     */
    public void advanceStage() {
        if (currentStage < 5) {
            currentStage++;
            lastStageAdvance = System.currentTimeMillis();
        }
    }
    
    /**
     * Set current horror stage
     */
    public void setCurrentStage(int stage) {
        if (stage >= 0 && stage <= 5) {
            this.currentStage = stage;
            this.lastStageAdvance = System.currentTimeMillis();
        }
    }
    
    /**
     * Get when the last stage advance occurred
     */
    public long getLastStageAdvance() {
        return lastStageAdvance;
    }
    
    /**
     * Set when the last stage advance occurred
     */
    public void setLastStageAdvance(long time) {
        this.lastStageAdvance = time;
    }
    
    /**
     * Get frequency modifier for events
     */
    public float getFrequencyModifier() {
        return frequencyModifier;
    }
    
    /**
     * Set frequency modifier for events
     */
    public void setFrequencyModifier(float modifier) {
        this.frequencyModifier = Math.max(0.0f, modifier);
    }
    
    /**
     * Reset player horror state to initial values
     */
    public void reset() {
        this.currentStage = 0;
        this.lastStageAdvance = System.currentTimeMillis();
        this.frequencyModifier = 1.0f;
    }
    
    /**
     * Check if enough time has passed to advance to the next stage
     */
    public boolean isReadyForNextStage() {
        // Return false for stage 5 (max stage)
        if (currentStage >= 5) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastAdvance = currentTime - lastStageAdvance;
        
        // Get config value for time between stages
        long requiredTime;
        if (HorrorManager.isConfigLoaded()) {
            // Use config value - convert minutes to milliseconds
            requiredTime = HorrorConfig.TIME_BETWEEN_STAGES.get() * 60L * 1000L;
            
            // Use the same time for each stage transition for more consistency
            // This replaces the previous formula that made later stages take longer
        } else {
            // Use default value - consistent across all stages
            requiredTime = DEFAULT_STAGE_DURATION_MS;
        }
        
        // Add debug log
        if (timeSinceLastAdvance >= requiredTime) {
            System.out.println("Player ready for stage advancement from " + currentStage + 
                " to " + (currentStage + 1) + " after " + (timeSinceLastAdvance / 1000 / 60) + 
                " minutes. Required: " + (requiredTime / 1000 / 60) + " minutes.");
        }
        
        return timeSinceLastAdvance >= requiredTime;
    }
    
    public void recordEvent() {
        lastStageAdvance = System.currentTimeMillis();
    }
    
    public long getTimeSinceLastEvent() {
        return System.currentTimeMillis() - lastStageAdvance;
    }
    
    /**
     * Save state to NBT for persistence
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Stage", currentStage);
        tag.putLong("LastAdvance", lastStageAdvance);
        tag.putFloat("FrequencyModifier", frequencyModifier);
        return tag;
    }
    
    /**
     * Load state from NBT
     */
    public void load(CompoundTag tag) {
        if (tag.contains("Stage")) {
            currentStage = tag.getInt("Stage");
        }
        
        if (tag.contains("LastAdvance")) {
            lastStageAdvance = tag.getLong("LastAdvance");
        } else {
            lastStageAdvance = System.currentTimeMillis();
        }
        
        if (tag.contains("FrequencyModifier")) {
            frequencyModifier = tag.getFloat("FrequencyModifier");
        }
    }
} 