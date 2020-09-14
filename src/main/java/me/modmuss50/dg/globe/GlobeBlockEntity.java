package me.modmuss50.dg.globe;

import me.modmuss50.dg.DimensionGlobeMod;
import me.modmuss50.dg.dim.GlobeDimensionPlacer;
import me.modmuss50.dg.utils.GlobeManager;
import me.modmuss50.dg.utils.GlobeSectionManagerServer;
import me.modmuss50.dg.utils.ServerExt;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class GlobeBlockEntity extends BlockEntity implements Tickable, BlockEntityClientSerializable {

    private int globeID = -1;
    private Block baseBlock;

    private BlockPos returnPos;
    private RegistryKey<World> worldRegistryKey;

    public GlobeBlockEntity() {
        super(DimensionGlobeMod.globeBlockEntityType);
    }

    @Override
    public void tick() {
        if (!world.isClient && globeID != -1) {
            if (!isInner()) {
                GlobeManager.getInstance((ServerWorld) world)
                    .markGlobeForTicking(globeID);
            }
        }
        if (!world.isClient) {
            if (world.getTime() % 20 == 0) {
                GlobeSectionManagerServer.updateAndSyncToPlayers(this, true);
            } else {
                GlobeSectionManagerServer.updateAndSyncToPlayers(this, false);
            }
        }
    }

    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);
        globeID = tag.getInt("globe_id");
        if (tag.contains("base_block")) {
            Identifier identifier = new Identifier(tag.getString("base_block"));
            if (Registry.BLOCK.getOrEmpty(identifier).isPresent()) {
                baseBlock = Registry.BLOCK.get(identifier);
            }
        }
        if (tag.contains("return_x")) {
            returnPos = new BlockPos(tag.getInt("return_x"), tag.getInt("return_y"), tag.getInt("return_z"));
            Identifier returnType = new Identifier(tag.getString("return_dim"));
//            DimensionType dimensionType = GlobeManager.getDimensionType(world, returnType);
            RegistryKey<World> returnWorld = RegistryKey.of(Registry.DIMENSION, returnType);
            if (returnWorld != null) {
                worldRegistryKey = returnWorld;
            } else {
                returnPos = null;
                worldRegistryKey = null;
            }
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        tag.putInt("globe_id", globeID);
        if (baseBlock != null) {
            tag.putString("base_block", Registry.BLOCK.getId(baseBlock).toString());
        }

        if (returnPos != null && worldRegistryKey != null) {
            tag.putInt("return_x", returnPos.getX());
            tag.putInt("return_y", returnPos.getY());
            tag.putInt("return_z", returnPos.getZ());

//            GlobeManager.getDimensionType(world, returnDimType);

            tag.putString("return_dim", worldRegistryKey.getValue().toString());
        }

        return super.toTag(tag);
    }


    private void newGlobe() {
        if (world.isClient) {
            throw new RuntimeException();
        }

        globeID = GlobeManager.getInstance((ServerWorld) world).getNextGlobe().getId();
        markDirty();
        sync();
    }
    public void transportEntity(Entity entity) {
        if (world.isClient) {
            throw new RuntimeException();
        }

        if (DimensionGlobeMod.isGlobe(entity.world)) {
//            transportPlayerOut(entity);
        } else {
            if (globeID == -1) {
                newGlobe();
            }
            ServerExt serverExt = (ServerExt) entity.getServer();


            GlobeDimensionPlacer globeDimensionPlacer = new GlobeDimensionPlacer(globeID, entity.world.getRegistryKey(), getPos(), baseBlock);
            globeDimensionPlacer.placeEntity(entity, (ServerWorld) entity.world);
//            FabricDimensions.teleport(playerEntity, DimensionGlobeMod.globeDimension, globeDimensionPlacer);
        }
    }

    public void transportPlayer(ServerPlayerEntity playerEntity) {
        if (world.isClient) {
            throw new RuntimeException();
        }

        if (DimensionGlobeMod.isGlobe(playerEntity.getServerWorld())) {
            transportPlayerOut(playerEntity);
        } else {
            if (globeID == -1) {
                newGlobe();
            }
            ServerExt serverExt = (ServerExt) playerEntity.server;


            GlobeDimensionPlacer globeDimensionPlacer = new GlobeDimensionPlacer(globeID, playerEntity.world.getRegistryKey(), getPos(), baseBlock);
            globeDimensionPlacer.placeEntity(playerEntity, (ServerWorld) playerEntity.world);
//            FabricDimensions.teleport(playerEntity, DimensionGlobeMod.globeDimension, globeDimensionPlacer);
        }
    }

    public void setReturnPos(BlockPos returnPos, RegistryKey<World> returnDimType) {
        this.returnPos = returnPos;
        this.worldRegistryKey = returnDimType;
        markDirty();
    }

    public void transportPlayerOut(ServerPlayerEntity playerEntity) {
        if (isInner()) {

            RegistryKey<World> teleportDim = worldRegistryKey == null ? World.OVERWORLD : worldRegistryKey;

            ServerWorld targetWorld = playerEntity.server.getWorld(teleportDim);
            playerEntity.teleport(targetWorld, returnPos.getX(), returnPos.getY(), returnPos.getZ(), playerEntity.yaw, playerEntity.pitch);
//            FabricDimensions.teleport(playerEntity, teleportDim, new ExitPlacer(returnPos));
        }
    }

    public RegistryKey<World> getWorldRegistryKey() {
        return worldRegistryKey == null ? World.OVERWORLD : worldRegistryKey;
    }

    public BlockPos getInnerScanPos() {
        if (returnPos == null) {
            return BlockPos.ORIGIN;
        }
        return returnPos.subtract(new Vec3i(8, 8, 8));
    }

    public boolean isInner() {
        World world = getWorld();
        return world != null && DimensionGlobeMod.isGlobe(world);
    }

    public int getGlobeID() {
        return globeID;
    }

    public void setGlobeID(int globeID) {
        this.globeID = globeID;
    }

    @Override
    public void fromClientTag(CompoundTag compoundTag) {
        fromTag(null, compoundTag);
    }

    @Override
    public CompoundTag toClientTag(CompoundTag compoundTag) {
        return toTag(compoundTag);
    }

    public Block getBaseBlock() {
        if (baseBlock == null) {
            return Blocks.OAK_PLANKS;
        }
        return baseBlock;
    }

    public void setBaseBlock(Block baseBlock) {
        this.baseBlock = baseBlock;
        markDirty();
    }
}
