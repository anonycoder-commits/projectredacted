package net.tasuposed.projectredacted.horror.events;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import net.tasuposed.projectredacted.config.HorrorConfig;
import net.tasuposed.projectredacted.entity.EntityRegistry;
import net.tasuposed.projectredacted.entity.Iteration;
import net.tasuposed.projectredacted.entity.MiningEntity;
import net.tasuposed.projectredacted.entity.Protocol_37;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.network.packets.GlitchEntityPacket;
import net.tasuposed.projectredacted.network.packets.GlitchScreenPacket;
import net.tasuposed.projectredacted.network.packets.RenderDistancePacket;

/**
 * Handles entity-related horror events
 */
public class EntityEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityEvent.class);
    private final Random random = new Random();
    
    // Radius for multiplayer entity experience
    private static final double MULTIPLAYER_SYNC_RADIUS = 50.0;
    
    /**
     * Spawn a temporary shadow figure in the player's peripheral vision
     */
    public void spawnTemporaryShadowFigure(ServerPlayer player) {
        // Choose a position in the player's peripheral vision
        Vec3 lookVec = player.getViewVector(1.0F).normalize();
        Vec3 sideVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();
        
        // Random side (left or right) and slight behind
        double side = random.nextBoolean() ? 1.0 : -1.0;
        Vec3 spawnPos = player.position()
                .add(lookVec.scale(10 + random.nextDouble() * 5)) // 10-15 blocks ahead
                .add(sideVec.scale(side * (5 + random.nextDouble() * 8))); // 5-13 blocks to the side
        
        // Create a temporary entity (something like an enderman would work well in a real implementation)
        // Use any mob that can be easily seen at a distance
        ServerLevel level = player.serverLevel();
        
        // In a real implementation, you'd spawn a custom entity here
        // For now, we'll just send a glitch entity packet to make an existing entity appear glitched
        List<Monster> nearbyMonsters = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(20.0));
        if (nearbyMonsters != null && !nearbyMonsters.isEmpty()) {
            // Find the nearest monster to glitch
            Monster entity = nearbyMonsters.get(0);
            
            // Send packet to triggering player and nearby players
            applyEffectToPlayerAndNearby(player, 
                p -> NetworkHandler.sendToPlayer(new GlitchEntityPacket(entity.getId()), p));
        } else {
            // No monsters found, log this event
            LOGGER.debug("No monsters found for shadow figure effect for player {}", player.getName().getString());
        }
    }
    
    /**
     * Spawn a glimpse of Iteration entity that quickly disappears
     */
    public void spawnIterationGlimpse(ServerPlayer player) {
        // Spawn a temporary Iteration that will despawn quickly
        ServerLevel level = player.serverLevel();
        
        // Position behind the player
        Vec3 lookVec = player.getViewVector(1.0F).normalize().scale(-10); // 10 blocks behind
        Vec3 spawnPos = player.position().add(lookVec);
        
        // Spawn and schedule removal
        spawnTemporaryEntity(level, EntityRegistry.ITERATION.get(), spawnPos, 60);
    }
    
    /**
     * Make a player see Protocol_37 in the distance with customizable parameters
     * This allows for flexible spawning across different stages
     * 
     * @param player The player who will see the entity
     * @param distance How far away the entity should spawn (in blocks)
     * @param duration How long before the entity disappears (in ticks)
     * @param glitchIntensity The intensity of the screen glitch effect (0.0-1.0)
     * @param sendMessage Whether to send a cryptic message to the player
     */
    public void spawnCustomProtocol37(ServerPlayer player, double distance, int duration, 
                                      float glitchIntensity, boolean sendMessage) {
        // Optional screen glitch effect
        if (glitchIntensity > 0) {
            // Apply glitch effects to player and nearby players
            applyEffectToPlayerAndNearby(player, p -> {
                NetworkHandler.sendToPlayer(new GlitchEntityPacket(p.getId()), p);
                
                float intensity = p == player ? glitchIntensity : glitchIntensity * 0.7f;
                NetworkHandler.sendToPlayer(new GlitchScreenPacket(0, intensity, 15), p);
            });
        }
        
        // Choose a position in the direction the player is looking
        Vec3 lookVec = player.getViewVector(1.0F).normalize().scale(distance);
        Vec3 spawnPos = player.position().add(lookVec)
                .add((random.nextDouble() - 0.5) * 3, 0, (random.nextDouble() - 0.5) * 3);
        
        // Try to find a suitable position for spawning
        ServerLevel level = player.serverLevel();
        BlockPos blockPos = new BlockPos((int)spawnPos.x, (int)spawnPos.y, (int)spawnPos.z);
        
        // Make sure we have a valid spawn position by finding ground
        boolean found = false;
        
        // First, check if the current position is valid
        if (level.getBlockState(blockPos.below()).isSolid() &&
            level.getBlockState(blockPos).isAir() &&
            level.getBlockState(blockPos.above()).isAir()) {
            
            // Current position is valid
            spawnPos = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
            found = true;
            LOGGER.info("Found valid Protocol_37 spawn position at initial position {}", blockPos);
        }
        
        // If initial position is not valid, search up and down more aggressively
        if (!found) {
            // First try -20 to +20 blocks vertically from initial position
            for (int y = -20; y <= 20; y++) {
                BlockPos checkPos = blockPos.offset(0, y, 0);
                
                if (level.getBlockState(checkPos.below()).isSolid() &&
                    level.getBlockState(checkPos).isAir() &&
                    level.getBlockState(checkPos.above()).isAir()) {
                    
                    // Update spawn position to this valid location
                    spawnPos = new Vec3(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
                    found = true;
                    
                    // Log the successful position
                    LOGGER.info("Found valid Protocol_37 spawn position at {}", checkPos);
                    break;
                }
            }
        }
        
        // If still no valid position, try around the player in a wider area
        if (!found) {
            // Try up to 10 random locations in a wider area
            for (int attempt = 0; attempt < 10; attempt++) {
                // Try a position within 30 blocks of player
                BlockPos randomPos = player.blockPosition().offset(
                    random.nextInt(60) - 30,
                    0,
                    random.nextInt(60) - 30
                );
                
                // Find ground at this random position by checking vertically
                for (int y = -20; y <= 20; y += 2) { // Check every other block to cover more ground quickly
                    BlockPos checkPos = randomPos.offset(0, y, 0);
                    
                    // Ensure position is valid
                    if (level.getBlockState(checkPos.below()).isSolid() &&
                        level.getBlockState(checkPos).isAir() &&
                        level.getBlockState(checkPos.above()).isAir()) {
                        
                        // Calculate distance to player
                        double distanceToPlayer = player.distanceToSqr(
                            checkPos.getX() + 0.5, 
                            checkPos.getY(), 
                            checkPos.getZ() + 0.5
                        );
                        
                        // Ensure it's not too close or too far (between 10-30 blocks)
                        if (distanceToPlayer >= 10*10 && distanceToPlayer <= 30*30) {
                            // Update spawn position to this valid location
                            spawnPos = new Vec3(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
                            found = true;
                            
                            // Log the successful fallback position
                            LOGGER.info("Found fallback Protocol_37 spawn position at {} - {} blocks from player", 
                                checkPos, Math.sqrt(distanceToPlayer));
                            break;
                        }
                    }
                }
                
                if (found) break;
            }
        }
        
        // If still no valid position, try spawning near the player as last resort
        if (!found) {
            LOGGER.warn("Could not find valid spawn position for Protocol_37 - spawning near player instead");
            BlockPos playerPos = player.blockPosition();
            
            // Try positions around the player (5-8 blocks away)
            for (int attempt = 0; attempt < 8; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double spawnDistance = 5 + random.nextInt(4); // 5-8 blocks
                
                int x = (int)(playerPos.getX() + Math.sin(angle) * spawnDistance);
                int z = (int)(playerPos.getZ() + Math.cos(angle) * spawnDistance);
                
                // Try to find a valid Y position
                for (int y = -5; y <= 5; y++) {
                    BlockPos checkPos = new BlockPos(x, playerPos.getY() + y, z);
                    
                    if (level.getBlockState(checkPos.below()).isSolid() &&
                        level.getBlockState(checkPos).isAir() &&
                        level.getBlockState(checkPos.above()).isAir()) {
                        
                        spawnPos = new Vec3(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
                        found = true;
                        LOGGER.info("Using emergency spawn position near player at {}", checkPos);
                        break;
                    }
                }
                
                if (found) break;
            }
        }
        
        // Spawn the entity with scheduled removal if duration > 0
        Protocol_37 entity = spawnTemporaryEntity(player.serverLevel(), 
                EntityRegistry.PROTOCOL_37.get(), spawnPos, duration);
        
        if (entity != null) {
            LOGGER.info("Successfully spawned Protocol_37 at {}", spawnPos);
        } else {
            LOGGER.error("Failed to spawn Protocol_37 entity at {}", spawnPos);
        }
    }
    
    /**
     * Spawn a permanent hostile Iteration entity that actively pursues the player
     * This version is more intimidating and is part of the attack sequence
     */
    public void spawnHostileIteration(ServerPlayer player, Vec3 spawnPos) {
        // Spawn the entity at the specified position
        ServerLevel level = player.serverLevel();
        
        // Create the entity with custom attributes
        Iteration entity = new Iteration(EntityRegistry.ITERATION.get(), level);
        entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        
        // Set the player as the target
        entity.setTarget(player);
        
        // Add the entity to the world
        level.addFreshEntity(entity);
        
        // Play a terrifying sound when it spawns
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.0F, 0.5F);
    }
    
    /**
     * Helper method to apply an effect to a player and nearby players
     */
    private void applyEffectToPlayerAndNearby(ServerPlayer player, Consumer<ServerPlayer> effect) {
        // Apply to the main player
        effect.accept(player);
        
        // If multiplayer syncing is enabled, also apply to nearby players
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    effect.accept(otherPlayer);
                }
            }
        }
    }
    
    /**
     * Helper method to send messages to a player and nearby players
     */
    private void sendMessageToPlayerAndNearby(ServerPlayer player, String mainMessage, String nearbyMessage) {
        // Send main message to the triggering player
        player.sendSystemMessage(Component.literal(mainMessage));
        
        // If multiplayer syncing is enabled, send nearby message to other players
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    otherPlayer.sendSystemMessage(Component.literal(nearbyMessage));
                }
            }
        }
    }
    
    /**
     * Helper method to spawn a temporary entity with optional auto-removal
     * @return The spawned entity
     */
    private <T extends LivingEntity> T spawnTemporaryEntity(ServerLevel level, EntityType<T> entityType, 
                                                          Vec3 pos, int duration) {
        try {
            // Create and position the entity
            T entity = entityType.create(level);
            if (entity == null) {
                LOGGER.error("Failed to create entity of type {}", entityType.getDescriptionId());
                return null;
            }
            
            entity.setPos(pos.x, pos.y, pos.z);
            
            // Add to the world
            if (!level.addFreshEntity(entity)) {
                LOGGER.error("Failed to add entity to the world");
                return null;
            }
            
            // Schedule it to be removed after specified duration (if duration > 0)
            if (duration > 0) {
                level.getServer().tell(new net.minecraft.server.TickTask(
                        level.getServer().getTickCount() + duration, entity::discard));
            }
            
            return entity;
        } catch (Exception e) {
            LOGGER.error("Exception spawning entity: ", e);
            return null;
        }
    }
    
    /**
     * Get a list of all nearby players to share the experience with
     */
    private List<ServerPlayer> getNearbyPlayers(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (level.getServer() == null) {
            return java.util.Collections.emptyList();
        }
        
        return level.getServer().getPlayerList().getPlayers();
    }
    
    /**
     * Check if multiplayer synchronization is enabled
     */
    private boolean shouldSyncMultiplayer() {
        return HorrorConfig.SYNC_MULTIPLAYER_EVENTS.get();
    }
    
    /**
     * Make a player see Protocol_37 in the distance
     * This is the standard method used by later stages
     */
    public void spawnDistantProtocol37(ServerPlayer player) {
        // Use the customizable method with standard parameters for the Obvious stage
        // Reduced distance for better visibility in caves
        spawnCustomProtocol37(player, 12.0, 0, 0.4f, false);
    }
    
    /**
     * Temporarily reduces the player's render distance to create a claustrophobic or foggy effect
     * 
     * @param player The player to affect
     * @param renderDistance The reduced render distance (2-5 chunks recommended for horror effect)
     * @param duration Duration in ticks before returning to normal render distance
     * @param fadeEffect Whether to apply a visual fade effect during the transition
     */
    public void reduceRenderDistance(ServerPlayer player, int renderDistance, int duration, boolean fadeEffect) {
        // Send the render distance packet to the player
        NetworkHandler.sendToPlayer(
                new RenderDistancePacket(renderDistance, duration, fadeEffect),
                player);
        
        // If multiplayer syncing is enabled, also apply to nearby players with reduced effects
        if (shouldSyncMultiplayer()) {
            for (ServerPlayer otherPlayer : getNearbyPlayers(player)) {
                if (otherPlayer != player && otherPlayer.distanceTo(player) < MULTIPLAYER_SYNC_RADIUS) {
                    // Apply to nearby players with slightly less intense effect
                    int nearbyRenderDistance = Math.min(renderDistance + 2, 8); // Less severe for nearby players
                    int nearbyDuration = duration / 2; // Shorter duration
                    
                    NetworkHandler.sendToPlayer(
                            new RenderDistancePacket(nearbyRenderDistance, nearbyDuration, fadeEffect),
                            otherPlayer);
                }
            }
        }
    }
    
    /**
     * Check if a player is underground (below sea level and has blocks above)
     */
    public boolean isPlayerUnderground(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        
        // Check if player is below sea level (Y < 63 in most Minecraft worlds)
        boolean belowSeaLevel = pos.getY() < 63;
        
        // Check if there are solid blocks above the player (cave detection)
        boolean hasCeiling = false;
        for (int y = 1; y <= 20; y++) {
            BlockPos checkPos = pos.above(y);
            if (level.getBlockState(checkPos).isSolid()) {
                hasCeiling = true;
                break;
            }
        }
        
        // Player is considered underground if below sea level and has a ceiling
        return belowSeaLevel && hasCeiling;
    }
    
    /**
     * Get the spawn chance multiplier based on player's environment
     * Returns higher values for underground environments
     */
    public float getEnvironmentSpawnMultiplier(ServerPlayer player) {
        // Base multiplier
        float multiplier = 1.0f;
        
        // Check if player is underground
        if (isPlayerUnderground(player)) {
            // Significantly increase spawn chances underground
            multiplier *= 2.5f;
            
            // Further increase if player is deep underground
            if (player.blockPosition().getY() < 40) {
                multiplier *= 1.5f;
            }
        }
        
        return multiplier;
    }
    
    /**
     * Spawn a MiningEntity at an appropriate distance from the player (8-16 blocks away)
     * This creates an entity that will follow the player while breaking blocks
     * 
     * @param player The player to spawn the entity for
     * @return The spawned entity, or null if spawning failed
     */
    public MiningEntity spawnMiningEntity(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        
        // Try to find a suitable spawn position
        // We want the entity to be 8-16 blocks away from the player
        Vec3 spawnPos = null;
        boolean foundPosition = false;
        BlockPos playerPos = player.blockPosition();
        
        // First try directly behind the player
        Vec3 behindPlayer = player.position().subtract(
                player.getViewVector(1.0F).normalize().scale(12)); // 12 blocks behind
        
        // Check if this position is valid
        BlockPos behindPos = new BlockPos((int)behindPlayer.x, (int)behindPlayer.y, (int)behindPlayer.z);
        if (isValidSpawnPosition(level, behindPos)) {
            spawnPos = new Vec3(behindPos.getX() + 0.5, behindPos.getY(), behindPos.getZ() + 0.5);
            foundPosition = true;
            LOGGER.debug("Found valid MiningEntity spawn position behind player at {}", behindPos);
        }
        
        // If behind position didn't work, try random positions
        if (!foundPosition) {
            // Try several random positions at different angles
            for (int attempt = 0; attempt < 10; attempt++) {
                // Random angle around the player
                double angle = random.nextDouble() * Math.PI * 2;
                // Distance between 8-16 blocks
                double distance = 8 + random.nextDouble() * 8;
                
                // Calculate position
                double x = player.getX() + Math.sin(angle) * distance;
                double z = player.getZ() + Math.cos(angle) * distance;
                
                // Find a suitable Y position (on solid ground)
                BlockPos basePos = new BlockPos((int)x, playerPos.getY(), (int)z);
                
                // Check positions at player height and below
                for (int yOffset = 0; yOffset >= -5; yOffset--) {
                    BlockPos checkPos = basePos.offset(0, yOffset, 0);
                    
                    if (isValidSpawnPosition(level, checkPos)) {
                        spawnPos = new Vec3(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
                        foundPosition = true;
                        LOGGER.debug("Found valid MiningEntity spawn position at {} (attempt {})", checkPos, attempt + 1);
                        break;
                    }
                }
                
                if (foundPosition) break;
            }
        }
        
        // If we couldn't find a valid position, log and return null
        if (!foundPosition || spawnPos == null) {
            LOGGER.warn("Could not find valid spawn position for MiningEntity near player {}", player.getName().getString());
            return null;
        }
        
        // Spawn the entity
        MiningEntity entity = spawnTemporaryEntity(level, EntityRegistry.MINING_ENTITY.get(), spawnPos, 0);
        
        if (entity != null) {
            LOGGER.info("Successfully spawned MiningEntity at {} at distance {}", 
                    spawnPos, player.position().distanceTo(spawnPos));
            
            // Play a distant mining sound as a warning
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.STONE_BREAK, SoundSource.BLOCKS,
                    0.7F, 0.8F + random.nextFloat() * 0.3F);
        } else {
            LOGGER.error("Failed to spawn MiningEntity at {}", spawnPos);
        }
        
        return entity;
    }
    
    /**
     * Helper method to check if a position is valid for spawning a MiningEntity
     */
    private boolean isValidSpawnPosition(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid() && // Solid block below
               level.getBlockState(pos).isAir() &&          // Air at position
               level.getBlockState(pos.above()).isAir();    // Air above position
    }
} 