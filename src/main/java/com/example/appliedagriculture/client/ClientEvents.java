package com.example.appliedagriculture.client;

import appeng.client.gui.style.StyleManager;
import com.example.appliedagriculture.AppliedAgriculture;
import com.example.appliedagriculture.menu.BiomaterialSynthesisChamberMenu;
import com.example.appliedagriculture.menu.CropAssemblyChamberMenu;
import com.example.appliedagriculture.registration.ModBlockEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AppliedAgriculture.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.CROP_ASSEMBLY_CHAMBER_BLOCK_ENTITY.get(),
                CropAssemblyChamberRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BIOMATERIAL_SYNTHESIS_CHAMBER_BLOCK_ENTITY.get(),
                BiomaterialSynthesisChamberRenderer::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(CropAssemblyChamberRenderer.LIGHTS_MODEL);
        event.register(BiomaterialSynthesisChamberRenderer.LIGHTS_MODEL);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(CropAssemblyChamberMenu.TYPE, ClientEvents::createCropAssemblyChamberScreen);
        event.register(BiomaterialSynthesisChamberMenu.TYPE, ClientEvents::createBiomaterialSynthesisChamberScreen);
    }

    private static CropAssemblyChamberScreen createCropAssemblyChamberScreen(
            CropAssemblyChamberMenu menu, Inventory inv, Component title) {
        var style = StyleManager.loadStyleDoc("/screens/crop_assembly_chamber.json");
        return new CropAssemblyChamberScreen(menu, inv, title, style);
    }

    private static BiomaterialSynthesisChamberScreen createBiomaterialSynthesisChamberScreen(
            BiomaterialSynthesisChamberMenu menu, Inventory inv, Component title) {
        var style = StyleManager.loadStyleDoc("/screens/biomaterial_synthesis_chamber.json");
        return new BiomaterialSynthesisChamberScreen(menu, inv, title, style);
    }
}
