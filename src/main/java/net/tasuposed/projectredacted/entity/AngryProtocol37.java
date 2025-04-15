package net.tasuposed.projectredacted.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
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
 * "Protocol 37's angry form" - an aggressive version of Protocol 37 that chases 
 * and attacks the player. Credit for the idea goes to a mod user.
 */
public class AngryProtocol37 extends Monster {
    private static final Logger LOGGER = LoggerFactory.getLogger(AngryProtocol37.class);
    
    // Behavior configuration
    private int attackCooldown = 0;
    private static final int ATTACK_COOLDOWN_TIME = 60; // 3 seconds between attacks
    
    // Block breaking behavior
    private int breakBlockCooldown = 0;
    private static final int BLOCK_BREAK_INTERVAL = 40; // 2 seconds between breaking blocks
    
    // Target player
    private Player targetPlayer = null;
    
    // Lifespan management
    private int lifespan = 1200; // 1 minute maximum before giving up
    
    public AngryProtocol37(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        
        // Give it glowing eyes effect for visual distinction
        this.setGlowingTag(true);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new ChaseAndAttackGoal(this));
        
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.7D) // Much faster than Protocol_37 for intimidation
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Always keep glowing eyes effect
        this.setGlowingTag(true);
        
        if (!this.level().isClientSide()) {
            // Decrement cooldowns
            if (attackCooldown > 0) attackCooldown--;
            if (breakBlockCooldown > 0) breakBlockCooldown--;
            
            // Find player target if we don't have one
            if (targetPlayer == null) {
                targetPlayer = this.level().getNearestPlayer(this, 64.0D);
                if (targetPlayer != null) {
                    // Set immediate path to player
                    this.getNavigation().moveTo(targetPlayer, 1.0D);
                }
            }
            
            // Lifespan management
            lifespan--;
            if (lifespan <= 0) {
                LOGGER.debug("AngryProtocol37 despawning due to timeout");
                this.discard();
            }
        }
    }
    
    /**
     * Break a block in front of the entity if it's in the way
     */
    private void breakBlockInFront() {
        if (breakBlockCooldown > 0) return;
        
        // Get movement direction
        Vec3 lookVec = this.getLookAngle();
        
        // Check blocks in front at different heights
        for (int yOffset = -1; yOffset <= 2; yOffset++) {
            BlockPos checkPos = new BlockPos(
                (int)(this.getX() + lookVec.x * 1.5),
                (int)(this.getY() + yOffset),
                (int)(this.getZ() + lookVec.z * 1.5)
            );
            
            if (tryBreakBlock(checkPos)) {
                // Reset cooldown if we broke a block
                breakBlockCooldown = BLOCK_BREAK_INTERVAL;
                return;
            }
        }
    }
    
    /**
     * Try to break a specific block
     */
    private boolean tryBreakBlock(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        
        // Don't break air, bedrock, obsidian, or other very hard blocks
        if (!state.isAir() && 
            state.getBlock() != Blocks.BEDROCK && 
            state.getBlock() != Blocks.OBSIDIAN && 
            state.getBlock() != Blocks.END_PORTAL_FRAME &&
            state.getDestroySpeed(this.level(), pos) < 7.0F) {
            
            // Break the block
            this.level().destroyBlock(pos, true);
            this.level().playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            LOGGER.debug("AngryProtocol37 broke a block at {}", pos);
            return true;
        }
        
        return false;
    }
    
    /**
     * Attack the player and apply blindness effect
     */
    private void attackPlayerWithBlindness(Player player) {
        if (attackCooldown > 0) return;
        
        if (this.distanceTo(player) <= 2.0D) {
            boolean success = this.doHurtTarget(player);
            
            if (success) {
                // Apply blindness effect
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.BLINDNESS, 100, 0));
                
                // Disappear after attacking
                LOGGER.debug("AngryProtocol37 attacked player and will now disappear");
                this.discard();
            }
            
            attackCooldown = ATTACK_COOLDOWN_TIME;
        }
    }
    
    /**
     * Custom goal to chase player and break blocks
     */
    private class ChaseAndAttackGoal extends Goal {
        private final AngryProtocol37 entity;
        private Player target;
        private int pathUpdateCooldown = 0;
        
        public ChaseAndAttackGoal(AngryProtocol37 entity) {
            this.entity = entity;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            target = entity.level().getNearestPlayer(entity, 64.0D);
            return target != null;
        }
        
        @Override
        public void start() {
            if (target != null) {
                // Start chasing
                entity.getNavigation().moveTo(target, 1.0D);
                entity.targetPlayer = target;
            }
        }
        
        @Override
        public void tick() {
            if (target != null) {
                // Update path to player periodically
                if (pathUpdateCooldown <= 0) {
                    entity.getNavigation().moveTo(target, 1.0D);
                    pathUpdateCooldown = 10;
                } else {
                    pathUpdateCooldown--;
                }
                
                // Look at player
                entity.getLookControl().setLookAt(
                    target.getX(), 
                    target.getEyeY(), 
                    target.getZ(), 
                    30.0F, 
                    30.0F
                );
                
                // Try to break blocks in our way
                entity.breakBlockInFront();
                
                // Try to attack if close enough
                entity.attackPlayerWithBlindness(target);
            }
        }
        
        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && entity.distanceTo(target) <= 64.0D;
        }
        
        @Override
        public void stop() {
            entity.getNavigation().stop();
            entity.targetPlayer = null;
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
     * Spawn rules for this entity - not used for natural spawning
     * This entity should only be spawned when a Protocol_37 transforms
     */
    public static boolean checkSpawnRules(EntityType<? extends Monster> entity, ServerLevelAccessor level, 
                                          MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // Check if community entities are enabled in config
        if (!HorrorConfig.ENABLE_COMMUNITY_ENTITIES.get()) {
            return false;
        }
        
        // This entity shouldn't naturally spawn
        return spawnType != MobSpawnType.NATURAL;
    }
} 