# Two-Direction Hopper: Implementation Plan

## Design Summary

The hopper gets two independent direction properties:
- **Input direction** (`input_facing`): which face items are pulled FROM (container on that side feeds the hopper)
- **Output direction** (`output_facing`, replaces vanilla `FACING`): which face items are pushed TO

**Controls:**
- **Pickaxe right-click**: cycles `input_facing` through all 6 directions. Output snaps to `input_facing.getOpposite()` (parallel reset).
- **Axe right-click**: cycles `output_facing` through all 6 directions independently.

**Examples:**
- Default placement: `input=UP, output=DOWN` (or `output=NORTH/S/E/W` depending on where player is looking — vanilla behavior preserved)
- Pickaxe once on `UP→WEST`: input becomes `WEST`, output snaps to `EAST` → `WEST→EAST`
- Axe on `WEST→EAST`: cycles output → `WEST→NORTH`, `WEST→UP`, `WEST→DOWN`, etc.

---

## Files to Touch

| File | Action |
|------|--------|
| `mixin/HopperBlockMixin.java` | CREATE — add INPUT_FACING property, expand FACING to 6 dirs, fix interactionShapes crash, set placement defaults |
| `mixin/BlockBehaviourMixin.java` | MODIFY — add axe handler, update pickaxe handler for two properties |
| `mixin/HopperBlockEntityMixin.java` | CREATE — redirect suckInItems to pull from input_facing |
| `rotatable_hoppers.mixins.json` | MODIFY — add HopperBlockMixin and HopperBlockEntityMixin back |
| `assets/minecraft/blockstates/hopper.json` | CREATE — blockstate JSON covering all (input, output) pairs |
| `assets/rotatable_hoppers/models/block/` | CREATE — model JSON files for non-vanilla orientations |

---

## Task 1: Block State Expansion — `HopperBlockMixin`

**Goal:** Add `INPUT_FACING` (all 6 dirs), expand `FACING` to 6 dirs, register both, fix placement, fix crash.

### 1a. Add properties via `<clinit>`

```java
@Mixin(HopperBlock.class)
public class HopperBlockMixin {

    @Shadow @Final @Mutable
    public static EnumProperty<Direction> FACING;

    public static EnumProperty<Direction> INPUT_FACING =
        EnumProperty.create("input_facing", Direction.class);

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void expandProperties(CallbackInfo ci) {
        FACING = EnumProperty.create("facing", Direction.class); // all 6
    }
}
```

### 1b. Register INPUT_FACING in `createBlockStateDefinition`

Vanilla signature: `protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)`

```java
@Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
private void registerInputFacing(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
    builder.add(INPUT_FACING);
}
```

### 1c. Set placement defaults in `getStateForPlacement`

Vanilla returns a BlockState with `FACING` set. We need to also set `INPUT_FACING=UP`.

```java
@Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
private void setInputFacingDefault(BlockPlaceContext ctx, CallbackInfoReturnable<BlockState> cir) {
    BlockState result = cir.getReturnValue();
    if (result != null) {
        cir.setReturnValue(result.setValue(INPUT_FACING, Direction.UP));
    }
}
```

### 1d. Fix `getInteractionShape` crash

Vanilla `HopperBlock.getInteractionShape` looks up `interactionShapes` map (keyed by BlockState). New states (with INPUT_FACING) aren't in that map → NPE.

Fix: inject at HEAD, catch the null case, return a fallback shape.

```java
@Shadow
private Map<BlockState, VoxelShape> interactionShapes;  // need to verify field name via javap

@Inject(method = "getInteractionShape", at = @At("HEAD"), cancellable = true)
private void fixInteractionShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext ctx, CallbackInfoReturnable<VoxelShape> cir) {
    VoxelShape shape = interactionShapes.get(state);
    if (shape == null) {
        // Fall back to FACING=DOWN shape (or NORTH) for unknown states
        BlockState fallback = state.setValue(FACING, Direction.DOWN)
                                   .setValue(INPUT_FACING, Direction.UP);
        shape = interactionShapes.get(fallback);
        if (shape != null) cir.setReturnValue(shape);
    }
}
```

**NOTE:** Verify `interactionShapes` field name via `javap -p` on the deobf jar before implementing. It may differ.

---

## Task 2: Pickaxe + Axe Controls — `BlockBehaviourMixin`

```java
@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void handleToolInteraction(ItemStack stack, BlockState state, Level level,
                                       BlockPos pos, Player player, InteractionHand hand,
                                       BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(state.getBlock() instanceof HopperBlock)) return;

        if (stack.is(ItemTags.PICKAXES)) {
            // Cycle input, snap output to parallel
            if (!level.isClientSide()) {
                Direction newInput = cycle(state.getValue(HopperBlockMixin.INPUT_FACING));
                Direction newOutput = newInput.getOpposite();
                level.setBlock(pos, state
                    .setValue(HopperBlockMixin.INPUT_FACING, newInput)
                    .setValue(HopperBlock.FACING, newOutput), 3);
            }
            cir.setReturnValue(level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
            cir.cancel();

        } else if (stack.is(ItemTags.AXES)) {
            // Cycle output only
            if (!level.isClientSide()) {
                Direction newOutput = cycle(state.getValue(HopperBlock.FACING));
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
```

**Note on INPUT_FACING access:** `INPUT_FACING` is declared in `HopperBlockMixin` as a `public static` field, so it can be accessed from `BlockBehaviourMixin`. At runtime, after the `<clinit>` injection fires, the field holds the correct `EnumProperty`. Confirm this works at test time — if there are classloading order issues, move `INPUT_FACING` to a shared constants class.

