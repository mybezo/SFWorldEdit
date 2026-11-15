# SFWorldEdit

**English** | [简体中文](README.zh-CN.md)

[![Build CI](https://github.com/balugaq/SFWorldEdit/actions/workflows/build.yml/badge.svg)](https://github.com/balugaq/SFWorldEdit/actions/workflows/build.yml)
[![Compatibility](https://github.com/balugaq/SFWorldEdit/actions/workflows/test-compatibility.yml/badge.svg)](https://github.com/balugaq/SFWorldEdit/actions/workflows/test-compatibility.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A [Slimefun4](https://github.com/SlimefunGuguProject/Slimefun4) addon that works like WorldEdit, but understands **Slimefun blocks**: their block data, their inventories (BlockMenus) and whether they are ticking.

Use it to fill a region with machines, clone a whole Slimefun factory, bulk-edit machine data such as stored energy, or save a build to a file and paste it elsewhere.

## Features

- **Region selection** with a dedicated wand (`SFW_WORLDEDITOR`) or commands, with a particle outline.
- **Paste**: fill the selection with any Slimefun block.
- **Clone**: copy the selection, including Slimefun data, inventories and ticking state.
- **Clear**: remove vanilla blocks, Slimefun data, or both, optionally running each machine's break handler.
- **Bulk data editing**: add or remove Slimefun block data keys, set or audit a BlockMenu slot across the whole region.
- **Save and load** selections as YAML files.
- **Preview mode** (`-prepare` / `-p`): show the result as display entities before touching the world, and move the preview around.
- **Undo** history per player.
- **Throttled editing**: large edits are spread over several seconds, a set number of chunks at a time, to keep the server responsive.
- Bundled translations: **English** (`en-US`) and **Simplified Chinese** (`zh-CN`).

## Requirements

| | |
| --- | --- |
| Server | Paper or Spigot (`api-version: 1.20`). CI tests **1.20.1** and **26.2** |
| Java | **17 or newer** at runtime (the plugin is compiled for Java 21) |
| Required | [Slimefun4](https://github.com/SlimefunGuguProject/Slimefun4) |
| Optional | [GuizhanLibPlugin](https://github.com/ybw0014/GuizhanLibPlugin), only needed for auto-update on `Build` versions |

> [!NOTE]
> Preview mode uses display entities, which need Minecraft **1.19.4 or newer**.

## Installation

1. Download the latest `SFWorldEdit-<version>.jar` from [Releases](https://github.com/balugaq/SFWorldEdit/releases).
2. Put it in your server's `plugins/` folder, next to Slimefun.
3. Restart the server.
4. Optionally, set `language` in `plugins/SFWorldEdit/config.yml` and run `//sreload`.

## Quick start

```text
/sf give <player> SFW_WORLDEDITOR   # get the wand (also listed in the Slimefun guide)
```

1. **Left-click** a block with the wand to set position 1, **right-click** to set position 2.
   You can also stand on a block and run `//spos1` / `//spos2`.
2. Run an operation, for example:

   ```text
   //spaste ELECTRIC_FURNACE -p    # preview the result first
   //spaste ELECTRIC_FURNACE       # place it
   //sundo                         # changed your mind
   ```

## Commands

Every command has a full name (`/sfw<name>`) and a short alias (`//s<name>`).

| Alias | Arguments | Description |
| --- | --- | --- |
| `//shelp` | `[subcommand]` | List all commands, or show the usage of one |
| `//sversion` | | Show the plugin version |
| `//sreload` | | Reload the plugin |
| `//spos1` | | Set position 1 to the block you are standing on |
| `//spos2` | | Set position 2 to the block you are standing on |
| `//sclearpos` | | Clear your selection |
| `//spaste` | `[sfid] [flags]` | Fill the selection with a Slimefun block (uses the item in your hand if `sfid` is omitted) |
| `//sclone` | `[flags]` | Clone the selection to where you are standing |
| `//sclear` | `[flags]` | Clear the blocks in the selection |
| `//sblockinfoadd` | `<key> <value>` | Set a data key on every Slimefun block in the selection |
| `//sblockinforemove` | `<key>` | Remove a data key from every Slimefun block in the selection |
| `//sblockmenuslotset` | `<slot>` | Put the item in your main hand into this slot of every BlockMenu in the selection |
| `//sblockmenuslotfind` | `<slot>` | Count BlockMenus whose slot matches the item in your hand and report the ones that do not |
| `//ssavefile` | | Save the selection to a file |
| `//sloadfile` | `<file_name> [-p]` | Load a saved file at your position |
| `//smove` | `[distance\|cancel]` | Move the preview in the direction you are looking, or remove it |
| `//sclearprojectile` | | Remove every preview entity |
| `//sundo` | | Undo your last region operation (only if `allow-undo: true`) |
| `//srule` | `<rule> <value>` | Change a limit and save it to `config.yml` |
| `//sconfirm` | | Run a pending request that needs confirmation |

### `//spaste` flags

| Flag | Effect |
| --- | --- |
| `-override`, `-o` | Replace Slimefun data that already exists at the target |
| `-force`, `-f` | Skip the `NotPlaceable` check (the machine may not work properly) |
| `-withoutblock`, `-w` | Only write Slimefun data and keep the vanilla blocks |
| `-prepare`, `-p` | Preview only |

```text
//spaste <sfid> -o -f
```

### `//sclone` flags

| Flag | Effect |
| --- | --- |
| `-override`, `-o` | Replace Slimefun blocks that already exist at the target |
| `-prepare`, `-p` | Preview only |

The selection is copied relative to position 1: position 1 lands on the block you are standing on.

### `//sclear` flags

```text
//sclear [-callhandler|-c] [-skipvanilla|-v] [-skipslimefun|-s]
```

| Flag | Effect |
| --- | --- |
| `-callhandler`, `-c` | Call the block's `BlockBreakHandler.onPlayerBreak` first, so the machine can drop its items and clean up |
| `-skipvanilla`, `-v` | Only remove Slimefun data and keep the vanilla blocks |
| `-skipslimefun`, `-s` | Keep Slimefun blocks and only clear vanilla blocks |

`-skipvanilla` and `-skipslimefun` cannot be combined.

### Block data and BlockMenus

```text
//sblockinfoadd <key> <value>     # setData(key, value) on every Slimefun block
//sblockinforemove <key>          # removeData(key)
//sblockmenuslotset <slot>        # put the item in your hand into this slot
//sblockmenuslotfind <slot>       # count menus whose slot matches the item in your hand
```

- Tab completion for `<key>` lists the data keys of the Slimefun block you are looking at (within 8 blocks), plus `energy-charge`. A common use is to change the stored energy or progress of many machines at once.
- `<slot>` ranges from `0` to `53`.
- `//sblockmenuslotfind` sends a clickable chat message that copies a YAML list of the **non-matching** locations to your clipboard. Handy for finding the one machine with the wrong item in its input slot.

### Saving and loading

```text
//ssavefile
//sloadfile <file_name> [-p]
```

- Files are saved as `plugins/SFWorldEdit/clones/clone<timestamp>.yml`.
- Pass the file name without `.yml`, for example `//sloadfile clone1730000000000`. Only plain names are accepted (letters, digits, `.`, `_`, `-`).
- The saved position 1 is placed at your feet.
- Preview mode (`-p`) currently shows vanilla block data only.

### Rules

`//srule` changes a limit at runtime and writes it to `config.yml`. Values must be positive integers.

| Rule | Config key |
| --- | --- |
| `limitBlocks` | `worldedit.modification-block-limit` |
| `limitChunkPerSecond` | `worldedit.modification-chunk-limit-per-second` |

## Permissions

All permissions default to `op`, except `sfworldedit.command.confirm`, which is granted to everyone.

| Permission | Grants |
| --- | --- |
| `sfworldedit.admin` | Every command |
| `sfworldedit.command.admin` | Every command |
| `sfworldedit.command.<name>` | One command, e.g. `sfworldedit.command.paste`, `sfworldedit.command.savefile` |

The console can always run every command.

## Configuration

`plugins/SFWorldEdit/config.yml`:

```yaml
auto-update: true                       # Only used by "Build" versions
language: en-US                         # en-US or zh-CN
worldedit:
  modification-block-limit: 32768       # Max blocks per operation
  modification-chunk-limit-per-second: 16
  allow-undo: true                      # Restart required to change
  max-backups: 20                       # Undo history size per player
config-version: 1
debug: false
```

Language files live in `plugins/SFWorldEdit/lang/`. Missing keys are added from the bundled defaults on startup, but your own edits are never overwritten. To add a language, copy `en-US.yml`, translate it, and set `language` to its file name.

## Building from source

```bash
git clone https://github.com/balugaq/SFWorldEdit.git
cd SFWorldEdit
./gradlew clean build
```

The shaded jar is written to `build/libs/SFWorldEdit-<version>.jar`. You need JDK 21 or newer. Dependencies are declared in `gradle/libs.versions.toml`.

> [!NOTE]
> The Maven repository of MetaLib and DisplayModelLib (`repo.metamechanists.org`) is offline, so both libraries are shipped in `lib/` and shaded into the jar. Do not delete them.

CI (`.github/workflows/`) builds every push and pull request to `main` and starts a real Paper server for each version in the compatibility matrix. Changing `version=` in `gradle.properties` publishes a new release automatically.

## Contributing

Bug reports and suggestions are welcome in [Issues](https://github.com/balugaq/SFWorldEdit/issues). Pull requests should pass `./gradlew clean build`. Player-facing text goes in **both** `lang/en-US.yml` and `lang/zh-CN.yml`. See [AGENTS.md](AGENTS.md) for the project conventions.

## License

[MIT](LICENSE) © balugaq
