package com.jeefbeebos23.rotatable_hoppers.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {

    @Shadow
    private static boolean tryTakeInItemFromSlot(Hopper hopper, Container container, int slot, Direction side) {
        throw new AssertionError("mixin shadow");
    }

    @Shadow
    private static int[] getSlots(Container container, Direction side) {
        throw new AssertionError("mixin shadow");
    }

    @Inject(method = "suckInItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void redirectPullDirection(Level level, Hopper hopper,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (!(hopper instanceof BlockEntity be)) return;

        BlockPos pos = be.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Direction inputFacing = state.getValue(HopperBlockMixin.INPUT_FACING);

        if (inputFacing == Direction.UP) return;

        BlockPos sourcePos = pos.relative(inputFacing);
        Container container = HopperBlockEntity.getContainerAt(level, sourcePos);
        if (container == null) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        Direction extractSide = inputFacing.getOpposite();
        boolean extracted = false;
        for (int slot : getSlots(container, extractSide)) {
            if (tryTakeInItemFromSlot(hopper, container, slot, extractSide)) {
                extracted = true;
                break;
            }
        }
        cir.setReturnValue(extracted);
        cir.cancel();
    }
}
