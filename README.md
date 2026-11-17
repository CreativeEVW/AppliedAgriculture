# AppliedAgriculture

An **Applied Energistics 2** addon for **Minecraft 1.21.1** on **NeoForge**, bringing agriculture and livestock production into your ME network.

## Machines

### Crop Assembly Chamber
Simulates planting, force-growing (age set to max — no bonemeal) and breaking crops. Nothing is ever placed in the world; the machine outputs exactly what a fully grown crop would drop.

- **Inputs (not consumed):** crop seeds (any block with an age property, incl. modded crops), flowers (3× of the same flower), chorus flower (chorus flower + chorus fruit)
- **Costs per operation:** 200 AE + 1b water
- **Internal water tank:** 200b, water input only, consumed before network water — with a full tank and FE power the chamber runs fully offline
- **Upgrades:** Amplification Card ×6 (output ×3 each), Speed Card ×2 (10s → 1s per operation)

### Biomaterial Synthesis Chamber
Converts biological templates into materials.

- **Inputs (not consumed):** spawn eggs, monster disks (MESpawner), saplings, vanilla mushrooms
- **Products:** chicken → egg, sheep → white wool, cow → milk protein, mooshroom → milk protein + fungus mixture, sapling → 5× matching log + sapling back (oak adds apples), mushroom → 3× of the same mushroom
- **Costs per operation:** 200 AE
- **Upgrades:** Yield Synthesis Card ×6 (output ×3 each), Speed Card ×2

## Items

- **Amplification Card / Yield Synthesis Card** — machine upgrade cards (×3 output per card, max 6)
- **Milk Protein** — edible (3 hunger + clears all effects like milk); milk protein + water bucket → milk bucket
- **Fungus Mixture** — edible (same stats as mushroom stew); any two mushrooms → fungus mixture; fungus mixture + bowl → mushroom stew

## Mechanics

- **FE energy input** (AE2 charger-style): an internal 2M AE buffer is fed directly by adjacent FE cables (2 FE = 1 AE). Power is drawn buffer-first, with the ME network covering the deficit — machines can run fully offline on FE.
- **Storage mode button:** insert products into the ME network (default) or push them strictly into adjacent containers on the configured sides.
- **Output side configuration:** sub-GUI with per-side toggles; sides default to all enabled.
- **JEI & GuideME:** full JEI integration (recipes enumerated dynamically from installed mods) and in-game guide pages.

## Integrations

- **Applied Energistics 2** (required) and **GuideME**
- **JEI** — recipe viewing for both machines
- **Industrial Foregoing** (optional) — saplings also produce 1b latex; entity outputs follow IF's fluid laser drill recipes (e.g. wither → ether gas); mobs without a dedicated product yield pink slime + liquid meat scaled by their max health
- **MESpawner** (optional) — monster disks are accepted as templates, all formed entities are produced per operation

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.230+
- Applied Energistics 2 19.2+

## License

MIT
