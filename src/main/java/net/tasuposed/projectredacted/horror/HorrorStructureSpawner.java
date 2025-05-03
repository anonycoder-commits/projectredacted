package net.tasuposed.projectredacted.horror;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Handles the spawning of creepy structures in the world.
 * These structures are designed to create an ARG-like atmosphere.
 */
public class HorrorStructureSpawner {
    private static final Logger LOGGER = LoggerFactory.getLogger(HorrorStructureSpawner.class);
    private static final RandomSource RANDOM = RandomSource.create();
    
    // Structure categories by rarity level
    private enum StructureRarity {
        SUBTLE(0.7f),      // Very common, barely noticeable (70% chance)
        UNUSUAL(0.25f),    // Uncommon, slightly obvious (25% chance)
        UNSETTLING(0.04f), // Rare, distinctly unnatural (4% chance)
        OBVIOUS(0.01f);    // Very rare, clearly out of place (1% chance)
        
        private final float chance;
        
        StructureRarity(float chance) {
            this.chance = chance;
        }
        
        public float getChance() {
            return chance;
        }
    }
    
    // Base chance of structure spawn
    private static final float BASE_STRUCTURE_CHANCE = 0.0025f; // Increased from 0.0012f
    
    // Map to track structures spawned per chunk
    private static final Map<Long, StructureData> structuresPerChunk = new HashMap<>();
    
    // Minimum distance from player (blocks)
    private static final int MIN_SPAWN_DISTANCE = 30;
    // Maximum distance from player (blocks)
    private static final int MAX_SPAWN_DISTANCE = 60;
    // Maximum structures per chunk
    private static final int MAX_STRUCTURES_PER_CHUNK = 3;
    // Time before chunk data is cleaned up (in ticks)
    private static final long CHUNK_CLEANUP_INTERVAL = 72000; // 1 hour
    
    private static long lastCleanupTime = 0;
    
    // Data class to track structures in chunks
    private static class StructureData {
        int count;
        long lastSpawnTime;
        
        public StructureData(int count, long time) {
            this.count = count;
            this.lastSpawnTime = time;
        }
    }
    
    /**
     * Registers the structure spawner event handlers
     */
    public static void init() {
        MinecraftForge.EVENT_BUS.register(HorrorStructureSpawner.class);
        LOGGER.info("Horror Structure Spawner initialized");
    }
    
    /**
     * Player tick event handler - checks for structure spawning
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer)) {
            return;
        }
        
        ServerPlayer player = (ServerPlayer) event.player;
        ServerLevel level = player.serverLevel();
        
        // Only spawn in the overworld
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        
        // Clean up old chunk data periodically
        if (level.getGameTime() - lastCleanupTime > CHUNK_CLEANUP_INTERVAL) {
            cleanupChunkData();
            lastCleanupTime = level.getGameTime();
        }
        
        // Check for structure spawning
        if (RANDOM.nextFloat() < BASE_STRUCTURE_CHANCE) {
            attemptStructureSpawn(level, player);
        }
    }
    
    /**
     * Command to forcibly spawn a structure for testing
     */
    public static void debugSpawnStructure(ServerPlayer player, ServerLevel level, int type) {
        BlockPos pos = findSuitableLocation(level, player.blockPosition(), MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE);
        if (pos != null) {
            spawnStructure(level, pos, type);
            player.sendSystemMessage(Component.literal("Spawned test structure at " + pos.toShortString()));
        } else {
            player.sendSystemMessage(Component.literal("Could not find suitable location for structure"));
        }
    }
    
    /**
     * Attempt to spawn a structure near a player
     */
    private static void attemptStructureSpawn(ServerLevel level, ServerPlayer player) {
        // Get the player position and check for safety
        BlockPos playerPos = player.blockPosition();
        
        // Find a suitable location
        BlockPos spawnPos = findSuitableLocation(level, playerPos, MIN_SPAWN_DISTANCE, MAX_SPAWN_DISTANCE);
        
        if (spawnPos != null) {
            // Process and spawn a structure
            processStructureGeneration(level, spawnPos, -1);
        }
    }
    
    /**
     * Process and choose a structure to spawn at the given position
     * If type is -1, a random structure is chosen
     */
    private static void processStructureGeneration(ServerLevel level, BlockPos pos, int type) {
        // Skip if we've reached the maximum structures for this chunk
        long chunkKey = getChunkKey(pos);
        
        if (!canSpawnStructureInChunk(level, chunkKey)) {
            return;
        }
        
        // Determine structure type based on rarity
        if (type < 0) {
            float randomValue = RANDOM.nextFloat();
            
            // Very rare chance to spawn a portal fragment (new)
            if (randomValue < 0.01f) {
                // 1% chance to spawn a dimensional portal fragment
                net.tasuposed.projectredacted.world.TheVoidPortalHandler.tryGenerateNaturalPortal(level, pos, RANDOM);
                
                // Record structure in chunk
                recordStructureSpawn(level, chunkKey);
                return;
            }
            
            // Select a structure category based on rarity
            StructureRarity rarity;
            if (randomValue < StructureRarity.SUBTLE.getChance()) {
                rarity = StructureRarity.SUBTLE;
            } else if (randomValue < StructureRarity.SUBTLE.getChance() + StructureRarity.UNUSUAL.getChance()) {
                rarity = StructureRarity.UNUSUAL;
            } else if (randomValue < StructureRarity.SUBTLE.getChance() + StructureRarity.UNUSUAL.getChance() + StructureRarity.UNSETTLING.getChance()) {
                rarity = StructureRarity.UNSETTLING;
            } else {
                rarity = StructureRarity.OBVIOUS;
            }
            
            // Choose structure from the appropriate category
            type = chooseStructureByRarity(rarity);
        }
        
        // Spawn the structure based on type
        generateStructure(level, pos, type);
        
        // Record that we spawned a structure
        recordStructureSpawn(level, chunkKey);
    }
    
    /**
     * Spawn a structure at the given position
     * If type is -1, a random structure is chosen
     */
    private static void generateStructure(ServerLevel level, BlockPos pos, int type) {
        // Skip if we've reached the maximum structures for this chunk
        long chunkKey = getChunkKey(pos);
        
        if (!canSpawnStructureInChunk(level, chunkKey)) {
            return;
        }
        
        // Determine structure type based on rarity
        if (type < 0) {
            float randomValue = RANDOM.nextFloat();
            
            // Very rare chance to spawn a portal fragment (new)
            if (randomValue < 0.01f) {
                // 1% chance to spawn a dimensional portal fragment
                net.tasuposed.projectredacted.world.TheVoidPortalHandler.tryGenerateNaturalPortal(level, pos, RANDOM);
                
                // Record structure in chunk
                recordStructureSpawn(level, chunkKey);
                return;
            }
            
            // Select a structure category based on rarity
            StructureRarity rarity;
            if (randomValue < StructureRarity.SUBTLE.getChance()) {
                rarity = StructureRarity.SUBTLE;
            } else if (randomValue < StructureRarity.SUBTLE.getChance() + StructureRarity.UNUSUAL.getChance()) {
                rarity = StructureRarity.UNUSUAL;
            } else if (randomValue < StructureRarity.SUBTLE.getChance() + StructureRarity.UNUSUAL.getChance() + StructureRarity.UNSETTLING.getChance()) {
                rarity = StructureRarity.UNSETTLING;
            } else {
                rarity = StructureRarity.OBVIOUS;
            }
            
            // Choose structure from the appropriate category
            type = chooseStructureByRarity(rarity);
        }
        
        // Spawn the structure based on type
        switch (type) {
            // ... existing structure cases ...
            // ... existing code ...
        }
    }
    
    /**
     * Select a rarity level based on weighted chances
     */
    private static StructureRarity selectRarityLevel() {
        float roll = RANDOM.nextFloat();
        float sum = 0;
        
        for (StructureRarity rarity : StructureRarity.values()) {
            sum += rarity.getChance();
            if (roll < sum) {
                return rarity;
            }
        }
        
        return StructureRarity.SUBTLE; // Fallback
    }
    
    /**
     * Find a suitable location for structure placement
     */
    private static BlockPos findSuitableLocation(ServerLevel level, BlockPos origin, int minDistance, int maxDistance) {
        // Try multiple times to find a good spot
        for (int attempt = 0; attempt < 40; attempt++) {
            // Generate a random distance between min and max
            double distance = minDistance + RANDOM.nextDouble() * (maxDistance - minDistance);
            // Generate a random angle
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            
            // Calculate offset
            int xOffset = (int) (Math.cos(angle) * distance);
            int zOffset = (int) (Math.sin(angle) * distance);
            
            // Find the ground position
            BlockPos targetPos = findGroundPos(level, origin.offset(xOffset, 0, zOffset), 30);
            
            if (targetPos != null && isValidStructurePosition(level, targetPos)) {
                return targetPos;
            }
        }
        
        return null;
    }
    
