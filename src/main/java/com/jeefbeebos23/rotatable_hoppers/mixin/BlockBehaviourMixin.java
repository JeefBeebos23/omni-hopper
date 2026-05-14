package com.jeefbeebos23.rotatable_hoppers.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {

    // Intercept right-click with any pickaxe on a hopper to cycle its facing direction.
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void rotateFacing(ItemStack stack, BlockState state, Level level, BlockPos pos,
                               Player player, InteractionHand hand, BlockHitResult hit,
                               CallbackInfoReturnable<InteractionResult> cir) {
        if (!(state.getBlock() instanceof HopperBlock)) return;
        if (!stack.is(ItemTags.PICKAXES)) return;
        if (!level.isClientSide()) {
            Direction next = nextFacing(state.getValue(HopperBlock.FACING));
            level.setBlock(pos, state.setValue(HopperBlock.FACING, next), 3);
        }
        cir.setReturnValue(level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
        cir.cancel();
    }

    private static Direction nextFacing(Direction d) {
        return switch (d) {
            case DOWN  -> Direction.NORTH;
            case NORTH -> Direction.EAST;
            case EAST  -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            default    -> Direction.DOWN;
        };
    }
}
