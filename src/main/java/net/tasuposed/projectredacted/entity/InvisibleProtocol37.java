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
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
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
 * Protocol 37's invisible form - an invisible entity that follows the player around
 * and sometimes teleports around them. Credit for the idea goes to a mod user.
 */
public class InvisibleProtocol37 extends Monster {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvisibleProtocol37.class);
    
    // Teleportation behavior
    private int teleportCooldown = 0;
    private static final int MIN_TELEPORT_INTERVAL = 100; // 5 seconds
    private static final int MAX_TELEPORT_INTERVAL = 400; // 20 seconds
    
    // Following behavior
    private Player targetPlayer = null;
    private static final double STOP_DISTANCE = 5.0D; // Reduced from 8.0D - follow closer
    private static final double DISAPPEAR_DISTANCE = 2.5D; // Reduced from 4.0D - don't disappear as easily
    
    // Block breaking behavior
    private int breakBlockCooldown = 0;
    private static final int MIN_BLOCK_BREAK_INTERVAL = 600; // 30 seconds
    private static final int MAX_BLOCK_BREAK_INTERVAL = 2400; // 2 minutes
    
    // Disappearance behavior
    private static final int DISAPPEAR_CHANCE = 5; // 1 in 5 chance to deal damage and disappear
    private static final int TRANSFORM_CHANCE = 20; // 1 in 20 chance to transform before disappearing
    
    // Lifetime management
    private int lifespan = 3600; // 3 minutes maximum lifespan
    
    // Player caching optimization
    private Player cachedPlayer = null;
    private int playerCacheTimer = 0;
    private static final int PLAYER_CACHE_REFRESH = 20; // Refresh player cache every second
    
    // Entity limits
    private static final int MAX_ENTITIES_PER_AREA = 2;
    
    public InvisibleProtocol37(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setInvisible(true);
        this.noPhysics = false; // Still collides with blocks
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.5D));
        
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
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
        if (!this.level().hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 64.0D)) {
            // Still decrement lifespan even when players aren't nearby
            lifespan--;
            if (lifespan <= 0) {
                this.discard();
            }
            return;
        }
        
        // Always ensure invisibility
        this.setInvisible(true);
        
        if (!this.level().isClientSide()) {
            // Decrement cooldowns
            if (teleportCooldown > 0) teleportCooldown--;
            if (breakBlockCooldown > 0) breakBlockCooldown--;
            
            // Update player cache
            if (playerCacheTimer <= 0) {
                cachedPlayer = this.level().getNearestPlayer(this, 64.0D);
                playerCacheTimer = PLAYER_CACHE_REFRESH;
                
                // Store the cached player as target
                if (cachedPlayer != null) {
                    this.targetPlayer = cachedPlayer;
                }
            } else {
                playerCacheTimer--;
            }
            
            // Use cached player if available, otherwise find nearest
            Player nearestPlayer = targetPlayer;
            if (nearestPlayer == null) {
                nearestPlayer = this.level().getNearestPlayer(this, 64.0D);
                this.targetPlayer = nearestPlayer;
            }
            
            if (nearestPlayer != null) {
                // Calculate distance to player
                double distanceToPlayer = this.distanceTo(nearestPlayer);
                
                // Only play sounds if close enough to be heard
                if (distanceToPlayer < 20.0D) {
                    // Occasionally play creepy sounds to indicate presence (once every ~10 seconds on average)
                    if (random.nextInt(200) == 0) {
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                SoundEvents.AMBIENT_CAVE.get(), SoundSource.HOSTILE,
                                0.3F, 0.5F + random.nextFloat() * 0.2F);
                    }
                }
                
                // Try to teleport around player occasionally - less frequently when far away
                if (teleportCooldown <= 0 && (distanceToPlayer < 20.0D || random.nextInt(50) == 0)) {
                    tryTeleportAroundPlayer(nearestPlayer);
                    teleportCooldown = MIN_TELEPORT_INTERVAL + random.nextInt(MAX_TELEPORT_INTERVAL - MIN_TELEPORT_INTERVAL);
                }
                
                // Try to break blocks occasionally - only when close and cooldown expired
                if (breakBlockCooldown <= 0 && distanceToPlayer < 8.0D && random.nextInt(200) == 0) {
                    tryBreakRandomBlock();
                    breakBlockCooldown = MIN_BLOCK_BREAK_INTERVAL + random.nextInt(MAX_BLOCK_BREAK_INTERVAL - MIN_BLOCK_BREAK_INTERVAL);
                }
                
                // Check if player is too close
                if (distanceToPlayer < DISAPPEAR_DISTANCE) {
                    handleDisappearance(nearestPlayer);
                }
            }
            
            // Lifespan management
            lifespan--;
            if (lifespan <= 0) {
                this.discard();
            }
        }
    }
    
    /**
     * Try to teleport around the player
     */
    private void tryTeleportAroundPlayer(Player player) {
        try {
            // Calculate random position around player
            double distance = 10.0D + random.nextDouble() * 15.0D;
            double angle = random.nextDouble() * Math.PI * 2;
            
            double x = player.getX() + Math.sin(angle) * distance;
            double z = player.getZ() + Math.cos(angle) * distance;
            
            // Find Y position with ground beneath
            BlockPos targetPos = new BlockPos((int)x, (int)player.getY(), (int)z);
            
            // Find valid teleport location
            for (int yOffset = -5; yOffset <= 5; yOffset++) {
                BlockPos checkPos = targetPos.offset(0, yOffset, 0);
                BlockPos belowPos = checkPos.below();
                
                if (this.level().getBlockState(belowPos).isSolid() && 
                    this.level().getBlockState(checkPos).isAir() &&
                    this.level().getBlockState(checkPos.above()).isAir()) {
                    
                    // Valid position found, teleport
                    this.teleportTo(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
                    return;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to teleport InvisibleProtocol37", e);
            // Just continue if teleportation fails
        }
    }
    
    /**
     * Try to break a random block near the entity
     */
    private void tryBreakRandomBlock() {
        // Only in rare cases
        if (random.nextInt(10) != 0) return;
        
        try {
            // Check blocks in a small radius
            int radius = 2;
            for (int attempt = 0; attempt < 10; attempt++) {
                int x = this.blockPosition().getX() + random.nextInt(radius * 2) - radius;
                int y = this.blockPosition().getY() + random.nextInt(3) - 1;
                int z = this.blockPosition().getZ() + random.nextInt(radius * 2) - radius;
                
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = this.level().getBlockState(pos);
                
                // Don't break bedrock, obsidian, or other very hard blocks
                if (state.getBlock() != Blocks.BEDROCK && 
                    state.getBlock() != Blocks.OBSIDIAN && 
                    state.getBlock() != Blocks.END_PORTAL_FRAME &&
                    state.getDestroySpeed(this.level(), pos) < 10.0F) {
                    
                    // Break the block
                    this.level().destroyBlock(pos, true);
                    this.level().playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                    LOGGER.debug("InvisibleProtocol37 broke a block at {}", pos);
                    return;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to break block by InvisibleProtocol37", e);
        }
    }
    
    /**
     * Handle behavior when player gets too close
     */
    private void handleDisappearance(Player player) {
        try {
            int roll = random.nextInt(100); // Changed from 20 to 100 for rarer effects
            
            // Very rare chance to transform into Protocol_37 (1%)
            if (roll == 0) {
                // Try to spawn a Protocol_37 entity at our position
                if (this.level() instanceof ServerLevelAccessor) {
                    EntityType<Protocol_37> type = EntityRegistry.PROTOCOL_37.get();
                    Protocol_37 visibleForm = type.create(this.level());
                    if (visibleForm != null) {
                        visibleForm.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                        this.level().addFreshEntity(visibleForm);
                        
                        // Schedule it to disappear shortly
                        visibleForm.setPersistenceRequired();
                    }
                }
            } 
            // Very rare chance to apply blindness effect (2% instead of 25%)
            else if (roll < 3) { // Changed from DISAPPEAR_CHANCE (5) to just 3 out of 100
                this.doHurtTarget(player);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0)); // Reduced duration from 100 to 60 ticks (3 seconds)
                
                // Play a sound instead of a disruptive effect
                this.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.5F, 0.8F);
            }
            
            // Always disappear
            this.discard();
        } catch (Exception e) {
            LOGGER.error("Error during InvisibleProtocol37 disappearance", e);
            // Just disappear
            this.discard();
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
     * Spawn rules for the invisible entity
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
                int countInArea = serverLevel.getEntitiesOfClass(InvisibleProtocol37.class, 
                    net.minecraft.world.phys.AABB.ofSize(new Vec3(pos.getX(), pos.getY(), pos.getZ()), 128, 128, 128)).size();
                    
                if (countInArea >= MAX_ENTITIES_PER_AREA) {
                    return false;
                }
            }
            
            // Much higher chance to spawn (1 in INVISIBLE_PROTOCOL_37_SPAWN_CHANCE/10)
            // This effectively makes it 10x more likely to spawn
            int adjustedChance = Math.max(HorrorConfig.INVISIBLE_PROTOCOL_37_SPAWN_CHANCE.get() / 10, 1);
            if (random.nextInt(adjustedChance) != 0) {
                return false;
            }
            
            // Check if there's a player nearby
            Player nearestPlayer = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 64.0, false);
            
            if (nearestPlayer != null) {
                double distanceToPlayer = nearestPlayer.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                
                // Expanded spawn range from 3-4 to 3-10 blocks
                if (distanceToPlayer >= 3*3 && distanceToPlayer <= 10*10) {
                    // Use simplified spawn check to work in any biome
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
} 