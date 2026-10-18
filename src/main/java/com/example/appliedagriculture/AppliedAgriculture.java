package com.example.appliedagriculture;

import appeng.api.AECapabilities;
import appeng.api.upgrades.Upgrades;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.core.definitions.AEItems;
import com.example.appliedagriculture.blockentity.BiomaterialSynthesisChamberBlockEntity;
import com.example.appliedagriculture.blockentity.CropAssemblyChamberBlockEntity;
import com.example.appliedagriculture.network.AAConfigButtonPacket;
import com.example.appliedagriculture.network.AAOutputSidePacket;
import com.example.appliedagriculture.registration.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AppliedAgriculture.MOD_ID)
public class AppliedAgriculture {

    public static final String MOD_ID = "appliedagriculture";
    public static final Logger LOGGER = LoggerFactory.getLogger(AppliedAgriculture.class);

    public AppliedAgriculture(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerPayloadHandlers);
    }

    /** Register serverbound payloads (config-button cycling and output-side toggles). */
    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(MOD_ID);
        registrar.playToServer(
                AAConfigButtonPacket.TYPE,
                AAConfigButtonPacket.STREAM_CODEC,
                AAConfigButtonPacket::handle);
        registrar.playToServer(
                AAOutputSidePacket.TYPE,
                AAOutputSidePacket.STREAM_CODEC,
                AAOutputSidePacket::handle);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Crop assembly chamber
        var cropBlock = ModBlocks.CROP_ASSEMBLY_CHAMBER.get();
        var cropBeType = ModBlockEntities.CROP_ASSEMBLY_CHAMBER_BLOCK_ENTITY.get();
        AEBaseBlockEntity.registerBlockEntityItem(cropBeType, ModItems.CROP_ASSEMBLY_CHAMBER.get());
        cropBlock.setBlockEntity(
                CropAssemblyChamberBlockEntity.class,
                cropBeType, null, null);

        // Biomaterial synthesis chamber
        var bioBlock = ModBlocks.BIOMATERIAL_SYNTHESIS_CHAMBER.get();
        var bioBeType = ModBlockEntities.BIOMATERIAL_SYNTHESIS_CHAMBER_BLOCK_ENTITY.get();
        AEBaseBlockEntity.registerBlockEntityItem(bioBeType, ModItems.BIOMATERIAL_SYNTHESIS_CHAMBER.get());
        bioBlock.setBlockEntity(
                BiomaterialSynthesisChamberBlockEntity.class,
                bioBeType, null, null);

        // Register upgrade cards with AE2's native upgrade system
        Upgrades.add(ModItems.AMPLIFICATION_CARD.get(), cropBlock, 6);
        Upgrades.add(AEItems.SPEED_CARD, cropBlock, 2);
        Upgrades.add(ModItems.YIELD_SYNTHESIS_CARD.get(), bioBlock, 6);
        Upgrades.add(AEItems.SPEED_CARD, bioBlock, 2);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.CROP_ASSEMBLY_CHAMBER_BLOCK_ENTITY.get(),
                (be, ctx) -> be);
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.BIOMATERIAL_SYNTHESIS_CHAMBER_BLOCK_ENTITY.get(),
                (be, ctx) -> be);

        // FE input — only accepted on the configured side (see the GUI side button)
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.CROP_ASSEMBLY_CHAMBER_BLOCK_ENTITY.get(),
                (be, side) -> be.getEnergyStorage(side));
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.BIOMATERIAL_SYNTHESIS_CHAMBER_BLOCK_ENTITY.get(),
                (be, side) -> be.getEnergyStorage(side));

        // Crop chamber internal water tank (input only)
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.CROP_ASSEMBLY_CHAMBER_BLOCK_ENTITY.get(),
                (be, side) -> be);
    }
}
