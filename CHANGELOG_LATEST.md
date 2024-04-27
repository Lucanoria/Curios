The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).
Prior to version 5.2.0, this project used [Forge Recommended Versioning](https://mcforge.readthedocs.io/en/latest/conventions/versioning/).

This is a copy of the changelog for the most recent version. For the full version history, go [here](https://github.com/TheIllusiveC4/Curios/blob/1.20.x/docs/CHANGELOG.md).

## [5.9.0+1.20.1] - 2024.04.27
### Added
- Added `enableLegacyMenu` configuration option to `curios-server.toml` to opt-out from the new screen to the old screen
- [API] Added `CuriosApi#getCurioPredicates`
### Changed
- Changed default Curios GUI to the new interface introduced in 5.8.0
- New interface no longer shifts the screen to the right
- Scrolling through pages in the new interface is twice as fast
- Lowered the maximum value of `maxSlotsPerPage` configuration option from 64 to 48
### Fixed
- Fixed generic curio slots from failing validation checks when only those slots exist on an entity [#402](https://github.com/TheIllusiveC4/Curios/issues/402)
### Removed
- Removed `enableExperimentalMenu` configuration option
