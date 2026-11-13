# Clean Caves

A powerful and customizable modification of Mojang's overworld cave generator, for
**Minecraft 1.12.2** on the [Cleanroom](https://github.com/CleanroomMC/Cleanroom) loader.

Clean Caves replaces vanilla cave generation with a fully data-driven generator. Every kind of
feature it can produce — tunnels, ravines, caverns, burrows, stone layers, ore clusters,
stalactites, pillars, and NBT structures — is described in Hjson preset files under
`config/cleancaves/presets/`, with an import and macro system for sharing definitions between them.

This is a modernized port of [Cave Generator](https://github.com/PersonTheCat/CaveGenerator) by
PersonTheCat. The preset format is unchanged; the code has been moved to Java 25 and the Cleanroom
toolchain, and the Lombok annotation processor has been removed in favour of records and
hand-written builders. Generation is not identical to Cave Generator's: a number of its bugs are
fixed here, listed under [Differences from Cave Generator](#differences-from-cave-generator).

## Presets

On first launch the mod seeds `config/cleancaves/`:

| Directory | Contents |
|---|---|
| `presets/` | Active presets. Ships with twenty that run on a vanilla game — stone layers, cliffs, caverns, ravines, lush caves — described in [docs/default-presets.md](docs/default-presets.md). |
| `examples/` | 15 further presets to copy into `presets/` — caverns, ravines, spirals, flooded vanilla, underground forest, and more. |
| `imports/` | Shared definitions used by the presets, including `defaults.cave` (bound to `$VANILLA` in every preset). |
| `structures/` | NBT templates used by `examples/underground_forest.cave`. |
| `generated/` | Fully-expanded presets, written when `autoGenerate` is on. Useful for debugging the import system. |
| `backup/` | Rotating backups made before a preset is overwritten. |

`TUTORIAL.cave` and `REFERENCE.cave` in the root of `config/cleancaves/` document every available
field. Both are rewritten on each launch, so edit a copy.

## Commands

`/cave <subcommand>` — `reload`, `list`, `test`, `jump`, `combine`, `enable`, `disable`, `new`,
`copy`, `move`, `delete`, `rename`, `expand`, `compress`, `import`, `eval`, and more. Run
`/cave help` in game for the full list.

## Performance

Cave generation is parallelized across helper threads by default. The expensive, pure parts are
computed ahead of the writing pass and consumed in the same order regardless of how many threads
produced them, so a given seed generates exactly the same caves as running everything on the server
thread would:

- **Stone layers, caverns and clusters** are prepared one chunk column at a time. Each column
  evaluates its own floor, ceiling, region and 3D cave noise, and records which y coordinates will
  be replaced. The server thread then applies the columns in their original order and makes every
  random choice.
- **Tunnel, ravine and room segments** have their block position lists filled in parallel x bands.
  The bands are merged in ascending x order, which is the order a single-threaded fill produces, so
  decoration walks an identical position list.

Whether a particular workload is split is decided from measured timings. Small work (or a machine
where coordination costs more than it saves) stays on the server thread; the server thread always
takes a share of whatever is split, so a split never waits on a helper that has not started.

Two settings in `config/cleancaves.cfg` control it:

| Setting | Default | Meaning |
|---|---|---|
| `parallelGeneration` | `true` | Set to `false` to keep all generation on the server thread. |
| `maxGeneratorThreads` | `0` | Helper threads. `0` picks CPU threads minus a reserve for the server, the client renderer and Pulsar, capped at 7. A positive value overrides that, up to 7. |

The design is modelled on [CaveSpeedThing](https://www.curseforge.com/minecraft/mc-mods/cavespeedthing)
by ChristKind, which parallelizes Better Caves carving, and on
[NoiseThreader](https://www.curseforge.com/minecraft/mc-mods/noisethreader) by fonnymunkey, which
inspired it. See [docs/parallel-generation.md](docs/parallel-generation.md) for how the work is
split and why it cannot change the result.

## Differences from Cave Generator

A preset written for Cave Generator 1.x loads here, with two caveats. Presets still using
pre-1.0 key names (`stoneLayers`, `scaleY`, `minHeight`/`maxHeight`, ...) are no longer translated on
load; the startup log lists every key a preset contains that nothing read. And the same seed does
not produce the same caves, because these Cave Generator bugs are fixed:

Settings that had no effect, or the wrong one:

- A scalable value written as a bare number (`stretch: 1.5`) was truncated to a whole number.
- Preset-level `blacklistDimensions` toggled the biome blacklist, and preset-level `region` was ignored.
- `region.invert` was ignored.
- A structure with no `directions` never spawned; it now spawns in every direction, as documented.
- `checkSurface: false` on a structure also dropped the upper bound of its `height`.

Terrain:

- Layer `noise` repeated identically in every chunk.
- Chunks mirrored across the diagonal (and all chunks on it) rolled identical decoration and clusters.
- Overlapping clusters could resolve differently from one launch to the next.
- Region borders were not detected on the +Z side of a chunk.
- `wallInterpolation` placed its border points 8 blocks off.
- Ponds with partial integrity were written into the air above the floor, and only the first pond in a list ever placed.
- Columns pushed outside of y 0-255 by floor or ceiling noise crashed or wrote into the next column.
- The number of tunnels per system and of pillars per chunk was re-rolled on every loop pass, skewing both low.
- Noise with `cache: true` could return the value of an unrelated block when a generator sampled outside the current chunk.
- Stalactite `region` was sampled along the world's diagonal; speleothems were built without finding the floor;
  asymmetrical stalactites shrank around the ring.
- `rotateRandomly` never chose one of the four rotations, and rotated structures were placed off-center.

Preset syntax:

- `$FUNC(1,2)` needs no space after the comma.
- Variables that refer to each other are reported instead of hanging the game.
- Arithmetic accepts `.5` and `-(2 + 3)`; an empty string or a version such as `1.0.0` is no longer taken for arithmetic.
- A root-level `$VANILLA: ALL` may be written anywhere in the preset without overriding the keys above it.
  Cave Generator achieved this by rewriting the file; here the file is left as written.

## Building

The build uses [CleanroomGradle](https://github.com/CleanroomMC/CleanroomGradle) and
[Conventions](https://github.com/CleanroomMC/Conventions). Requires JDK 25 and Cleanroom 0.7.0 or above.

```bash
./gradlew build        # compile, unit tests, Checkstyle, formatter check, and the mod jar in build/libs
./gradlew runClient
./gradlew runServer
```

`./gradlew clearSkiesApply formatJavaApply` fixes formatting. The vendored `org.hjson` and `fastnoise` sources are
excluded from the style tools.

The version is computed from git tags: tag `1.0.0` on `main` to release it, and `./gradlew -q printVersion` prints
the current value. `@{NAME}` markers in classes and resources are replaced at build time from `tokens.properties`.

## License

GPL-3.0, inherited from Cave Generator. See [LICENSE](LICENSE).

Bundled third-party code:

- `org.hjson` — [hjson-java](https://github.com/hjson/hjson-java) by Christian Zangl (MIT), forked
  by PersonTheCat to add comment preservation and key-access tracking.
- `fastnoise` — [FastNoise](https://github.com/Auburn/FastNoise) by Jordan Peck (MIT), extended by
  PersonTheCat with ranges, thresholds, and noise-derived booleans.
