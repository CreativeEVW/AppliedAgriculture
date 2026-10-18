package com.example.appliedagriculture.client.button;

import appeng.client.gui.style.Blitter;
import com.example.appliedagriculture.AppliedAgriculture;
import net.minecraft.resources.ResourceLocation;

/** Button icons living in appliedagriculture's states.png (copied from AppliedGenerators' AGIcon). */
public enum AAIcon {
    ME_EXPORT_ON(0, 0),
    ME_EXPORT_OFF(16, 0),
    OUTPUT_SIDES(32, 0),

    TOOLBAR_BUTTON_BACKGROUND(176, 128, 18, 20);

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AppliedAgriculture.MOD_ID, "textures/guis/states.png");
    public static final int TEXTURE_WIDTH = 256;
    public static final int TEXTURE_HEIGHT = 256;

    AAIcon(int x, int y) {
        this(x, y, 16, 16);
    }

    AAIcon(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Blitter getBlitter() {
        return Blitter.texture(TEXTURE, TEXTURE_WIDTH, TEXTURE_HEIGHT).src(x, y, width, height);
    }
}
