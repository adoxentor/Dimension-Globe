package me.modmuss50.dg.dim;

import me.modmuss50.dg.utils.GlobeManager;
import me.modmuss50.dg.utils.GlobeSection;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

public class GlobeDimensionPlacer {

    private int globeId = -1;
    private RegistryKey<World> returnDimension = null;
    private BlockPos returnPos = null;
    private Block baseBlock = null;

    public GlobeDimensionPlacer() {
    }

    public GlobeDimensionPlacer(int globeId, RegistryKey<World> dimensionType, BlockPos returnPos, Block baseBlock) {
        this.globeId = globeId;
        this.returnDimension = dimensionType;
        this.returnPos = returnPos;
        this.baseBlock = baseBlock;
    }

    public void placeEntity(Entity entity, ServerWorld serverWorld) {
        if (globeId == -1) {
            throw new RuntimeException("Unknown globe: " + globeId);
        }
        GlobeManager.Globe globe = GlobeManager.getInstance(serverWorld).getGlobeByID(globeId);
//		GlobeManager.getDimensionType(serverWorld,DimensionGlobeMod.dimensionRegistryKey);
        BlockPos globePos = globe.getGlobeLocation();
        BlockPos spawnPos = globePos.add(8, 1, 8);
        ServerWorld globeWorld = GlobeManager.getInstance(serverWorld).getGlobeWorld();
        buildGlobe(globeWorld, globePos);
        GlobeManager.updateGlobe(baseBlock, globeId, returnDimension, returnPos, serverWorld);

        if (entity instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) entity).teleport(globeWorld, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0, 0);
        } else {
            moveToWorld(entity,globeWorld,spawnPos);
//            Entity entity1 = entity.moveToWorld(globeWorld);
//            if (entity1 != null)
//                entity = entity1;
//			entity.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        }

//		return new BlockPattern.Result(new Vec3d(spawnPos.getX(),spawnPos.getY(),spawnPos.getZ()).add(0.5, 0,0.5), new Vec3d(0, 0, 0), 0);
    }

    private void buildGlobe(ServerWorld world, BlockPos globePos) {
        final BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int x = 0; x < GlobeSection.GLOBE_SIZE; x++) {
            for (int y = 0; y < GlobeSection.GLOBE_SIZE; y++) {
                for (int z = 0; z < GlobeSection.GLOBE_SIZE; z++) {
                    if (x == 0 || x == GlobeSection.GLOBE_SIZE - 1 || y == 0 || y == GlobeSection.GLOBE_SIZE - 1 || z == 0 || z == GlobeSection.GLOBE_SIZE - 1) {
                        mutable.set(globePos.getX() + x, globePos.getY() + y, globePos.getZ() + z);
                        world.setBlockState(mutable, Blocks.BARRIER.getDefaultState());
                    }

                }
            }
        }

    }

    public Entity moveToWorld(Entity thisEntity, ServerWorld destination, BlockPos blockPos2) {
        if (thisEntity.world instanceof ServerWorld && !thisEntity.removed) {
            thisEntity.world.getProfiler().push("changeDimension");
            thisEntity.detach();
            thisEntity.world.getProfiler().push("reposition");
            TeleportTarget teleportTarget =new TeleportTarget(new Vec3d((double)blockPos2.getX() + 0.5D, (double)blockPos2.getY(), (double)blockPos2.getZ() + 0.5D), thisEntity.getVelocity(), thisEntity.yaw, thisEntity.pitch);

            if (teleportTarget == null) {
                return null;
            } else {
                thisEntity.world.getProfiler().swap("reloading");
                Entity entity = thisEntity.getType().create(destination);
                if (entity != null) {
                    entity.copyFrom(thisEntity);
                    entity.refreshPositionAndAngles(teleportTarget.position.x, teleportTarget.position.y, teleportTarget.position.z, teleportTarget.yaw, entity.pitch);
                    entity.setVelocity(teleportTarget.velocity);
                    destination.onDimensionChanged(entity);
                    if (destination.getRegistryKey() == World.END) {
                        ServerWorld.createEndSpawnPlatform(destination);
                    }
                }

                thisEntity.removed = true;
                thisEntity.world.getProfiler().pop();
                ((ServerWorld)thisEntity.world).resetIdleTimeout();
                destination.resetIdleTimeout();
                thisEntity.world.getProfiler().pop();
                return entity;
            }
        } else {
            return null;
        }
    }

}
