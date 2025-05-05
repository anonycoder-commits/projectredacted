package net.tasuposed.projectredacted.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.tasuposed.projectredacted.config.HorrorConfig;

/**
 * "Mining form of entity" - an invisible entity that spawns in caves and walks toward 
 * the player while breaking blocks. Credit for the idea goes to a mod user.
 */
public class MiningEntity extends Monster {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiningEntity.class);
    
    // Following behavior
    private static final double DISAPPEAR_DISTANCE = 2.5D; // Decreased from 4.0D - don't disappear as easily
    
    // Block breaking behavior
    private int breakBlockCooldown = 0;
    private static final int BLOCK_BREAK_INTERVAL = 20; // 1 second between breaking blocks
    
    // Disappearance behavior
    private static final int DISAPPEAR_CHANCE = 5; // 1 in 5 chance to hit player before disappearing
    private static final int TRANSFORM_CHANCE = 20; // 1 in 20 chance to transform before disappearing
    
    // Player tracking behavior
    private Player targetPlayer = null;
    private int pathfindFailures = 0;
    private static final int MAX_PATHFIND_FAILURES = 10; // Disappear after 10 successive failures
    private int followTimeout = 0;
    private static final int FOLLOW_TIMEOUT_MAX = 600; // 30 seconds before giving up if can't get closer
    
    // Player caching optimization
    private Player cachedPlayer = null;
    private int playerCacheTimer = 0;
    private static final int PLAYER_CACHE_REFRESH = 20; // Refresh player cache every second
    
    // Entity limits
    private static final int MAX_ENTITIES_PER_AREA = 2;
    private final Vec3 tempVec = new Vec3(0, 0, 0); // Reusable vector
    
    // Keep track of processed blocks to avoid trying the same ones repeatedly
    private final java.util.Set<BlockPos> recentlyProcessedBlocks = new java.util.HashSet<>();
    private int clearProcessedBlocksTimer = 100; // Clear list every 5 seconds
    
    public MiningEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setInvisible(true);
        this.noPhysics = false; // Still collides with blocks
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MoveToPlayerAndBreakBlocks(this));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.3D));
        
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Skip processing in dimensions where entities shouldn't operate
        if (this.level().dimension() == Level.END || 
            (this.level() instanceof ServerLevel && 
             ((ServerLevel)this.level()).dimension().location().toString().contains("void"))) {
            return;
        }
        
        // Only run logic if players are close enough to care
        if (!this.level().hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 48.0D)) {
            return;
        }
        
        if (!this.level().isClientSide()) {
            // Decrement cooldown
            if (breakBlockCooldown > 0) breakBlockCooldown--;
            
            // Clear processed blocks list periodically
            if (clearProcessedBlocksTimer-- <= 0) {
                recentlyProcessedBlocks.clear();
                clearProcessedBlocksTimer = 100;
            }
            
            // Update player cache
            if (playerCacheTimer <= 0) {
                cachedPlayer = this.level().getNearestPlayer(this, 48.0D);
                playerCacheTimer = PLAYER_CACHE_REFRESH;
                
                if (cachedPlayer != null) {
                    targetPlayer = cachedPlayer;
                }
            } else {
                playerCacheTimer--;
            }
            
            // Find nearest player (using cache)
            Player nearestPlayer = targetPlayer;
            if (nearestPlayer == null) {
                nearestPlayer = this.level().getNearestPlayer(this, 48.0D);
                targetPlayer = nearestPlayer;
            }
            
            if (nearestPlayer != null) {
                // Calculate distance to player
                double distanceToPlayer = this.distanceTo(nearestPlayer);
                
                // Always try to follow the player if not already following
                if (!this.getNavigation().isInProgress() && pathfindFailures < MAX_PATHFIND_FAILURES) {
                    boolean pathSuccess = this.getNavigation().moveTo(targetPlayer, 0.35D);
                    if (!pathSuccess) {
                        pathfindFailures++;
                        LOGGER.debug("MiningEntity pathfinding failed {} times", pathfindFailures);
                    } else {
                        pathfindFailures = 0;
                    }
                }
                
                // Track if we're getting closer to the player
                if (distanceToPlayer > 20.0D) {
                    followTimeout++;
                    if (followTimeout >= FOLLOW_TIMEOUT_MAX) {
                        LOGGER.debug("MiningEntity giving up after timeout - couldn't get closer to player");
                        this.discard();
                        return;
                    }
                } else {
                    // Reset timeout when we get closer
                    followTimeout = 0;
                }
                
                // Only play sounds when player is close enough to hear them
                if (distanceToPlayer < 16.0D) {
                    // More frequently play mining sounds with greater volume (once every 2-3 seconds)
                    if (random.nextInt(50) == 0) {  
                        // Randomize sound type for variety
                        boolean breakSound = random.nextBoolean();
                        if (breakSound) {
                            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                    SoundEvents.STONE_BREAK, SoundSource.BLOCKS,
                                    0.8F, 0.7F + random.nextFloat() * 0.3F); // Increased volume from 0.4F to 0.8F
                        } else {
                            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                    SoundEvents.STONE_HIT, SoundSource.BLOCKS,
                                    0.9F, 0.7F + random.nextFloat() * 0.3F); // Increased volume from 0.4F to 0.9F
                        }
                    }
                }
                
                // Check if player is too close
                if (distanceToPlayer < DISAPPEAR_DISTANCE) {
                    handleDisappearance(nearestPlayer);
                }
            }
            
            // Disappear if we've had too many pathfinding failures
            if (pathfindFailures >= MAX_PATHFIND_FAILURES) {
                LOGGER.debug("MiningEntity giving up due to pathfinding failures");
                this.discard();
            }
        }
    }
    
    /**
     * Break a block in front of the entity in the direction of movement
     */
    private void breakBlockInFront() {
        if (breakBlockCooldown > 0) return;
        
        try {
            // Get the direction to the player if we have a target
            Vec3 directionVec;
            if (targetPlayer != null) {
                // Calculate direction to target player
                directionVec = new Vec3(
                    targetPlayer.getX() - this.getX(),
                    targetPlayer.getY() - this.getY(),
                    targetPlayer.getZ() - this.getZ()
                ).normalize();
            } else {
                // Fallback to look direction if no target
                directionVec = this.getLookAngle();
            }
            
            // Break blocks in path to player - check more positions
            boolean brokeBlock = false;
            
            // Check blocks at player height and head height
            for (int distance = 1; distance <= 2 && !brokeBlock; distance++) {
                for (int heightOffset = 0; heightOffset <= 1 && !brokeBlock; heightOffset++) {
                    BlockPos checkPos = new BlockPos(
                        (int)(this.getX() + directionVec.x * distance),
                        (int)(this.getY() + heightOffset),
                        (int)(this.getZ() + directionVec.z * distance)
                    );
                    
                    // Skip if we've recently processed this block
                    if (recentlyProcessedBlocks.contains(checkPos)) continue;
                    
                    // Mark as processed
                    recentlyProcessedBlocks.add(checkPos);
                    
                    if (tryBreakBlock(checkPos)) {
                        brokeBlock = true;
                    }
                }
            }
            
            // Also check blocks above and below if we've encountered obstacles
            if (!brokeBlock && pathfindFailures > 0) {
                // Check blocks above and below in case of vertical obstacles
                for (int yOffset = -1; yOffset <= 2 && !brokeBlock; yOffset++) {
                    if (yOffset == 0 || yOffset == 1) continue; // Already checked these
                    
                    BlockPos checkPos = new BlockPos(
                        (int)(this.getX() + directionVec.x),
                        (int)(this.getY() + yOffset),
                        (int)(this.getZ() + directionVec.z)
                    );
                    
                    // Skip if we've recently processed this block
                    if (recentlyProcessedBlocks.contains(checkPos)) continue;
                    
                    // Mark as processed
                    recentlyProcessedBlocks.add(checkPos);
                    
                    if (tryBreakBlock(checkPos)) {
                        brokeBlock = true;
                    }
                }
                
                // Check blocks diagonally if we're still stuck
                if (!brokeBlock && pathfindFailures > 3) {
                    // Try breaking blocks to the sides
                    for (int xOffset = -1; xOffset <= 1 && !brokeBlock; xOffset += 2) {
                        for (int zOffset = -1; zOffset <= 1 && !brokeBlock; zOffset += 2) {
                            for (int yOffset = 0; yOffset <= 1 && !brokeBlock; yOffset++) {
                                BlockPos checkPos = new BlockPos(
                                    (int)(this.getX() + xOffset),
                                    (int)(this.getY() + yOffset),
                                    (int)(this.getZ() + zOffset)
                                );
                                
                                // Skip if we've recently processed this block
                                if (recentlyProcessedBlocks.contains(checkPos)) continue;
                                
                                // Mark as processed
                                recentlyProcessedBlocks.add(checkPos);
                                
                                if (tryBreakBlock(checkPos)) {
                                    brokeBlock = true;
                                }
                            }
                        }
                    }
                }
            }
            
            // Reset cooldown only if we broke a block or enough time has passed
            if (brokeBlock || random.nextInt(3) == 0) {
                breakBlockCooldown = BLOCK_BREAK_INTERVAL;
            }
        } catch (Exception e) {
            LOGGER.error("Error breaking blocks for MiningEntity", e);
            breakBlockCooldown = BLOCK_BREAK_INTERVAL; // Set cooldown to avoid spam errors
        }
    }
    
    /**
     * Try to break a specific block
     */
    private boolean tryBreakBlock(BlockPos pos) {
        try {
            BlockState state = this.level().getBlockState(pos);
            
            // Don't break air, bedrock, obsidian, or other very hard blocks
            if (!state.isAir() && 
                state.getBlock() != Blocks.BEDROCK && 
                state.getBlock() != Blocks.OBSIDIAN && 
                state.getBlock() != Blocks.END_PORTAL_FRAME &&
                state.getDestroySpeed(this.level(), pos) < 10.0F) {
                
                // Break the block with proper sound effects
                this.level().destroyBlock(pos, true);
                
                // Play breaking sound with slightly randomized pitch and higher volume
                this.level().playSound(null, pos, 
                        SoundEvents.STONE_BREAK, 
                        SoundSource.BLOCKS, 
                        1.0F,  // Full volume 
                        0.8F + random.nextFloat() * 0.4F); // Varied pitch
                        
                LOGGER.debug("MiningEntity broke a block at {}", pos);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            LOGGER.error("Error in tryBreakBlock for MiningEntity at " + pos, e);
            return false;
        }
    }
    
    /**
     * Handle behavior when player gets too close
     */
    private void handleDisappearance(Player player) {
        try {
            int roll = random.nextInt(100); // Changed from 20 to 100 for rarer effects
            
            // Very rare chance to transform into DistantStalker (1%)
            if (roll == 0) {
                // Try to spawn a DistantStalker entity at our position
                if (this.level() instanceof ServerLevelAccessor) {
                    EntityType<DistantStalker> type = EntityRegistry.DISTANT_STALKER.get();
                    DistantStalker stalker = type.create(this.level());
                    if (stalker != null) {
                        stalker.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                        this.level().addFreshEntity(stalker);
                        
                        // Schedule it to disappear shortly
                        stalker.setPersistenceRequired();
                        LOGGER.debug("MiningEntity transformed into DistantStalker at {},{},{}", 
                                this.getX(), this.getY(), this.getZ());
                    }
                }
            } 
            // Very rare chance to apply effects (2% instead of 25%)
            else if (roll < 3) { // Changed from DISAPPEAR_CHANCE (5) to just 3 out of 100
                this.doHurtTarget(player);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0)); // Reduced duration from 100 to 60 ticks (3 seconds)
                
                // Play a sound instead of a disruptive effect
                this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.0F, 0.8F);
            }
            
            // Always disappear
            this.discard();
        } catch (Exception e) {
            LOGGER.error("Error during MiningEntity disappearance", e);
            // Just disappear
            this.discard();
        }
    }
    
    /**
     * Custom goal to move to player and break blocks along the way
     */
    private class MoveToPlayerAndBreakBlocks extends Goal {
        private final MiningEntity entity;
        private Player target;
        private int pathUpdateTimer = 0;
        private static final int PATH_UPDATE_INTERVAL = 20; // Update path every second
        
        public MoveToPlayerAndBreakBlocks(MiningEntity entity) {
            this.entity = entity;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            // Use cached player if available
            target = entity.cachedPlayer;
            if (target == null) {
                target = entity.level().getNearestPlayer(entity, 32.0D);
            } else if (entity.distanceTo(target) > 32.0D) {
                target = null;
            }
            
            return target != null;
        }
        
        @Override
        public void start() {
            // Start moving to the player
            if (target != null) {
                entity.getNavigation().moveTo(target, 0.3D);
            }
        }
        
        @Override
        public void tick() {
            if (target != null) {
                // Only update path when needed to reduce path calculations
                pathUpdateTimer--;
                if (pathUpdateTimer <= 0 || !entity.getNavigation().isInProgress()) {
                    entity.getNavigation().moveTo(target, 0.35D);
                    pathUpdateTimer = PATH_UPDATE_INTERVAL;
                    
                    // Play a mining sound on path update with probability
                    if (entity.distanceTo(target) < 16.0D && entity.random.nextInt(3) == 0) {
                        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                                SoundEvents.STONE_BREAK, SoundSource.BLOCKS,
                                0.7F, 0.7F + entity.random.nextFloat() * 0.3F);
                    }
                }
                
                // Look at the player
                entity.getLookControl().setLookAt(
                    target.getX(), 
                    target.getEyeY(), 
                    target.getZ(), 
                    30.0F, 
                    30.0F
                );
                
                // Try to break blocks in our path at reduced frequency
                if (entity.tickCount % 5 == 0) {
                    entity.breakBlockInFront();
                }
            }
        }
        
        @Override
        public boolean canContinueToUse() {
            return target != null && 
                   target.isAlive() && 
                   entity.distanceTo(target) <= 32.0D && 
                   entity.distanceTo(target) >= DISAPPEAR_DISTANCE;
        }
        
        @Override
        public void stop() {
            entity.getNavigation().stop();
            target = null;
        }
    }
    
    @Override
    public void checkDespawn() {
        // Custom despawn logic to prevent normal despawning
        if (this.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            this.discard();
        }
    }
    
    /**
     * Custom spawn conditions for the mining entity
     * Greatly increased chance to spawn in caves
     */
    public static boolean checkSpawnRules(EntityType<? extends Monster> entity, ServerLevelAccessor level, 
                                         MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // Skip spawning in wrong dimensions
        if (level.dimensionType().natural() == false || 
            level.dimensionType().hasCeiling() || 
            (level instanceof ServerLevel && ((ServerLevel)level).dimension().location().toString().contains("void"))) {
            return false;
        }
        
        // Always allow command/event/spawn egg spawns
        if (spawnType == MobSpawnType.COMMAND || spawnType == MobSpawnType.EVENT || spawnType == MobSpawnType.SPAWN_EGG) {
            return true;
        }
        
        // For natural spawning, perform specific checks
        if (spawnType == MobSpawnType.NATURAL) {
            // Check entity cap in area
            if (level instanceof ServerLevel serverLevel) {
                int countInArea = serverLevel.getEntitiesOfClass(MiningEntity.class, 
                    net.minecraft.world.phys.AABB.ofSize(new Vec3(pos.getX(), pos.getY(), pos.getZ()), 128, 128, 128)).size();
                    
                if (countInArea >= MAX_ENTITIES_PER_AREA) {
                    return false;
                }
            }
        
            // Much higher chance to spawn (1 in MINING_ENTITY_SPAWN_CHANCE/10)
            // This effectively makes it 10x more likely to spawn
            int baseChance = HorrorConfig.MINING_ENTITY_SPAWN_CHANCE.get();
            int adjustedChance = Math.max(baseChance / 10, 1);
            
            // Check if we pass the chance check
            if (random.nextInt(adjustedChance) != 0) {
                return false;
            }
            
            // Check for appropriate distance from player (not too close, not too far)
            Player nearestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 32.0, false);
            if (nearestPlayer == null) {
                // No player within 32 blocks, don't spawn
                return false;
            }
            
            double distanceToPlayer = nearestPlayer.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            
            // Expanded spawn range - 6 to 20 blocks from player (squared for efficiency)
            if (distanceToPlayer < 6*6 || distanceToPlayer > 20*20) {
                // Too close or too far, don't spawn
                return false;
            }
            
            // Check if position is in a cave (has ceiling)
            boolean hasCeiling = false;
            for (int y = 1; y <= 20; y++) {
                BlockPos checkPos = pos.above(y);
                if (level.getBlockState(checkPos).isSolid()) {
                    hasCeiling = true;
                    break;
                }
            }
            
            // Check if below sea level (Y < 63 in most Minecraft worlds)
            boolean belowSeaLevel = pos.getY() < 63;
            
            // Check if there's a solid floor for mining sounds
            if (!level.getBlockState(pos.below()).isSolid()) {
                return false;
            }
            
            // Underground position (cave) - greatly increased chance to spawn
            if (hasCeiling) {
                // Prefer underground positions but not required
                return true;
            }
            
            // Surface spawning is now allowed (though with lower priority)
            return !level.canSeeSky(pos) || random.nextInt(3) == 0; // 33% chance to spawn on surface
        }
        
        // For non-natural spawns, use standard monster rules but with relaxed light requirements
        return level.getBlockState(pos.below()).isSolid() && 
               level.getBlockState(pos).isAir() && 
               level.getBlockState(pos.above()).isAir();
    }
    
    /**
     * Make entity unhittable by overriding damage method
     */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // If the source is a player, completely ignore the damage and provide no feedback
        if (source.getEntity() instanceof Player) {
            // Return true to indicate we "processed" the damage, but actually do nothing
            return true;
        }
        
        // For non-player damage sources, use the regular logic
        return super.hurt(source, amount);
    }
    
    /**
     * Prevent hit indicator from showing (client-side)
     */
    @Override
    public boolean skipAttackInteraction(net.minecraft.world.entity.Entity entity) {
        // Always skip attack interaction for players to hide feedback
        return entity instanceof Player || super.skipAttackInteraction(entity);
    }
} 