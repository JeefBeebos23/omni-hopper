package com.jeefbeebos23.rotatable_hoppers.mixin;

import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.Hopper;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {

    /**
     * Shadow the private static extract(Hopper, Inventory, int, Direction) helper
     * so we can call it from our injection without reflection.
     */
    @Shadow
    private static boolean extract(Hopper hopper, Inventory inventory, int slot, Direction side) {
        throw new AssertionError("mixin shadow");
    }

    /**
     * Redirect the hopper's pull direction from always-above to facing.getOpposite().
     *
     * Vanilla extract(World, Hopper) hardcodes blockPos = Y+1 (above the hopper).
     * We inject at HEAD (cancellable) and re-run the extraction logic using
     * facing.getOpposite() so that:
     *   - FACING=DOWN  → opposite=UP  → pulls from above  (vanilla behaviour)
     *   - FACING=NORTH → opposite=SOUTH → pulls from south
     *   - etc.
     *
     * getInventoryAt(World, BlockPos) is public static, so no @Shadow needed.
     */
    @Inject(method = "extract(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void redirectPullDirection(World world, Hopper hopper,
                                              CallbackInfoReturnable<Boolean> cir) {
        // Only redirect when the hopper is a block entity (i.e. placed in the world).
        if (!(hopper instanceof BlockEntity be)) return;

        BlockPos pos = be.getPos();
        Direction facing = world.getBlockState(pos).get(HopperBlock.FACING);

        // For FACING=DOWN (vanilla default), opposite=UP — identical to vanilla.
        Direction pullFrom = facing.getOpposite();
        BlockPos targetPos = pos.offset(pullFrom);

        @Nullable Inventory inventory = HopperBlockEntity.getInventoryAt(world, targetPos);
        if (inventory != null) {
            Direction extractSide = Direction.DOWN;
            boolean extracted = false;
            for (int slot : getAvailableSlots(inventory, extractSide)) {
                if (extract(hopper, inventory, slot, extractSide)) {
                    extracted = true;
                    break;
                }
            }
            cir.setReturnValue(extracted);
            cir.cancel();
        }
        // No block inventory at that side — do NOT cancel; let vanilla continue
        // so that item entities above the hopper can still be picked up.
    }

    /** Mirror of the private static helper to get available slots for an inventory side. */
    private static int[] getAvailableSlots(Inventory inventory, Direction side) {
        if (inventory instanceof SidedInventory sidedInventory) {
            return sidedInventory.getAvailableSlots(side);
        }
        int size = inventory.size();
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) slots[i] = i;
        return slots;
    }
}
