package me.modmuss50.dg.mixin;

import me.modmuss50.dg.DimensionGlobeMod;
import me.modmuss50.dg.globe.GlobeItemRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltinModelItemRenderer.class)
public class MixinBuiltinModelItemRenderer {

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	public void render(ItemStack stack, ModelTransformation.Mode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, CallbackInfo info) {
		if (stack.getItem() instanceof BlockItem) {
			Block block = ((BlockItem) stack.getItem()).getBlock();
			if (block == DimensionGlobeMod.globeBlock) {
				GlobeItemRenderer.render(stack, matrices, vertexConsumers, light, overlay);
				info.cancel();
			}
		}
	}
}
