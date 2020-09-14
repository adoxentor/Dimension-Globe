package me.modmuss50.dg.utils;

import it.unimi.dsi.fastutil.ints.*;
import me.modmuss50.dg.DimensionGlobeMod;
import me.modmuss50.dg.globe.GlobeBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.Comparator;

import static net.minecraft.world.World.OVERWORLD;

public class GlobeManager extends PersistentState {

    private static final String SAVE_KEY = DimensionGlobeMod.MOD_ID + "_globes";
    private static long KEEP_ALIVE_TIME = 20 * 10; // 10 seconds

    private static final ChunkTicketType<ChunkPos> GLOBE_CHUNK_LOADER = ChunkTicketType.create(DimensionGlobeMod.MOD_ID + ":globe_chunk_loader", Comparator.comparingLong(ChunkPos::toLong));

    public static GlobeManager getInstance(ServerWorld world) {
        RegistryKey<DimensionType> overworldRegistryKey = DimensionType.OVERWORLD_REGISTRY_KEY;
        String path = overworldRegistryKey.getValue().getPath();
        DimensionType dimensionType = getDimensionType(world, overworldRegistryKey);
        world = world.getServer().getWorld(OVERWORLD);
        final ServerWorld serverWorld = world;
        return serverWorld != null ? serverWorld.getPersistentStateManager()
            .getOrCreate(() -> new GlobeManager(serverWorld), SAVE_KEY) : null;
    }

    public static DimensionType getDimensionType(World world, RegistryKey<DimensionType> overworldRegistryKey) {
        MinecraftServer server = world.getServer();
        return server == null ? null : server.getRegistryManager().getDimensionTypes().get(overworldRegistryKey);
    }

    public static DimensionType getDimensionType(World world, String key) {
        Identifier identifier = new Identifier(key);
        return getDimensionType(world, identifier);
    }

    public static DimensionType getDimensionType(World world, Identifier identifier) {
        RegistryKey<DimensionType> overworldRegistryKey = RegistryKey.of(Registry.DIMENSION_TYPE_KEY, identifier);
        return getDimensionType(world, overworldRegistryKey);
    }


    private Int2ObjectMap<Globe> globes = new Int2ObjectArrayMap<>();
    private Int2LongMap tickingGlobes = new Int2LongArrayMap();

    private final ServerWorld world;

    public GlobeManager(ServerWorld world) {
        super(SAVE_KEY);
        this.world = world;
    }

    public static void updateGlobe(Block baseBlock, int globeId, RegistryKey<World> returnDimension, BlockPos returnPos, ServerWorld serverWorld) {
        Globe globe = getInstance(serverWorld).getGlobeByID(globeId);
//		GlobeManager.getDimensionType(serverWorld,DimensionGlobeMod.dimensionRegistryKey);
        BlockPos globePos = globe.getGlobeLocation();
        BlockPos spawnPos = globePos.add(8, 1, 8);
        ServerWorld globeWorld = getInstance(serverWorld).getGlobeWorld();

        globeWorld.setBlockState(spawnPos.down(), DimensionGlobeMod.globeBlock.getDefaultState());
        GlobeBlockEntity exitBlockEntity = (GlobeBlockEntity) globeWorld.getBlockEntity(spawnPos.down());
        exitBlockEntity.setGlobeID(globeId);
        if(baseBlock != null){
            exitBlockEntity.setBaseBlock(baseBlock);
        }
        if (returnPos != null && returnDimension != null) {
            exitBlockEntity.setReturnPos(returnPos, returnDimension);
        }
    }

    public Globe getNextGlobe() {
        final Globe globe = new Globe(globes.size());
        globes.put(globe.id, globe);
        return globe;
    }

    public Globe getGlobeByID(int id) {
        if (!globes.containsKey(id)) {
            throw new RuntimeException("Could not find globe with id: " + id);
        }
        return globes.get(id);
    }

    public void tick() {
        final long currentTime = world.getTime();
        final IntList destoryQueue = new IntArrayList();
        for (Int2LongMap.Entry entry : tickingGlobes.int2LongEntrySet()) {
            if (currentTime - entry.getLongValue() > KEEP_ALIVE_TIME) {
                destoryQueue.add(entry.getIntKey());
            }
        }
        for (Integer forRemoval : destoryQueue) {
            unloadGlobe(forRemoval);
            tickingGlobes.remove(forRemoval);
        }
    }

