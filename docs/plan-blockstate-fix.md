# Blockstate Rotation Fix Plan

## Problem Summary

The blockstate JSON at `src/main/resources/assets/minecraft/blockstates/hopper.json` has systematic rotation errors. Item routing works correctly (mixin logic is fine); only the visual model is wrong.

Three distinct bug patterns exist:

| Pattern | Scope | Effect |
|---|---|---|
| x=90 ↔ x=270 swapped | facing=down and facing=up L-shapes | Pipe points opposite vertical direction |
| y=0 ↔ y=180 swapped | All hopper_side entries for facing=north and facing=south | Pipe points wrong horizontal direction |
| x=90 ↔ x=270 swapped | facing=east/west parallel cases | Funnel and pipe directions are flipped |

---

## Rotation Reference (established ground truth)

**Base models:**
- `minecraft:block/hopper` — funnel=UP, pipe=DOWN
- `minecraft:block/hopper_side` — funnel=UP, pipe=NORTH

**x rotation effects** (EAST/WEST are on the X axis and are never affected by x rotation):
- x=90: UP→SOUTH, SOUTH→DOWN, DOWN→NORTH, NORTH→UP
- x=180: UP→DOWN, DOWN→UP, NORTH→SOUTH, SOUTH→NORTH
- x=270: UP→NORTH, NORTH→DOWN, DOWN→SOUTH, SOUTH→UP

**y rotation effects** (CCW from above — verified by vanilla: `facing=west` → `hopper_side y=90` → pipe=WEST):
- y=90: NORTH→WEST, WEST→SOUTH, SOUTH→EAST, EAST→NORTH
- y=180: NORTH→SOUTH, SOUTH→NORTH, EAST→WEST, WEST→EAST
- y=270: NORTH→EAST, EAST→SOUTH, SOUTH→WEST, WEST→NORTH

**Key derivations:**
- `hopper_side x=270`: funnel(UP→NORTH), pipe(NORTH→DOWN) → (NORTH, DOWN)
- `hopper_side x=90`: funnel(UP→SOUTH), pipe(NORTH→UP) → (SOUTH, UP)
- `hopper_side x=180`: funnel(UP→DOWN), pipe(NORTH→SOUTH) → (DOWN, SOUTH)
- `hopper_side x=180 y=180`: then pipe(SOUTH→NORTH) → (DOWN, NORTH)
- `hopper x=90`: funnel(UP→SOUTH), pipe(DOWN→NORTH) → (SOUTH, NORTH)
- `hopper x=270`: funnel(UP→NORTH), pipe(DOWN→SOUTH) → (NORTH, SOUTH)

---

## Correct Rotation Table

For each (facing=OUTPUT, input_facing=INPUT) pair, the model must show:
- **Funnel** pointing toward INPUT direction
- **Pipe** pointing toward OUTPUT direction

### facing=DOWN (pipe must go DOWN)

| input_facing | Model | x | y | Funnel | Pipe | Notes |
|---|---|---|---|---|---|---|
| up | hopper | — | — | UP | DOWN | vanilla default ✓ |
| north | hopper_side | 270 | — | NORTH | DOWN | |
| south | hopper_side | 270 | 180 | SOUTH | DOWN | |
| east | hopper_side | 270 | 270 | EAST | DOWN | |
| west | hopper_side | 270 | 90 | WEST | DOWN | |
| down | hopper | 180 | — | DOWN | UP | self-loop placeholder |

**Current bugs (all facing=down horizontal inputs):** uses x=90 (pipe→UP) instead of x=270 (pipe→DOWN).

### facing=UP (pipe must go UP)

| input_facing | Model | x | y | Funnel | Pipe | Notes |
|---|---|---|---|---|---|---|
| down | hopper | 180 | — | DOWN | UP | ✓ |
| north | hopper_side | 90 | 180 | NORTH | UP | |
| south | hopper_side | 90 | — | SOUTH | UP | |
| east | hopper_side | 90 | 90 | EAST | UP | |
| west | hopper_side | 90 | 270 | WEST | UP | |
| up | hopper | — | — | UP | DOWN | self-loop placeholder |

**Current bugs (all facing=up horizontal inputs):** uses x=270 (pipe→DOWN) instead of x=90 (pipe→UP).

### facing=NORTH (pipe must go NORTH)

| input_facing | Model | x | y | Funnel | Pipe | Notes |
|---|---|---|---|---|---|---|
| up | hopper_side | — | — | UP | NORTH | |
| down | hopper_side | 180 | 180 | DOWN | NORTH | |
| south | hopper | 90 | — | SOUTH | NORTH | ✓ already correct |
| east | hopper_side | — | — | UP | NORTH | approx (horizontal L-shape) |
| west | hopper_side | — | — | UP | NORTH | approx (horizontal L-shape) |
| north | hopper_side | — | — | UP | NORTH | self-loop |

