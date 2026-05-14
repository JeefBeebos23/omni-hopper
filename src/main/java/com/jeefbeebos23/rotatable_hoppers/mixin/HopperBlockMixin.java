package com.jeefbeebos23.rotatable_hoppers.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Function;

@Mixin(HopperBlock.class)
public class HopperBlockMixin {

    @Shadow @Final @Mutable
    public static EnumProperty<Direction> FACING;

    @Unique
    public static final EnumProperty<Direction> INPUT_FACING =
        EnumProperty.create("input_facing", Direction.class);

    @Shadow
    private Map<Direction, VoxelShape> interactionShapes;

    @Shadow
    private Function<BlockState, VoxelShape> shapes;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void expandFacing(CallbackInfo ci) {
        FACING = EnumProperty.create("facing", Direction.class);
    }

    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void registerInputFacing(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(INPUT_FACING);
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void setInputFacingDefault(BlockPlaceContext ctx, CallbackInfoReturnable<BlockState> cir) {
        BlockState result = cir.getReturnValue();
        cir.setReturnValue(result.setValue(INPUT_FACING, Direction.UP));
    }

    @Inject(method = "getInteractionShape", at = @At("HEAD"), cancellable = true)
    private void fixInteractionShape(BlockState state, BlockGetter level, BlockPos pos,
                                     CallbackInfoReturnable<VoxelShape> cir) {
        Direction facing = state.getValue(FACING);
        if (!interactionShapes.containsKey(facing)) {
            VoxelShape fallback = interactionShapes.get(Direction.DOWN);
            if (fallback == null) fallback = net.minecraft.world.phys.shapes.Shapes.empty();
            cir.setReturnValue(fallback);
            cir.cancel();
        }
    }

    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void fixGetShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx,
                             CallbackInfoReturnable<VoxelShape> cir) {
        Direction facing = state.getValue(FACING);
        if (facing == Direction.UP) {
            BlockState fallback = state.setValue(FACING, Direction.DOWN);
            cir.setReturnValue(shapes.apply(fallback));
        }
    }
}