    public void markGlobeForTicking(int id) {
        if (!tickingGlobes.containsKey(id)) {
            chunkLoadGlobe(id);
        }
        tickingGlobes.put(id, world.getTime());
    }

    private void chunkLoadGlobe(int id) {
        ChunkPos chunk = getGlobeByID(id).getChunkPos();
        getGlobeWorld().setChunkForced(chunk.x, chunk.z, true);
    }

    private void unloadGlobe(int id) {
        ChunkPos chunk = getGlobeByID(id).getChunkPos();
        getGlobeWorld().setChunkForced(chunk.x, chunk.z, false);
    }

    @Override
    public boolean isDirty() {
        //Always save please
        return true;
    }

    public ServerWorld getGlobeWorld() {
        ServerWorld world = this.world.getServer().getWorld(DimensionGlobeMod.dimensionRegistryKey);
        return world;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        globes.clear();
        CompoundTag globesTag = tag.getCompound("globes");
        for (String key : globesTag.getKeys()) {
            int keyID = Integer.parseInt(key);
            globes.put(keyID, new Globe(keyID, globesTag.getCompound(key)));
        }

        CompoundTag tickingGlobesTag = tag.getCompound("ticking_globes");
        for (String key : tickingGlobesTag.getKeys()) {
            int keyID = Integer.parseInt(key);
            tickingGlobes.put(keyID, world.getTime());
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        CompoundTag globesTag = new CompoundTag();
        for (Int2ObjectMap.Entry<Globe> entry : globes.int2ObjectEntrySet()) {
            globesTag.put(entry.getIntKey() + "", entry.getValue().toTag());
        }
        tag.put("globes", globesTag);

        CompoundTag tickingGlobesTag = new CompoundTag();
        for (Int2LongMap.Entry entry : tickingGlobes.int2LongEntrySet()) {
            tickingGlobesTag.putBoolean(entry.getIntKey() + "", true);
        }
        tag.put("ticking_globes", tickingGlobesTag);
        return tag;
    }

    public static class Globe {
        private final int id;
        private GlobeSection globeSection = null;
        private GlobeSection innerGlobeSection = null;

        public Globe(int id) {
            this.id = id;
        }

        public Globe(int id, CompoundTag compoundTag) {
            this(id);
            fromTag(compoundTag);
        }

        public ChunkPos getChunkPos() {
            return new ChunkPos(0, id * 100);
        }

        public BlockPos getGlobeLocation() {
            BlockPos chunkPos = getChunkPos().getStartPos();
            return new BlockPos(chunkPos.getX(), 128, chunkPos.getZ());
        }

        public void fromTag(CompoundTag tag) {

        }

        public CompoundTag toTag() {
            return new CompoundTag();
        }

        public void updateBlockSection(ServerWorld world, boolean inner, GlobeBlockEntity blockEntity) {
            if (inner) {
                if (innerGlobeSection == null) {
                    innerGlobeSection = new GlobeSection();
                }
                innerGlobeSection.buildBlockMap(world, blockEntity.getInnerScanPos());
            } else {
                if (globeSection == null) {
                    globeSection = new GlobeSection();
                }
                globeSection.buildBlockMap(world, getGlobeLocation());
            }

        }

        public void updateEntitySection(ServerWorld world, boolean inner, GlobeBlockEntity blockEntity) {
            if (inner) {
                if (innerGlobeSection == null) {
                    innerGlobeSection = new GlobeSection();
                }
                innerGlobeSection.buildEntityList(world, blockEntity.getInnerScanPos());
            } else {
                if (globeSection == null) {
                    globeSection = new GlobeSection();
                }
                globeSection.buildEntityList(world, getGlobeLocation());
            }

        }

        public GlobeSection getGlobeSection(boolean inner) {
            if (inner) {
                return innerGlobeSection;
            }
            return globeSection;
        }

        public int getId() {
            return id;
        }
    }
}
