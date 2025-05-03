package net.tasuposed.projectredacted.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.tasuposed.projectredacted.config.HorrorConfig;

/**
 * A mysterious entity that watches players from a distance
 * and disappears when approached
 */
public class Protocol_37 extends Monster {
    private static final Logger LOGGER = LoggerFactory.getLogger(Protocol_37.class);
    
    private int disappearTimer = 0;
    private boolean shouldDisappear = false;
    
    // New field to track existence time
    private int existenceTimer = 2400; // 2 minutes lifetime if not seen (increased from 400)
    private boolean hasBeenSeen = false; // Track if any player has seen the entity
    
    public Protocol_37(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Replace LookAtPlayerGoal with our custom implementation
        this.goalSelector.addGoal(1, new SafeLookAtPlayerGoal(this, 64.0F));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }
    
    /**
     * Define default attribute values
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)  // Increased from 20.0D
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D) // Doesn't attack directly
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);  // Added knockback immunity
    }
    
    /**
     * Custom goal that looks at players safely without NPEs
     */
    private class SafeLookAtPlayerGoal extends Goal {
        private Player lookTarget;
        private final float maxLookDistance;
        private int lookTime;
        
        public SafeLookAtPlayerGoal(Protocol_37 entity, float maxDistance) {
            this.maxLookDistance = maxDistance;
            this.setFlags(java.util.EnumSet.of(Goal.Flag.LOOK));
        }
        
        @Override
        public boolean canUse() {
            lookTarget = level().getNearestPlayer(Protocol_37.this, maxLookDistance);
            return lookTarget != null;
        }
        
        @Override
        public boolean canContinueToUse() {
            if (lookTarget == null || !lookTarget.isAlive()) {
                return false;
            }
            
            if (Protocol_37.this.distanceToSqr(lookTarget) > maxLookDistance * maxLookDistance) {
                return false;
            }
            
            return lookTime > 0;
        }
        
        @Override
        public void start() {
            lookTime = 40 + random.nextInt(40);
        }
        
        @Override
        public void stop() {
            lookTarget = null;
        }
        
        @Override
        public void tick() {
            if (lookTarget != null) {
                Protocol_37.this.getLookControl().setLookAt(
                    lookTarget.getX(),
                    lookTarget.getEyeY(),
                    lookTarget.getZ(),
                    180.0F,
                    180.0F
                );
                lookTime--;
            }
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide()) {
            // Find nearest player - already handled by SafeLookAtPlayerGoal for looking
            Player nearestPlayer = this.level().getNearestPlayer(this, 64.0D);
            
            if (nearestPlayer != null) {
                // Calculate distance to player
                double distanceToPlayer = this.distanceTo(nearestPlayer);
                
                // Check if player is looking at the entity - if so, mark as seen
                if (distanceToPlayer < 30.0D && isPlayerLookingAt(nearestPlayer)) {
                    hasBeenSeen = true;
                    existenceTimer = 400; // Reset the timer if seen
                }
                
                // Disappear if player gets too close or looks directly at the entity for too long
                // Increased these values to make Protocol_37 easier to find
                if (distanceToPlayer < 4.0D || (distanceToPlayer < 30.0D && isPlayerLookingAt(nearestPlayer))) {
                    if (!shouldDisappear) {
                        shouldDisappear = true;
                        disappearTimer = 40; // 2 seconds before disappearing (increased from 15 ticks/0.75 seconds)
                        
                        // Use config value for transform chance
                        if (HorrorConfig.ENABLE_COMMUNITY_ENTITIES.get() && 
                            random.nextInt(HorrorConfig.PROTOCOL_37_TRANSFORM_CHANCE.get()) == 0) {
                            tryTransformToAngryForm(nearestPlayer);
                            return; // Skip normal disappearance handling
                        }
                    }
                }
                
                // Process disappearing
                if (shouldDisappear) {
                    // Remove all particle effects - the entity should just silently disappear
                    
                    disappearTimer--;
                    
                    // Teleport away if the timer reaches zero
                    if (disappearTimer <= 0) {
                        tryTeleportAway(nearestPlayer);
                    }
                }
            }
            
            // Count down existence timer if never seen by a player
            if (!hasBeenSeen) {
                existenceTimer--;
                
                // Despawn if never seen and timer expires
                if (existenceTimer <= 0) {
                    LOGGER.debug("Protocol_37 despawning due to not being spotted - timer expired");
                    this.discard();
                    return;
                }
            }
            
            // Occasionally spawn an invisible form that follows the player
            if (nearestPlayer != null && 
                HorrorConfig.ENABLE_COMMUNITY_ENTITIES.get() && 
                random.nextInt(HorrorConfig.INVISIBLE_PROTOCOL_37_SPAWN_CHANCE.get()) == 0) {
                trySpawnInvisibleForm(nearestPlayer);
            }
        }
    }
    
