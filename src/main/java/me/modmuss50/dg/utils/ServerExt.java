package me.modmuss50.dg.utils;

import me.modmuss50.dg.dim.GlobeDimension;
import me.modmuss50.dg.mixin.MixinMinecraftServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;

import java.util.Map;

public interface ServerExt {

    static void addDim(ServerExt mixinMinecraftServer, SaveProperties saveProperties, DynamicRegistryManager.Impl registryManager) {
        System.out.println("Adding Globe Dim");
        GeneratorOptions generatorOptions = saveProperties.getGeneratorOptions();
        SimpleRegistry<DimensionOptions> simpleRegistry = generatorOptions.getDimensions();

        DynamicRegistryManager rm = registryManager;
        long seed = saveProperties.getGeneratorOptions().getSeed();


        GlobeDimension.addAlternateDimensions(simpleRegistry, rm, seed);
    }
    abstract Map<RegistryKey<World>, ServerWorld> getWorldsMutable();

    void registerWorld(RegistryKey<World> newWorld, DimensionType dimensionType, MinecraftServer server);
}
