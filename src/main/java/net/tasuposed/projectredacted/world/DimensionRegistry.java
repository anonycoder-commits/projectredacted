package net.tasuposed.projectredacted.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.tasuposed.projectredacted.ProjectRedacted;

/**
 * Registry for custom dimensions
 */
public class DimensionRegistry {
    // The Void dimension - a corrupted realm where the horror entities originate from
    public static final ResourceKey<Level> THE_VOID = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(ProjectRedacted.MODID, "the_void"));
    
    // The Void dimension type
    public static final ResourceKey<DimensionType> THE_VOID_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            new ResourceLocation(ProjectRedacted.MODID, "the_void_type"));
} 