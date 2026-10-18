package com.example.appliedagriculture.client.button;

import appeng.api.config.Setting;
import appeng.api.config.YesNo;
import appeng.client.gui.AEBaseScreen;
import appeng.core.localization.ButtonToolTips;
import com.example.appliedagriculture.api.AASettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/** Setting toggle button with per-value icons/tooltips (copied from AppliedGenerators' AGSettingToggleButton). */
public class AASettingToggleButton<T extends Enum<T>> extends AAIconButton {

    private static Map<EnumPair<?>, ButtonAppearance> appearances;
    private final Setting<T> buttonSetting;
    private final IHandler<AASettingToggleButton<T>> onPress;
    private T currentValue;

    @FunctionalInterface
    public interface IHandler<T extends AASettingToggleButton<?>> {
        void handle(T button, boolean backwards);
    }

    public AASettingToggleButton(
            Setting<T> setting, T val, IHandler<AASettingToggleButton<T>> onPress) {
        this(setting, val, t -> true, onPress);
    }

    public AASettingToggleButton(
            Setting<T> setting,
            T val,
            Predicate<T> isValidValue,
            IHandler<AASettingToggleButton<T>> onPress) {
        super(AASettingToggleButton::onPress);
        this.onPress = onPress;

        // Build a list of values (in order) that are valid w.r.t. the given predicate
        EnumSet<T> validValues = EnumSet.allOf(val.getDeclaringClass());
        validValues.removeIf(isValidValue.negate());
        validValues.removeIf(s -> !setting.getValues().contains(s));

        this.buttonSetting = setting;
        this.currentValue = val;

        if (appearances == null) {
            appearances = new HashMap<>();
            registerApp(AAIcon.ME_EXPORT_ON, AASettings.ME_EXPORT, YesNo.YES,
                    Component.translatable("gui.appliedagriculture.me_export"),
                    Component.translatable("gui.appliedagriculture.me_export.on"));
            registerApp(AAIcon.ME_EXPORT_OFF, AASettings.ME_EXPORT, YesNo.NO,
                    Component.translatable("gui.appliedagriculture.me_export"),
                    Component.translatable("gui.appliedagriculture.me_export.off"));
        }
    }

    private static void onPress(Button btn) {
        if (btn instanceof AASettingToggleButton) {
            ((AASettingToggleButton<?>) btn).triggerPress();
        }
    }

    private void triggerPress() {
        boolean backwards = false;
        // We don't get any information about right-clicks otherwise
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof AEBaseScreen) {
            backwards = ((AEBaseScreen<?>) currentScreen).isHandlingRightClick();
        }
        onPress.handle(this, backwards);
    }

    @SuppressWarnings("SameParameterValue")
    private static <T extends Enum<T>> void registerApp(
            AAIcon icon, Setting<T> setting, T val, Component... tooltipLines) {
        var lines = new ArrayList<Component>();
        Collections.addAll(lines, tooltipLines);
        appearances.put(
                new EnumPair<>(setting, val),
                new ButtonAppearance(icon, null, lines));
    }

    @Nullable
    private ButtonAppearance getAppearance() {
        if (this.buttonSetting != null && this.currentValue != null) {
            return appearances.get(new EnumPair<>(this.buttonSetting, this.currentValue));
        }
        return null;
    }

    @Override
    protected AAIcon getIcon() {
        var app = getAppearance();
        if (app != null && app.icon != null) {
            return app.icon;
        }
        return AAIcon.TOOLBAR_BUTTON_BACKGROUND;
    }

    @Override
    protected Item getItemOverlay() {
        var app = getAppearance();
        if (app != null && app.item != null) {
            return app.item;
        }
        return null;
    }

    public Setting<T> getSetting() {
        return this.buttonSetting;
    }

    public T getCurrentValue() {
        return this.currentValue;
    }

    public void set(T e) {
        if (this.currentValue != e) {
            this.currentValue = e;
        }
    }

    @Override
    public List<Component> getTooltipMessage() {
        if (this.buttonSetting == null || this.currentValue == null) {
            return Collections.emptyList();
        }

        var buttonAppearance = appearances.get(new EnumPair<>(this.buttonSetting, this.currentValue));
        if (buttonAppearance == null) {
            return Collections.singletonList(ButtonToolTips.NoSuchMessage.text());
        }

        return buttonAppearance.tooltipLines;
    }

    private record EnumPair<T extends Enum<T>>(Setting<T> setting, T value) {

        @Override
        public int hashCode() {
            return this.setting.hashCode() ^ this.value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            final EnumPair<?> other = (EnumPair<?>) obj;
            return other.setting == this.setting && other.value == this.value;
        }
    }

    private record ButtonAppearance(@Nullable AAIcon icon, @Nullable Item item, List<Component> tooltipLines) {
    }
}