    /**
     * Find the ground position for a structure
     */
    private static BlockPos findGroundPos(ServerLevel level, BlockPos start, int maxY) {
        // Start from the player's Y position and scan downward
        // This prevents structures from spawning too deep underground when player is on the surface
        BlockPos playerPos = start;
        int startY = Math.min(playerPos.getY() + 5, level.getMaxBuildHeight() - 10);
        
        // Cap the search to prevent going too deep
        int minY = Math.max(level.getMinBuildHeight() + 10, playerPos.getY() - 25);
        
        for (int y = startY; y >= minY; y--) {
            BlockPos checkPos = new BlockPos(start.getX(), y, start.getZ());
            BlockPos posBelow = checkPos.below();
            
            // Check if current position is air and below is solid
            if (level.getBlockState(checkPos).isAir() && 
                !level.getBlockState(posBelow).isAir() && 
                level.getBlockState(posBelow).isSolid()) {
                
                // Don't place structures deep underground if player is outside
                boolean playerIsOutside = level.canSeeSky(playerPos);
                boolean posIsOutside = level.canSeeSky(checkPos);
                
                // If player is outside, prefer positions that are also outside
                if (playerIsOutside && !posIsOutside && y < playerPos.getY() - 10) {
                    continue;
                }
                
                return posBelow;
            }
        }
        
        // Fallback to the starting position's Y-level if no suitable ground found
        return new BlockPos(start.getX(), start.getY(), start.getZ());
    }
    
    /**
     * Check if a position is valid for structure placement
     */
    private static boolean isValidStructurePosition(ServerLevel level, BlockPos pos) {
        // Get the block at the position and below
        BlockState blockAtPos = level.getBlockState(pos.above());
        BlockState blockBelow = level.getBlockState(pos);
        
        // Get AABB for liquid check (create a 1x1x1 box centered on the position)
        AABB aabb = new AABB(
            pos.above().getX() - 0.5, pos.above().getY() - 0.5, pos.above().getZ() - 0.5,
            pos.above().getX() + 0.5, pos.above().getY() + 0.5, pos.above().getZ() + 0.5
        );
        
        // Check if there's air at the position, and solid ground below
        return blockAtPos.isAir() && 
               blockBelow.isFaceSturdy(level, pos, Direction.UP) &&
               !level.containsAnyLiquid(aabb) &&
               !level.getBlockState(pos).is(Blocks.BEDROCK);
    }
    
    /**
     * Generate a unique key for a chunk
     */
    private static long getChunkKey(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
    
    /**
     * Clean up old chunk data
     */
    private static void cleanupChunkData() {
        structuresPerChunk.clear();
        LOGGER.debug("Cleaned up horror structure chunk data");
    }
    
    /**
     * Spawn a random structure based on the selected rarity
     */
    private static boolean spawnRandomStructure(ServerLevel level, BlockPos pos, StructureRarity rarity) {
        // Select a structure type based on rarity
        int structureId;
        
        switch (rarity) {
            case SUBTLE:
                structureId = RANDOM.nextInt(10); // 0-9
                break;
            case UNUSUAL:
                structureId = 10 + RANDOM.nextInt(10); // 10-19
                break;
            case UNSETTLING:
                structureId = 20 + RANDOM.nextInt(10); // 20-29
                break;
            case OBVIOUS:
                structureId = 30 + RANDOM.nextInt(10); // 30-39
                break;
            default:
                structureId = RANDOM.nextInt(40);
        }
        
        return spawnStructure(level, pos, structureId);
    }
    
    /**
     * Spawn a specific structure by ID
     */
    private static boolean spawnStructure(ServerLevel level, BlockPos pos, int structureId) {
        try {
            // Adjust for ground - pos is the block we're standing on
            BlockPos placePos = pos.above();
            
            // Make sure the position is clear
            clearSnowAtPosition(level, placePos);
            
            // Spawn the structure based on ID
            switch (structureId) {
                // SUBTLE structures (0-9) - very minimal, could be natural
                case 0 -> spawnTorch(level, placePos);
                case 1 -> spawnSingleBlock(level, placePos, Blocks.COBBLESTONE);
                case 2 -> spawnStoneButton(level, placePos);
                case 3 -> spawnFlower(level, placePos);
                case 4 -> spawnSticks(level, placePos);
                case 5 -> spawnCoalPile(level, placePos);
                case 6 -> spawnStoneCracks(level, pos); // Placed AT ground level
                case 7 -> spawnSuspiciousDirt(level, pos); // Placed AT ground level
                case 8 -> spawnSingleBlock(level, placePos, Blocks.MOSSY_COBBLESTONE);
                case 9 -> spawnSingleBlock(level, placePos, Blocks.STONE_BUTTON);
                
                // UNUSUAL structures (10-19) - might raise questions
                case 10 -> spawnRedstoneWire(level, placePos);
                case 11 -> spawnSmallStoneFormation(level, placePos);
                case 12 -> spawnWoodenPost(level, placePos);
                case 13 -> spawnSign(level, placePos);
                case 14 -> spawnObserver(level, placePos);
                case 15 -> spawnChains(level, placePos);
                case 16 -> spawnCobwebCorner(level, placePos);
                case 17 -> spawnStrangeCarpet(level, placePos);
                case 18 -> spawnTinyPillar(level, placePos);
                case 19 -> spawnIronBars(level, placePos);
                
                // UNSETTLING structures (20-29) - clearly unnatural
                case 20 -> spawnNetherPortalRemnant(level, placePos);
                case 21 -> spawnAbandonedCampfire(level, placePos);
                case 22 -> spawnRedstoneCircle(level, placePos); 
                case 23 -> spawnSkullOnFence(level, placePos);
                case 24 -> spawnSoulSandPatch(level, pos); // Placed AT ground level
                case 25 -> spawnHangingChains(level, placePos);
                case 26 -> spawnRedstoneWithRedTorch(level, placePos);
                case 27 -> spawnMysteriousCircle(level, placePos);
                case 28 -> spawnBreakingBedrock(level, pos); // Placed AT ground level
                case 29 -> spawnWatchingObservers(level, placePos);
                
                // OBVIOUS structures (30-39) - impossible to miss, major creep factor
                case 30 -> spawnAltarArea(level, placePos);
                case 31 -> spawnCross(level, placePos);
                case 32 -> spawnPlayerGrave(level, placePos);
                case 33 -> spawnStrangePillar(level, placePos); // Replacing AbandonedPlayerCamp
                case 34 -> spawnNametag(level, placePos);
                case 35 -> spawnRustyChain(level, placePos); // Replacing EnscasedVillager
                case 36 -> spawnDoorToNowhere(level, placePos);
                case 37 -> spawnCryptographicSymbols(level, placePos); // Replacing FakePlayerHouse
                case 38 -> spawnRealismBreak(level, placePos);
                case 39 -> spawnHiddenMessage(level, placePos);
                
                // NEW STRUCTURES (40+) - player buildings
                case 40 -> spawnStarterHouse(level, pos);
            }
            
            // Play a sound when structure is generated (audible within a short range)
            if (structureId >= 30) { // For obvious structures, make a noticeable sound
                level.playSound(null, pos, 
                        SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), 
                        SoundSource.AMBIENT, 0.7F, 0.8F + RANDOM.nextFloat() * 0.2F);
            } else if (structureId >= 20) { // For unsettling structures, make a subtle sound
                level.playSound(null, pos, 
                        SoundEvents.AMBIENT_CAVE.value(), 
                        SoundSource.AMBIENT, 0.25F, 0.8F + RANDOM.nextFloat() * 0.4F);
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Error spawning structure ID: " + structureId, e);
            return false;
        }
    }
    
    /**
     * Clear snow at a position
     */
    private static void clearSnowAtPosition(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() == Blocks.SNOW) {
            level.removeBlock(pos, false);
        }
    }
    
    //
    // SUBTLE STRUCTURES (0-9) - Very minimal, could be natural
    //
    