**Current bugs:** All hopper_side entries use y=180 (pipe→SOUTH) — should use y=0 (pipe→NORTH). The `facing=north,input_facing=down` entry uses `x=180` (pipe→SOUTH) instead of `x=180 y=180` (pipe→NORTH).

### facing=SOUTH (pipe must go SOUTH)

| input_facing | Model | x | y | Funnel | Pipe | Notes |
|---|---|---|---|---|---|---|
| up | hopper_side | — | 180 | UP | SOUTH | |
| down | hopper_side | 180 | — | DOWN | SOUTH | |
| north | hopper | 270 | — | NORTH | SOUTH | ✓ already correct |
| east | hopper_side | — | 180 | UP | SOUTH | approx |
| west | hopper_side | — | 180 | UP | SOUTH | approx |
| south | hopper_side | — | 180 | UP | SOUTH | self-loop |

**Current bugs:** All hopper_side entries use y=0 (pipe→NORTH) — should use y=180 (pipe→SOUTH). The `facing=south,input_facing=down` entry uses `x=180 y=180` (pipe→NORTH) instead of `x=180` (pipe→SOUTH).

### facing=EAST (pipe must go EAST)

| input_facing | Model | x | y | Funnel | Pipe | Notes |
|---|---|---|---|---|---|---|
| up | hopper_side | — | 270 | UP | EAST | ✓ |
| down | hopper_side | 180 | 90 | DOWN | EAST | ✓ |
| west | hopper | 90 | 270 | WEST | EAST | |
| north | hopper_side | — | 270 | UP | EAST | approx ✓ |
| south | hopper_side | — | 270 | UP | EAST | approx ✓ |
| east | hopper_side | — | 270 | UP | EAST | self-loop |

**Current bug:** `facing=east,input_facing=west` uses `hopper x=270 y=270` → (EAST, WEST) — wrong. Should be `hopper x=90 y=270` → (WEST, EAST).

### facing=WEST (pipe must go WEST)

| input_facing | Model | x | y | Funnel | Pipe | Notes |
|---|---|---|---|---|---|---|
| up | hopper_side | — | 90 | UP | WEST | ✓ |
| down | hopper_side | 180 | 270 | DOWN | WEST | ✓ |
| east | hopper | 90 | 90 | EAST | WEST | |
| north | hopper_side | — | 90 | UP | WEST | approx ✓ |
| south | hopper_side | — | 90 | UP | WEST | approx ✓ |
| west | hopper_side | — | 90 | UP | WEST | self-loop |

**Current bug:** `facing=west,input_facing=east` uses `hopper x=270 y=90` → (WEST, EAST) — wrong. Should be `hopper x=90 y=90` → (EAST, WEST).

---

## Complete List of Entries to Change

20 entries need to change (each has both enabled=true and enabled=false variants, so 40 lines total):

| Key | Current | Correct |
|---|---|---|
| facing=down, input_facing=north | x=90 y=180 | x=270 |
| facing=down, input_facing=south | x=90 | x=270 y=180 |
| facing=down, input_facing=east | x=90 y=270 | x=270 y=270 |
| facing=down, input_facing=west | x=90 y=90 | x=270 y=90 |
| facing=up, input_facing=north | x=270 | x=90 y=180 |
| facing=up, input_facing=south | x=270 y=180 | x=90 |
| facing=up, input_facing=east | x=270 y=90 | x=90 y=90 |
| facing=up, input_facing=west | x=270 y=270 | x=90 y=270 |
| facing=north, input_facing=up | y=180 | (no rotation) |
| facing=north, input_facing=down | x=180 | x=180 y=180 |
| facing=north, input_facing=east | y=180 | (no rotation) |
| facing=north, input_facing=west | y=180 | (no rotation) |
| facing=north, input_facing=north | y=180 | (no rotation) |
| facing=south, input_facing=up | (no rotation) | y=180 |
| facing=south, input_facing=down | x=180 y=180 | x=180 |
| facing=south, input_facing=east | (no rotation) | y=180 |
| facing=south, input_facing=west | (no rotation) | y=180 |
| facing=south, input_facing=south | (no rotation) | y=180 |
| facing=east, input_facing=west | x=270 y=270 | x=90 y=270 |
| facing=west, input_facing=east | x=270 y=90 | x=90 y=90 |

---

## Implementation

Rewrite `src/main/resources/assets/minecraft/blockstates/hopper.json` using the correct table above. Then:

1. Build: `$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot"; .\gradlew.bat build`
2. Copy JAR to mods folder: `Copy-Item build\libs\rotatable-hoppers-1.0.0.jar "$env:APPDATA\.minecraft\mods\rotatable-hoppers-1.0.0.jar" -Force`
3. Commit on master, update download branch, push both.
