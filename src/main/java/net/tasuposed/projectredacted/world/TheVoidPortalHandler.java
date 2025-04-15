package net.tasuposed.projectredacted.world;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tasuposed.projectredacted.ProjectRedacted;
import net.tasuposed.projectredacted.horror.HorrorManager;
import net.tasuposed.projectredacted.horror.PlayerHorrorState;
import net.tasuposed.projectredacted.network.packets.GlitchScreenPacket;
import net.tasuposed.projectredacted.network.NetworkHandler;
import net.tasuposed.projectredacted.network.packets.PlaySoundPacket;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles portals to and from The Void dimension
 */
@Mod.EventBusSubscriber(modid = ProjectRedacted.MODID)
public class TheVoidPortalHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TheVoidPortalHandler.class);
    private static final RandomSource RANDOM = RandomSource.create();
    
    // Map to track players in portal cooldown
    private static final Map<UUID, Long> portalCooldowns = new HashMap<>();
    private static final long PORTAL_COOLDOWN = 60 * 20; // 60 seconds cooldown in ticks
    
    // Map to track players' return positions
    private static final Map<UUID, PortalDestination> returnDestinations = new HashMap<>();
    
    private static class PortalDestination {
        final ResourceKey<Level> dimension;
        final Vec3 position;
        
        public PortalDestination(ResourceKey<Level> dimension, Vec3 position) {
            this.dimension = dimension;
            this.position = position;
        }
    }
    
    /**
     * Initialize the portal handler
     */
    public static void init() {
        LOGGER.info("Initializing The Void portal handler");
        MinecraftForge.EVENT_BUS.register(TheVoidPortalHandler.class);
    }
    
    /**
     * Check for portal activation when players interact with certain blocks
     */
    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            // Check if it's a valid portal activation block
            if (isPortalActivationBlock(event.getLevel(), event.getPos())) {
                // Check horror stage progression - only available at later stages
                PlayerHorrorState state = HorrorManager.getInstance().getPlayerState(player);
                
                if (state.getCurrentStage() >= 3 && RANDOM.nextFloat() < 0.25f) {
                    // 25% chance of portal activation if in stage 3 or higher
                    attemptPortalActivation(player, event.getPos());
                    event.setCanceled(true);
                }
            }
        }
    }
    
    /**
     * Check if this block can be used to activate a portal
     */
    private static boolean isPortalActivationBlock(Level level, BlockPos pos) {
        // Portal can be activated on:
        // 1. Crying Obsidian
        // 2. Ancient Debris
        // 3. An End Portal Frame
        // 4. A specially configured structure block
        
        return level.getBlockState(pos).is(Blocks.CRYING_OBSIDIAN) || 
               level.getBlockState(pos).is(Blocks.ANCIENT_DEBRIS) ||
               level.getBlockState(pos).is(Blocks.END_PORTAL_FRAME) ||
               (level.getBlockState(pos).is(Blocks.STRUCTURE_BLOCK) && 
                !level.getBlockState(pos.above()).is(Blocks.AIR));
    }
    
    /**
     * Attempt to create a portal at the given position
     */
    private static void attemptPortalActivation(ServerPlayer player, BlockPos pos) {
        // Check cooldown
        if (portalCooldowns.containsKey(player.getUUID())) {
            long lastPortalTime = portalCooldowns.get(player.getUUID());
            long currentTime = player.level().getGameTime();
            
            if (currentTime - lastPortalTime < PORTAL_COOLDOWN) {
                // Still on cooldown
                player.sendSystemMessage(Component.literal("§8§o§kThe void§r §8§ogets restless..."));
                return;
            }
        }
        
        // Reset cooldown
        portalCooldowns.put(player.getUUID(), player.level().getGameTime());
        
        // Create portal effects
        ServerLevel level = (ServerLevel)player.level();
        
        // Particle and sound effects
        for (int i = 0; i < 50; i++) {
            double offsetX = RANDOM.nextDouble() * 2.0 - 1.0;
            double offsetY = RANDOM.nextDouble() * 2.0 - 1.0;
            double offsetZ = RANDOM.nextDouble() * 2.0 - 1.0;
            
            level.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    pos.getX() + 0.5 + offsetX * 0.5,
                    pos.getY() + 0.5 + offsetY * 0.5,
                    pos.getZ() + 0.5 + offsetZ * 0.5,
                    1, 0, 0, 0, 0.05);
        }
        
        // Play sound
        level.playSound(null, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0f, 0.5f);
        
        // Send screen effects to the player
        NetworkHandler.sendToPlayer(
                new GlitchScreenPacket(2, 0.9f, 60), // Strong corruption for 3 seconds
                player);
        
        NetworkHandler.sendToPlayer(
                new PlaySoundPacket(
                        ForgeRegistries.SOUND_EVENTS.getKey(SoundEvents.PORTAL_TRAVEL),
                        SoundSource.AMBIENT, 0.7f, 0.5f, true, false),
                player);
        
        // Schedule teleportation after a delay
        level.getServer().tell(new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + 40, // 2 second delay
                () -> teleportPlayerToVoid(player, pos)));
    }
    
    /**
     * Teleport a player to The Void dimension
     */
    public static void teleportPlayerToVoid(ServerPlayer player, BlockPos sourcePos) {
        if (player.level().dimension() == DimensionRegistry.THE_VOID) {  // Fixed: THE_VOID to THE_VOID_KEY
            // Already in The Void, try to return to overworld
            returnFromVoid(player);
            return;
        }
        
        // Store current position for the return journey
        returnDestinations.put(player.getUUID(), new PortalDestination(
                player.level().dimension(),
                player.position()));
        
        // Get the server and the destination level
        ServerLevel serverLevel = player.server.getLevel(DimensionRegistry.THE_VOID);  // Fixed: THE_VOID to THE_VOID_KEY
        
        if (serverLevel != null) {
            // Find a safe destination in The Void
            BlockPos destPos = findSafeDestination(serverLevel);
            
            // Teleport the player
            player.sendSystemMessage(Component.literal("§8§oYou feel yourself being pulled into the void..."));
            
            player.changeDimension(serverLevel, new VoidTeleporter(destPos));
            
            // Effects on arrival
            NetworkHandler.sendToPlayer(
                    new GlitchScreenPacket(1, 0.6f, 40), // Distortion
                    player);
            
            // After teleport trigger some residual effects
            serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                    serverLevel.getServer().getTickCount() + 20,
                    () -> {
                        // Whispers on arrival
                        NetworkHandler.sendToPlayer(
                                new PlaySoundPacket(
                                        ForgeRegistries.SOUND_EVENTS.getKey(SoundEvents.AMBIENT_CAVE.value()),
                                        SoundSource.AMBIENT,
                                        0.5f,
                                        0.5f,
                                        true, false),
                                player);
                        
                        // Subtle lore message
                        player.sendSystemMessage(Component.literal("§8§oThe source of all fear - you can feel it watching you..."));
                    }));
        } else {
            LOGGER.error("Failed to get The Void dimension for teleportation");
            player.sendSystemMessage(Component.literal("§cThe portal fizzles out..."));
        }
    }
    
    /**
     * Return a player from The Void back to their original dimension
     */
    public static void returnFromVoid(ServerPlayer player) {
        PortalDestination dest = returnDestinations.get(player.getUUID());
        
        if (dest != null) {
            // Get the destination level
            ServerLevel destLevel = player.server.getLevel(dest.dimension);
            
            if (destLevel != null) {
                // Return effects
                NetworkHandler.sendToPlayer(
                        new GlitchScreenPacket(0, 0.8f, 40),
                        player);
                
                player.sendSystemMessage(Component.literal("§8§oYou feel yourself being pulled back to reality..."));
                
                // Teleport back
                Vec3 destPos = dest.position;
                player.changeDimension(destLevel, new VoidTeleporter(new BlockPos(
                        (int)destPos.x, (int)destPos.y, (int)destPos.z)));
                
                // Remove from return map
                returnDestinations.remove(player.getUUID());
                
                // After return effects
                destLevel.getServer().tell(new net.minecraft.server.TickTask(
                        destLevel.getServer().getTickCount() + 20,
                        () -> {
                            player.sendSystemMessage(Component.literal("§8§oSomething followed you back..."));
                            
                            // Chance to spawn an entity when returning
                            if (RANDOM.nextFloat() < 0.3f) {
                                HorrorManager.getInstance().triggerRandomEvent(player);
                            }
                        }));
            }
        } else {
            // No return destination saved, spawn at overworld spawn
            ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                player.changeDimension(overworld, new VoidTeleporter(overworld.getSharedSpawnPos()));
            }
        }
    }
    
    /**
     * Find a safe location in The Void for arrival
     */
    private static BlockPos findSafeDestination(ServerLevel level) {
        // Start searching from the central region
        BlockPos startPos = new BlockPos(0, 50, 0);
        
        // Look for platforms within a 200 block radius
        for (int attempt = 0; attempt < 100; attempt++) {
            int offsetX = RANDOM.nextInt(400) - 200;
            int offsetZ = RANDOM.nextInt(400) - 200;
            
            // Search for a platform at various heights
            for (int y = 30; y < 100; y++) {
                BlockPos checkPos = new BlockPos(offsetX, y, offsetZ);
                
                if (isSafeArrivalLocation(level, checkPos)) {
                    return checkPos.above();
                }
            }
        }
        
        // If no safe position found, create a small platform
        BlockPos fallbackPos = new BlockPos(
                RANDOM.nextInt(100) - 50,
                50,
                RANDOM.nextInt(100) - 50);
        
        createSafetyPlatform(level, fallbackPos);
        return fallbackPos.above();
    }
    
    /**
     * Check if a position is safe for arrival
     */
    private static boolean isSafeArrivalLocation(Level level, BlockPos pos) {
        // Check if there's a block to stand on
        if (!level.getBlockState(pos).isSolid()) {
            return false;
        }
        
        // Check if there's space for the player
        if (!level.getBlockState(pos.above()).isAir() || 
            !level.getBlockState(pos.above(2)).isAir()) {
            return false;
        }
        
        // Make sure there's a platform, not just a single block
        int solidBlockCount = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (level.getBlockState(pos.offset(x, 0, z)).isSolid()) {
                    solidBlockCount++;
                }
            }
        }
        
        return solidBlockCount >= 3; // At least 3 solid blocks to form minimal platform
    }
    
    /**
     * Create a simple safety platform for emergency arrival
     */
    private static void createSafetyPlatform(ServerLevel level, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlockAndUpdate(pos.offset(x, 0, z), Blocks.OBSIDIAN.defaultBlockState());
            }
        }
    }
    
    /**
     * Custom teleporter for dimension travel
     */
    private static class VoidTeleporter implements net.minecraftforge.common.util.ITeleporter {
        private final BlockPos targetPos;
        
        public VoidTeleporter(BlockPos targetPos) {
            this.targetPos = targetPos;
        }
        
        @Override
        public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, 
                                 float yaw, java.util.function.Function<Boolean, Entity> repositionEntity) {
            Entity repositioned = repositionEntity.apply(false);
            
            if (repositioned instanceof ServerPlayer player) {
                // Set the player's position to our target
                player.teleportTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
            }
            
            return repositioned;
        }
    }
    
    /**
     * Check for rare natural portal generation in the overworld
     */
    public static void tryGenerateNaturalPortal(ServerLevel level, BlockPos pos, RandomSource random) {
        // Very rare random generation of "broken" portal fragments in the overworld
        // These function as lore elements and hints about The Void dimension
        if (level.dimension() != Level.OVERWORLD || random.nextFloat() >= 0.3f) {
            return;
        }
        
        // Create a small ruined portal with crying obsidian
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    if ((x == -1 || x == 1 || z == -1 || z == 1) && random.nextFloat() < 0.7f) {
                        if (y < 2 || (x == 0 && z == 0)) {
                            BlockPos ruinPos = pos.offset(x, y, z);
                            
                            // Decide which block to use
                            if (random.nextFloat() < 0.7f) {
                                level.setBlock(ruinPos, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
                            } else {
                                level.setBlock(ruinPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }
        }
        
        // Add a lore book or mysterious item in the center
        BlockPos centerPos = pos.offset(0, 0, 0);
        if (level.getBlockState(centerPos).isAir()) {
            if (random.nextBoolean()) {
                // Add a chest with a lore book
                level.setBlock(centerPos, Blocks.CHEST.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, 
                                Direction.Plane.HORIZONTAL.getRandomDirection(random)), 3);
                
                if (level.getBlockEntity(centerPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                    // Add mysterious book
                    net.minecraft.world.item.ItemStack book = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);
                    net.minecraft.nbt.CompoundTag bookTag = book.getOrCreateTag();
                    bookTag.putString("title", "The Void Beckons");
                    bookTag.putString("author", "Unknown");
                    
                    // Add lore pages
                    net.minecraft.nbt.ListTag pages = new net.minecraft.nbt.ListTag();
                    pages.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(
                            Component.literal("I've found it. The source of all these entities. A place between dimensions where they wait, watching us. The signals are clear now."))));
                    pages.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(
                            Component.literal("They call it Protocol 37. It's not just an entity, but a project. A system to watch us. To study us. I think someone created them, but lost control."))));
                    pages.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(
                            Component.literal("The portal requires crying obsidian. Ancient debris. Pieces from The End. They're afraid of what's coming through. But I need to see for myself."))));
                    
                    bookTag.put("pages", pages);
                    chest.setItem(0, book);
                }
            } else {
                // Or add structure block that could be used for a portal
                level.setBlock(centerPos, Blocks.STRUCTURE_BLOCK.defaultBlockState(), 3);
            }
        }
    }
}