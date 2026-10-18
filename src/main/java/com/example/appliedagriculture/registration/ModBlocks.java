package com.example.appliedagriculture.registration;

import com.example.appliedagriculture.AppliedAgriculture;
import com.example.appliedagriculture.block.BiomaterialSynthesisChamberBlock;
import com.example.appliedagriculture.block.CropAssemblyChamberBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AppliedAgriculture.MOD_ID);

    public static final DeferredBlock<CropAssemblyChamberBlock> CROP_ASSEMBLY_CHAMBER = BLOCKS.register(
            "crop_assembly_chamber",
            () -> new CropAssemblyChamberBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5, 6)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<BiomaterialSynthesisChamberBlock> BIOMATERIAL_SYNTHESIS_CHAMBER = BLOCKS.register(
            "biomaterial_synthesis_chamber",
            () -> new BiomaterialSynthesisChamberBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5, 6)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
}
