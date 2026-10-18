package com.example.appliedagriculture.registration;

import com.example.appliedagriculture.AppliedAgriculture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, AppliedAgriculture.MOD_ID);

    public static final Supplier<CreativeModeTab> APPLIED_AGRICULTURE_TAB = CREATIVE_TABS.register(
            "appliedagriculture_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.appliedagriculture"))
                    .icon(() -> new ItemStack(ModItems.CROP_ASSEMBLY_CHAMBER.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.CROP_ASSEMBLY_CHAMBER.get());
                        output.accept(ModItems.BIOMATERIAL_SYNTHESIS_CHAMBER.get());
                        output.accept(ModItems.AMPLIFICATION_CARD.get());
                        output.accept(ModItems.YIELD_SYNTHESIS_CARD.get());
                        output.accept(ModItems.MILK_PROTEIN.get());
                        output.accept(ModItems.FUNGUS_MIXTURE.get());
                    })
                    .build()
    );
}