    // Structure 0: Simple torch in the wilderness
    private static void spawnTorch(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.TORCH.defaultBlockState(), 3);
    }
    
    // Structure 1: Single cobblestone block
    private static void spawnSingleBlock(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
        level.setBlock(pos, block.defaultBlockState(), 3);
    }
    
    // Structure 2: Stone button on ground (looks like a small stone)
    private static void spawnStoneButton(ServerLevel level, BlockPos pos) {
        level.setBlock(pos.below(), 
                Blocks.STONE_BUTTON.defaultBlockState()
                    .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 
                3);
    }
    
    // Structure 3: Single flower in unusual place
    private static void spawnFlower(ServerLevel level, BlockPos pos) {
        net.minecraft.world.level.block.Block[] flowers = {
            Blocks.POPPY, Blocks.DANDELION, Blocks.BLUE_ORCHID, 
            Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.RED_TULIP
        };
        
        level.setBlock(pos, flowers[RANDOM.nextInt(flowers.length)].defaultBlockState(), 3);
    }
    
    // Structure 4: Dropped sticks (item entities)
    private static void spawnSticks(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < 1 + RANDOM.nextInt(3); i++) {
            double offsetX = RANDOM.nextDouble() * 0.5 - 0.25;
            double offsetZ = RANDOM.nextDouble() * 0.5 - 0.25;
            
            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                    level, pos.getX() + 0.5 + offsetX, pos.getY() + 0.1, pos.getZ() + 0.5 + offsetZ,
                    new ItemStack(Items.STICK, 1)));
        }
    }
    
    // Structure 5: Small coal pile
    private static void spawnCoalPile(ServerLevel level, BlockPos pos) {
        // Add coal items on the ground
        for (int i = 0; i < 2 + RANDOM.nextInt(3); i++) {
            double offsetX = RANDOM.nextDouble() * 0.8 - 0.4;
            double offsetZ = RANDOM.nextDouble() * 0.8 - 0.4;
            
            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                    level, pos.getX() + 0.5 + offsetX, pos.getY() + 0.1, pos.getZ() + 0.5 + offsetZ,
                    new ItemStack(Items.COAL, 1)));
        }
    }
    
    // Structure 6: Cracked stone on the ground
    private static void spawnStoneCracks(ServerLevel level, BlockPos groundPos) {
        // Create a small patch of cracked stone bricks
        level.setBlock(groundPos, Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3);
        
        // Sometimes add a bit more cracked stone nearby
        if (RANDOM.nextBoolean()) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
            BlockPos adjacent = groundPos.relative(dir);
            
            if (level.getBlockState(adjacent).isFaceSturdy(level, adjacent, Direction.UP)) {
                level.setBlock(adjacent, Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3);
            }
        }
    }
    
    // Structure 7: Suspicious looking dirt
    private static void spawnSuspiciousDirt(ServerLevel level, BlockPos groundPos) {
        // Replace the top block with coarse dirt
        level.setBlock(groundPos, Blocks.COARSE_DIRT.defaultBlockState(), 3);
        
        // Sometimes add more coarse dirt in a small pattern
        if (RANDOM.nextBoolean()) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
            BlockPos adjacent = groundPos.relative(dir);
            
            if (level.getBlockState(adjacent).isFaceSturdy(level, adjacent, Direction.UP)) {
                level.setBlock(adjacent, Blocks.COARSE_DIRT.defaultBlockState(), 3);
            }
        }
    }
    
    //
    // UNUSUAL STRUCTURES (10-19) - Might raise questions
    //
    
    // Structure 10: Strange redstone wire
    private static void spawnRedstoneWire(ServerLevel level, BlockPos pos) {
        // Place redstone wire
        level.setBlock(pos, Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
        
        // Add more redstone in a line
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
        for (int i = 1; i <= 2 + RANDOM.nextInt(3); i++) {
            BlockPos wirePos = pos.relative(dir, i);
            
            // Ensure we have a solid block below
            if (level.getBlockState(wirePos.below()).isFaceSturdy(level, wirePos.below(), Direction.UP)) {
                level.setBlock(wirePos, Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
            } else {
                break;
            }
        }
    }
    
    // Structure 11: Small rock formation
    private static void spawnSmallStoneFormation(ServerLevel level, BlockPos pos) {
        // Center stone
        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        
        // Add some more stones in a small formation
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (RANDOM.nextFloat() < 0.7f) {
                BlockPos stonePos = pos.relative(dir);
                
                // Check for suitable placement
                if (level.getBlockState(stonePos).isAir() && 
                    level.getBlockState(stonePos.below()).isFaceSturdy(level, stonePos.below(), Direction.UP)) {
                    
                    // Different stone variants
                    net.minecraft.world.level.block.Block stoneType;
                    float choice = RANDOM.nextFloat();
                    
                    if (choice < 0.3f) {
                        stoneType = Blocks.COBBLESTONE;
                    } else if (choice < 0.6f) {
                        stoneType = Blocks.MOSSY_COBBLESTONE;
                    } else if (choice < 0.8f) {
                        stoneType = Blocks.ANDESITE;
                    } else {
                        stoneType = Blocks.STONE;
                    }
                    
                    level.setBlock(stonePos, stoneType.defaultBlockState(), 3);
                }
            }
        }
    }
    
    // Structure 12: Wooden post
    private static void spawnWoodenPost(ServerLevel level, BlockPos pos) {
        // Create a wooden post (fence post)
        for (int y = 0; y < 2 + RANDOM.nextInt(2); y++) {
            level.setBlock(pos.above(y), Blocks.OAK_FENCE.defaultBlockState(), 3);
        }
        
        // Sometimes add a torch on top
        if (RANDOM.nextFloat() < 0.3f) {
            int height = 2 + RANDOM.nextInt(2);
            level.setBlock(pos.above(height), Blocks.TORCH.defaultBlockState(), 3);
        }
    }
    
    // Structure 13: Empty sign
    private static void spawnSign(ServerLevel level, BlockPos pos) {
        // Create a sign post
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState(), 3);
        
        // Get the sign block entity
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            // Create sign text using NBT
            CompoundTag signData = new CompoundTag();
            
            // Create text for each line
            String[] creepyLines = {
                "§4W A T C H I N G",
                "§0Y O U",
                "",
                "§8§o- H"
            };
            
            // Set front text - in 1.20.1 we need to use proper NBT structure
            CompoundTag frontText = new CompoundTag();
            ListTag messages = new ListTag();
            
            // Add each message as string NBT
            for (String line : creepyLines) {
                messages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
            }
            
            // Add messages to front text
            frontText.put("messages", messages);
            frontText.putBoolean("has_glowing_text", RANDOM.nextFloat() < 0.3f);
            frontText.putString("color", "red");
            
            // Set front text in sign data
            signData.put("front_text", frontText);
            
            // Apply the NBT data to the sign
            sign.load(signData);
            sign.setChanged();
        }
    }
    
    // Structure 14: Observer looking in a random direction
    private static void spawnObserver(ServerLevel level, BlockPos pos) {
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
        level.setBlock(pos, Blocks.OBSERVER.defaultBlockState()
                .setValue(BlockStateProperties.FACING, facing), 3);
    }
    
    // Structure 15: Chain
    private static void spawnChains(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.CHAIN.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y), 3);
        
        // Sometimes add another chain on top
        if (RANDOM.nextFloat() < 0.5f) {
            level.setBlock(pos.above(), Blocks.CHAIN.defaultBlockState()
                    .setValue(BlockStateProperties.AXIS, Direction.Axis.Y), 3);
        }
    }
    
    // Structure 16: Cobweb corner
    private static void spawnCobwebCorner(ServerLevel level, BlockPos pos) {
        // Place cobweb
        level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
        
        // Add a second cobweb
        if (RANDOM.nextBoolean()) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
            BlockPos webPos = pos.relative(dir);
            
            if (level.getBlockState(webPos).isAir() && 
                level.getBlockState(webPos.below()).isFaceSturdy(level, webPos.below(), Direction.UP)) {
                level.setBlock(webPos, Blocks.COBWEB.defaultBlockState(), 3);
            }
        }
    }
    
    // Structure 17: Strange carpet placement
    private static void spawnStrangeCarpet(ServerLevel level, BlockPos pos) {
        // Choose a color for the carpet
        net.minecraft.world.level.block.Block carpet;
        int color = RANDOM.nextInt(5);
        switch (color) {
            case 0 -> carpet = Blocks.RED_CARPET;
            case 1 -> carpet = Blocks.BLACK_CARPET;
            case 2 -> carpet = Blocks.PURPLE_CARPET;
            case 3 -> carpet = Blocks.GRAY_CARPET;
            default -> carpet = Blocks.BROWN_CARPET;
        }
        
        // Place the carpet
        level.setBlock(pos, carpet.defaultBlockState(), 3);
        
        // Sometimes add a pattern of carpet
        if (RANDOM.nextFloat() < 0.4f) {
            // Choose a direction for the carpet line
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
            
            for (int i = 1; i <= 2 + RANDOM.nextInt(3); i++) {
                BlockPos carpetPos = pos.relative(dir, i);
                
                if (level.getBlockState(carpetPos).isAir() && 
                    level.getBlockState(carpetPos.below()).isFaceSturdy(level, carpetPos.below(), Direction.UP)) {
                    level.setBlock(carpetPos, carpet.defaultBlockState(), 3);
                } else {
                    break;
                }
            }
        }
    }
    
    // Structure 18: Small pillar/totem-like structure
    private static void spawnTinyPillar(ServerLevel level, BlockPos pos) {
        // Base block
        level.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 3);
        
        // Middle block
        level.setBlock(pos.above(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
        
        // Top decoration
        if (RANDOM.nextBoolean()) {
            level.setBlock(pos.above(2), Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3);
        }
    }
    
    // Structure 19: Iron bars in strange place
    private static void spawnIronBars(ServerLevel level, BlockPos pos) {
        // Place iron bars
        level.setBlock(pos, Blocks.IRON_BARS.defaultBlockState(), 3);
        
        // Sometimes add a second bar
        if (RANDOM.nextBoolean()) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
            BlockPos barPos = pos.relative(dir);
            
            if (level.getBlockState(barPos).isAir() && 
                level.getBlockState(barPos.below()).isFaceSturdy(level, barPos.below(), Direction.UP)) {
                level.setBlock(barPos, Blocks.IRON_BARS.defaultBlockState(), 3);
            }
        }
    }
    
    //
    // UNSETTLING STRUCTURES (20-29) - Clearly unnatural
    //
    
    // Structure 20: Remnants of a nether portal
    private static void spawnNetherPortalRemnant(ServerLevel level, BlockPos pos) {
        // Create a partial obsidian frame
        level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.OBSIDIAN.defaultBlockState(), 3);
        
        // Add some broken pieces around
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
        Direction perpDir = dir.getClockWise();
        
        // Add one corner
        BlockPos cornerPos = pos.relative(dir);
        if (level.getBlockState(cornerPos).isAir() && 
            level.getBlockState(cornerPos.below()).isFaceSturdy(level, cornerPos.below(), Direction.UP)) {
            level.setBlock(cornerPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
        }
        
        // Add one more piece to make it look broken
        BlockPos extraPos = pos.relative(perpDir);
        if (level.getBlockState(extraPos).isAir() && 
            level.getBlockState(extraPos.below()).isFaceSturdy(level, extraPos.below(), Direction.UP)) {
            level.setBlock(extraPos, Blocks.OBSIDIAN.defaultBlockState(), 3);
        }
        
        // Add some netherrack and soul sand nearby to enhance the effect
        BlockPos netherrackPos = pos.relative(dir.getOpposite());
        if (level.getBlockState(netherrackPos).isAir() && 
            level.getBlockState(netherrackPos.below()).isFaceSturdy(level, netherrackPos.below(), Direction.UP)) {
            level.setBlock(netherrackPos, Blocks.NETHERRACK.defaultBlockState(), 3);
            
            // Sometimes add fire on the netherrack
            if (RANDOM.nextBoolean()) {
                level.setBlock(netherrackPos.above(), Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }
    
    // Structure 21: Abandoned campfire with mysterious surroundings
    private static void spawnAbandonedCampfire(ServerLevel level, BlockPos pos) {
        // Place the campfire (extinguished)
        level.setBlock(pos, Blocks.CAMPFIRE.defaultBlockState()
                .setValue(BlockStateProperties.LIT, false), 3);
        
        // Add some "seating" logs around it
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (RANDOM.nextFloat() < 0.6f) {
                BlockPos logPos = pos.relative(dir, 2);
                
                if (level.getBlockState(logPos).isAir() && 
                    level.getBlockState(logPos.below()).isFaceSturdy(level, logPos.below(), Direction.UP)) {
                    
                    // Either a log or a stump
                    if (RANDOM.nextBoolean()) {
                        // Log
                        Direction.Axis axis = dir.getAxis() == Direction.Axis.X ? 
                                Direction.Axis.Z : Direction.Axis.X;
                        level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState()
                                .setValue(BlockStateProperties.AXIS, axis), 3);
                    } else {
                        // Stump
                        level.setBlock(logPos, Blocks.OAK_LOG.defaultBlockState(), 3);
                    }
                }
            }
        }
        
        // Add some dropped items if lucky (30% chance)
        if (RANDOM.nextFloat() < 0.3f) {
            ItemStack[] possibleItems = {
                new ItemStack(Items.BONE),
                new ItemStack(Items.LEATHER),
                new ItemStack(Items.PAPER),
                new ItemStack(Items.BOOK),
                new ItemStack(Items.MAP),
                new ItemStack(Items.COMPASS)
            };
            
            ItemStack droppedItem = possibleItems[RANDOM.nextInt(possibleItems.length)];
            
            // Drop the item near the campfire
            double offsetX = RANDOM.nextDouble() * 0.8 - 0.4;
            double offsetZ = RANDOM.nextDouble() * 0.8 - 0.4;
            
            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                    level, pos.getX() + 0.5 + offsetX, pos.getY() + 0.5, pos.getZ() + 0.5 + offsetZ,
                    droppedItem));
        }
    }
    
    // Structure 22: Redstone circle on the ground
    private static void spawnRedstoneCircle(ServerLevel level, BlockPos pos) {
        // Create a circle of redstone dust
        int radius = 2;
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Approximate circle using distance formula
                if (Math.sqrt(x*x + z*z) <= radius && Math.sqrt(x*x + z*z) > radius - 1) {
                    BlockPos circlePos = pos.offset(x, 0, z);
                    
                    if (level.getBlockState(circlePos).isAir() && 
                        level.getBlockState(circlePos.below()).isFaceSturdy(level, circlePos.below(), Direction.UP)) {
                        level.setBlock(circlePos, Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
                    }
                }
            }
        }
        
        // Add a redstone torch in the middle
        level.setBlock(pos, Blocks.REDSTONE_TORCH.defaultBlockState(), 3);
    }
    
    // Structure 23: Skull on a fence
    private static void spawnSkullOnFence(ServerLevel level, BlockPos pos) {
        // Place a fence post
        level.setBlock(pos, Blocks.OAK_FENCE.defaultBlockState(), 3);
        
        // Place the skull on top
        BlockState skullState;
        if (RANDOM.nextFloat() < 0.7f) {
            // Regular skeleton skull
            skullState = Blocks.SKELETON_SKULL.defaultBlockState();
        } else {
            // Wither skeleton skull (rarer)
            skullState = Blocks.WITHER_SKELETON_SKULL.defaultBlockState();
        }
        
        level.setBlock(pos.above(), skullState, 3);
    }
    
    // Structure 24: Soul sand patch on ground
    private static void spawnSoulSandPatch(ServerLevel level, BlockPos pos) {
        // Create a patch of soul sand/soil
        level.setBlock(pos, Blocks.SOUL_SAND.defaultBlockState(), 3);
        
        // Add more soul sand around
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (RANDOM.nextFloat() < 0.4f) {
                BlockPos sandPos = pos.relative(dir);
                
                if (level.getBlockState(sandPos).isFaceSturdy(level, sandPos, Direction.UP)) {
                    level.setBlock(sandPos, RANDOM.nextBoolean() ? 
                            Blocks.SOUL_SAND.defaultBlockState() : 
                            Blocks.SOUL_SOIL.defaultBlockState(), 3);
                }
            }
        }
        
        // Sometimes add soul fire on top
        if (RANDOM.nextFloat() < 0.3f) {
            level.setBlock(pos.above(), Blocks.SOUL_FIRE.defaultBlockState(), 3);
        }
    }
    
    // Structure 25: Hanging chains from nowhere
    private static void spawnHangingChains(ServerLevel level, BlockPos pos) {
        // Create a hanging chain with no support
        int height = 2 + RANDOM.nextInt(3);
        
        for (int y = 0; y < height; y++) {
            level.setBlock(pos.above(y), Blocks.CHAIN.defaultBlockState()
                    .setValue(BlockStateProperties.AXIS, Direction.Axis.Y), 3);
        }
        
        // Occasionally add a lantern or similar at the bottom
        if (RANDOM.nextFloat() < 0.4f) {
            if (RANDOM.nextBoolean()) {
                level.setBlock(pos.above(height), Blocks.LANTERN.defaultBlockState()
                        .setValue(BlockStateProperties.HANGING, true), 3);
            } else {
                level.setBlock(pos.above(height), Blocks.SOUL_LANTERN.defaultBlockState()
                        .setValue(BlockStateProperties.HANGING, true), 3);
            }
        }
    }
    
    // Structure 26: Redstone with redstone torch
    private static void spawnRedstoneWithRedTorch(ServerLevel level, BlockPos pos) {
        // Create a small redstone circuit
        // Center torch
        level.setBlock(pos, Blocks.REDSTONE_TORCH.defaultBlockState(), 3);
        
        // Redstone dust around it
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos dustPos = pos.relative(dir);
            
            if (level.getBlockState(dustPos).isAir() && 
                level.getBlockState(dustPos.below()).isFaceSturdy(level, dustPos.below(), Direction.UP)) {
                level.setBlock(dustPos, Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
            }
        }
    }
    
    // Structure 27: Mysterious Circle (replaced TrialRoom)
    private static void spawnMysteriousCircle(ServerLevel level, BlockPos pos) {
        // Create a ritual circle using redstone
        int radius = 3;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x*x + z*z <= radius*radius && x*x + z*z >= (radius-1)*(radius-1)) {
                    level.setBlock(pos.offset(x, 0, z), Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
                }
            }
        }
        
        // Add some strange blocks in the center and at cardinal points
        level.setBlock(pos, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        
        // Place ancient debris (shouldn't normally appear in overworld) at center sometimes
        if (RANDOM.nextFloat() < 0.2f) {
            level.setBlock(pos, Blocks.ANCIENT_DEBRIS.defaultBlockState(), 3);
        }
        
        // Add candles at the cardinal points
        BlockPos[] cardinalPoints = {
            pos.north(radius - 1),
            pos.south(radius - 1),
            pos.east(radius - 1),
            pos.west(radius - 1)
        };
        
        for (BlockPos candlePos : cardinalPoints) {
            if (RANDOM.nextFloat() < 0.7f) {
                level.setBlock(candlePos, Blocks.BLACK_CANDLE.defaultBlockState()
                        .setValue(BlockStateProperties.CANDLES, 1)
                        .setValue(BlockStateProperties.LIT, RANDOM.nextBoolean()), 3);
            }
        }
        
        // Sometimes add soul fire in the center
        if (RANDOM.nextFloat() < 0.3f) {
            level.setBlock(pos.above(), Blocks.SOUL_FIRE.defaultBlockState(), 3);
        }
    }
    
    // Structure 28: Bedrock breaking through the surface
    private static void spawnBreakingBedrock(ServerLevel level, BlockPos groundPos) {
        // Replace the ground with bedrock (should NEVER be possible in survival)
        level.setBlock(groundPos, Blocks.BEDROCK.defaultBlockState(), 3);
        
        // Add cracked stone bricks around to make it look like it broke through
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (RANDOM.nextFloat() < 0.7f) {
                BlockPos crackPos = groundPos.relative(dir);
                
                if (level.getBlockState(crackPos).isFaceSturdy(level, crackPos, Direction.UP)) {
                    level.setBlock(crackPos, Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3);
                }
            }
        }
    }
    
    // Structure 29: Ring of observers all looking inward
    private static void spawnWatchingObservers(ServerLevel level, BlockPos pos) {
        // Create a circle of observers all facing inward
        int radius = 2;
        
        // Place central block (redstone lamp)
        level.setBlock(pos, Blocks.REDSTONE_LAMP.defaultBlockState(), 3);
        
        // Place observers in a circle
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos observerPos = pos.relative(dir, radius);
            
            if (level.getBlockState(observerPos).isAir() && 
                level.getBlockState(observerPos.below()).isFaceSturdy(level, observerPos.below(), Direction.UP)) {
                
                // Face toward the center
                level.setBlock(observerPos, Blocks.OBSERVER.defaultBlockState()
                        .setValue(BlockStateProperties.FACING, dir.getOpposite()), 3);
            }
            
            // Add diagonal observers
            Direction clockwise = dir.getClockWise();
            BlockPos diagPos = pos.relative(dir, radius - 1).relative(clockwise, radius - 1);
            
            if (level.getBlockState(diagPos).isAir() && 
                level.getBlockState(diagPos.below()).isFaceSturdy(level, diagPos.below(), Direction.UP)) {
                
                // Find the direction toward center
                double xDir = pos.getX() - diagPos.getX();
                double zDir = pos.getZ() - diagPos.getZ();
                
                Direction faceDir;
                if (Math.abs(xDir) > Math.abs(zDir)) {
                    faceDir = xDir > 0 ? Direction.EAST : Direction.WEST;
                } else {
                    faceDir = zDir > 0 ? Direction.SOUTH : Direction.NORTH;
                }
                
                level.setBlock(diagPos, Blocks.OBSERVER.defaultBlockState()
                        .setValue(BlockStateProperties.FACING, faceDir), 3);
            }
        }
    }
    
    //
    // OBVIOUS STRUCTURES (30-39) - Impossible to miss, major creep factor
    //
    
    // Structure 30: Altar area with ominous decorations
    private static void spawnAltarArea(ServerLevel level, BlockPos pos) {
        // Create a platform
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Only fill the outer ring and center
                if (Math.abs(x) == 2 || Math.abs(z) == 2 || (x == 0 && z == 0)) {
                    BlockPos floorPos = pos.offset(x, -1, z);
                    level.setBlock(floorPos, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 3);
                }
            }
        }
        
        // Add an altar in the center
        level.setBlock(pos, Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState(), 3);
        
        // Add corner structures
        for (int x = -2; x <= 2; x += 4) {
            for (int z = -2; z <= 2; z += 4) {
                BlockPos cornerPos = pos.offset(x/2, 0, z/2); // Using half values to get corners
                
                // Create a pillar
                for (int y = 0; y < 2; y++) {
                    level.setBlock(cornerPos.above(y), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 3);
                }
                
                // Add top decoration
                if (RANDOM.nextBoolean()) {
                    level.setBlock(cornerPos.above(2), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
                } else {
                    level.setBlock(cornerPos.above(2), Blocks.SKELETON_SKULL.defaultBlockState(), 3);
                }
            }
        }
        
        // Add a central object on the altar
        BlockState centerObject;
        if (RANDOM.nextFloat() < 0.5f) {
            // Book
            centerObject = Blocks.LECTERN.defaultBlockState();
        } else if (RANDOM.nextFloat() < 0.7f) {
            // Skull
            centerObject = Blocks.WITHER_SKELETON_SKULL.defaultBlockState();
        } else {
            // Fire
            centerObject = Blocks.SOUL_FIRE.defaultBlockState();
        }
        level.setBlock(pos.above(), centerObject, 3);
        
        // Add some redstone dust in specific patterns
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos redstonePos = pos.relative(dir);
            if (level.getBlockState(redstonePos).isAir()) {
                level.setBlock(redstonePos, Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
            }
        }
    }
    
    // Structure 31: Large ominous cross (updated to match image)
    private static void spawnCross(ServerLevel level, BlockPos pos) {
        // Material for the cross
        BlockState crossMaterial = Blocks.STONE_BRICKS.defaultBlockState();
        
        // Create ground platform for the cross
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(pos.offset(x, -1, z), crossMaterial, 3);
            }
        }
        
        // Create a vertical pillar (3 blocks tall)
        for (int y = 0; y < 3; y++) {
            level.setBlock(pos.above(y), crossMaterial, 3);
        }
        
        // Create horizontal crossbars (1 block to each side)
        level.setBlock(pos.above(1).east(), crossMaterial, 3);
        level.setBlock(pos.above(1).west(), crossMaterial, 3);
        
        // Add subtle decoration around it
        if (RANDOM.nextFloat() < 0.3f) {
            // Sometimes add soul soil patch beneath
            level.setBlock(pos.below(), Blocks.SOUL_SOIL.defaultBlockState(), 3);
            
            // Scattered subtle redstone dust (like blood trails)
            for (int i = 0; i < 3; i++) {
                int offsetX = RANDOM.nextInt(5) - 2;
                int offsetZ = RANDOM.nextInt(5) - 2;
                BlockPos redstonePos = pos.offset(offsetX, 0, offsetZ);
                
                if (level.getBlockState(redstonePos).isAir() && 
                    level.getBlockState(redstonePos.below()).isFaceSturdy(level, redstonePos.below(), Direction.UP)) {
                    level.setBlock(redstonePos, Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
                }
            }
        }
        
        // Sometimes add flowers near base
        for (int i = 0; i < 2 + RANDOM.nextInt(3); i++) {
            int offsetX = RANDOM.nextInt(3) - 1;
            int offsetZ = RANDOM.nextInt(3) - 1;
            BlockPos flowerPos = pos.offset(offsetX, 0, offsetZ);
            
            if (level.getBlockState(flowerPos).isAir() && 
                level.getBlockState(flowerPos.below()).isFaceSturdy(level, flowerPos.below(), Direction.UP)) {
                
                if (RANDOM.nextFloat() < 0.7f) {
                    level.setBlock(flowerPos, Blocks.POPPY.defaultBlockState(), 3);
                } else {
                    level.setBlock(flowerPos, Blocks.WITHER_ROSE.defaultBlockState(), 3);
                }
            }
        }
    }
    
    // Structure 32: Player grave with signs
    private static void spawnPlayerGrave(ServerLevel level, BlockPos pos) {
        // Create grave mound
        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(pos.east(), Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(pos.west(), Blocks.DIRT.defaultBlockState(), 3);
        
        // Add path blocks on top
        level.setBlock(pos.above(), Blocks.DIRT_PATH.defaultBlockState(), 3);
        level.setBlock(pos.east().above(), Blocks.DIRT_PATH.defaultBlockState(), 3);
        level.setBlock(pos.west().above(), Blocks.DIRT_PATH.defaultBlockState(), 3);
        
        // Create headstone using a sign
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
        BlockPos signPos = pos.relative(dir).above();
        
        BlockState signState = Blocks.OAK_SIGN.defaultBlockState();
        level.setBlock(signPos, signState, 3);
        
        // Get server player names if available
        String playerName = "PLAYER";
        if (level.getServer() != null && !level.getServer().getPlayerList().getPlayers().isEmpty()) {
            int playerCount = level.getServer().getPlayerList().getPlayers().size();
            if (playerCount > 0) {
                int playerIndex = RANDOM.nextInt(playerCount);
                ServerPlayer randomPlayer = level.getServer().getPlayerList().getPlayers().get(playerIndex);
                playerName = randomPlayer.getName().getString();
            }
        }
        
        // Set sign text
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            // Create sign text using NBT
            CompoundTag signData = new CompoundTag();
            
            // Create text for each line
            String[] graveLines = {
                "RIP",
                playerName,
                "§8They saw too much",
                "§o" + (2000 + RANDOM.nextInt(24))
            };
            
            // Set front text - in 1.20.1 we need to use proper NBT structure
            CompoundTag frontText = new CompoundTag();
            ListTag messages = new ListTag();
            
            // Add each message as string NBT
            for (String line : graveLines) {
                messages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
            }
            
            // Add messages to front text
            frontText.put("messages", messages);
            frontText.putBoolean("has_glowing_text", false);
            frontText.putString("color", "black");
            
            // Set front text in sign data
            signData.put("front_text", frontText);
            
            // Apply the NBT data to the sign
            sign.load(signData);
            sign.setChanged();
        }
        
        // Add flowers
        for (int i = 0; i < 2 + RANDOM.nextInt(3); i++) {
            int offsetX = RANDOM.nextInt(3) - 1;
            int offsetZ = RANDOM.nextInt(3) - 1;
            
            BlockPos flowerPos = pos.offset(offsetX, 1, offsetZ);
            
            if (level.getBlockState(flowerPos).isAir() && 
                level.getBlockState(flowerPos.below()).isFaceSturdy(level, flowerPos.below(), Direction.UP)) {
                
                if (RANDOM.nextFloat() < 0.7f) {
                    level.setBlock(flowerPos, Blocks.POPPY.defaultBlockState(), 3);
                } else {
                    level.setBlock(flowerPos, Blocks.WITHER_ROSE.defaultBlockState(), 3);
                }
            }
        }
    }
    
    // Structure 33: Strange Pillar (replaced AbandonedPlayerCamp)
    private static void spawnStrangePillar(ServerLevel level, BlockPos pos) {
        // Materials to use
        BlockState[] materials = {
            Blocks.DEEPSLATE.defaultBlockState(),
            Blocks.COBBLESTONE.defaultBlockState(),
            Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
            Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
        };
        
        // Height of the pillar (5-8 blocks)
        int height = 5 + RANDOM.nextInt(4);
        
        // Create main pillar with mixed materials
        for (int y = 0; y < height; y++) {
            BlockState material = materials[RANDOM.nextInt(materials.length)];
            level.setBlock(pos.above(y), material, 3);
        }
        
        // Sometimes add strange decorations at the top
        if (RANDOM.nextFloat() < 0.5f) {
            // Option 1: Skull
            if (RANDOM.nextFloat() < 0.6f) {
                Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
                level.setBlock(pos.above(height), 
                    Blocks.SKELETON_SKULL.defaultBlockState(), 3);
            }
            // Option 2: Soul Fire
            else {
                level.setBlock(pos.above(height - 1), Blocks.SOUL_SOIL.defaultBlockState(), 3);
                level.setBlock(pos.above(height), Blocks.SOUL_FIRE.defaultBlockState(), 3);
            }
        }
        
        // Add a couple of random blocks at the base
        for (int i = 0; i < 3; i++) {
            int offsetX = RANDOM.nextInt(3) - 1;
            int offsetZ = RANDOM.nextInt(3) - 1;
            
            // Don't place directly at the pillar position
            if (offsetX != 0 || offsetZ != 0) {
                BlockPos blockPos = pos.offset(offsetX, 0, offsetZ);
                
                if (level.getBlockState(blockPos).isAir() && 
                    level.getBlockState(blockPos.below()).isFaceSturdy(level, blockPos.below(), Direction.UP)) {
                    
                    BlockState material = materials[RANDOM.nextInt(materials.length)];
                    level.setBlock(blockPos, material, 3);
                    
                    // Sometimes add a fence on top
                    if (RANDOM.nextFloat() < 0.3f) {
                        level.setBlock(blockPos.above(), Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
    
    // Structure 34: Nametag that shouldn't be there
    private static void spawnNametag(ServerLevel level, BlockPos pos) {
        try {
            // Make an invisible armor stand with a custom name
            net.minecraft.world.entity.decoration.ArmorStand armorStand = 
                new net.minecraft.world.entity.decoration.ArmorStand(level, 
                    pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            
            String[] creepyNames = {
                "Herobrine",
                "NULL",
                "Entity_303",
                "WATCHING",
                "404",
                "[REDACTED]",
                "DELETED",
                "YOU",
                "SYSTEM",
                "ADMIN"
            };
            
            // Set a random creepy name
            armorStand.setCustomName(Component.literal(creepyNames[RANDOM.nextInt(creepyNames.length)]));
            armorStand.setCustomNameVisible(true);
            
            // Make it invisible but keep the name visible
            armorStand.setInvisible(true);
            
            // Add to world
            level.addFreshEntity(armorStand);
        } catch (Exception e) {
            // If we can't spawn an armor stand, spawn a sign instead
            level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState(), 3);
            // Skip sign text to avoid compatibility issues
        }
    }
    
    // Structure 35: Rusty Chain (replaced EnscasedVillager)
    private static void spawnRustyChain(ServerLevel level, BlockPos pos) {
        // Height of the chain (5-9 blocks)
        int height = 5 + RANDOM.nextInt(5);
        
        // Create a vertical chain hanging from... nothing
        for (int y = 0; y < height; y++) {
            level.setBlock(pos.above(y), Blocks.CHAIN.defaultBlockState(), 3);
        }
        
        // Sometimes add something at the bottom of the chain
        if (RANDOM.nextFloat() < 0.4f) {
            if (RANDOM.nextFloat() < 0.5f) {
                // Option 1: Soul lantern
                level.setBlock(pos.above(height), 
                    Blocks.SOUL_LANTERN.defaultBlockState()
                    .setValue(BlockStateProperties.HANGING, true), 3);
            } else {
                // Option 2: Bell
                level.setBlock(pos.above(height), 
                    Blocks.BELL.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.from2DDataValue(RANDOM.nextInt(4))), 3);
            }
        }
        
        // Add some cobwebs around it
        for (int i = 0; i < 3; i++) {
            int y = RANDOM.nextInt(height);
            int offsetX = RANDOM.nextInt(3) - 1;
            int offsetZ = RANDOM.nextInt(3) - 1;
            
            BlockPos webPos = pos.offset(offsetX, y, offsetZ);
            if (level.getBlockState(webPos).isAir()) {
                level.setBlock(webPos, Blocks.COBWEB.defaultBlockState(), 3);
            }
        }
    }
    
    // Structure 36: Door to nowhere
    private static void spawnDoorToNowhere(ServerLevel level, BlockPos pos) {
        // Create a doorframe in the middle of nowhere
        
        // Build frame
        for (int y = 0; y < 4; y++) {
            // Left pillar
            level.setBlock(pos.offset(-1, y, 0), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            
            // Right pillar
            level.setBlock(pos.offset(1, y, 0), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            
            // Top piece (only at top)
            if (y == 3) {
                level.setBlock(pos.offset(0, y, 0), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
        }
        
        // Place the door in the frame
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
        level.setBlock(pos, Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.DOOR_HINGE, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), 3);
        
        level.setBlock(pos.above(), Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.DOOR_HINGE, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),
                3);
        
        // Sometimes add a sign above the door
        if (RANDOM.nextFloat() < 0.4f) {
            // Add sign above the door
            BlockPos signPos = pos.above(2);
            
            level.setBlock(signPos, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing), 3);
            
            // Set sign text
            if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
                // Create sign text using NBT
                CompoundTag signData = new CompoundTag();
                
                // Choose one of several creepy door signs
                String[][] doorSigns = {
                    {"DO NOT", "ENTER", "", "§8§o- Management"},
                    {"KEEP OUT", "", "§4NO EXCEPTIONS", ""},
                    {"EXIT", "ONLY", "", "§8§o<--"},
                    {"§4DANGER", "§0BEYOND", "§0THIS", "§0POINT"}
                };
                
                String[] chosenSign = doorSigns[RANDOM.nextInt(doorSigns.length)];
                
                // Set front text - in 1.20.1 we need to use proper NBT structure
                CompoundTag frontText = new CompoundTag();
                ListTag messages = new ListTag();
                
                // Add each message as string NBT
                for (String line : chosenSign) {
                    messages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
                }
                
                // Add messages to front text
                frontText.put("messages", messages);
                frontText.putBoolean("has_glowing_text", RANDOM.nextFloat() < 0.5f);
                frontText.putString("color", "red");
                
                // Set front text in sign data
                signData.put("front_text", frontText);
                
                // Apply the NBT data to the sign
                sign.load(signData);
                sign.setChanged();
            }
        }
    }
    
    // Structure 37: Cryptographic Symbols (replaced FakePlayerHouse) 
    private static void spawnCryptographicSymbols(ServerLevel level, BlockPos pos) {
        // Clear area first
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 3; y++) {
                    BlockPos clearPos = pos.offset(x, y, z);
                    level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        
        // Create a base platform
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(pos.offset(x, -1, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
        }
        
        // Generate a strange pattern using redstone and stone buttons
        // This represents "cryptographic" symbols often found in ARGs
        
        // Create central obsidian block
        level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
        
        // Add redstone "symbols" in a pattern
        BlockPos[] patternPositions = {
            pos.north(), pos.south(), pos.east(), pos.west(),
            pos.north().east(), pos.north().west(),
            pos.south().east(), pos.south().west()
        };
        
        // Select random pattern points
        for (int i = 0; i < 5 + RANDOM.nextInt(3); i++) {
            BlockPos patternPos = patternPositions[RANDOM.nextInt(patternPositions.length)];
            
            if (level.getBlockState(patternPos).isAir()) {
                if (RANDOM.nextFloat() < 0.7f) {
                    // Redstone dust
                    level.setBlock(patternPos, Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
                } else {
                    // Stone button
                    Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
                    level.setBlock(patternPos, 
                        Blocks.STONE_BUTTON.defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                        .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR), 3);
                }
            }
        }
        
        // Add a sign with cryptic text
        BlockPos signPos = pos.north(2);
        Direction facing = Direction.SOUTH;
        level.setBlock(signPos, Blocks.DARK_OAK_SIGN.defaultBlockState(), 3);
        
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            // Create sign text using NBT
            CompoundTag signData = new CompoundTag();
            
            // Create cryptic text
            String[] crypticLines = {
                "§k11010",
                "§kX4553",
                "§kDE114",
                "§k9211"
            };
            
            // Set front text - in 1.20.1 we need to use proper NBT structure
            CompoundTag frontText = new CompoundTag();
            ListTag messages = new ListTag();
            
            // Add each message as string NBT
            for (String line : crypticLines) {
                messages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
            }
            
            // Add messages to front text
            frontText.put("messages", messages);
            frontText.putBoolean("has_glowing_text", false);
            frontText.putString("color", "white");
            
            // Set front text in sign data
            signData.put("front_text", frontText);
            
            // Apply the NBT data to the sign
            sign.load(signData);
            sign.setChanged();
        }
    }
    
    // New structure: Creepy Abandoned House (ID 40) - replaced starter house
    private static void spawnStarterHouse(ServerLevel level, BlockPos groundPos) {
        // House dimensions
        final int width = 5;  // Interior width
        final int length = 7; // Interior length 
        final int height = 4; // Total height including roof
        
        // STEP 1: Clear the area
        for (int x = -1; x <= width + 1; x++) {
            for (int z = -1; z <= length + 1; z++) {
                for (int y = 0; y <= height + 1; y++) {
                    level.setBlock(groundPos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        
        // STEP 2: Create foundation with mossy cobblestone to look old and decayed
        for (int x = -1; x <= width + 1; x++) {
            for (int z = -1; z <= length + 1; z++) {
                level.setBlock(groundPos.offset(x, -1, z), Blocks.DIRT.defaultBlockState(), 3);
                
                // Floor of house - mix of cracked stone bricks and mossy variants
                if (x >= 0 && x <= width && z >= 0 && z <= length) {
                    if (RANDOM.nextFloat() < 0.3f) {
                        level.setBlock(groundPos.offset(x, 0, z), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 3);
                    } else if (RANDOM.nextFloat() < 0.5f) {
                        level.setBlock(groundPos.offset(x, 0, z), Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3);
                    } else {
                        level.setBlock(groundPos.offset(x, 0, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    }
                    
                    // Occasionally add podzol or mycelium patches for decay effect
                    if (RANDOM.nextFloat() < 0.1f) {
                        level.setBlock(groundPos.offset(x, 0, z), Blocks.MYCELIUM.defaultBlockState(), 3);
                    }
                }
            }
        }
        
        // STEP 3: Build decaying walls
        net.minecraft.world.level.block.Block[] wallMaterials = {
            Blocks.DARK_OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.MOSSY_COBBLESTONE
        };
        
        for (int y = 1; y <= height - 1; y++) {
            // Build the perimeter walls
            for (int x = 0; x <= width; x++) {
                for (int z = 0; z <= length; z++) {
                    // Only build walls around perimeter
                    if (x == 0 || x == width || z == 0 || z == length) {
                        // Create holes in the walls for a decayed look
                        if (!(y == 1 && z == length/2 && x == 0) && // Door position
                            RANDOM.nextFloat() > 0.15f) { // 15% chance of a hole
                            
                            // Use different materials for variety
                            net.minecraft.world.level.block.Block material = wallMaterials[RANDOM.nextInt(wallMaterials.length)];
                            level.setBlock(groundPos.offset(x, y, z), material.defaultBlockState(), 3);
                            
                            // Add vines for overgrown effect
                            if (RANDOM.nextFloat() < 0.2f) {
                                Direction vineDir = Direction.from2DDataValue(RANDOM.nextInt(4));
                                BlockPos vinePos = groundPos.offset(x, y, z).relative(vineDir.getOpposite());
                                if (level.isEmptyBlock(vinePos)) {
                                    level.setBlock(vinePos, Blocks.VINE.defaultBlockState(), 3);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // STEP 4: Add broken door
        int doorX = 0;
        int doorZ = length/2;
        
        // Just the bottom half of the door, hanging off its hinges
        level.setBlock(groundPos.offset(doorX, 1, doorZ), Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.DOOR_HINGE, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), 3);
        
        // STEP 5: Add broken windows (iron bars)
        // Add a few broken windows (iron bars)
        for (int i = 0; i < 3; i++) {
            int x = RANDOM.nextBoolean() ? 0 : width;
            int z = 1 + RANDOM.nextInt(length - 1);
            level.setBlock(groundPos.offset(x, 2, z), Blocks.IRON_BARS.defaultBlockState(), 3);
        }
        
        // STEP 6: Build collapsed/damaged roof
        for (int z = 0; z <= length; z++) {
            for (int x = 0; x <= width; x++) {
                // Create holes in the roof
                if (RANDOM.nextFloat() > 0.3f) { // 30% chance of a hole
                    level.setBlock(groundPos.offset(x, height-1, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 3);
                }
            }
        }
        
        // STEP 7: Add interior creepy items
        
        // Cobwebs in corners and ceiling
        for (int i = 0; i < 12; i++) {
            int x = RANDOM.nextInt(width);
            int z = RANDOM.nextInt(length);
            int y = 1 + RANDOM.nextInt(height - 1);
            
            // More cobwebs near ceiling and corners
            if (y >= height-2 || x <= 1 || x >= width-1 || z <= 1 || z >= length-1) {
                if (level.isEmptyBlock(groundPos.offset(x, y, z))) {
                    level.setBlock(groundPos.offset(x, y, z), Blocks.COBWEB.defaultBlockState(), 3);
                }
            }
        }
        
        // Broken/damaged crafting table
        level.setBlock(groundPos.offset(1, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        
        // Furnace with soul fire inside
        level.setBlock(groundPos.offset(1, 1, 2), Blocks.FURNACE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 3);
        
        // Chest with creepy items
        Direction chestFacing = Direction.WEST;
        level.setBlock(groundPos.offset(width-1, 1, 1), Blocks.CHEST.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, chestFacing), 3);
        
        if (level.getBlockEntity(groundPos.offset(width-1, 1, 1)) instanceof ChestBlockEntity chest) {
            // Add ominous items
            chest.setItem(0, new ItemStack(Items.BONE, 6 + RANDOM.nextInt(10)));
            chest.setItem(1, new ItemStack(Items.ROTTEN_FLESH, 3 + RANDOM.nextInt(5)));
            chest.setItem(2, new ItemStack(Items.SPIDER_EYE, 1 + RANDOM.nextInt(3)));
            chest.setItem(3, new ItemStack(Items.SUSPICIOUS_STEW));
            
            // Add damaged tools
            ItemStack rustyPick = new ItemStack(Items.IRON_PICKAXE);
            rustyPick.setDamageValue(rustyPick.getMaxDamage() - 5); // Almost broken
            chest.setItem(9, rustyPick);
            
            // Add mysterious book
            ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
            CompoundTag bookTag = book.getOrCreateTag();
            bookTag.putString("title", "DO NOT READ");
            bookTag.putString("author", "Unknown");
            ListTag pages = new ListTag();
            pages.add(StringTag.valueOf(Component.Serializer.toJson(
                    Component.literal("It's watching you through the windows. Don't turn around."))));
            pages.add(StringTag.valueOf(Component.Serializer.toJson(
                    Component.literal("They're coming back tonight. Hide."))));
            bookTag.put("pages", pages);
            chest.setItem(13, book);
            
            // Add withered items
            chest.setItem(14, new ItemStack(Items.WITHER_ROSE, 1));
        }
        
        // Damaged/stained bed
        if (RANDOM.nextBoolean()) {
            Direction bedFacing = Direction.NORTH;
            level.setBlock(groundPos.offset(width-1, 1, length-1), Blocks.RED_BED.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, bedFacing)
                    .setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT), 3);
            
            level.setBlock(groundPos.offset(width-1, 1, length-2), Blocks.RED_BED.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, bedFacing)
                    .setValue(BlockStateProperties.BED_PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD), 3);
            
            // Add redstone dust next to the bed (like blood stains)
            if (RANDOM.nextBoolean()) {
                level.setBlock(groundPos.offset(width-2, 1, length-1), Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
                level.setBlock(groundPos.offset(width-2, 1, length-2), Blocks.REDSTONE_WIRE.defaultBlockState(), 3);
            }
        } else {
            // Or just a carved pumpkin where the bed would be
            level.setBlock(groundPos.offset(width-1, 1, length-1), 
                    Blocks.CARVED_PUMPKIN.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.from2DDataValue(RANDOM.nextInt(4))), 3);
        }
        
        // Add soul lanterns for creepy lighting
        level.setBlock(groundPos.offset(1, 3, 1), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        level.setBlock(groundPos.offset(width-1, height-2, 1), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        
        // Add a small altar in the center
        level.setBlock(groundPos.offset(width/2, 1, length/2), Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);
        level.setBlock(groundPos.offset(width/2, 2, length/2), Blocks.SKELETON_SKULL.defaultBlockState(), 3);
        
        // Add a small hidden cross
        if (RANDOM.nextFloat() < 0.5f) {
            BlockPos crossPos = groundPos.offset(width-1, 2, length-1);
            // Main vertical part
            level.setBlock(crossPos, Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(crossPos.above(), Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);
            
            // Horizontal crossbar
            Direction dir = Direction.from2DDataValue(RANDOM.nextInt(4));
            level.setBlock(crossPos.above().relative(dir), Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(crossPos.above().relative(dir.getOpposite()), Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);
        }
        
        // Add signs with creepy messages
        if (RANDOM.nextFloat() < 0.7f) {
            BlockPos signPos = groundPos.offset(width/2, 1, 1);
            level.setBlock(signPos, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH), 3);
            
            if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
                CompoundTag signData = new CompoundTag();
                String[] signLines = {
                    "GET OUT",
                    "WHILE YOU",
                    "STILL CAN",
                    ""
                };
                
                // Set front text using NBT structure
                CompoundTag frontText = new CompoundTag();
                ListTag messages = new ListTag();
                
                // Add each message as string NBT
                for (String line : signLines) {
                    messages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
                }
                
                // Add messages to front text
                frontText.put("messages", messages);
                frontText.putBoolean("has_glowing_text", true);
                frontText.putString("color", "dark_red");
                
                // Set front text in sign data
                signData.put("front_text", frontText);
                
                // Apply the NBT data to the sign
                sign.load(signData);
                sign.setChanged();
            }
        }
        
        // Add a nether portal frame (without portal blocks) for extra creepiness
        if (RANDOM.nextFloat() < 0.3f) {
            for (int y = 1; y <= 3; y++) {
                level.setBlock(groundPos.offset(1, y, length-1), Blocks.OBSIDIAN.defaultBlockState(), 3);
                level.setBlock(groundPos.offset(3, y, length-1), Blocks.OBSIDIAN.defaultBlockState(), 3);
            }
            level.setBlock(groundPos.offset(2, 1, length-1), Blocks.OBSIDIAN.defaultBlockState(), 3);
            level.setBlock(groundPos.offset(2, 3, length-1), Blocks.OBSIDIAN.defaultBlockState(), 3);
        }
    }
    
    // Structure 38: Reality-breaking structure
    private static void spawnRealismBreak(ServerLevel level, BlockPos pos) {
        // We'll make blocks float higher in the air (6-12 blocks up)
        int baseHeight = 6 + RANDOM.nextInt(7);
        
        // Get common falling blocks
        net.minecraft.world.level.block.Block[] fallingBlocks = {
            Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, 
            Blocks.DIRT, Blocks.NETHERRACK, Blocks.STONE
        };
        
        // Place 3-7 floating blocks in a small area
        int count = 3 + RANDOM.nextInt(5);
        for (int i = 0; i < count; i++) {
            // Choose a random spot within 3 blocks of the center
            int xOffset = RANDOM.nextInt(7) - 3;
            int zOffset = RANDOM.nextInt(7) - 3;
            
            // Vary the height by a few blocks to make it look more chaotic
            int yOffset = baseHeight + RANDOM.nextInt(5) - 2;
            
            // Make sure we're not too close to the ground
            if (yOffset < 5) yOffset = 5;
            
            // Get position
            BlockPos floatPos = pos.offset(xOffset, yOffset, zOffset);
            
            // Place tripwire below (nearly invisible support)
            level.setBlock(floatPos.below(), Blocks.TRIPWIRE.defaultBlockState(), 3);
            
            // Choose a random block type
            net.minecraft.world.level.block.Block blockType = fallingBlocks[RANDOM.nextInt(fallingBlocks.length)];
            
            // Place the floating block
            level.setBlock(floatPos, blockType.defaultBlockState(), 3);
        }
    }
    
    // Structure 39: Hidden message using format codes
    private static void spawnHiddenMessage(ServerLevel level, BlockPos pos) {
        // Create a sign with an ominous message
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState(), 3);
        
        // Set sign text
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            // Create sign text using NBT
            CompoundTag signData = new CompoundTag();
            
            // Create text for each line with cryptic messages
            String[][] possibleMessages = {
                {"§4HELP ME", "§4I'M TRAPPED", "§4IN THE", "§4GAME"},
                {"§kXXXXX", "§kXXXXX", "§kXXXXX", "§kXXXXX"},
                {"§4LEAVE", "§4WHILE", "§4YOU", "§4CAN"},
                {"§4I SEE", "§4YOU", "§4PLAYING", "§4RIGHT NOW"}
            };
            
            String[] chosenMessage = possibleMessages[RANDOM.nextInt(possibleMessages.length)];
            
            // Set front text - in 1.20.1 we need to use proper NBT structure
            CompoundTag frontText = new CompoundTag();
            ListTag messages = new ListTag();
            
            // Add each message as string NBT
            for (String line : chosenMessage) {
                messages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
            }
            
            // Add messages to front text
            frontText.put("messages", messages);
            frontText.putBoolean("has_glowing_text", true);
            frontText.putString("color", "red");
            
            // Set front text in sign data
            signData.put("front_text", frontText);
            
            // Apply the NBT data to the sign
            sign.load(signData);
            sign.setChanged();
        }
        
        // Add a chest with "forbidden items"
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
        BlockPos chestPos = pos.relative(dir);
        
        if (level.getBlockState(chestPos).isAir() && 
            level.getBlockState(chestPos.below()).isFaceSturdy(level, chestPos.below(), Direction.UP)) {
            
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
            
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                // Add some "impossible" or renamed items
                
                // Create a renamed item that feels creepy
                ItemStack renamedItem = new ItemStack(Items.PAPER);
                renamedItem.setHoverName(Component.literal("§4SYSTEM LOGS"));
                
                chest.setItem(13, renamedItem); // Center slot
                
                // Add a few more creepy items
                ItemStack ancientDebris = new ItemStack(Items.ANCIENT_DEBRIS, 1);
                chest.setItem(0, ancientDebris); // Should not appear in overworld
                
                // Add dragon egg (extremely rare, shouldn't be in multiple places)
                if (RANDOM.nextFloat() < 0.1f) {
                    ItemStack dragonEgg = new ItemStack(Items.DRAGON_EGG);
                    chest.setItem(26, dragonEgg);
                }
            }
        }
    }
    
    /**
     * Check if we can spawn more structures in a chunk
     */
    private static boolean canSpawnStructureInChunk(ServerLevel level, long chunkKey) {
        StructureData data = structuresPerChunk.get(chunkKey);
        if (data == null) return true;
        
        // If we've reached the max structures per chunk, no more spawning
        return data.count < MAX_STRUCTURES_PER_CHUNK;
    }
    
    /**
     * Record that a structure was spawned in this chunk
     */
    private static void recordStructureSpawn(ServerLevel level, long chunkKey) {
        StructureData data = structuresPerChunk.get(chunkKey);
        if (data == null) {
            data = new StructureData(1, level.getGameTime());
        } else {
            data.count++;
            data.lastSpawnTime = level.getGameTime();
        }
        structuresPerChunk.put(chunkKey, data);
    }
    
    /**
     * Choose a structure type based on rarity
     */
    private static int chooseStructureByRarity(StructureRarity rarity) {
        Random random = new Random();
        
        // Assign structure types based on rarity
        if (rarity == StructureRarity.SUBTLE) {
            // Subtle structures (0-15)
            return random.nextInt(16);
        } else if (rarity == StructureRarity.UNUSUAL) {
            // Unusual structures (16-25)
            return 16 + random.nextInt(10); 
        } else if (rarity == StructureRarity.UNSETTLING) {
            // Unsettling structures (26-35)
            return 26 + random.nextInt(10);
        } else {
            // Obvious structures (36+)
            return 36 + random.nextInt(5);
        }
    }
} 