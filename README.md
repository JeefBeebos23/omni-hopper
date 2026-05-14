# Omni-Hopper — Download & Install Guide

A Minecraft Fabric mod that lets you rotate hoppers to pull from and push to any direction.

## What it does

Vanilla hoppers always pull items from above and push items in the direction they face. This mod lets you change both directions independently:

- **Pickaxe right-click** — cycles the **input** direction (where items are pulled from). The output direction snaps to the opposite (parallel) automatically.
- **Axe right-click** — cycles the **output** direction independently, enabling perpendicular transfers.

**Examples:** `top→west` (vanilla), `top→bottom`, `down→east`, `east→west`, `north→up`, and any other combination.

---

## Download

| Minecraft Version | Mod File |
|---|---|
| **26.1.2** (latest) | `rotatable-hoppers-1.0.0.jar` |

The file is right here on this page.

---

## What You Need

You need **3 files** total before starting.

### 1. Minecraft with Fabric Loader

1. Go to **https://fabricmc.net/use/installer/**
2. Download and run the installer for your OS
3. Set "Minecraft Version" to **26.1.2**
4. Click Install
5. Open the Minecraft Launcher — select the new Fabric profile and launch once to set up

### 2. Fabric API

A required library the mod depends on.

- Download: https://modrinth.com/mod/fabric-api/versions?g=26.1.2
- Pick the latest entry that shows **26.1.2** and download the `.jar`

### 3. This Mod

Download `rotatable-hoppers-1.0.0.jar` from this page.

---

## Installing

1. Press **Windows + R**, type `%appdata%\.minecraft`, press Enter
2. Open the **mods** folder (create it if it doesn't exist)
3. Drop both `.jar` files in:
   - `fabric-api-x.x.x+26.1.2.jar`
   - `rotatable-hoppers-1.0.0.jar`
4. Open the Minecraft Launcher, select the Fabric profile, and press Play

---

## Troubleshooting

**Game crashes on launch:** Make sure Fabric API is for the **same version** as your Minecraft, and that you're launching with the Fabric profile.

**Mod doesn't appear:** Check that both `.jar` files are directly in `.minecraft/mods`, not in a subfolder.