---

## Task 3: Input-Direction Item Pull — `HopperBlockEntityMixin`

Vanilla `suckInItems` hardcodes pulling from above. We redirect it to pull from `input_facing`.

```java
@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {

    @Shadow
    private static boolean tryTakeInItemFromSlot(Hopper hopper, Container container,
                                                  int slot, Direction side) {
        throw new AssertionError("shadow");
    }

    @Shadow
    private static int[] getSlots(Container container, Direction side) {
        throw new AssertionError("shadow");
    }

    @Inject(method = "suckInItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void redirectPullDirection(Level level, Hopper hopper,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (!(hopper instanceof BlockEntity be)) return;

        BlockPos pos = be.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Direction inputFacing = state.getValue(HopperBlockMixin.INPUT_FACING);

        // UP = vanilla default — let vanilla run (it also picks up item entities, which we keep)
        if (inputFacing == Direction.UP) return;

        BlockPos sourcePos = pos.relative(inputFacing);
        Container container = HopperBlockEntity.getContainerAt(level, sourcePos);
        if (container == null) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }

        // extractSide = face of source container facing the hopper = opposite of inputFacing
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
```

---

## Task 4: Block State JSON + Models

### 4a. Blockstate file

`src/main/resources/assets/minecraft/blockstates/hopper.json`

We need an entry for every `(input_facing, facing)` combination. That's 36 total.

Structure (abbreviated — full file has all 36):
```json
{
  "variants": {
    "input_facing=up,facing=down":   { "model": "minecraft:block/hopper" },
    "input_facing=up,facing=north":  { "model": "minecraft:block/hopper_side", "y": 180 },
    "input_facing=up,facing=south":  { "model": "minecraft:block/hopper_side" },
    "input_facing=up,facing=west":   { "model": "minecraft:block/hopper_side", "y": 90 },
    "input_facing=up,facing=east":   { "model": "minecraft:block/hopper_side", "y": 270 },
    "input_facing=up,facing=up":     { "model": "rotatable_hoppers:block/hopper_straight", "x": 180 },

    "input_facing=down,facing=up":   { "model": "rotatable_hoppers:block/hopper_straight", "x": 180 },
    "input_facing=down,facing=down": { "model": "minecraft:block/hopper" },
    ...
    "input_facing=north,facing=south": { "model": "rotatable_hoppers:block/hopper_straight_ns" },
    ...
  }
}
```

**Key rotation rules to work out (verify against vanilla model by testing in-game):**
- `input=UP` variants → same as vanilla (vanilla model rotated to match output direction)
- `input=DOWN` variants → flip the vanilla model upside-down (x=180)
- `input=NORTH/S/E/W` variants → tilt the model 90° so funnel faces that direction

### 4b. Custom model files needed

The vanilla `hopper` and `hopper_side` models don't cover all orientations via simple x/y rotation. Create custom models in `assets/rotatable_hoppers/models/block/`:

- `hopper_straight.json` — straight-through (input and output on opposite faces), derived from vanilla `hopper` with appropriate parent/transform
- `hopper_side_tilted.json` — L-shape with funnel on a horizontal face (for input=NORTH/S/E/W variants)

**Strategy:** reference vanilla model files (`assets/minecraft/models/block/hopper.json` and `hopper_side.json`) to understand the geometry, then build variants. The vanilla models are in the extracted client jar at `~/.gradle/caches/fabric-loom/.../minecraft-client-*.jar`.

---

## Task 5: Update `rotatable_hoppers.mixins.json`

```json
{
  "required": true,
  "package": "com.jeefbeebos23.rotatable_hoppers.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": [
    "BlockBehaviourMixin",
    "HopperBlockMixin",
    "HopperBlockEntityMixin"
  ],
  "injectors": { "defaultRequire": 1 }
}
```

---

## Task 6: Build, Deploy, Test

```
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot"
cd C:\Users\wbgui\coding_projects\rotatable-hoppers
.\gradlew.bat build
Copy-Item build\libs\rotatable-hoppers-1.0.0.jar "$env:APPDATA\.minecraft\mods\rotatable-hoppers-1.0.0.jar" -Force
```

**Test checklist:**
- [ ] Place hopper normally → `input=UP, output=DOWN` or `output=<horizontal>` depending on placement
- [ ] Pickaxe once → input cycles, output snaps to parallel
- [ ] Axe once → output cycles, input stays
- [ ] Items transfer from source in `input_facing` direction
- [ ] Items transfer to container in `output_facing` direction
- [ ] No crash when rotating to any of the 36 states
- [ ] Push to GitHub after confirmed working

---

## Known Risks / Watch Points

1. **`interactionShapes` field name** — verify via `javap` before assuming name; if wrong the mixin silently no-ops.
2. **`INPUT_FACING` access from `BlockBehaviourMixin`** — if classloading order causes `null` on the static field, move the constant to a dedicated `ModProperties` class that both mixins import.
3. **Blockstate JSON completeness** — 36 entries; a missing entry causes the game to warn and fall back to a default model. Write all 36 even if some models look wrong initially.
4. **Item entity pickup** — vanilla `suckInItems` also picks up item entities floating above the hopper via `getItemsAtAndAbove`. Our mixin skips vanilla for non-UP inputs. Consider whether to add item-entity pickup for other directions (probably not needed for MVP).
5. **Model geometry** — will need in-game iteration to get rotations right; plan for at least 2-3 test builds for this step.
