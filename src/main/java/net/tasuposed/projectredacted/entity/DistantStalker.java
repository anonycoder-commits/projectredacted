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
import net.minecraft.server.level.ServerLevel;
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
    private static final int PARTICLE_INTERVAL = 5; // Reduced from 15 - more frequent particles
    private static final int REPOSITION_CHECK_INTERVAL = 60; // Reduced from 100 - more frequent repositioning checks
    
    // Player caching optimization
    private Player cachedPlayer = null;
    private int playerCacheTimer = 0;
    private static final int PLAYER_CACHE_REFRESH = 20; // Refresh player cache every second
    
    // Dimension validation
    private static final int MAX_ENTITIES_PER_AREA = 3;
    private final Vec3 tempVec = new Vec3(0, 0, 0); // Reusable vector
    
    public DistantStalker(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        // Remove glowing tag and replace with creepy nametag
        this.setCustomName(net.minecraft.network.chat.Component.literal("§8§kX§r"));
        this.setCustomNameVisible(true);
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
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D); // Added complete knockback immunity
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
        if (!this.level().hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 100.0D)) {
            // Still decrement inactivity timer even when players aren't nearby
            inactivityTimer--;
            if (inactivityTimer <= 0) {
                this.discard();
            }
            return;
        }
        
        // Always ensure custom name is visible (both client and server)
        if (!this.hasCustomName()) {
            this.setCustomName(net.minecraft.network.chat.Component.literal("§8§kX§r"));
        }
        this.setCustomNameVisible(true);
        
        if (!this.level().isClientSide()) {
            // Update player cache
            if (playerCacheTimer <= 0) {
                cachedPlayer = this.level().getNearestPlayer(this, 100.0D);
                playerCacheTimer = PLAYER_CACHE_REFRESH;
            } else {
                playerCacheTimer--;
            }
            
            // Find nearest player (using cache)
            Player nearestPlayer = cachedPlayer;
            
            if (nearestPlayer != null) {
                // Always look at the player regardless of other behaviors
                this.getLookControl().setLookAt(
                    nearestPlayer.getX(),
                    nearestPlayer.getEyeY(),
                    nearestPlayer.getZ(),
                    30.0F,
                    30.0F
                );
                
                // Calculate distance to player
                double distanceToPlayer = this.distanceTo(nearestPlayer);
                
                // Check visibility every 5 ticks to reduce unnecessary ray casting
                if (this.tickCount % 5 == 0) {
                    // Check if player has line of sight to this entity (can see it)
                    boolean canPlayerSee = this.level().clip(new ClipContext(
                        new Vec3(nearestPlayer.getX(), nearestPlayer.getEyeY(), nearestPlayer.getZ()),
                        new Vec3(this.getX(), this.getEyeY(), this.getZ()),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        nearestPlayer
                    )).getType() == HitResult.Type.MISS;
                    
                    // Disappear when player sees the entity (has line of sight)
                    if (canPlayerSee && distanceToPlayer < 64.0) {
                        // Set flag to disappear after a short delay
                        if (!shouldDisappear) {
                            shouldDisappear = true;
                            disappearTimer = DISAPPEAR_TIME;
                            LOGGER.debug("DistantStalker noticed by player - preparing to disappear");
                        }
                    }
                }
                
                // If we're set to disappear, count down and do it
                if (shouldDisappear) {
                    disappearTimer--;
                    
                    if (disappearTimer <= 0) {
                        LOGGER.debug("DistantStalker disappearing due to being spotted");
                        
                        // Remove particle effect before disappearing
                        this.discard();
                        return;
                    }
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
        // Particle emission completely removed
    }
    
    /**
     * Try to reposition the entity to a more visible location if necessary
     */
    private void tryRepositionToVisibleLocation(Player player) {
        // Only run this method if we're in a valid dimension
        if (this.level().dimension() == Level.END || 
            (this.level() instanceof ServerLevel && 
             ((ServerLevel)this.level()).dimension().location().toString().contains("void"))) {
            return;
        }
        
        try {
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
                for (int attempt = 0; attempt < 15; attempt++) { // Increased attempts from 10 to 15
                    // Calculate random position within desired distance range (15-25 blocks) - closer range
                    double distance = 15.0 + random.nextDouble() * 10.0;
                    double angle = random.nextDouble() * Math.PI * 2;
                    
                    double x = player.getX() + Math.sin(angle) * distance;
                    double z = player.getZ() + Math.cos(angle) * distance;
                    
                    // Find a Y position that's visible and has solid ground - wider vertical range
                    for (int yOffset = -3; yOffset <= 15; yOffset++) {
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
                                
                                // Removed particle effects
                                
                                LOGGER.debug("DistantStalker repositioned to more visible location at {},{},{}", x, y, z);
                                return;
                            }
                        }
                    }
                }
                
                // If repositioning failed, try a more drastic approach - move much closer to player to be seen
                if (random.nextInt(3) == 0) { // 33% chance
                    double closeDistance = 8.0 + random.nextDouble() * 4.0; // 8-12 blocks from player
                    double angle = random.nextDouble() * Math.PI * 2;
                    
                    // Get position in player's line of sight
                    Vec3 lookVec = player.getViewVector(1.0F).normalize().scale(closeDistance);
                    Vec3 lookPos = player.position().add(lookVec);
                    
                    // Try to find ground
                    BlockPos basePos = new BlockPos((int)lookPos.x, (int)player.getY(), (int)lookPos.z);
                    for (int yOffset = -2; yOffset <= 5; yOffset++) {
                        BlockPos checkPos = basePos.offset(0, yOffset, 0);
                        
                        if (this.level().getBlockState(checkPos.below()).isSolid() && 
                            this.level().getBlockState(checkPos).isAir() && 
                            this.level().getBlockState(checkPos.above()).isAir()) {
                            
                            // Teleport to position in front of player
                            this.teleportTo(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
                            
                            // Removed particle effects
                            
                            LOGGER.debug("DistantStalker emergency repositioning directly in player's view at {}", checkPos);
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to reposition DistantStalker", e);
            // No need to discard, just continue operating
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
        
        if (spawnType == MobSpawnType.NATURAL) {
            // Check entity cap in area
            if (level instanceof ServerLevel serverLevel) {
                int countInArea = serverLevel.getEntitiesOfClass(DistantStalker.class, 
                    net.minecraft.world.phys.AABB.ofSize(new Vec3(pos.getX(), pos.getY(), pos.getZ()), 128, 128, 128)).size();
                    
                if (countInArea >= MAX_ENTITIES_PER_AREA) {
                    return false;
                }
            }
        
            // Much higher chance to spawn (1 in DISTANT_STALKER_SPAWN_CHANCE/10)
            // This effectively makes it 10x more likely to spawn
            int adjustedChance = Math.max(HorrorConfig.DISTANT_STALKER_SPAWN_CHANCE.get() / 10, 1);
            if (random.nextInt(adjustedChance) != 0) {
                return false;
            }
            
            // Check if there's a player nearby
            Player nearestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 100.0, false);
            
            if (nearestPlayer != null) {
                double distanceToPlayer = nearestPlayer.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                
                // Increased spawn range from 20-30 to 15-40 blocks
                if (distanceToPlayer >= 15*15 && distanceToPlayer <= 40*40) {
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
        
        public RunFromPlayerGoal(DistantStalker stalker) {
            this.stalker = stalker;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            // Use cached player if available
            fleeFrom = stalker.cachedPlayer;
            if (fleeFrom == null) {
                fleeFrom = this.stalker.level().getNearestPlayer(
                    this.stalker, RUN_DISTANCE);
            } else if (stalker.distanceTo(fleeFrom) > RUN_DISTANCE) {
                fleeFrom = null;
            }
            
            if (fleeFrom == null) {
                return false;
            }
            
            // Only check visibility every 5 ticks to reduce ray casting
            if (this.stalker.tickCount % 5 == 0) {
                // Check if player can see the entity
                boolean canPlayerSee = this.stalker.level().clip(new ClipContext(
                    new Vec3(fleeFrom.getX(), fleeFrom.getEyeY(), fleeFrom.getZ()),
                    new Vec3(this.stalker.getX(), this.stalker.getEyeY(), this.stalker.getZ()),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    fleeFrom
                )).getType() == HitResult.Type.MISS;
                
                // If player can see us, don't use this goal - we'll disappear instead
                if (canPlayerSee) {
                    return false;
                }
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
            // Start fleeing immediately without staring first
            this.stalker.getNavigation().moveTo(fleeX, fleeY, fleeZ, 1.2D);
        }
        
        @Override
        public void tick() {
            if (fleeFrom == null) return;
            
            // Look at player always (this will be overridden by the main tick method)
            this.stalker.getLookControl().setLookAt(
                fleeFrom.getX(),
                fleeFrom.getEyeY(),
                fleeFrom.getZ(),
                30.0F,
                30.0F
            );
            
            // Check navigation and update if needed
            if (!this.stalker.getNavigation().isInProgress()) {
                Vec3 fleeVector = findFleeVector();
                if (fleeVector != null) {
                    this.fleeX = fleeVector.x;
                    this.fleeY = fleeVector.y;
                    this.fleeZ = fleeVector.z;
                    this.stalker.getNavigation().moveTo(fleeX, fleeY, fleeZ, 1.2D);
                }
            }
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