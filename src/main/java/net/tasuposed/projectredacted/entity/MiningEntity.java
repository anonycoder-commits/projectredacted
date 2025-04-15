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
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide()) {
            // Decrement cooldown
            if (breakBlockCooldown > 0) breakBlockCooldown--;
            
            // Find nearest player
            Player nearestPlayer = this.level().getNearestPlayer(this, 48.0D);
            if (nearestPlayer != null) {
                // Calculate distance to player
                double distanceToPlayer = this.distanceTo(nearestPlayer);
                
                // Occasionally play distant mining sounds to indicate presence
                if (random.nextInt(100) == 0) {  // Roughly once every 5 seconds
                    // Randomize sound type for variety
                    SoundEvent sound = random.nextBoolean() ? 
                            SoundEvents.STONE_BREAK : SoundEvents.STONE_HIT;
                    
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            sound, SoundSource.BLOCKS,
                            0.4F, 0.7F + random.nextFloat() * 0.3F);
                }
                
                // Check if player is too close
                if (distanceToPlayer < DISAPPEAR_DISTANCE) {
                    handleDisappearance(nearestPlayer);
                }
            }
        }
    }
    
    /**
     * Break a block in front of the entity in the direction of movement
     */
    private void breakBlockInFront() {
        if (breakBlockCooldown > 0) return;
        
        // Get the block in front
        Vec3 lookVec = this.getLookAngle();
        BlockPos frontPos = new BlockPos(
            (int)(this.getX() + lookVec.x * 1.5),
            (int)(this.getY() + lookVec.y),
            (int)(this.getZ() + lookVec.z * 1.5)
        );
        
        // Also check the block above (to create a 2-block high passage)
        BlockPos frontPosUpper = frontPos.above();
        
        // Try to break these blocks if they're not air and not bedrock/obsidian
        tryBreakBlock(frontPos);
        tryBreakBlock(frontPosUpper);
        
        // Reset cooldown
        breakBlockCooldown = BLOCK_BREAK_INTERVAL;
    }
    
    /**
     * Try to break a specific block
     */
    private void tryBreakBlock(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        
        // Don't break air, bedrock, obsidian, or other very hard blocks
        if (!state.isAir() && 
            state.getBlock() != Blocks.BEDROCK && 
            state.getBlock() != Blocks.OBSIDIAN && 
            state.getBlock() != Blocks.END_PORTAL_FRAME &&
            state.getDestroySpeed(this.level(), pos) < 10.0F) {
            
            // Break the block
            this.level().destroyBlock(pos, true);
            this.level().playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            LOGGER.debug("MiningEntity broke a block at {}", pos);
        }
    }
    
    /**
     * Handle behavior when player gets too close
     */
    private void handleDisappearance(Player player) {
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
    }
    
    /**
     * Custom goal to move to player and break blocks along the way
     */
    private class MoveToPlayerAndBreakBlocks extends Goal {
        private final MiningEntity entity;
        private Player target;
        
        public MoveToPlayerAndBreakBlocks(MiningEntity entity) {
            this.entity = entity;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            target = entity.level().getNearestPlayer(entity, 32.0D);
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
                // Update path if player moved
                if (!entity.getNavigation().isInProgress() || 
                    entity.random.nextInt(20) == 0) {
                    entity.getNavigation().moveTo(target, 0.3D);
                }
                
                // Look at the player
                entity.getLookControl().setLookAt(
                    target.getX(), 
                    target.getEyeY(), 
                    target.getZ(), 
                    30.0F, 
                    30.0F
                );
                
                // Try to break blocks in our path
                entity.breakBlockInFront();
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
     * Spawn rules for this entity - only in caves
     */
    public static boolean checkSpawnRules(EntityType<? extends Monster> entity, ServerLevelAccessor level, 
                                          MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // Always allow command/event spawning for debugging
        if (spawnType == MobSpawnType.COMMAND || spawnType == MobSpawnType.EVENT || spawnType == MobSpawnType.SPAWN_EGG) {
            return true;
        }
        
        // Check if community entities are enabled in config
        if (!HorrorConfig.ENABLE_COMMUNITY_ENTITIES.get()) {
            return false;
        }
        
        // Basic spawn requirements
        // Check if spawn location has enough space and solid ground
        if (!level.getBlockState(pos).isAir() || 
            !level.getBlockState(pos.above()).isAir() || 
            !level.getBlockState(pos.below()).isSolid()) {
            return false;
        }
        
        if (spawnType == MobSpawnType.NATURAL) {
            // Only spawn with configured chance, but decreased to make spawning more likely
            int spawnChance = Math.max(1, HorrorConfig.MINING_ENTITY_SPAWN_CHANCE.get() / 2);
            if (random.nextInt(spawnChance) != 0) {
                return false;
            }
            
            // Check if we're underground - MUST be underground
            boolean isUnderground = !level.canSeeSky(pos);
            
            if (!isUnderground) {
                return false;
            }
            
            // Check if there's a player nearby
            Player nearestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 100.0, false);
            
            if (nearestPlayer != null) {
                double distanceToPlayer = nearestPlayer.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                
                // Spawn much closer to player (4-5 blocks away) - for more immediate scary effect
                boolean isInRange = distanceToPlayer >= 4*4 && distanceToPlayer <= 5*5;
                
                // Debug messages to help troubleshoot spawning
                if (isInRange) {
                    LOGGER.debug("MiningEntity spawn conditions met at ({}, {}, {}) - {} blocks from player", 
                            pos.getX(), pos.getY(), pos.getZ(), Math.sqrt(distanceToPlayer));
                }
                
                return isInRange;
            }
        }
        
        return Monster.checkMonsterSpawnRules(entity, level, spawnType, pos, random);
    }
} 