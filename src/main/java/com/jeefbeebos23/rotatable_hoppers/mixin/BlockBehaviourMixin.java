package com.jeefbeebos23.rotatable_hoppers.mixin;

import com.jeefbeebos23.rotatable_hoppers.HopperProperties;
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

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void handleToolInteraction(ItemStack stack, BlockState state, Level level,
                                       BlockPos pos, Player player, InteractionHand hand,
                                       BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(state.getBlock() instanceof HopperBlock)) return;

        if (stack.is(ItemTags.PICKAXES)) {
            if (!level.isClientSide()) {
                Direction newInput = cycle(state.getValue(HopperProperties.INPUT_FACING));
                level.setBlock(pos, state
                    .setValue(HopperProperties.INPUT_FACING, newInput)
                    .setValue(HopperBlock.FACING, newInput.getOpposite()), 3);
            }
            cir.setReturnValue(level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
            cir.cancel();

        } else if (stack.is(ItemTags.AXES)) {
            if (!level.isClientSide()) {
                Direction inputDir = state.getValue(HopperProperties.INPUT_FACING);
                Direction newOutput = cycle(state.getValue(HopperBlock.FACING));
                while (newOutput == inputDir) {
                    newOutput = cycle(newOutput);
                }
                level.setBlock(pos, state.setValue(HopperBlock.FACING, newOutput), 3);
            }
            cir.setReturnValue(level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
            cir.cancel();
        }
    }

    private static Direction cycle(Direction d) {
        return switch (d) {
            case DOWN  -> Direction.UP;
            case UP    -> Direction.NORTH;
            case NORTH -> Direction.EAST;
            case EAST  -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST  -> Direction.DOWN;
        };
    }
}
