package com.example.appliedagriculture;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = AppliedAgriculture.MOD_ID)
public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Declare config values here, e.g.:
    // public static final ModConfigSpec.IntValue EXAMPLE_VALUE = BUILDER
    //         .comment("An example value")
    //         .defineInRange("exampleValue", 1, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    // Runtime values mirroring the config, e.g.: public static int exampleValue;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        // exampleValue = EXAMPLE_VALUE.get();
    }
}
