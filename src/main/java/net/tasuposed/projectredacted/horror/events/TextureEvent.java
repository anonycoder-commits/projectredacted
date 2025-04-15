package net.tasuposed.projectredacted.horror.events;

import net.minecraft.server.level.ServerPlayer;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.network.packets.TextureGlitchPacket;
import net.tasuposed.projectredacted.config.HorrorConfig;

import java.util.List;
import java.util.Random;

/**
 * Handles texture-related horror events
 */
public class TextureEvent {
    private final Random random = new Random();
    
    // Radius for multiplayer texture synchronization
    private static final double MULTIPLAYER_SYNC_RADIUS = 50.0;
    
    /**
     * Send a texture glitch packet to the player
     */
    public void sendTextureGlitchPacket(ServerPlayer player) {
        // Choose a random glitch type from our available options
        int glitchType = random.nextInt(3); // 0: corrupt, 1: swap, 2: flicker
        
        // Determine a short duration for subtle stage
        int duration = 10 + random.nextInt(20); // 10-30 ticks (0.5-1.5 seconds)
        
        // Send the packet to the player
        NetworkHandler.sendToPlayer(
                new TextureGlitchPacket(glitchType, duration),
                player);
        
        // If multiplayer synchronization is enabled, also send to nearby players
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new TextureGlitchPacket(glitchType, duration),
                            otherPlayer);
                }
            }
        }
    }
    
    /**
     * Send a more intense texture corruption for later horror stages
     */
    public void sendCorruptTexturePacket(ServerPlayer player, float intensity) {
        // Always use corrupt type for this effect
        int glitchType = 0; // GLITCH_CORRUPT
        
        // Determine a longer duration based on intensity
        int duration = 40 + (int)(intensity * 60); // 40-100 ticks (2-5 seconds)
        
        // Send the packet to the player
        NetworkHandler.sendToPlayer(
                new TextureGlitchPacket(glitchType, duration),
                player);
        
        // If multiplayer synchronization is enabled, also send to nearby players
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new TextureGlitchPacket(glitchType, duration),
                            otherPlayer);
                }
            }
        }
    }
    
    /**
     * Cause textures to flicker between normal and alternate versions
     */
    public void sendTextureFlickerPacket(ServerPlayer player) {
        // Use flicker type for this effect
        int glitchType = 2; // GLITCH_FLICKER
        
        // Medium duration
        int duration = 60 + random.nextInt(60); // 60-120 ticks (3-6 seconds)
        
        // Send the packet to the player
        NetworkHandler.sendToPlayer(
                new TextureGlitchPacket(glitchType, duration),
                player);
    }
    
    /**
     * Swap textures between different game objects
     * Very disorienting for later horror stages
     */
    public void sendTextureSwapPacket(ServerPlayer player) {
        // Use swap type for this effect
        int glitchType = 1; // GLITCH_SWAP
        
        // Medium duration for texture swap
        int duration = 40 + random.nextInt(40); // 40-80 ticks (2-4 seconds)
        
        // Send the packet to the player
        NetworkHandler.sendToPlayer(
                new TextureGlitchPacket(glitchType, duration),
                player);
        
        // If multiplayer synchronization is enabled, also send to nearby players
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new TextureGlitchPacket(glitchType, duration),
                            otherPlayer);
                }
            }
        }
    }
    
    /**
     * Get a list of all nearby players to share the experience with
     */
    private List<ServerPlayer> getNearbyPlayers(ServerPlayer player) {
        if (player.level().getServer() == null) {
            return java.util.Collections.emptyList();
        }
        
        return player.level().getServer().getPlayerList().getPlayers();
    }
    
    /**
     * Check if multiplayer synchronization is enabled
     */
    private boolean shouldSyncMultiplayer() {
        return HorrorConfig.SYNC_MULTIPLAYER_EVENTS.get();
    }
} 