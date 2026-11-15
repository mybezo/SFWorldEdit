# AGENTS.md — SFWorldEdit Operating Charter

> This file is the **working contract** between AI coding assistants and the developers of this repository. Every change (code, build, docs) must follow it. When this file and the code disagree, the code wins and this file must be updated to match.

## TL;DR

1. This is a **Paper/Spigot + Slimefun4 addon** (Gradle Kotlin DSL, compiled with Java 21, runs on Java ≥ 17). The default branch is `main`. A change must pass `./gradlew clean build` before it is committed.
2. Player-facing text always goes through `lang/*.yml` + `LocalizationService`; logging always goes through `Debug.*`. **Never** use `System.out.println`, **never** change Slimefun IDs such as `SFW_WORLDEDITOR`, **never** `git push -f`, **never** bypass a failing build (`-x test` / `--offline`).
3. Dependencies are declared only in `gradle/libs.versions.toml`. `MetaLib` / `DisplayModelLib` come from `lib/*.jar` because their source repository is offline; do not delete them.
4. Committing is the developer's call. The assistant leaves the working tree in a reviewable state and does not commit, push, or rewrite git history.

---

## 1. Project identity and scope

| Aspect | Fact |
| --- | --- |
| Purpose | A **region editor** for Slimefun blocks and BlockData: selection, bulk place/clone/clear, BlockMenu slot read/write, undo, YAML save/load and display-entity previews |
| Main class | `com.balugaq.sfworldedit.implementation.SFWorldedit` → `extends ISFWorldEdit implements Listener`; `ISFWorldEdit extends JavaPlugin implements SlimefunAddon` |
| Plugin name | `SFWorldEdit` (`plugin.yml`), `api-version: 1.20`, `depend: [Slimefun]`, `softdepend: [GuizhanLibPlugin]` |
| Coordinates | group `com.balugaq.sfworldedit`; the version comes from `version=` in `gradle.properties` (currently 51) |
| Version targets | Source needs at least Java 16 (records / `Stream.toList()` / text blocks / pattern matching); runtime gate Java ≥ 17; compile and CI use Java 21; compatibility matrix `1.20.1`, `26.2` |
| Repository | `github.com/balugaq/SFWorldEdit`, default branch `main` |
| AI responsibilities | Feature code, bug fixes, build/CI maintenance, documentation |
| Not the AI's job | Releasing versions, pushing to the remote, rewriting git history |

---

## 2. Package map (find the right place before editing)

| Package | Responsibility | Key classes |
| --- | --- | --- |
| `api/plugin` | Plugin base class | `ISFWorldEdit` (`getString` / `send` / `sendList`) |
| `api/objects` | Command and request abstractions | `SubCommand` (`TabExecutor`), `CachedRequest`, `enums.MinecraftVersion` (record), `enums.Facing` |
| `api/data` | Data model | `Content` (abstract + `action()`), `BukkitContent`, `SFContent`, `ChunkData` (record), `Backup` (undo stack), `Language` |
| `core/commands` | 20 command implementations | `PasteCommand`, `ClearCommand`, `CloneCommand`, `SaveFileCommand`, `LoadFileCommand`, `UndoCommand`, ... |
| `core/managers` | Lifecycle managers | `IManager` (`onLoad`/`onUnload`), `CommandManager`, `ConfigManager`, `DisplayManager` |
| `core/services` | Localization | `LocalizationService` |
| `implementation` | Wiring and Slimefun items | `SFWorldedit`, `SFWItemsSetup`, `slimefun.SFWItemGroups` / `SFWItems` / `SFWRecipes` / `items.SFWorleditor` |
| `utils` | Utilities | `WorldUtils` (editing core), `Debug`, `Constants`, `ReflectionUtil`, `ClipboardUtil`, `YamlWriter`, `TextUtil` |

Items are registered in `SFWItemsSetup.setup()`. The `SFW_WORLDEDITOR` wand sets pos1 on left click (`ToolUseHandler`) and pos2 on right click (`ItemUseHandler`); its recipe type is `RecipeType.NULL`.

---

## 3. Tech stack and build

