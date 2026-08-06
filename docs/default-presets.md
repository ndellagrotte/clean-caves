# The shipped default presets

This is the config Clean Caves ships as its defaults. It began as a refactor of
`cavegenerator-modded-config`, a preset pack built for a heavily modded 1.12.2
pack, and runs on a **mostly vanilla** game — Clean Caves can be the **only** mod
installed.

The modded config crashed the game on startup without its mod dependencies,
because every block and biome name in every preset is validated while loading
(even in disabled presets) and loading aborts when a name is missing. Every
modded reference has been removed or replaced with a vanilla equivalent.

## Where these live

Nothing to install — the twenty presets below are bundled in the jar and seeded
into `config/cleancaves/presets/` the first time the mod runs. After that first
launch the folder belongs to you: the mod will not overwrite, restore, or delete
anything in it, so edit the files freely. Deleting the whole `presets/` folder
makes the next launch re-seed it from the jar.

The rest of the layout is seeded alongside it:

```
config/cleancaves/
  presets/     the twenty presets below — active worldgen
  examples/    fifteen demo presets, all disabled; restored individually if deleted
  imports/     shared variable definitions ($NATURAL_STONE, $VANILLA, block aliases)
  structures/  NBT templates used by examples/underground_forest.cave
  TUTORIAL.cave, REFERENCE.cave   rewritten on every launch — edit a copy, not these
```

Source of truth for all of it is `src/main/resources/assets/cleancaves/`; the
manifest of what gets copied out is hard-coded in `JarFiles`, so a new preset
has to be added to the matching list there as well as to the assets folder.

## What was kept (adapted to vanilla)

| Preset | Function | Modded content replaced |
| --- | --- | --- |
| `00_stone_layers_basaltic` | Jungle/Extreme-Hills-Edge strata | UB stones -> granite/andesite/terracotta; BOP biomes removed |
| `00_stone_layers_cold` | Taiga strata | UB stones -> diorite/terracotta; BOP biomes removed |
| `00_stone_layers_desert` | Desert strata + desert varnish | UB stones -> sandstone/terracotta; pyrotech limestone -> brown terracotta |
| `00_stone_layers_frozen` | Frozen-biome strata | UB stones -> granite/andesite/diorite; BOP hard ice -> packed ice |
| `00_stone_layers_generic` | Plains/forest/hills/savanna strata | UB stones -> granite/andesite/diorite/terracotta; BOP biomes removed |
| `00_stone_layers_mesa` | Mesa strata | UB stones -> vanilla rocks + red sandstone; mutated mesa biomes added |
| `00_stone_layers_mudstones` | Swamp/river strata | UB stones -> granite/terracotta/hardened clay; BOP biomes removed |
| `00_stone_layers_ocean` | Ocean-floor strata | UB stones -> granite/andesite/diorite |
| `00_stone_layers_stone_beach` | Stone-beach strata | UB stones -> diorite/terracotta |
| `00_stone_layers_volcanic` | Mushroom-island strata | UB stones -> granite/terracotta |
| `01_arid_cliffs` | Terrain sculptor | biometweaker atacamian_desert -> vanilla Desert Hills; weathered dirt -> coarse dirt; komatiite -> andesite |
| `01_overgrown_cliffs` | Terrain sculptor | BOP overgrown_cliffs -> vanilla Extreme Hills+ M; BOP grass -> grass; shale -> dirt |
| `01_tropics_cliffs` | Terrain sculptor | BOP coral_reef -> vanilla Deep Ocean; BOP grass -> grass; shale -> dirt |
| `02_caverns` | Main caverns + tunnels | Modded `NATURAL_STONE` -> vanilla list; modded biomes removed from blacklist |
| `04_bands_dirt_on_arid_tuff` | Dirt bands in the arid cliffs | salty crust -> coarse dirt; BOP loamy dirt -> dirt; biome -> Desert Hills |
| `04_moor_crevice` | Hot-spring caverns | BOP moor -> vanilla Swampland M; BOP mud -> clay (duplicate file dropped) |
| `10_jungle_ravines` | Jungle ravines | Modded `NATURAL_STONE` -> vanilla list |
| `10_lush_caves` | Lush forest caverns | rotn cave moss -> mossy cobblestone; mushroom lamp -> glowstone; verribyne -> hanging leaves; green magicite -> emerald ore |
| `10_rocky_dirt_entrances` | Dirt/stone transitions near entrances | contenttweaker rocky dirt -> coarse dirt |
| `10_stone_varnish` | Desert ravines/tunnels + varnish | pyrotech limestone -> brown terracotta; modded `NATURAL_STONE` -> vanilla list |

