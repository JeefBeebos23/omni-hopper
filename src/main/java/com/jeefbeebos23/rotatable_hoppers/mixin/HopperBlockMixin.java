package com.jeefbeebos23.rotatable_hoppers.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlock.class)
public class HopperBlockMixin {

    @Shadow @Final @Mutable
    public static EnumProperty<Direction> FACING;

    // Replace FACING after vanilla's static initializer sets it, adding Direction.UP.
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void addUpDirection(CallbackInfo ci) {
        FACING = EnumProperty.of("facing", Direction.class,
            Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP);
    }

    // Intercept right-click with any pickaxe to cycle the hopper's facing direction.
    @Inject(method = "onUseWithItem", at = @At("HEAD"), cancellable = true)
    private void rotateFacing(ItemStack stack, BlockState state, World world, BlockPos pos,
                               PlayerEntity player, Hand hand, BlockHitResult hit,
                               CallbackInfoReturnable<ActionResult> cir) {
        if (!(stack.getItem() instanceof PickaxeItem)) return;
        if (!world.isClient()) {
            Direction next = nextFacing(state.get(HopperBlock.FACING));
            world.setBlockState(pos, state.with(HopperBlock.FACING, next));
        }
        cir.setReturnValue(world.isClient() ? ActionResult.SUCCESS : ActionResult.SUCCESS_SERVER);
    }

    private static Direction nextFacing(Direction d) {
        return switch (d) {
            case DOWN  -> Direction.NORTH;
            case NORTH -> Direction.EAST;
            case EAST  -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST  -> Direction.UP;
            case UP    -> Direction.DOWN;
        };
    }
}
