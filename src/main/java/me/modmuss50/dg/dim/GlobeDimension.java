package me.modmuss50.dg.dim;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Lifecycle;
import me.modmuss50.dg.DimensionGlobeMod;
import net.minecraft.block.Blocks;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.registry.*;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.HorizontalVoronoiBiomeAccessType;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.chunk.*;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;

public class GlobeDimension extends DimensionType {

    public static ChunkGenerator createVoidGenerator(DynamicRegistryManager rm) {
        MutableRegistry<Biome> biomeRegistry = rm.get(Registry.BIOME_KEY);

        StructuresConfig structuresConfig = new StructuresConfig(
            Optional.of(StructuresConfig.DEFAULT_STRONGHOLD),
            Maps.newHashMap(ImmutableMap.of(
//                StructureFeature.VILLAGE, StructuresConfig.DEFAULT_STRUCTURES.get(StructureFeature.VILLAGE)
            ))
        );
        FlatChunkGeneratorConfig flatChunkGeneratorConfig = new FlatChunkGeneratorConfig(structuresConfig,
            biomeRegistry);
        flatChunkGeneratorConfig.getLayers().add(new FlatChunkGeneratorLayer(1, Blocks.BEDROCK));
        flatChunkGeneratorConfig.updateLayerBlocks();

        return new FlatChunkGenerator(flatChunkGeneratorConfig);
    }

    public static void addDimension(
        long argSeed,
        SimpleRegistry<DimensionOptions> registry,
        RegistryKey<DimensionOptions> key,
        RegistryKey<World> worldKey,
        Supplier<DimensionType> dimensionTypeSupplier,
        ChunkGenerator chunkGenerator
    ) {
        if (!registry.getIds().contains(key.getValue())) {
            registry.add(
                key,
                new DimensionOptions(
                    dimensionTypeSupplier,
                    chunkGenerator
                ),
                Lifecycle.experimental()
            );
        }
    }

    public static void addAlternateDimensions(SimpleRegistry<DimensionOptions> registry,
                                              DynamicRegistryManager rm,
                                              long seed) {
        addDimension(
            seed,
            registry,
            DimensionGlobeMod.alternate5Option,
            DimensionGlobeMod.dimensionRegistryKey,
            () -> DimensionGlobeMod.surfaceTypeObject,
            createVoidGenerator(rm)
        );
    }

    public GlobeDimension() {
        super(OptionalLong.empty(), true, false, false, true, 1.0D, false, false, true, false, true, 256, HorizontalVoronoiBiomeAccessType.INSTANCE, BlockTags.INFINIBURN_OVERWORLD.getId(), OVERWORLD_ID, 0.0F);

    }


}
