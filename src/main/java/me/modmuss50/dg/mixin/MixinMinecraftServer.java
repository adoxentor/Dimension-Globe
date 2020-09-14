package me.modmuss50.dg.mixin;

import com.google.common.collect.ImmutableList;
import me.modmuss50.dg.dim.GlobeDimension;
import me.modmuss50.dg.utils.ServerExt;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements ServerExt {
    @Override
    @Accessor("worlds")
    public abstract Map<RegistryKey<World>, ServerWorld> getWorldsMutable();

    @Shadow
    Executor workerExecutor;

    @Shadow
    LevelStorage.Session session;
    @Shadow
    public SaveProperties saveProperties;
    @Shadow
    DynamicRegistryManager.Impl registryManager;

    @Override
    public void registerWorld(RegistryKey<World> newWorld, DimensionType dimensionType, MinecraftServer server) {
        ServerWorldProperties serverWorldProperties = this.saveProperties.getMainWorldProperties();
        GeneratorOptions generatorOptions = this.saveProperties.getGeneratorOptions();

        ServerWorld serverWorld = new ServerWorld(server,
            this.workerExecutor,
            this.session,
            serverWorldProperties,
            World.OVERWORLD,
            dimensionType,
            new WorldGenerationProgressListener() {
                @Override
                public void start(ChunkPos spawnPos) {

                }

                @Override
                public void setChunkStatus(ChunkPos pos, ChunkStatus status) {

                }

                @Override
                public void stop() {

                }
            },
            (ChunkGenerator) GlobeDimension.createVoidGenerator(this.registryManager),
            generatorOptions.isDebugWorld(),
            BiomeAccess.hashSeed(generatorOptions.getSeed()),
            ImmutableList.of(),
            false);
        this.getWorldsMutable().put(newWorld, serverWorld);

    }


    @Shadow
    public abstract DynamicRegistryManager getRegistryManager();

    @Inject(method = "createWorlds", at = @At("HEAD"))
    private void onBeforeCreateWorlds(
        WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci
    ) {
        ServerExt.addDim(this,this.saveProperties,this.registryManager);

    }


}
