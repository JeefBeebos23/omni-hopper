package com.jeefbeebos23.rotatable_hoppers.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.jetbrains.annotations.Nullable;
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

    /**
     * Redirect the hopper's pull direction from always-above to facing.getOpposite().
     *
     * Vanilla suckInItems hardcodes Y+1 (above the hopper) as the pull source.
     * For FACING=DOWN we let vanilla run (handles item entities above too).
     * For all other facings we cancel and pull from facing.getOpposite() instead.
     */
    @Inject(method = "suckInItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void redirectPullDirection(Level level, Hopper hopper,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!(hopper instanceof BlockEntity be)) return;

        BlockPos pos = be.getBlockPos();
        Direction facing = level.getBlockState(pos).getValue(HopperBlock.FACING);

        // For FACING=DOWN (vanilla default) let vanilla run: pulls from above (block + item entities).
        if (facing == Direction.DOWN) return;

        Direction pullFrom = facing.getOpposite();
        BlockPos targetPos = pos.relative(pullFrom);

        @Nullable Container container = HopperBlockEntity.getContainerAt(level, targetPos);
        if (container == null) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        // extractSide = the face of the source container through which items exit toward the hopper = facing
        Direction extractSide = facing;
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
