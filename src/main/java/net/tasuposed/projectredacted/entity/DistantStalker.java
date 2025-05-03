package net.tasuposed.projectredacted.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
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
    private int inactivityTimer = 4800; // Increased from 2400 to 4800 (4 minutes)
    
    // Visibility enhancement
    private int particleTimer = 0;
    private int repositionTimer = 0;
    private static final int PARTICLE_INTERVAL = 15; // Every 15 ticks
    private static final int REPOSITION_CHECK_INTERVAL = 100; // Check every 5 seconds
    
    public DistantStalker(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setGlowingTag(true); // Make the entity glow for better visibility
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
                
                // Emit particles occasionally to make entity more visible
                particleTimer--;
                if (particleTimer <= 0) {
                    emitVisibilityParticles();
                    particleTimer = PARTICLE_INTERVAL;
                }
                
                // Check if we need to reposition to a more visible location
                repositionTimer--;
                if (repositionTimer <= 0) {
                    tryRepositionToVisibleLocation(nearestPlayer);
                    repositionTimer = REPOSITION_CHECK_INTERVAL;
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
    
    /**
     * Emit particles to make entity more visible in dense terrain
     */
    private void emitVisibilityParticles() {
        // Every third particle interval, create a larger burst
        boolean largeBurst = particleTimer % (PARTICLE_INTERVAL * 3) == 0;
        
        // Get particle count based on burst size
        int particleCount = largeBurst ? 15 : 5;
        
        // Create particles around the entity
        for (int i = 0; i < particleCount; i++) {
            double offsetX = random.nextDouble() * 1.0 - 0.5;
            double offsetY = random.nextDouble() * 2.0;
            double offsetZ = random.nextDouble() * 1.0 - 0.5;
            
            this.level().addParticle(
                ParticleTypes.SMOKE,
                this.getX() + offsetX,
                this.getY() + 1.0 + offsetY,
                this.getZ() + offsetZ,
                0, 0.05, 0
            );
        }
    }
    
    /**
     * Try to reposition the entity to a more visible location if necessary
     */
    private void tryRepositionToVisibleLocation(Player player) {
        // Check if player has line-of-sight to this entity
        boolean canPlayerSee = this.level().clip(new ClipContext(
            new Vec3(player.getX(), player.getEyeY(), player.getZ()),
            new Vec3(this.getX(), this.getEyeY(), this.getZ()),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        )).getType() == HitResult.Type.MISS;
        
        // Only reposition if player can't see us and we're far enough away
        if (!canPlayerSee && this.distanceTo(player) > 10.0) {
            // Find a better visible position around the player
            for (int attempt = 0; attempt < 10; attempt++) {
                // Calculate random position within desired distance range (20-30 blocks)
                double distance = 20.0 + random.nextDouble() * 10.0;
                double angle = random.nextDouble() * Math.PI * 2;
                
                double x = player.getX() + Math.sin(angle) * distance;
                double z = player.getZ() + Math.cos(angle) * distance;
                
                // Find a Y position that's visible and has solid ground
                for (int yOffset = -5; yOffset <= 10; yOffset++) {
                    int y = (int)(player.getY() + yOffset);
                    BlockPos pos = new BlockPos((int)x, y, (int)z);
                    
                    // Check if this position has solid ground and open space
                    if (this.level().getBlockState(pos.below()).isSolid() && 
                        this.level().getBlockState(pos).isAir() && 
                        this.level().getBlockState(pos.above()).isAir()) {
                        
                        // Check if player would have line-of-sight to this position
                        boolean wouldBeVisible = this.level().clip(new ClipContext(
                            new Vec3(player.getX(), player.getEyeY(), player.getZ()),
                            new Vec3(x, y + 1.0, z),
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,
                            player
                        )).getType() == HitResult.Type.MISS;
                        
                        if (wouldBeVisible) {
                            // Found a visible position - teleport there
                            this.teleportTo(x, y, z);
                            
                            // Create smoke effect at old and new positions
                            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                                serverLevel.sendParticles(
                                    ParticleTypes.LARGE_SMOKE,
                                    this.getX(), this.getY() + 1.0, this.getZ(),
                                    10, 0.2, 0.5, 0.2, 0.02
                                );
                            }
                            
                            LOGGER.debug("DistantStalker repositioned to more visible location at {},{},{}", x, y, z);
                            return;
                        }
                    }
                }
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
                
                // Only spawn at a distance of 20-30 blocks
                if (distanceToPlayer >= 20*20 && distanceToPlayer <= 30*30) {
                    // Only check for a solid block below, ignore other Monster spawn rules
                    // that would restrict biomes and require light level checks
                    return level.getBlockState(pos.below()).isSolid() && 
                           level.getBlockState(pos).isAir() && 
                           level.getBlockState(pos.above()).isAir();
                }
            }
            return false;
        }
        
        // For non-natural spawns, use standard monster rules
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