package com.jeefbeebos23.rotatable_hoppers.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HopperBlock.class)
public class HopperBlockMixin {

    @Shadow @Final @Mutable
    public static EnumProperty<Direction> FACING;

    // Replace FACING after vanilla's static initializer sets it, adding Direction.UP.
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void addUpDirection(CallbackInfo ci) {
        FACING = EnumProperty.create("facing", Direction.class,
            Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP);
    }
}
