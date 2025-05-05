package net.tasuposed.projectredacted.horror.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.network.packets.PlaySoundPacket;
import net.tasuposed.projectredacted.sound.SoundRegistry;
import net.tasuposed.projectredacted.config.HorrorConfig;
import net.minecraft.server.TickTask;

import java.util.List;
import java.util.Random;

/**
 * Handles sound-related horror events
 */
public class HorrorSoundEvent {
    private final Random random = new Random();

    // Radius for multiplayer sound synchronization
    private static final double MULTIPLAYER_SYNC_RADIUS = 50.0;

    /**
     * Play a mysterious distant sound for the player
     */
    public void playDistantSound(ServerPlayer player) {
        // Choose a random direction from the player
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 15.0 + random.nextDouble() * 10.0;

        Vec3 playerPos = player.position();
        double x = playerPos.x + Math.sin(angle) * distance;
        double z = playerPos.z + Math.cos(angle) * distance;

        // Instead of playing direct sound, send a packet to the client
        // This avoids the need to use SoundEvent references directly
        NetworkHandler.sendToPlayer(
                new PlaySoundPacket(
                        new ResourceLocation("minecraft:ambient.cave"),
                        SoundSource.AMBIENT,
                        0.3f + random.nextFloat() * 0.2f,
                        0.8f + random.nextFloat() * 0.4f,
                        false, false),
                player);

        // If multiplayer synchronization is enabled, also play for nearby players
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new PlaySoundPacket(
                                    new ResourceLocation("minecraft:ambient.cave"),
                                    SoundSource.AMBIENT,
                                    0.3f + random.nextFloat() * 0.2f,
                                    0.8f + random.nextFloat() * 0.4f,
                                    false, false),
                            otherPlayer);
                }
            }
        }
    }

    /**
     * Play a distorted cave sound directly to the player
     */
    public void playDistortedSound(ServerPlayer player) {
        // Send a packet to play distorted sound on client
        NetworkHandler.sendToPlayer(
                new PlaySoundPacket(
                        new ResourceLocation("minecraft:ambient.cave"),
                        SoundSource.HOSTILE,
                        0.6f,
                        0.5f,
                        true, false),
                player);

        // If multiplayer synchronization is enabled, also play for nearby players
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new PlaySoundPacket(
                                    new ResourceLocation("minecraft:ambient.cave"),
                                    SoundSource.HOSTILE,
                                    0.6f,
                                    0.5f,
                                    true, false),
                            otherPlayer);
                }
            }
        }
    }

    /**
     * Play a creepy whisper directly to the player
     */
    public void playWhisper(ServerPlayer player) {
        // Get the registered sound event
        SoundEvent whisperSound = SoundRegistry.WHISPER.get();

        // Play a single whisper with low volume and random pitch for subtlety
        // Use non-locational sound (isLocational=false) to make it feel like it's in the player's head
        NetworkHandler.sendToPlayer(
                new PlaySoundPacket(
                        whisperSound.getLocation(),
                        SoundSource.HOSTILE,
                        0.35f, // Slightly increased volume for YouTube clarity
                        random.nextFloat() * 0.3f + 0.4f, // Random pitch between 0.4-0.7
                        true,
                        false), // Non-locational sound
                player);

        // If multiplayer synchronization is enabled, also play for nearby players
        if (shouldSyncMultiplayer()) {
            // Use the same pitch for all players to keep it synchronized
            float sharedPitch = random.nextFloat() * 0.3f + 0.4f;

            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new PlaySoundPacket(
                                    whisperSound.getLocation(),
                                    SoundSource.HOSTILE,
                                    0.35f,
                                    sharedPitch, // Use the same pitch for consistency
                                    true,
                                    false), // Non-locational sound
                            otherPlayer);
                }
            }
        }
    }

    /**
     * Play a heartbeat sound that gets faster and louder
     */
    public void playHeartbeat(ServerPlayer player) {
        // Get the registered sound event
        SoundEvent heartbeatSound = SoundRegistry.HEARTBEAT.get();

        // Use our custom heartbeat sound
        NetworkHandler.sendToPlayer(
                new PlaySoundPacket(
                        heartbeatSound.getLocation(),
                        SoundSource.MASTER,
                        0.8f, // Increased for YouTube clarity
                        1.0f,
                        false,
                        false), // Non-locational sound
                player);

        // If multiplayer synchronization is enabled, also play for nearby players
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new PlaySoundPacket(
                                    heartbeatSound.getLocation(),
                                    SoundSource.MASTER,
                                    0.8f,
                                    1.0f,
                                    false,
                                    false), // Non-locational sound
                            otherPlayer);
                }
            }
        }
    }

    /**
     * Play a growling sound effect
     */
    public void playGrowl(ServerPlayer player) {
        // Get the registered sound event
        SoundEvent growlSound = SoundRegistry.GROWL.get();

        // Use our custom growl sound
        NetworkHandler.sendToPlayer(
                new PlaySoundPacket(
                        growlSound.getLocation(),
                        SoundSource.HOSTILE,
                        0.9f, // Increased for YouTube clarity
                        0.7f,
                        true, false),
                player);

        // If multiplayer synchronization is enabled, also play for nearby players
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new PlaySoundPacket(
                                    growlSound.getLocation(),
                                    SoundSource.HOSTILE,
                                    0.9f,
                                    0.7f,
                                    true, false),
                            otherPlayer);
                }
            }
        }
    }

    /**
     * Play a system error sound for UI horror effects
     */
    public void playSystemError(ServerPlayer player) {
        // Use vanilla block breaking sound at high pitch for a "system error" effect
        NetworkHandler.sendToPlayer(
                new PlaySoundPacket(
                        SoundEvents.GLASS_BREAK.getLocation(),
                        SoundSource.MASTER,
                        0.5f,
                        2.0f, // High pitch for error sound
                        true, false),
                player);

        // If multiplayer synchronization is enabled, also play for nearby players (initial sound)
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new PlaySoundPacket(
                                    SoundEvents.GLASS_BREAK.getLocation(),
                                    SoundSource.MASTER,
                                    0.5f,
                                    2.0f,
                                    true, false),
                            otherPlayer);
                }
            }
        }

        // Quickly follow with a second sound for more intensity
        player.getServer().tell(new TickTask(
                player.getServer().getTickCount() + 5, () -> {
            NetworkHandler.sendToPlayer(
                    new PlaySoundPacket(
                            SoundEvents.ELDER_GUARDIAN_CURSE.getLocation(),
                            SoundSource.HOSTILE,
                            0.7f,
                            0.5f, // Low pitch for ominous effect
                            true, false),
                    player);

            // If multiplayer synchronization is enabled, also play for nearby players (second sound)
            if (shouldSyncMultiplayer()) {
                for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                    if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                        NetworkHandler.sendToPlayer(
                                new PlaySoundPacket(
                                        SoundEvents.ELDER_GUARDIAN_CURSE.getLocation(),
                                        SoundSource.HOSTILE,
                                        0.7f,
                                        0.5f,
                                        true, false),
                                otherPlayer);
                    }
                }
            }
        }));
    }

    /**
     * Play a more intense whisper directly to the player
     * This is a more terrifying version of the regular whisper
     */
    public void playHorrorWhisper(ServerPlayer player) {
        // Get the registered sound event
        SoundEvent whisperSound = SoundRegistry.WHISPER.get();

        // Play a sequence of terrifying whispers from different directions
        // First whisper - from behind (non-locational)
        NetworkHandler.sendToPlayer(
                new PlaySoundPacket(
                        whisperSound.getLocation(),
                        SoundSource.HOSTILE,
                        0.6f, // Louder than the subtle whisper
                        0.4f, // Low pitch for creepiness
                        true,
                        false), // Non-locational sound
                player);

        // If multiplayer synchronization is enabled, also play for nearby players (first whisper)
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new PlaySoundPacket(
                                    whisperSound.getLocation(),
                                    SoundSource.HOSTILE,
                                    0.6f,
                                    0.4f,
                                    true,
                                    false), // Non-locational sound
                            otherPlayer);
                }
            }
        }

        // Second whisper - from left/right after a short delay (non-locational)
        player.getServer().tell(new TickTask(
                player.getServer().getTickCount() + 8, () -> {
            NetworkHandler.sendToPlayer(
                    new PlaySoundPacket(
                            whisperSound.getLocation(),
                            SoundSource.HOSTILE,
                            0.7f, // Even louder
                            0.3f, // Even lower pitch
                            true,
                            false), // Non-locational sound
                    player);

            // If multiplayer synchronization is enabled, also play for nearby players (second whisper)
            if (shouldSyncMultiplayer()) {
                for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                    if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                        NetworkHandler.sendToPlayer(
                                new PlaySoundPacket(
                                        whisperSound.getLocation(),
                                        SoundSource.HOSTILE,
                                        0.7f,
                                        0.3f,
                                        true,
                                        false), // Non-locational sound
                                otherPlayer);
                    }
                }
            }
        }));

        // Third whisper - very close after another delay (non-locational)
        player.getServer().tell(new TickTask(
                player.getServer().getTickCount() + 15, () -> {
            NetworkHandler.sendToPlayer(
                    new PlaySoundPacket(
                            whisperSound.getLocation(),
                            SoundSource.MASTER, // Use MASTER to make it feel "in your head"
                            0.8f, // Very loud
                            0.2f, // Very low pitch
                            true,
                            false), // Non-locational sound
                    player);

            // If multiplayer synchronization is enabled, also play for nearby players (third whisper)
            if (shouldSyncMultiplayer()) {
                for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                    if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                        NetworkHandler.sendToPlayer(
                                new PlaySoundPacket(
                                        whisperSound.getLocation(),
                                        SoundSource.MASTER,
                                        0.8f,
                                        0.2f,
                                        true,
                                        false), // Non-locational sound
                                otherPlayer);
                    }
                }
            }
        }));
    }

    /**
     * Play a horrifying scream sound - great for jump scares
     */
    public void playScream(ServerPlayer player) {
        // Get the registered sound event
        SoundEvent screamSound = SoundRegistry.SCREAM.get();

        // Play a single scream with high volume for impact
        NetworkHandler.sendToPlayer(
                new PlaySoundPacket(
                        screamSound.getLocation(),
                        SoundSource.HOSTILE,
                        0.95f, // Very loud for impact
                        random.nextFloat() * 0.2f + 0.9f, // Slightly randomized high pitch
                        true,
                        true), // Locational for better effect
                player);

        // If multiplayer synchronization is enabled, also play for nearby players
        if (shouldSyncMultiplayer()) {
            float sharedPitch = random.nextFloat() * 0.2f + 0.9f;
            
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new PlaySoundPacket(
                                    screamSound.getLocation(),
                                    SoundSource.HOSTILE,
                                    0.95f,
                                    sharedPitch,
                                    true,
                                    true),
                            otherPlayer);
                }
            }
        }
    }
    
    /**
     * Play reality warping sound effect for major events
     */
    public void playRealityWarp(ServerPlayer player) {
        // Get the registered sound event
        SoundEvent warpSound = SoundRegistry.REALITY_WARP.get();

        // Play a reality warp with environmental sound properties
        NetworkHandler.sendToPlayer(
                new PlaySoundPacket(
                        warpSound.getLocation(),
                        SoundSource.AMBIENT,
                        0.8f,
                        1.0f,
                        true,
                        false), // Non-locational for better immersion
                player);

        // If multiplayer synchronization is enabled, also play for nearby players
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new PlaySoundPacket(
                                    warpSound.getLocation(),
                                    SoundSource.AMBIENT,
                                    0.8f,
                                    1.0f,
                                    true,
                                    false),
                            otherPlayer);
                }
            }
        }
    }
    
    /**
     * Play distant eerie music for atmospheric scenes
     */
    public void playDistantMusic(ServerPlayer player) {
        // Get the registered sound event
        SoundEvent musicSound = SoundRegistry.DISTANT_MUSIC.get();

        // Play distant music with environmental sound properties
        NetworkHandler.sendToPlayer(
                new PlaySoundPacket(
                        musicSound.getLocation(),
                        SoundSource.MUSIC,
                        0.6f, // Moderate volume to not overwhelm
                        1.0f,
                        false,
                        true), // Locational to feel like it's coming from somewhere
                player);

        // If multiplayer synchronization is enabled, also play for nearby players
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    NetworkHandler.sendToPlayer(
                            new PlaySoundPacket(
                                    musicSound.getLocation(),
                                    SoundSource.MUSIC,
                                    0.6f,
                                    1.0f,
                                    false,
                                    true),
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