- **Version detection**: `MinecraftVersion` uses the `record (major, minor, patch)` implementation from NetworksExpansion. It parses the `Bukkit.getMinecraftVersion()` string instead of enumerating constants, so 26.x is recognized out of the box. Conventional constants are `V1_20` / `V1_20_5` / `V1_21` / `V1_21_3` / `V1_21_4`; `UNKNOWN` = `(999,999,999)`. Use `current()`, `isAtLeast(...)` / `isBefore(...)`, `humanize()`; compare with UNKNOWN using `equals(UNKNOWN)` (records must not be compared with `==`).
- **Build**: `build.gradle.kts` (Gradle Kotlin DSL) + `com.gradleup.shadow`; `defaultTasks("clean", "build")`. Output is `build/libs/SFWorldEdit-<version>.jar` (fat jar, relocation `org.bstats` → `com.balugaq.sfworldedit.bstats`, `minimize()`, `META-INF/*` excluded).
- **Dependencies**: all declared in `gradle/libs.versions.toml`; `build.gradle.kts` only references `libs.xxx`. `compileOnly` replaces Maven's `provided`, `implementation` replaces the shaded `compile`.
- **Version injection**: `processResources { filesMatching("plugin.yml") { expand("version" to project.version.toString()) } }`, so `plugin.yml` must contain `version: ${version}`. **Do not change it back to Maven's `${project.version}`**: the jar would contain the literal string, Bukkit would fail to load it, and the compatibility workflow (which reads the version from the jar) would break.
- **Local JARs**: the repository of `org.metamechanists:MetaLib:46` and `DisplayModelLib:3` (`repo.metamechanists.org`) returns 404 for everything, so both come from `implementation(fileTree("lib"))`. `.gitignore` has a `!lib/*.jar` exception; do not delete these files.
- **Repositories**: `mavenCentral()`, `repo.papermc.io`, `jitpack.io`, `hub.spigotmc.org`. `net.guizhanss:GuizhanLibPlugin` is on **Maven Central** and needs no extra repository.
- **CI**: `.github/workflows/build.yml` (JDK 21, `./gradlew test` + `clean build`), `release.yml` (publishes a Release when the version in `gradle.properties` changes), `test-compatibility.yml` (matrix `1.20.1,26.2`, JDK 21/25 preinstalled, chosen from Paper API's `java.version.minimum`). Start a commit message with `[ci skip]` to skip the build.
- **Dependabot**: Gradle ecosystem, `monthly`.
- **gradlew** must keep its executable bit (`100755`); the workflows also `chmod +x` it defensively.

---

## 4. Startup order (do not reorder)

`SFWorldedit.onEnable()`:

1. `Preconditions.checkArgument(instance == null)` → `instance = this`
2. `saveDefaultConfig()` → `new ConfigManager(this)` → `configManager.onLoad()`
3. `new LocalizationService(this)` → `addLanguage(configManager.getLanguage())` (skipped when it equals the default) → `addLanguage(DEFAULT_LANGUAGE="en-US")`
4. `environmentCheck()`: `MinecraftVersion.current()` parses `Bukkit.getMinecraftVersion()`; MC below 1.16 or `javaVersion < 17` → `disablePlugin(this)` and `return`; a parse error falls back to `UNKNOWN` and only warns (`UNKNOWN` is defined as the highest version and does not block startup)
5. `tryUpdate()`: calls `GuizhanUpdater.start(...)` only when `configManager.isAutoUpdate()` and `getDescription().getVersion().startsWith("Build")` (version `51` does not trigger it)
6. `SFWItemsSetup.setup(this)` — triggers static initialization of `Constants`, which needs `getInstance()` and the localization service
7. `new CommandManager(this)` → `onLoad()` → `registerCommands()`
8. `new DisplayManager(this)` → `onLoad()`
9. `Debug.info(getString("messages.startup.done"))`

`onDisable()`: `SlimefunItemUtil.unregisterAllItems()` → `displayManager.onUnload()` → `commandManager.onUnload()` (also cancels the particle task) → **fetch the goodbye message first** → `localizationService = null` → clear version fields → `configManager.onUnload()` → `Debug.info(message)` → `instance = null`. `Debug.*` depends on `getInstance()`, so the goodbye log must be printed before `instance = null`.

---

## 5. Decision tree: when you hit A, do B

### 5.1 Adding or changing a command

- Create a `SubCommand` subclass in `core/commands/`; `getKey()` returns the key matching `sfw<key>` in `plugin.yml`.
- Register it in `CommandManager.onLoad()`. If it depends on a config switch (e.g. `UndoCommand` is only registered when `allow-undo: true`), follow the existing conditional.
- Add the command, its alias (with a leading `/`) and the permission node `sfworldedit.command.<key>` to `plugin.yml`.
- Add a `messages.command.help.usage.<key>` entry to both language files; `HelpCommand` matches keys case-insensitively.
- **Note**: the KEYs of `SaveFileCommand` / `LoadFileCommand` are `saveFile` / `loadFile` (capital F), while `plugin.yml` declares them in lower case. `PermissionUtil` builds the permission with `key.toLowerCase()`; do not "tidy up" the casing.
- Bukkit always passes at least one (possibly empty) argument to `onTabComplete`; never branch on `args.length == 0`.

### 5.2 Messages and language files

- Player-facing text goes into **both** `src/main/resources/lang/en-US.yml` and `zh-CN.yml` and is read through `LocalizationService`. **Never** hard-code text in Java.
- `send()` / `sendMessage()` **automatically prepend `messages.`**, so callers pass relative keys (e.g. `"error.no-permission"`). `getString(path)` and `sendList(...)` do **not** add a prefix; callers pass the full path (e.g. `"messages.startup.done"`).
- Placeholders are `MessageFormat` style `{0}` / `{1}`; colors use `ChatColor` codes like `&a`. `${...}` is a Gradle placeholder and belongs only in `plugin.yml`.
- On load, `Language` **adds** keys missing from the user's file using the jar defaults but never overwrites existing values. Both language files must keep the same key set.

### 5.3 Configuration

- Add new options to `src/main/resources/config.yml` and a getter with a default in `ConfigManager` (e.g. `getInt(path, default)`). **Never** scatter `getConfig().getXxx(...)` magic strings in business code.
- Numeric limits are clamped to at least 1 in `ConfigManager`; `RuleCommand` (`setConfig(path, value)`) rejects values below 1. Keep both guards: a chunk limit of 0 would divide by zero in `WorldUtils`.
- `ConfigManager.setupDefaultConfig()` only adds missing keys and never overwrites values; `config-version` is currently written but never read.

### 5.4 Logging and exceptions

- Regular logging always uses `Debug.info/warning/severe` (backed by `SFWorldedit.getInstance().getLogger()`).
- Debug output uses `Debug.debug(...)`, controlled by the `debug` switch in `config.yml`.
- Exceptions use `Debug.trace(e)` (logs through the plugin logger). Four bare `e.printStackTrace()` calls remain in `ReflectionUtil` / `YamlWriter`; new code must not add more.
- `Debug.*` depends on the static `instance`; do not call it before the plugin is enabled or after it is disabled.

### 5.5 World editing core

- `WorldUtils.doWorldEdit()` groups by `ChunkData`, batches with `runTaskLater` according to `modification-chunk-limit-per-second`, and collects `Content` backups when `allow-undo` is on. Understand all three parts before changing the batching.
- Access persisted data of registered Slimefun blocks through `StorageCacheUtils` / `SlimefunBlockData`; never bypass the storage layer.
- The static block of `WorldUtils` uses `ReflectionUtil` to reach the private `data` / `position` / `world` / `weakWorld` fields of `CraftBlockState` by name, with a `success` flag as a fallback. This is a **runtime coupling to CraftBukkit internals**: when adding support for a new MC version, verify the reflection still resolves. There is no compile-time NMS dependency.
- `LoadFileCommand.deserializeContent` temporarily changes the block at the saved location to build a `BlockState` and restores the original state in a `finally` block. Keep that restore.
- `CommandManager.selection` is read by an async particle task; keep it a `ConcurrentHashMap`.
- `ClipboardUtil` uses `player.spigot().sendMessage(...)` (BungeeChat), not Adventure; keep that in mind when switching APIs.
- Preview entities use `org.bukkit.entity.Display` / `BlockDisplay` (1.19.4+).

### 5.6 Dependencies and build

- New dependency: first confirm the coordinates resolve (POM/metadata returns 200), then add it to `libs.versions.toml`, reference it as `libs.xxx` in `build.gradle.kts`, and run `./gradlew clean build`.
- Before upgrading a third-party library, check the signatures you actually call with `javap` (precedent: `GuizhanUpdater.start(Plugin, File, String, String, String)` is identical in 1.7.6 and 2.5.0).
- Distinguish `compileOnly` (provided by the server) from `implementation` (shaded into the jar). `bstats` has no source references and is removed by `minimize()` after shading; it can be dropped safely, but do not do so unasked.
- When the build fails, read the error and locate the dependency / compile / shadow conflict. **Never** skip with `-x test` / `--offline`.

---

## 6. Acceptance

1. `./gradlew clean build` passes; the output is in `build/libs/`.
2. `plugin.yml` inside the jar has a real `version`, not `${...}`.
3. The CI compatibility tests (`1.20.1` and `26.2`) log `Enabling SFWorldEdit`, and `Disabling` does not appear before `Done (`.
4. `git status --short` shows no unexpected changes; nothing is committed or pushed.
5. Visual / style changes are confirmed by the developer; the assistant must not launch a browser or rendering tool to verify them.

---

## 7. Red lines

- Do not change Slimefun item IDs (especially `SFW_WORLDEDITOR`) or `depend` / `api-version` in `plugin.yml` unless the developer explicitly asks.
- Do not log with `System.out.println`; do not swallow exceptions.
- Do not reorder initialization and teardown in `onEnable` / `onDisable` on your own.
- Do not delete `lib/*.jar` or the Gradle wrapper, and do not write a local `org.gradle.java.home` into `gradle.properties`.
- Do not commit, push, `git push -f` or rewrite git history.

---

## 8. Known debt (be aware of it, do not "fix it in passing")

- The auto-update `branch` in the `SFWorldedit` constructor is `"master"` while the default branch is `main`. Version `51` does not start with `Build`, so `tryUpdate()` never runs; harmless for now.
- `bstats` has no source references; `MetaLib` has no direct import (it is shaded as a transitive dependency of DisplayModelLib).
- Four `e.printStackTrace()` calls remain (`ReflectionUtil` ×3, `YamlWriter` ×1).
- `WorldUtils.ALLOW_UNDO` and `Backup.MAX_BACKUP_SIZE` are read once in static initializers, so changing `allow-undo` / `max-backups` needs a server restart, not just `//sreload`.
- `ConfigManager.saveAll()` is unused.
