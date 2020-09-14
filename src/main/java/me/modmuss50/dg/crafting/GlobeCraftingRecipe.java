package me.modmuss50.dg.crafting;

import me.modmuss50.dg.DimensionGlobeMod;
import net.minecraft.block.Block;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class GlobeCraftingRecipe extends SpecialCraftingRecipe {

	private int[] glassSlots = new int[]{0, 1, 2, 3, 5};
	private int[] blockSlots = new int[]{6, 7, 8};

	public GlobeCraftingRecipe(Identifier id) {
		super(id);
	}

	@Override
	public boolean matches(CraftingInventory inv, World world) {
		return !craft(inv).isEmpty();
	}

	@Override
	public ItemStack craft(CraftingInventory inv) {
		for (int glassSlot : glassSlots) {
			if (inv.getStack(glassSlot).getItem() != Items.GLASS) {
				return ItemStack.EMPTY;
			}
		}
		if (!inv.getStack(4).isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack blockStack = ItemStack.EMPTY;
		for (int blockSlot : blockSlots) {
			if (!blockStack.isEmpty()) {
				if (blockStack.getItem() != inv.getStack(blockSlot).getItem()) {
					return ItemStack.EMPTY;
				}
			}
			blockStack = inv.getStack(blockSlot);
			if (blockStack.isEmpty()) {
				return ItemStack.EMPTY;
			}
			if (blockStack.getItem() instanceof BlockItem) {
				Block block = ((BlockItem) blockStack.getItem()).getBlock();
				if (!block.isIn(DimensionGlobeMod.BASE_BLOCK_TAG)) {
					return ItemStack.EMPTY;
				}
			} else {
				return ItemStack.EMPTY;
			}
		}
		Block block = ((BlockItem) blockStack.getItem()).getBlock();
		return DimensionGlobeMod.globeBlockItem.getWithBase(block);
	}

	@Override
	public boolean fits(int width, int height) {
		return width >= 3 && height >= 3;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return DimensionGlobeMod.GLOBE_CRAFTING;
	}
}
