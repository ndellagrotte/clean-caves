# Changelog

## 1.1.0 - 2026-11-13

> [!WARNING]
> This version has breaking changes! More details below.

### Breaking Changes

- **worldgen**: Stalactite, pillar and structure bugs inherited from Cave Generator *[commit by [@ndellagrotte](https://github.com/ndellagrotte), Claude Fable 5.2 in [fd3098e](https://github.com/ndellagrotte/clean-caves/commit/fd3098e9b537f023bb194e5af68dc8b5dd9499e7)]*
  - Generated terrain differs from 1.0.0 for the same seed.
- **worldgen**: Terrain bugs inherited from Cave Generator *[commit by [@ndellagrotte](https://github.com/ndellagrotte), Claude Fable 5.2 in [ab36dba](https://github.com/ndellagrotte/clean-caves/commit/ab36dba0ac26fcd2054bfe2d74f5547bb45b3c45)]*
  - Generated terrain differs from 1.0.0 for the same seed.

### Feature

- **presets**: Ship stone-only strata as the default config *[commit by [@ndellagrotte](https://github.com/ndellagrotte) in [f8187dd](https://github.com/ndellagrotte/clean-caves/commit/f8187dd230d179fe16572ac0c04ccca6f7a6ea2c)]*

### Bug Fix

- **config**: Say why a preset failed to parse *[commit by [@ndellagrotte](https://github.com/ndellagrotte), Claude Fable 5.2 in [32967b4](https://github.com/ndellagrotte/clean-caves/commit/32967b4424fd2b49d2e7fe57c3dc7cdac14cfc36)]*
- **config**: Preset parsing and diagnostics bugs inherited from Cave Generator *[commit by [@ndellagrotte](https://github.com/ndellagrotte), Claude Fable 5.2 in [adad211](https://github.com/ndellagrotte/clean-caves/commit/adad2111803a1a2c52a42f15998ffda69fe6e811)]*
- **presets**: Migrate bundled examples to the current format *[commit by [@ndellagrotte](https://github.com/ndellagrotte), Claude Fable 5.2 in [c1042ce](https://github.com/ndellagrotte/clean-caves/commit/c1042cebda96ff052a65e5ed302be04e6cf22964)]*
- **commands**: Resolve /cave paths inside the config folder *[commit by [@ndellagrotte](https://github.com/ndellagrotte), Claude Fable 5.2 in [b794049](https://github.com/ndellagrotte/clean-caves/commit/b7940491fff3ce0973c970788bc2441a40281394)]*
- **config**: Restore preset loading behaviour lost in the port *[commit by [@ndellagrotte](https://github.com/ndellagrotte), Claude Fable 5.2 in [ff71ffa](https://github.com/ndellagrotte/clean-caves/commit/ff71ffa331fe9e91584d47e33fc7c6b165361686)]*
- **worldgen**: Build generators in upstream order *[commit by [@ndellagrotte](https://github.com/ndellagrotte), Claude Fable 5.2 in [a2235b1](https://github.com/ndellagrotte/clean-caves/commit/a2235b10e9038de9e69a9ef34185157204afa7cb)]*

### Performance

- **worldgen**: Align parallel generation with CaveSpeedThing *[commit by [@ndellagrotte](https://github.com/ndellagrotte) in [51fdd9b](https://github.com/ndellagrotte/clean-caves/commit/51fdd9b513c98d253c916486b10d6dfe6f52a3a5)]*
- **worldgen**: Compute cave generation on helper threads *[commit by [@ndellagrotte](https://github.com/ndellagrotte) in [a879dc4](https://github.com/ndellagrotte/clean-caves/commit/a879dc406a6062c0424fcf15a70ca631a5365350)]*

### Documentation

- List behaviour differences from Cave Generator *[PR by [@ndellagrotte](https://github.com/ndellagrotte), Claude Fable 5.2 in [#1](https://github.com/ndellagrotte/clean-caves/pull/1)]*

**Full Changelog**: https://github.com/ndellagrotte/clean-caves/compare/1.0.0...1.1.0

## 1.0.0 - 2026-11-11

### Documentation

- Describe the shipped default presets in the README *[commit by [@ndellagrotte](https://github.com/ndellagrotte), Claude Fable 5.2 in [4be5725](https://github.com/ndellagrotte/clean-caves/commit/4be5725ddcad5fb6b3dea7e07bd6a357d5012301)]*

### Build and Dependencies

- Migrate from Unimined to CleanroomGradle and Conventions *[commit by [@ndellagrotte](https://github.com/ndellagrotte), Claude Fable 5.2 in [97ecc3b](https://github.com/ndellagrotte/clean-caves/commit/97ecc3b3d66fed76d2264365c4cb49c4f897f117)]*

### Other

- Replace default config with rotn config *[commit by [@ndellagrotte](https://github.com/ndellagrotte) in [14c3676](https://github.com/ndellagrotte/clean-caves/commit/14c3676b237dcb3a50c6edbff366ab7d84e0c37c)]*
- Uh *[commit by [@ndellagrotte](https://github.com/ndellagrotte) in [e4aeffa](https://github.com/ndellagrotte/clean-caves/commit/e4aeffaddeb5c13362c0c0044bcd8c9396101ab2)]*
- Oop *[commit by [@ndellagrotte](https://github.com/ndellagrotte) in [0b06f41](https://github.com/ndellagrotte/clean-caves/commit/0b06f41f49fd242e853e88a680fdf7df16232cb1)]*
- Initial commit *[commit by [@ndellagrotte](https://github.com/ndellagrotte) in [48876d3](https://github.com/ndellagrotte/clean-caves/commit/48876d3b5360add6ea71f4c50f9169ecea6b731f)]*


