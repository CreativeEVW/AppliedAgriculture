package com.example.appliedagriculture.registration;

import com.example.appliedagriculture.AppliedAgriculture;
import com.example.appliedagriculture.menu.BiomaterialSynthesisChamberMenu;
import com.example.appliedagriculture.menu.CropAssemblyChamberMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(BuiltInRegistries.MENU, AppliedAgriculture.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CropAssemblyChamberMenu>> CROP_ASSEMBLY_CHAMBER =
            MENU_TYPES.register("crop_assembly_chamber", () -> CropAssemblyChamberMenu.TYPE);

    public static final DeferredHolder<MenuType<?>, MenuType<BiomaterialSynthesisChamberMenu>> BIOMATERIAL_SYNTHESIS_CHAMBER =
            MENU_TYPES.register("biomaterial_synthesis_chamber", () -> BiomaterialSynthesisChamberMenu.TYPE);
}
