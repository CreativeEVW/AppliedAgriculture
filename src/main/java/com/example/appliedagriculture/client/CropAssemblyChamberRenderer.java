package com.example.appliedagriculture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.example.appliedagriculture.blockentity.CropAssemblyChamberBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renders the animated light strip (same model as the molecular assembler)
 * when the machine is powered. Mirrors AE2's MolecularAssemblerRenderer.
 */
@OnlyIn(Dist.CLIENT)
public class CropAssemblyChamberRenderer implements BlockEntityRenderer<CropAssemblyChamberBlockEntity> {

    public static final ModelResourceLocation LIGHTS_MODEL = ModelResourceLocation
            .standalone(ResourceLocation.fromNamespaceAndPath("appliedagriculture", "block/crop_assembly_chamber_lights"));

    public CropAssemblyChamberRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CropAssemblyChamberBlockEntity be, float partialTicks, PoseStack ms,
                       MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        if (be.isPowered()) {
            Minecraft minecraft = Minecraft.getInstance();
            BakedModel lightsModel = minecraft.getModelManager().getModel(LIGHTS_MODEL);
            // tripwire layer has the shader properties we're looking for: alpha testing + translucency
            VertexConsumer buffer = bufferIn.getBuffer(RenderType.tripwire());

            minecraft.getBlockRenderer().getModelRenderer().renderModel(ms.last(), buffer, null,
                    lightsModel, 1, 1, 1, combinedLightIn, combinedOverlayIn, ModelData.EMPTY, null);
        }

        var seed = be.seedSlot.getStackInSlot(0);
        if (!seed.isEmpty()) {
            renderSeed(ms, bufferIn, combinedLightIn, seed);
        }
    }

    /** Renders the seed floating in the block's center, like the molecular assembler shows its crafting item. */
    private void renderSeed(PoseStack ms, MultiBufferSource bufferIn, int combinedLightIn, ItemStack seed) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        // Same vertical offset as the molecular assembler's crafting display
        ms.translate(0, seed.getItem() instanceof BlockItem ? -0.2 : -0.3, 0);
        itemRenderer.renderStatic(seed, ItemDisplayContext.GROUND, combinedLightIn,
                OverlayTexture.NO_OVERLAY, ms, bufferIn, null, 0);
        ms.popPose();
    }
}