## What was dropped (no vanilla equivalent)

- All **nether** presets (`03_nether_spectre_wood`, `10_nether_basalt_features`,
  `10_nether_corrupted_sands_features`, `10_nether_hanging_eyes`,
  `10_nether_hell_structures`) — depend on NetherEx / Better Nether / BOP nether biomes.
- All **The Beneath** presets (`04_beneath_viridium`, `AA/AB/AC_beneath_*`) — custom dimension mod.
- Stone layers for **modded-only biomes** (`defiled`, `defiled_sand`, `highlands`,
  `oceanic_landmass`, `ominous`, `temperate_continental`, `unnatural`, `wastes`) —
  their vanilla-analog biomes are already covered by the surviving layer presets.
- All **structure-spawning** presets (`10_dense_redstone`, `10_desert_structures`,
  `10_hanging_ofuda`, `10_limestone_stalactites`, `10_mini_mushroom_island`,
  `10_wrought_ironwood`) — their NBT structures are built from modded blocks.
  (No preset in this config uses the `structures/` folder anymore.)

## Block substitution table (UndergroundBiomes -> vanilla)

| UB block | Rock | Vanilla substitute |
| --- | --- | --- |
| `igneous_stone:0` | red granite | `stone:1` (granite) |
| `igneous_stone:2` | rhyolite | `stone:1` (granite) |
| `igneous_stone:3` | andesite | `stone:5` (andesite) |
| `igneous_stone:4` | gabbro | `stone:5` (andesite) |
| `igneous_stone:5` | basalt | `stained_hardened_clay:15` (black) |
| `igneous_stone:6` | komatiite | `stained_hardened_clay:13` (green) |
| `igneous_stone:7` | dacite | `stone:3` (diorite) |
| `metamorphic_stone:0` | gneiss | `stone:5` (andesite) |
| `metamorphic_stone:1` | eclogite | `stained_hardened_clay:13` (green) |
| `metamorphic_stone:4` | blue schist | `stained_hardened_clay:9` (cyan) |
| `metamorphic_stone:5` | green schist | `stained_hardened_clay:13` (green) |
| `metamorphic_stone:6` | soapstone | `stained_hardened_clay:8` (light gray) |
| `sedimentary_stone:0` | limestone | `stone:3` (diorite) |
| `sedimentary_stone:1` | chalk | `stained_hardened_clay:0` (white) |
| `sedimentary_stone:2` | shale | `stained_hardened_clay:7` (gray) (brown `:12` in mesa) |
| `sedimentary_stone:3` | siltstone | `hardened_clay` |
| `sedimentary_stone:5` | dolomite | `stained_hardened_clay:8` (light gray) |
| `sedimentary_stone:6` | greywacke | `stained_hardened_clay:7` (gray) |
| `sedimentary_sandstone:*` | — | `sandstone` / `red_sandstone` |
| `sedimentary_sand:*` | — | `sandstone` (never a gravity block) |
| `biomesoplenty:hard_ice` | — | `packed_ice` |

## Notes

- **All twenty presets are enabled.** Six set `enabled: true` explicitly; the
  other fourteen omit the field, which defaults to true. This pack therefore
  changes default worldgen out of the box.
- The `01_*_cliffs` and `04_bands_*` presets sculpt terrain (they add solid
  blocks above ground). They are biome-scoped — Desert Hills, Extreme Hills+ M
  and Deep Ocean — but are the most visually experimental part of this pack.
  Verify in-game and set `enabled: false` if you don't like them.
- `imports/common_values.cave` and `imports/sand_cave_values.cave` define
  vanilla-only `NATURAL_STONE` lists. They include every block the stone
  layers place, so caves and wall decorators always cut through the strata.
- Stone layers replace only `stone`, `dirt` and `grass` (the mod's code
  default, exactly as in the modded config).
- `04_bands_dirt_on_arid_tuff.cave` arrived using `warp` / `warpAmplitude` /
  `warpFrequency` on its cavern generator. Those keys do not exist in this
  mod's noise settings and would have been read by nothing, so they were
  translated to the equivalent `perturb: true` / `perturbAmp` / `perturbFreq`.
- `structures/` is still shipped, but only `examples/underground_forest.cave`
  uses it — no preset in `presets/` spawns an NBT structure.
