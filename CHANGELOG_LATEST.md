The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/) and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).
Prior to version 5.2.0, this project used [Forge Recommended Versioning](https://mcforge.readthedocs.io/en/latest/conventions/versioning/).

This is a copy of the changelog for the most recent version. For the full version history, go [here](https://github.com/TheIllusiveC4/Curios/blob/1.20.x/docs/CHANGELOG.md).

## [5.10.0+1.20.1] - 2024.08.31
### Added
- [API] Added `CuriosTooltip` helper class to build Curios-style tooltips
### Changed
- Slot names without a localization will default to its identifier instead of its localization key
### Fixed
- Fixed slot resizing crash
- Fixed certain valid items being marked as invalid during loading and datapack reloading
- Fixed slot modifiers not being synced when the new inventory size is 0
