package com.example.appliedagriculture.blockentity;

import net.minecraft.core.Direction;

import java.util.Set;

/** Common config surface for the output-side chooser (adjacent-container mode). */
public interface IOutputSideConfigurable {

    Set<Direction> getOutputSides();

    void setOutputSide(Direction side, boolean value);
}
