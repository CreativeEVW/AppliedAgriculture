package com.example.appliedagriculture.registration;

import com.example.appliedagriculture.AppliedAgriculture;
import com.example.appliedagriculture.blockentity.BiomaterialSynthesisChamberBlockEntity;
import com.example.appliedagriculture.blockentity.CropAssemblyChamberBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, AppliedAgriculture.MOD_ID);

    public static final Supplier<BlockEntityType<CropAssemblyChamberBlockEntity>> CROP_ASSEMBLY_CHAMBER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("crop_assembly_chamber", () -> {
                var ref = new AtomicReference<BlockEntityType<CropAssemblyChamberBlockEntity>>();
                var type = BlockEntityType.Builder.of(
                        (pos, state) -> new CropAssemblyChamberBlockEntity(ref.get(), pos, state),
                        ModBlocks.CROP_ASSEMBLY_CHAMBER.get()
                ).build(null);
                ref.set(type);
                return type;
            });

    public static final Supplier<BlockEntityType<BiomaterialSynthesisChamberBlockEntity>> BIOMATERIAL_SYNTHESIS_CHAMBER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("biomaterial_synthesis_chamber", () -> {
                var ref = new AtomicReference<BlockEntityType<BiomaterialSynthesisChamberBlockEntity>>();
                var type = BlockEntityType.Builder.of(
                        (pos, state) -> new BiomaterialSynthesisChamberBlockEntity(ref.get(), pos, state),
                        ModBlocks.BIOMATERIAL_SYNTHESIS_CHAMBER.get()
                ).build(null);
                ref.set(type);
                return type;
            });
}
