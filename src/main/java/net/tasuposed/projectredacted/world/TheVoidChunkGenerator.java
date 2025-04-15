package net.tasuposed.projectredacted.world;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * A custom chunk generator for "The Void" dimension - a mysterious corrupted realm
 * where the horror entities originate from
 */
public class TheVoidChunkGenerator extends ChunkGenerator {
    
    // Codec for serialization
    public static final Codec<TheVoidChunkGenerator> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
            ).apply(instance, TheVoidChunkGenerator::new)
    );
    
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
    private static final BlockState CRYING_OBSIDIAN = Blocks.CRYING_OBSIDIAN.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState SOUL_SOIL = Blocks.SOUL_SOIL.defaultBlockState();
    private static final BlockState SOUL_SAND = Blocks.SOUL_SAND.defaultBlockState();
    
    public TheVoidChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }
    
    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState, BiomeManager biomeManager, 
            StructureManager structureManager, ChunkAccess chunk, Carving step) {
        // No carvers in The Void
    }
    
    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        // Create the surface of The Void - scattered platforms with occasional structures
        RandomSource random = RandomSource.create(level.getSeed() + chunk.getPos().x * 13L + chunk.getPos().z * 7L);
        
        // Only generate platforms occasionally
        if (random.nextFloat() < 0.1f) {
            int centerX = chunk.getPos().getMinBlockX() + random.nextInt(16);
            int centerZ = chunk.getPos().getMinBlockZ() + random.nextInt(16);
            int y = 40 + random.nextInt(30); // Variable platform height
            
            // Generate a small platform
            generatePlatform(chunk, random, centerX, y, centerZ);
            
            // Rare chance to generate a lore fragment
            if (random.nextFloat() < 0.1f) {
                // Mark position for structure generation in populate phase
                chunk.setBlockState(new BlockPos(centerX, y + 1, centerZ), Blocks.STRUCTURE_BLOCK.defaultBlockState(), false);
            }
        }
    }
    
    private void generatePlatform(ChunkAccess chunk, RandomSource random, int centerX, int y, int centerZ) {
        int radius = 3 + random.nextInt(5); // Random platform size
        
        // Generate the platform
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x*x + z*z <= radius*radius) {
                    BlockPos pos = new BlockPos(centerX + x, y, centerZ + z);
                    
                    // Calculate depth - deeper blocks are more stable/solid
                    for (int depth = 0; depth < 3; depth++) {
                        BlockPos depthPos = pos.below(depth);
                        
                        if (isInChunk(chunk, depthPos)) {
                            BlockState platformState = getPlatformBlock(random, depth);
                            chunk.setBlockState(depthPos, platformState, false);
                        }
                    }
                    
                    // Rarely add a decorative element
                    if (random.nextFloat() < 0.05f && isInChunk(chunk, pos.above())) {
                        BlockState decoration = getDecorationBlock(random);
                        chunk.setBlockState(pos.above(), decoration, false);
                    }
                }
            }
        }
    }
    
    private BlockState getPlatformBlock(RandomSource random, int depth) {
        float val = random.nextFloat();
        
        if (depth == 0) {
            // Surface layer
            if (val < 0.7f) return DEEPSLATE;
            else if (val < 0.9f) return OBSIDIAN;
            else return CRYING_OBSIDIAN;
        } else if (depth == 1) {
            // Middle layer
            if (val < 0.6f) return DEEPSLATE;
            else return OBSIDIAN;
        } else {
            // Bottom layer
            return BEDROCK;
        }
    }
    
    private BlockState getDecorationBlock(RandomSource random) {
        float val = random.nextFloat();
        if (val < 0.3f) return Blocks.SOUL_FIRE.defaultBlockState();
        else if (val < 0.6f) return Blocks.SCULK.defaultBlockState();
        else if (val < 0.85f) return Blocks.CHAIN.defaultBlockState();
        else return Blocks.BONE_BLOCK.defaultBlockState();
    }
    
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, 
            StructureManager structureManager, ChunkAccess chunk) {
        // Mostly empty world, handled by buildSurface
        return CompletableFuture.completedFuture(chunk);
    }
    
    @Override
    public int getBaseHeight(int x, int z, Types heightmapType, LevelHeightAccessor level, RandomState randomState) {
        return level.getMinBuildHeight();
    }
    
    public net.minecraft.world.level.biome.Climate.Sampler climateSampler() {
        return Climate.empty();
    }
    
    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        // Mostly void with occasional platforms
        return new NoiseColumn(heightAccessor.getMinBuildHeight(), new BlockState[0]);
    }
    
    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // No original mobs in The Void
    }
    
    @Override
    public int getGenDepth() {
        return 384; // Same as overworld
    }

    @Override
    public int getSeaLevel() {
        return -64; // No real sea in The Void
    }
    
    @Override
    public int getMinY() {
        return -64; // Same as overworld
    }
    
    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }
    
    @Override
    public CompletableFuture<ChunkAccess> createBiomes(Executor executor, RandomState randomState, Blender blender, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            chunk.fillBiomesFromNoise(this.getBiomeSource(), Climate.empty());
            return chunk;
        }, executor);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        // No debug info for The Void
    }

    // Add a helper method to check if a position is within the chunk
    private boolean isInChunk(ChunkAccess chunk, BlockPos pos) {
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        int blockX = pos.getX() >> 4;  // Divide by 16 to get chunk coordinate
        int blockZ = pos.getZ() >> 4;
        
        return blockX == chunkX && blockZ == chunkZ;
    }
} 