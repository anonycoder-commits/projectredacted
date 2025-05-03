package net.tasuposed.projectredacted.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.tasuposed.projectredacted.horror.events.EntityEvent;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.network.packets.GlitchScreenPacket;

/**
 * A mysterious entity that stalks players
 */
public class Iteration extends Monster {
    private static final Logger LOGGER = LoggerFactory.getLogger(Iteration.class);
    
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§4§lI̶͉̐T̸̻̕E̴̢̽R̴̫̀A̶̭̔T̶̟͘I̵̹̎O̸̜̎N̵̫͋"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_6);
    
    private int teleportCooldown = 0;
    private int attackCooldown = 0;
    private int glitchEffectCooldown = 0;
    private int specialAttackCooldown = 0;
    private int stalkingPhase = 0; // 0=normal, 1=aggressive, 2=hunting
    private boolean hasInitiatedAttack = false;
    
    // New fields for block breaking and despawn timer
    private int blockBreakCooldown = 0;
    private int despawnTimer = 400; // Restored to 20 seconds (was reduced to 200 in optimization)
    private boolean hasTargetedPlayer = false; // Track if we've ever targeted a player
    private int killAttemptTimer = 1800; // 1.5 minutes to kill the player after targeting (was 1200)
    
    public Iteration(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 100; // Increased reward for killing it
        this.bossEvent.setVisible(false); // Initially hidden
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true)); // Faster attack speed
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 1.0D, 32.0F)); // Move faster toward target
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D)); // Iteration avoids water - digital entity disrupted by water
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F)); // Look from further away
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    
    /**
     * Define default attribute values - enhanced for more intimidation
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D) // More health
                .add(Attributes.MOVEMENT_SPEED, 0.45D) // Slightly reduced speed (from 0.48D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D) // Reduced damage from 8.0D to 5.0D
                .add(Attributes.FOLLOW_RANGE, 80.0D) // Can detect players from further away
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D); // Complete knockback immunity
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Consolidate all cooldown decrements in one pass
        if (teleportCooldown > 0) teleportCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (glitchEffectCooldown > 0) glitchEffectCooldown--;
        if (specialAttackCooldown > 0) specialAttackCooldown--;
        if (blockBreakCooldown > 0) blockBreakCooldown--;
        
        // Check if our target is still alive
        LivingEntity currentTarget = this.getTarget();
        if (currentTarget instanceof ServerPlayer player) {
            // Reset despawn timer as we're actively engaged with a player
            despawnTimer = 400; // Restored to 20 seconds (was incorrectly reduced to 200)
            
            // We have targeted a player, start the kill attempt timer
            hasTargetedPlayer = true;
            
            // Decrement kill attempt timer only if we've targeted a player
            if (killAttemptTimer > 0) {
                killAttemptTimer--;
            }
            
            // If kill attempt timer reaches zero, disappear
            if (killAttemptTimer <= 0) {
                LOGGER.debug("Iteration despawning due to failed kill attempt");
                this.discard();
                return;
            }
            
            // Update the boss bar and stalking phase in a single operation
            updateBossBarAndPhase(player);
            
            // Apply visual effects to targeted player - only check RNG once
            int randomChance = random.nextInt(200);
            if (glitchEffectCooldown <= 0 && randomChance < 10) {
                applyGlitchEffect(player);
                glitchEffectCooldown = 80 + random.nextInt(40); // 4-6 seconds cooldown
            }
            
            // Try to perform special attacks based on stalking phase
            if (specialAttackCooldown <= 0) {
                performSpecialAttack(player);
            }
            
            // In hunting phase, try to break blocks to reach target
            if (stalkingPhase > 0 && blockBreakCooldown <= 0) {
                tryBreakBlocksToTarget(player);
            }
            
            // When getting close to the player in aggressive or hunting phase, reduce render distance
            // Optimize by only calculating distance once and using it for both checks
            double distanceToPlayer = this.distanceTo(player);
            if (stalkingPhase >= 1 && distanceToPlayer < 10.0 && random.nextInt(100) < 5) {
                // Only trigger this occasionally (5% chance per tick when close)
                EntityEvent entityEvent = new EntityEvent();
                
                // More aggressive render distance reduction in hunting phase
                int renderDistance = stalkingPhase == 2 ? 2 : 4;
                int duration = stalkingPhase == 2 ? 200 : 140; // 7-10 seconds
                
                // Apply the render distance reduction
                entityEvent.reduceRenderDistance(player, renderDistance, duration, true);
            }
        } else {
            // No target or target is not a player
            if (bossEvent.isVisible()) {
                bossEvent.setVisible(false);
            }
            
            // Count down despawn timer if we don't have a target and have previously targeted a player
            if (hasTargetedPlayer) {
                despawnTimer--;
                
                // Despawn if timer runs out
                if (despawnTimer <= 0) {
                    LOGGER.debug("Iteration despawning due to lost target");
                    this.discard();
                    return;
                }
            }
        }
        
        // Teleport randomly when not seen by the player - optimize by only checking every 100 ticks
        if (!this.level().isClientSide() && this.tickCount % 100 == 0) {
            LivingEntity target = this.getTarget();
            if (target instanceof Player && !this.isLookingAtMe((Player)target)) {
                attemptTeleport();
            }
        }
    }
    
    /**
     * Update both the boss bar appearance and stalking phase in a single method
     * to avoid redundant health calculations
     */
    private void updateBossBarAndPhase(ServerPlayer player) {
        // Calculate health percentage once
        float healthPercentage = this.getHealth() / this.getMaxHealth();
        
        // Update stalking phase based on health percentage
        if (healthPercentage <= 0.3f) {
            if (stalkingPhase != 2) {
                // Transition to hunting phase with special effect
                stalkingPhase = 2;
                
                player.sendSystemMessage(Component.literal("§4§l§kxxxx§r §4§lITERATION HUNTING PROTOCOL ACTIVE§r §4§l§kxxxx§r"));
                
                // Play hunting sound
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.0F, 0.5F);
                
                // Major screen effect
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                2, // EFFECT_CORRUPT
                                0.95f, // Maximum intensity
                                60), // 3 seconds
                        player);
                
                // Dramatically reduce render distance when entering hunting phase
                EntityEvent entityEvent = new EntityEvent();
                entityEvent.reduceRenderDistance(player, 2, 300, true); // 2 chunks for 15 seconds (300 ticks)
            }
        } else if (healthPercentage <= 0.6f) {
            if (stalkingPhase != 1) {
                // Transition to aggressive phase
                stalkingPhase = 1;
                
                player.sendSystemMessage(Component.literal("§5§lThe §4Iteration§5§l is becoming aggressive..."));
                
                // Add screen effect
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                1, // EFFECT_DISTORT
                                0.7f, // Medium intensity
                                40), // 2 seconds
                        player);
                
                // Moderately reduce render distance when entering aggressive phase
                EntityEvent entityEvent = new EntityEvent();
                entityEvent.reduceRenderDistance(player, 4, 200, true); // 4 chunks for 10 seconds (200 ticks)
            }
        }
        
        // Update boss bar
        bossEvent.setVisible(true);
        
        // Update boss bar color based on stalking phase
        switch(stalkingPhase) {
            case 0:
                bossEvent.setColor(BossEvent.BossBarColor.RED);
                break;
            case 1:
                bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
                break;
            case 2:
                bossEvent.setColor(BossEvent.BossBarColor.WHITE);
                if (this.tickCount % 20 < 10) {
                    // Flash the boss bar in hunting phase
                    bossEvent.setColor(BossEvent.BossBarColor.RED);
                }
                break;
        }
        
        // Set progress based on health
        bossEvent.setProgress(healthPercentage);
    }
    
    /**
     * Apply visual glitch effects to the player based on stalking phase
     */
    private void applyGlitchEffect(ServerPlayer player) {
        int effectType;
        float intensity;
        int duration;
        
        switch(stalkingPhase) {
            case 0:
                // Subtle effect in normal phase
                effectType = 0; // EFFECT_STATIC
                intensity = 0.3f + random.nextFloat() * 0.2f; // 0.3-0.5
                duration = 20 + random.nextInt(20); // 1-2 seconds
                break;
            case 1:
                // Stronger effect in aggressive phase
                effectType = 1; // EFFECT_DISTORT
                intensity = 0.5f + random.nextFloat() * 0.3f; // 0.5-0.8
                duration = 30 + random.nextInt(30); // 1.5-3 seconds
                break;
            case 2:
                // Severe effect in hunting phase
                effectType = 2; // EFFECT_CORRUPT
                intensity = 0.7f + random.nextFloat() * 0.3f; // 0.7-1.0
                duration = 40 + random.nextInt(40); // 2-4 seconds
                break;
            default:
                return;
        }
        
        // Send the effect packet to the player
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(
                        effectType,
                        intensity,
                        duration),
                player);
    }
    
    /**
     * Perform special attacks based on stalking phase
     */
    private void performSpecialAttack(ServerPlayer player) {
        switch(stalkingPhase) {
            case 0:
                // No special attacks in normal phase
                specialAttackCooldown = 100;
                break;
            case 1:
                // Quick teleport attack in aggressive phase
                if (this.distanceTo(player) < 20.0 && random.nextInt(100) < 30) {
                    // Teleport behind player
                    Vec3 lookVec = player.getViewVector(1.0F).normalize().scale(-2.0); // 2 blocks behind
                    Vec3 teleportPos = player.position().add(lookVec);
                    
                    // Try to find a valid position nearby
                    if (tryTeleportTo(teleportPos.x, teleportPos.y, teleportPos.z)) {
                        // Play teleport sound
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.5F);
                        
                        // Blind the player briefly
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
                        
                        // Add subtle screen effect
                        NetworkHandler.sendToPlayer(
                                new GlitchScreenPacket(
                                        0, // EFFECT_STATIC
                                        0.6f,
                                        15), // Short flash
                                player);
                    }
                }
                specialAttackCooldown = 120 + random.nextInt(80); // 6-10 seconds
                break;
            case 2:
                // In hunting phase, initialize attack sequence if not done yet
                if (!hasInitiatedAttack) {
                    initializeAttackSequence(player);
                    hasInitiatedAttack = true;
                } else {
                    // Aggressive teleport attacks
                    if (random.nextInt(100) < 60) { // 60% chance
                        // Try to teleport near the player from a random direction
                        double angle = random.nextDouble() * Math.PI * 2;
                        double distance = 3.0 + random.nextDouble() * 2.0; // 3-5 blocks away
                        
                        Vec3 offset = new Vec3(
                                Math.sin(angle) * distance,
                                0,
                                Math.cos(angle) * distance
                        );
                        Vec3 teleportPos = player.position().add(offset);
                        
                        if (tryTeleportTo(teleportPos.x, teleportPos.y, teleportPos.z)) {
                            // Play more intense teleport sound
                            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.3F);
                            
                            // Apply more severe effects to player
                            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                            
                            // Add severe screen effect
                            NetworkHandler.sendToPlayer(
                                    new GlitchScreenPacket(
                                            2, // EFFECT_CORRUPT
                                            0.8f,
                                            20), // 1 second
                                    player);
                        }
                    }
                }
                specialAttackCooldown = 80 + random.nextInt(60); // 4-7 seconds, faster in hunting phase
                break;
        }
    }
    
    /**
     * Initialize a terrifying attack sequence when entering hunting mode
     */
    private void initializeAttackSequence(ServerPlayer player) {
        // Play an intense sound
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.0F, 0.4F);
        
        // Send the terrifying message
        player.sendSystemMessage(Component.literal("§4§lT̶̢̝̓̋͑́̇͠h̶̙͗̿̽ę̴̝͕̹̬̖̇̃͝ ̵̢̻͔̜̹̈̓Į̸̢̖̬̹̀͋̉͐ţ̸̮͈̏̈́̓̉͒̀͝e̶̠̝̯̦̤̣̎̃r̶̩͍͑̈́a̴̡̹̲̬̹̱̍̏̀̑̍̓͜t̶̺͖̪̳̱͌͆̈̔i̶̜̼̹̙̰̇̽͛͗͠ỏ̸̮͔͔̱̘͙̞͜n̸̙̫̖͊̌̔͜ ̵̡͙͚̪̮̀̐͛̊́̂͝h̵̺̠̟̪̮̙̓͋̓̏̒̀͜ụ̷̱̃̒̽n̸̳̤̖̦̱̖̥͂̑̕ţ̶̟͈̲̬̲͈̄̊̋̎͊͠ŝ̶̗́ ̸̯̯͆͐̌̈͌̐̊y̶͕̐͂̎̈́̚͝ò̸̥̆̾̐̈́̽̔u̸̢̥͍̻̮͒̃̂̈́̑̽̍ ̵̼̮̙̣͎̖̋͂n̴̯̖̳̠̅̍̏̌̚ó̶̢̳͉̰̺̻̖͌̏͂̕͠w̴̢̦̗̙̻̐̌͠"));
        
        // Severe screen effect
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(
                        3, // EFFECT_INVERT
                        1.0f, // Maximum intensity
                        50), // 2.5 seconds
                player);
        
        // Apply negative effects
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        
        // Reduce render distance to create claustrophobic atmosphere
        EntityEvent entityEvent = new EntityEvent();
        entityEvent.reduceRenderDistance(player, 3, 400, true); // 3 chunks for 20 seconds (400 ticks)
        
        // Schedule a stalking message after a delay
        player.getServer().tell(new net.minecraft.server.TickTask(
                player.getServer().getTickCount() + 60, () -> {
            player.sendSystemMessage(Component.literal("§4§oYou cannot escape..."));
        }));
    }
    
    /**
     * Check if the player is looking at this entity
     */
    private boolean isLookingAtMe(Player player) {
        // Calculate the vector from the player to this entity
        double dx = this.getX() - player.getX();
        double dz = this.getZ() - player.getZ();
        
        // Get the player's looking direction
        double playerYaw = Math.toRadians(player.getYRot());
        double lookX = -Math.sin(playerYaw);
        double lookZ = Math.cos(playerYaw);
        
        // Calculate the dot product
        double dot = dx * lookX + dz * lookZ;
        
        // If the dot product is positive, the player is looking towards the entity
        // We also check if the player's view angle is within a 45 degree cone
        return dot > 0 && Math.abs(Math.atan2(dz, dx) - playerYaw) < Math.PI / 4;
    }
    
    /**
     * Attempt to teleport to a random location near the target
     */
    private void attemptTeleport() {
        if (this.getTarget() != null) {
            double targetX = this.getTarget().getX();
            double targetY = this.getTarget().getY();
            double targetZ = this.getTarget().getZ();
            
            // Try up to 15 times to find a valid location
            for (int attempt = 0; attempt < 15; attempt++) {
                // Get a random location within 16 blocks of the target (reduced from 32 blocks)
                double offsetX = targetX + (this.random.nextDouble() - 0.5) * 16.0;
                double offsetZ = targetZ + (this.random.nextDouble() - 0.5) * 16.0;
                
                // Start at slightly above target Y to avoid underground
                BlockPos posToCheck = new BlockPos((int)offsetX, (int)Math.max(targetY, 2), (int)offsetZ);
                
                // Scan downward to find a valid position
                boolean foundValidPos = false;
                for (int y = 0; y <= 5; y++) {
                    BlockPos currentPos = posToCheck.below(y);
                    
                    // Check if there's 2 blocks of open space for the entity height and a solid block below
                    if (this.level().getBlockState(currentPos).isAir() && 
                        this.level().getBlockState(currentPos.above()).isAir() &&
                        !this.level().getBlockState(currentPos.below()).isAir()) {
                        
                        // Safe spot found - teleport there
                        this.teleportTo(offsetX, currentPos.getY(), offsetZ);
                        
                        // Play teleport sound
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
                        
                        // Apply a brief blindness effect to the target player if they're close
                        if (this.getTarget() instanceof ServerPlayer player && this.distanceTo(player) < 16.0D) {
                            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
                        }
                        
                        foundValidPos = true;
                        break;
                    }
                }
                
                if (foundValidPos) {
                    return; // Successful teleport, exit method
                }
            }
        }
    }
    
    /**
     * Try to teleport to a specific position
     */
    private boolean tryTeleportTo(double x, double y, double z) {
        BlockPos posToCheck = new BlockPos((int)x, (int)y, (int)z);
        
        // Scan a few blocks up and down to find a valid position
        for (int yOffset = -1; yOffset <= 2; yOffset++) {
            BlockPos currentPos = posToCheck.offset(0, yOffset, 0);
            
            if (this.level().getBlockState(currentPos).isAir() && 
                this.level().getBlockState(currentPos.above()).isAir() &&
                !this.level().getBlockState(currentPos.below()).isAir()) {
                
                // Safe spot found - teleport there
                this.teleportTo(x, currentPos.getY(), z);
                return true;
            }
        }
        
        return false; // Couldn't find a valid position
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Chance to teleport when hurt
        if (!this.level().isClientSide() && source.getEntity() instanceof Player && random.nextInt(100) < 30) {
            // Find a random position to teleport to (reduced from 16 to 8 blocks)
            double tpX = this.getX() + (random.nextDouble() - 0.5D) * 8.0D;
            double tpZ = this.getZ() + (random.nextDouble() - 0.5D) * 8.0D;
            
            // Find a safe Y position
            BlockPos currentPos = new BlockPos((int)tpX, (int)this.getY(), (int)tpZ);
            
            // Scan downward to find a valid position
            for (int y = 0; y <= 5; y++) {
                BlockPos checkPos = currentPos.below(y);
                
                if (this.level().getBlockState(checkPos).isAir() && 
                    this.level().getBlockState(checkPos.above()).isAir() &&
                    !this.level().getBlockState(checkPos.below()).isAir()) {
                    
                    // Teleport to safe spot
                    this.teleportTo(tpX, checkPos.getY(), tpZ);
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), 
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.5F);
                    
                    // Send message based on stalking phase
                    if (source.getEntity() instanceof ServerPlayer player) {
                        if (stalkingPhase == 2) {
                            player.sendSystemMessage(Component.literal("§4§lY̵̤̓ö̵̟́u̸̯̿ ̵̡̌c̸̣̿ạ̶̓ń̵͔n̵̦̐ǒ̴̦t̵̪̔ ̷͎̐k̸̘̚ḯ̸͕l̵̘̀l̶̥̎ ̸̳̒m̵̩͗ê̷̲..."));
                        }
                    }
                    
                    return false; // Cancel damage
                }
            }
        }
        
        // Apply effects to the player when hit
        if (source.getEntity() instanceof ServerPlayer player) {
            // Different effects based on stalking phase
            switch (stalkingPhase) {
                case 0:
                    // Light screen distortion
                    NetworkHandler.sendToPlayer(
                            new GlitchScreenPacket(
                                    0, // EFFECT_STATIC
                                    0.4f, // Light effect
                                    15), // Brief duration
                            player);
                    break;
                case 1:
                    // Medium effect + slowness
                    NetworkHandler.sendToPlayer(
                            new GlitchScreenPacket(
                                    1, // EFFECT_DISTORT
                                    0.6f, // Medium effect
                                    20), // 1 second
                            player);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                    break;
                case 2:
                    // Severe effect + multiple debuffs
                    NetworkHandler.sendToPlayer(
                            new GlitchScreenPacket(
                                    2, // EFFECT_CORRUPT
                                    0.8f, // Strong effect
                                    30), // 1.5 seconds
                            player);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                    break;
            }
        }
        
        return super.hurt(source, amount);
    }
    
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = false;
        
        if (target instanceof Player) {
            // Apply a weaker version of the effect when attacking
            float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            
            // Reduce damage based on stalking phase (less damage in earlier phases)
            if (stalkingPhase == 0) {
                damage = damage * 0.7f; // 70% damage in normal phase
            } else if (stalkingPhase == 1) {
                damage = damage * 0.85f; // 85% damage in aggressive phase
            }
            
            // Do the actual attack with modified damage
            hit = target.hurt(this.damageSources().mobAttack(this), damage);
            
            if (hit && target instanceof ServerPlayer player) {
                // Only schedule despawn if the player was actually killed
                if (player.isDeadOrDying()) {
                    LOGGER.debug("Iteration successfully killed player, scheduling despawn");
                    
                    // Despawn after 3 seconds (60 ticks) to give time for death animation
                    this.level().getServer().tell(new net.minecraft.server.TickTask(
                        this.level().getServer().getTickCount() + 60, this::discard));
                }
                
                // Apply effects to the player when hit (with reduced potency)
                // Apply screen effect
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(
                                stalkingPhase, // Effect based on phase
                                0.6f + (stalkingPhase * 0.1f), // Reduced intensity
                                20 + (stalkingPhase * 10)), // Duration based on phase
                        player);
                
                // Apply confusion effect (reduced duration)
                int confusionDuration = 60 + (stalkingPhase * 40); // 3-7 seconds based on phase
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, confusionDuration, 0));
                
                // Only apply slowness in hunting phase with reduced effect
                if (stalkingPhase == 2) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0)); // Shorter duration, level 1
                }
                
                // Send thematic message based on phase, but only occasionally (around 30% chance)
                // This prevents message spam when being hit repeatedly
                if (random.nextFloat() < 0.3f) {
                    switch (stalkingPhase) {
                        case 0 -> player.sendSystemMessage(Component.literal("§7A cold chill runs down your spine..."));
                        case 1 -> {} // No message in aggressive phase
                        case 2 -> player.sendSystemMessage(Component.literal("§4§oT̶̬̎h̴̫̓e̶͔̔ ̴̜̊Ȟ̸̬ǘ̸̡n̸̮̐t̷̬̉ ̴̲̃i̸̘̓s̷̙̆ ̸̱̍ö̷̢n̷̬̄.̷̜̄.̴̨̇.̵̲̈́"));
                    }
                }
            }
            
            return hit;
        }
        
        // Normal attack for non-player targets
        hit = super.doHurtTarget(target);
        return hit;
    }
    
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }
    
    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
        
        if (compound.contains("StalkingPhase")) {
            this.stalkingPhase = compound.getInt("StalkingPhase");
        }
        
        if (compound.contains("HasInitiatedAttack")) {
            this.hasInitiatedAttack = compound.getBoolean("HasInitiatedAttack");
        }
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("StalkingPhase", this.stalkingPhase);
        compound.putBoolean("HasInitiatedAttack", this.hasInitiatedAttack);
    }
    
    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }
    
    /**
     * Check if the entity can spawn at the given position
     */
    public static boolean checkIterationSpawnRules(EntityType<? extends Monster> entity, ServerLevelAccessor level, 
                                                  MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // Allow spawning in caves now, but we'll check other conditions

        // Check if the spawn location is inside a block
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        
        // Need at least 3 blocks of air above to avoid tight spaces
        if (!level.getBlockState(pos.above(2)).isAir()) {
            return false;
        }
        
        // Require solid ground beneath
        if (!level.getBlockState(pos.below()).isSolid()) {
            return false;
        }
        
        // Allow specific spawn sources to bypass the remaining checks
        if (spawnType == MobSpawnType.COMMAND || spawnType == MobSpawnType.EVENT || spawnType == MobSpawnType.SPAWN_EGG) {
            return true;
        }
        
        // Additional normal monster spawn rules
        return Monster.checkMonsterSpawnRules(entity, level, spawnType, pos, random);
    }
    
    /**
     * Try to break blocks (doors, glass, etc.) to reach target
     */
    private void tryBreakBlocksToTarget(ServerPlayer player) {
        // Only break blocks in hunting phase or sometimes in aggressive phase
        if (stalkingPhase < 1) {
            blockBreakCooldown = 40; // 2 second cooldown in normal phase
            return;
        }
        
        // Build a path to the player and check for blocks in the way
        Vec3 entityPos = this.position();
        Vec3 playerPos = player.position();
        
        // Check for a direct line to the player
        Vec3 direction = playerPos.subtract(entityPos).normalize();
        double distance = entityPos.distanceTo(playerPos);
        double stepSize = 0.5; // Check more frequently for better precision
        
        // Don't check too far
        double maxCheckDistance = Math.min(distance, 8.0); // Increased reach from 5 to 8 blocks
        
        // Look for breakable blocks along the path
        for (double d = 0; d < maxCheckDistance; d += stepSize) {
            Vec3 checkPos = entityPos.add(direction.scale(d));
            BlockPos blockPos = new BlockPos((int)Math.floor(checkPos.x), (int)Math.floor(checkPos.y), (int)Math.floor(checkPos.z));
            BlockPos blockHeadPos = blockPos.above();
            
            // Check both block at entity level and head level
            if (isBreakableBlock(blockPos) || isBreakableBlock(blockHeadPos)) {
                BlockPos targetPos = isBreakableBlock(blockPos) ? blockPos : blockHeadPos;
                
                // Break the block
                breakBlock(targetPos);
                blockBreakCooldown = stalkingPhase == 2 ? 10 : 20; // Faster breaking in all phases
                
                return;
            }
        }
        
        // If no block found to break, set a short cooldown
        blockBreakCooldown = 10;
    }
    
    /**
     * Check if a block is breakable
     */
    private boolean isBreakableBlock(BlockPos pos) {
        BlockState blockState = this.level().getBlockState(pos);
        Block block = blockState.getBlock();
        
        // Easy to break blocks: doors, glass, etc.
        return block instanceof DoorBlock || 
               block instanceof FenceGateBlock || 
               block instanceof TrapDoorBlock ||
               block.defaultDestroyTime() < 1.0 || // Glass, leaves, etc. (increased threshold from 0.8 to 1.0)
               block instanceof LeavesBlock ||
               block.getDescriptionId().contains("glass") || // Ensure all glass blocks can be broken
               block.getDescriptionId().contains("window"); // Ensure window blocks can be broken
    }
    
    /**
     * Break a block with effects
     */
    private void breakBlock(BlockPos pos) {
        Level level = this.level();
        if (!level.isClientSide()) {
            BlockState blockState = level.getBlockState(pos);
            
            // Play break sound
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.0F, 1.0F);
            
            // Break the block
            level.destroyBlock(pos, true);
            
            // Show break effect
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    10, 0.1, 0.1, 0.1, 0.2);
            }
            
            // Notify nearby players with an ominous message if in hunting phase
            if (stalkingPhase == 2 && this.getTarget() instanceof ServerPlayer targetPlayer && random.nextFloat() < 0.3f) {
                targetPlayer.sendSystemMessage(Component.literal("§4§oThe Iteration is breaking through..."));
            }
        }
    }
} 