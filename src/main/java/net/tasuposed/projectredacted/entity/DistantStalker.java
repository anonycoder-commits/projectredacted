package net.tasuposed.projectredacted.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.tasuposed.projectredacted.config.HorrorConfig;

/**
 * "A guy that stalks #2" - an entity that spawns far away from player and 
 * either runs away and disappears or just disappears when they get too close.
 * Credit for the idea goes to a mod user.
 */
public class DistantStalker extends Monster {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistantStalker.class);
    
    // Following behavior
    private static final double RUN_DISTANCE = 48.0D; // Run/disappear at this distance
    
    // Disappear behavior
    private boolean shouldDisappear = false;
    private int disappearTimer = 0;
    private static final int DISAPPEAR_TIME = 15; // Ticks before disappearing
    
    // Inactivity timer - disappear if player doesn't get close
    private int inactivityTimer = 2400; // 2 minutes (120 seconds)
    
    public DistantStalker(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Custom goal to run away from players
        this.goalSelector.addGoal(1, new RunFromPlayerGoal(this));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.4D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D) // Doesn't attack directly
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide()) {
            // Find nearest player
            Player nearestPlayer = this.level().getNearestPlayer(this, 100.0D);
            
            if (nearestPlayer != null) {
                // Calculate distance to player
                double distanceToPlayer = this.distanceTo(nearestPlayer);
                
                // Run or disappear when player gets too close
                if (distanceToPlayer <= RUN_DISTANCE) {
                    // 50% chance to just disappear, 50% chance to run away
                    if (!shouldDisappear && random.nextBoolean()) {
                        // Set flag to disappear after a short delay
                        shouldDisappear = true;
                        disappearTimer = DISAPPEAR_TIME;
                    }
                }
                
                // If we're set to disappear, count down and do it
                if (shouldDisappear) {
                    disappearTimer--;
                    
                    if (disappearTimer <= 0) {
                        LOGGER.debug("DistantStalker disappearing due to player proximity");
                        this.discard();
                        return;
                    }
                }
            }
            
            // If no player interaction for too long, disappear
            inactivityTimer--;
            if (inactivityTimer <= 0) {
                LOGGER.debug("DistantStalker disappearing due to inactivity");
                this.discard();
            }
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
     * Spawn rules for this entity
     */
    public static boolean checkSpawnRules(EntityType<? extends Monster> entity, ServerLevelAccessor level, 
                                          MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // Check if community entities are enabled in config
        if (!HorrorConfig.ENABLE_COMMUNITY_ENTITIES.get()) {
            return false;
        }
        
        if (spawnType == MobSpawnType.NATURAL) {
            // Only spawn with configured chance
            if (random.nextInt(HorrorConfig.DISTANT_STALKER_SPAWN_CHANCE.get()) != 0) {
                return false;
            }
            
            // Check if there's a player nearby
            Player nearestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 100.0, false);
            
            if (nearestPlayer != null) {
                double distanceToPlayer = nearestPlayer.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                
                // Only spawn at a distance of 20-30 blocks (changed from 60-90 blocks)
                return distanceToPlayer >= 20*20 && distanceToPlayer <= 30*30;
            }
        }
        
        return Monster.checkMonsterSpawnRules(entity, level, spawnType, pos, random);
    }
    
    /**
     * Custom goal to make the entity run away from players
     */
    private static class RunFromPlayerGoal extends Goal {
        private final DistantStalker stalker;
        private Player fleeFrom;
        private double fleeX;
        private double fleeY;
        private double fleeZ;
        private int stareTime; // Time to stare at player before fleeing
        private boolean isStaring; // Flag to track if we're currently staring
        
        public RunFromPlayerGoal(DistantStalker stalker) {
            this.stalker = stalker;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            fleeFrom = this.stalker.level().getNearestPlayer(
                this.stalker, RUN_DISTANCE);
            
            if (fleeFrom == null) {
                return false;
            }
            
            Vec3 fleeVector = findFleeVector();
            if (fleeVector == null) {
                return false;
            }
            
            this.fleeX = fleeVector.x;
            this.fleeY = fleeVector.y;
            this.fleeZ = fleeVector.z;
            return true;
        }
        
        @Override
        public void start() {
            // Start by staring at the player
            isStaring = true;
            stareTime = 20 + this.stalker.random.nextInt(40); // Stare for 1-3 seconds
        }
        
        @Override
        public void tick() {
            if (fleeFrom == null) return;
            
            if (isStaring) {
                // Look at player while staring
                this.stalker.getLookControl().setLookAt(
                    fleeFrom.getX(),
                    fleeFrom.getEyeY(),
                    fleeFrom.getZ(),
                    30.0F,
                    30.0F
                );
                
                stareTime--;
                if (stareTime <= 0) {
                    // Done staring, start fleeing
                    isStaring = false;
                    this.stalker.getNavigation().moveTo(fleeX, fleeY, fleeZ, 1.2D);
                }
            }
        }
        
        @Override
        public boolean canContinueToUse() {
            return (isStaring || !this.stalker.getNavigation().isDone()) && 
                   fleeFrom != null && 
                   this.stalker.distanceToSqr(fleeFrom) < RUN_DISTANCE * RUN_DISTANCE;
        }
        
        private Vec3 findFleeVector() {
            if (fleeFrom == null) return null;
            
            Vec3 fleeDirection = new Vec3(
                stalker.getX() - fleeFrom.getX(),
                stalker.getY() - fleeFrom.getY(),
                stalker.getZ() - fleeFrom.getZ()
            ).normalize();
            
            double distance = 10.0;
            
            double targetX = stalker.getX() + fleeDirection.x * distance;
            double targetY = stalker.getY();
            double targetZ = stalker.getZ() + fleeDirection.z * distance;
            
            return new Vec3(targetX, targetY, targetZ);
        }
    }
} 