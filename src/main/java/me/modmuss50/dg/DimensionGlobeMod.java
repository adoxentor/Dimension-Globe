package me.modmuss50.dg;

import me.modmuss50.dg.crafting.GlobeCraftingRecipe;
import me.modmuss50.dg.dim.GlobeDimension;
import me.modmuss50.dg.globe.GlobeBlock;
import me.modmuss50.dg.globe.GlobeBlockEntity;
import me.modmuss50.dg.globe.GlobeBlockItem;
import me.modmuss50.dg.utils.ServerExt;
import me.modmuss50.dg.utils.GlobeManager;
import me.modmuss50.dg.utils.GlobeSectionManagerServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.Tag;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;

public class DimensionGlobeMod implements ModInitializer {

    public static final String MOD_ID = "globedimension";

    public static GlobeBlock globeBlock;
    public static GlobeBlockItem globeBlockItem;
    public static BlockEntityType<GlobeBlockEntity> globeBlockEntityType;
    public static final RegistryKey<DimensionOptions> alternate5Option = RegistryKey.of(
        Registry.DIMENSION_OPTIONS,
        new Identifier("globedimension:globedimension"));
    public static DimensionType surfaceTypeObject = new GlobeDimension();
    public static RegistryKey<World> dimensionRegistryKey;


    public static final ItemGroup GLOBE_ITEM_GROUP = FabricItemGroupBuilder.build(new Identifier(MOD_ID, "globes"), () -> globeBlockItem.getWithBase(Blocks.OAK_PLANKS));
    public static final Tag<Block> BASE_BLOCK_TAG = TagRegistry.block(new Identifier(MOD_ID, "base_blocks"));

    public static final SpecialRecipeSerializer<GlobeCraftingRecipe> GLOBE_CRAFTING = Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(MOD_ID, "globe_crafting"), new SpecialRecipeSerializer<>(GlobeCraftingRecipe::new));

    public static boolean isGlobe(World world) {
        boolean b = world.getRegistryKey() == dimensionRegistryKey;
        return b;
    }

    @Override
    public void onInitialize() {

        dimensionRegistryKey = RegistryKey.of(Registry.DIMENSION, new Identifier("globedimension", "globedimension"));


//        FabricDimensions.registerDefaultPlacer(dimensionRegistryKey, FabricDimensionTest::placeEntityInVoid);

        Identifier globeID = new Identifier(MOD_ID, "globe");

        Registry.register(Registry.BLOCK, globeID, globeBlock = new GlobeBlock());

        globeBlockItem = new GlobeBlockItem(globeBlock, new Item.Settings().group(GLOBE_ITEM_GROUP));
        globeBlockItem.appendBlocks(Item.BLOCK_ITEMS, globeBlockItem);
        Registry.register(Registry.ITEM, globeID, globeBlockItem);

        globeBlockEntityType = BlockEntityType.Builder.create(GlobeBlockEntity::new, globeBlock).build(null);
        Registry.register(Registry.BLOCK_ENTITY_TYPE, globeID, globeBlockEntityType);


        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!world.isClient && world.getDimension() ==GlobeManager.getDimensionType(world,DimensionType.OVERWORLD_ID)) {
				GlobeManager.getInstance((ServerWorld) world).tick();
			}
        });
//            world -> {
//
//		});
//        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
//            ServerWorld world = server.getWorld(dimensionRegistryKey);
//            if(world == null){
//                ServerExt mix = (ServerExt) server;
//                mix.registerWorld(dimensionRegistryKey,surfaceTypeObject,server);
//            }
//        });

        AttackBlockCallback.EVENT.register((playerEntity, world, hand, blockPos, direction) -> {
            if (isGlobe(world)) {
                BlockState state = world.getBlockState(blockPos);
                if (state.getBlock() == globeBlock || state.getBlock() == Blocks.BARRIER) {
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((playerEntity, world, hand, blockHitResult) -> {
			if (isGlobe(world)) {
                ItemStack stack = playerEntity.getStackInHand(hand);
                if (stack.getItem() == Items.BARRIER) {
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        GlobeSectionManagerServer.register();
    }
}
