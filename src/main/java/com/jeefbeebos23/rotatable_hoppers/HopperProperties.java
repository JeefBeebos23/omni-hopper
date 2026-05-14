package com.jeefbeebos23.rotatable_hoppers;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class HopperProperties {
    public static final EnumProperty<Direction> INPUT_FACING =
        EnumProperty.create("input_facing", Direction.class);

    private HopperProperties() {}
}
