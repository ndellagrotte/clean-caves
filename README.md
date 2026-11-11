# Clean Caves

A powerful and customizable modification of Mojang's overworld cave generator, for
**Minecraft 1.12.2** on the [Cleanroom](https://github.com/CleanroomMC/Cleanroom) loader.

Clean Caves replaces vanilla cave generation with a fully data-driven generator. Every kind of
feature it can produce — tunnels, ravines, caverns, burrows, stone layers, ore clusters,
stalactites, pillars, and NBT structures — is described in Hjson preset files under
`config/cleancaves/presets/`, with an import and macro system for sharing definitions between them.

This is a modernized port of [Cave Generator](https://github.com/PersonTheCat/CaveGenerator) by
PersonTheCat. The generation behaviour and the preset format are unchanged; the code has been moved
to Java 25 and the Cleanroom toolchain, and the Lombok annotation processor has been removed in
favour of records and hand-written builders.

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