    /**
     * Check if a player is looking at this entity
     */
    private boolean isPlayerLookingAt(Player player) {
        Vec3 viewVector = player.getViewVector(1.0F).normalize();
        Vec3 toEntityVector = new Vec3(
                this.getX() - player.getX(),
                this.getEyeY() - player.getEyeY(),
                this.getZ() - player.getZ()).normalize();
        
        // Calculate the dot product - use a lower threshold to make it easier to spot
        double dotProduct = viewVector.dot(toEntityVector);
        return dotProduct > 0.7D; // Wider cone of vision (was 0.9D) - makes it easier to see
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Protocol_37 is completely immune to damage
        return false; // Cancel all damage
    }
    
    /**
     * Try to teleport away from the player
     */
    private void tryTeleportAway(Player player) {
        // Always disappear completely when approached
        // This is more unsettling than teleporting away
        this.discard();
        shouldDisappear = false;
        disappearTimer = 0;
        
        // Play a faint sound when disappearing
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 
                0.3F, 0.5F);
    }
    
    /**
     * Try to transform into an angry form that chases the player
     */
    public void tryTransformToAngryForm(Player player) {
        LOGGER.debug("Protocol_37 attempting to transform to angry form");
        
        // Try to spawn an AngryProtocol37 entity at our position
        if (this.level() instanceof ServerLevelAccessor) {
            EntityType<AngryProtocol37> type = EntityRegistry.ANGRY_PROTOCOL_37.get();
            AngryProtocol37 angryForm = type.create(this.level());
            if (angryForm != null) {
                angryForm.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                this.level().addFreshEntity(angryForm);
                
                // Play a more menacing sound for the transformation
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                        SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 
                        1.0F, 0.5F);
            }
        }
        
        // Always disappear after transforming
        this.discard();
    }
    
    @Override
    public void checkDespawn() {
        // Override to prevent natural despawning
        if (this.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            this.discard();
        }
    }
    
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // Check if we're floating in the air without support
        if (!this.level().isClientSide() && !this.onGround() && this.fallDistance > 3.0F) {
            // Attempt to find ground or teleport to safety
            tryFindGroundBelow();
        }
    }
    
    /**
     * Try to find ground below the entity and teleport to it
     */
    private void tryFindGroundBelow() {
        BlockPos currentPos = this.blockPosition();
        
        // Scan down to find ground (increased scan range from 20 to 50)
        for (int y = 1; y < 50; y++) {
            BlockPos checkPos = currentPos.below(y);
            
            if (this.level().getBlockState(checkPos).isSolid() &&
                this.level().getBlockState(checkPos.above()).isAir()) {
                
                // Teleport to position above solid ground
                this.teleportTo(this.getX(), checkPos.getY() + 1, this.getZ());
                LOGGER.debug("Protocol_37 found ground below and teleported to it");
                return;
            }
        }
        
        // If no ground found, try to teleport away to a valid location
        // Don't immediately discard, try to find another position first
        LOGGER.debug("Protocol_37 couldn't find ground - attempting to find a new position");
        BlockPos newPos = currentPos.offset(
            random.nextInt(20) - 10,  // X offset: -10 to 10
            0,
            random.nextInt(20) - 10   // Z offset: -10 to 10
        );
        
        // Try to teleport to a new position at ground level
        for (int y = -10; y < 10; y++) {
            BlockPos checkPos = newPos.offset(0, y, 0);
            
            if (level().getBlockState(checkPos.below()).isSolid() &&
                level().getBlockState(checkPos).isAir() &&
                level().getBlockState(checkPos.above()).isAir()) {
                
                // Teleport to valid position
                this.teleportTo(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
                LOGGER.debug("Protocol_37 teleported to new valid position: {}", checkPos);
                return;
            }
        }
        
        // Check if there are any players nearby
        Player nearestPlayer = this.level().getNearestPlayer(this, 100.0);
        if (nearestPlayer != null) {
            tryTeleportAway(nearestPlayer);
        } else {
            // No player to teleport away from, just disappear
            LOGGER.debug("Protocol_37 couldn't find valid position and no players nearby - discarding");
            this.discard();
        }
    }
    
    /**
     * Check if Protocol_37 can spawn at the given position - more forgiving than Iteration's rules
     * to make it more likely to be found, and allowing cave spawning
     */
    public static boolean checkProtocol37SpawnRules(EntityType<? extends Monster> entity, ServerLevelAccessor level, 
                                                  MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // Always allow command/event spawning
        if (spawnType == MobSpawnType.COMMAND || spawnType == MobSpawnType.EVENT || spawnType == MobSpawnType.SPAWN_EGG) {
            return true;
        }
        
        // Basic spawn requirements
        // Check if spawn location is inside a block
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        
        // Need at least 2 blocks of air above
        if (!level.getBlockState(pos.above(1)).isAir()) {
            return false;
        }
        
        // Require solid ground beneath
        if (!level.getBlockState(pos.below()).isSolid()) {
            return false;
        }
        
        // For natural spawning, check distance to nearest player
        if (spawnType == MobSpawnType.NATURAL) {
            // Check if there's a player nearby
            Player nearestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 100.0, false);
            
            if (nearestPlayer != null) {
                double distanceToPlayer = nearestPlayer.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                
                // Spawn between 10-25 blocks away from player, increased range to make spawning more likely
                return distanceToPlayer >= 10*10 && distanceToPlayer <= 25*25;
            } else {
                // No players nearby, don't spawn to save resources
                return false;
            }
        }
        
        // Use monster spawn rules as fallback
        return Monster.checkMonsterSpawnRules(entity, level, spawnType, pos, random);
    }
    
    /**
     * Try to spawn an invisible form that follows the player
     */
    private void trySpawnInvisibleForm(Player player) {
        LOGGER.debug("Protocol_37 attempting to spawn invisible form");
        
        // Calculate position near the player but out of sight
        double distance = 3.0D + random.nextDouble(); // Changed from 3-6 to 3-4 blocks - exactly as requested
        double angle = random.nextDouble() * Math.PI * 2;
        
        double x = player.getX() + Math.sin(angle) * distance;
        double z = player.getZ() + Math.cos(angle) * distance;
        
        // Find suitable Y position
        BlockPos spawnPos = new BlockPos((int)x, (int)player.getY(), (int)z);
        
        // Find valid spawn location - check more aggressively for cave environments
        boolean found = false;
        
        // Expanded search to help with cave spawning (check more vertical positions)
        for (int yOffset = -8; yOffset <= 8; yOffset++) {
            BlockPos checkPos = spawnPos.offset(0, yOffset, 0);
            BlockPos belowPos = checkPos.below();
            
            if (this.level().getBlockState(belowPos).isSolid() && 
                this.level().getBlockState(checkPos).isAir() &&
                this.level().getBlockState(checkPos.above()).isAir()) {
                
                // Valid position found, spawn the entity
                if (this.level() instanceof ServerLevelAccessor) {
                    EntityType<InvisibleProtocol37> type = EntityRegistry.INVISIBLE_PROTOCOL_37.get();
                    InvisibleProtocol37 invisibleForm = type.create(this.level());
                    if (invisibleForm != null) {
                        invisibleForm.moveTo(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5, 0, 0);
                        this.level().addFreshEntity(invisibleForm);
                        LOGGER.debug("Protocol_37 spawned invisible form at {} ({} blocks from player)", checkPos, distance);
                        found = true;
                        return;
                    }
                }
            }
        }
        
        // If no valid position found using normal approach, try one more aggressive attempt
        if (!found) {
            // Try horizontal offsets to find a valid position nearby
            for (int xOffset = -2; xOffset <= 2; xOffset += 2) {
                for (int zOffset = -2; zOffset <= 2; zOffset += 2) {
                    if (xOffset == 0 && zOffset == 0) continue; // Skip center as already checked
                    
                    BlockPos offsetPos = spawnPos.offset(xOffset, 0, zOffset);
                    
                    // Check vertical positions at this offset
                    for (int yOffset = -8; yOffset <= 8; yOffset++) {
                        BlockPos checkPos = offsetPos.offset(0, yOffset, 0);
                        BlockPos belowPos = checkPos.below();
                        
                        if (this.level().getBlockState(belowPos).isSolid() && 
                            this.level().getBlockState(checkPos).isAir() &&
                            this.level().getBlockState(checkPos.above()).isAir()) {
                            
                            // Valid position found, spawn the entity
                            if (this.level() instanceof ServerLevelAccessor) {
                                EntityType<InvisibleProtocol37> type = EntityRegistry.INVISIBLE_PROTOCOL_37.get();
                                InvisibleProtocol37 invisibleForm = type.create(this.level());
                                if (invisibleForm != null) {
                                    invisibleForm.moveTo(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5, 0, 0);
                                    this.level().addFreshEntity(invisibleForm);
                                    LOGGER.debug("Protocol_37 spawned invisible form at alternate position {} (cave spawn)", checkPos);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            
            LOGGER.debug("Protocol_37 failed to spawn invisible form - no valid position found in cave environment");
        }
    }
} 