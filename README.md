# Deepslate Instamine

Mine deepslate at stone speed with a **netherite pickaxe, Efficiency V and Haste II**.

A small Fabric mod for **Minecraft 26.3**. Install it on **both the server and every client**, or in your client for singleplayer.

[Download](https://github.com/Sqeedys/DeepslateInstamine/releases/latest) · [Testing](TESTING.md) · [Changelog](CHANGELOG.md)

By [Sqeedys](https://www.youtube.com/@Sqeedys). MIT licensed.

## How it works

When all three requirements are met, ordinary deepslate uses Minecraft's own stone mining-progress calculation. With normal grounded mining conditions, this lets you break it instantly.

| Requirement | Needed |
| --- | --- |
| Main-hand tool | Vanilla netherite pickaxe |
| Enchantment | Efficiency V or higher |
| Effect | Haste II or higher |

Mining Fatigue, being airborne and underwater penalties still matter. This mod does not guarantee instant breaking under those conditions. Missing any requirement leaves deepslate's normal progress unchanged.

Only ordinary `minecraft:deepslate` is included by default. Deepslate ores, cobbled deepslate, bricks and tiles are unaffected unless you opt them in with a datapack.

## Install

1. Use Minecraft **26.3** with **Java 25**.
2. Install **Fabric Loader 0.20.1** and **Fabric API 0.165.2+26.3**.
3. Put `deepslate-instamine-1.1.0.jar` in the `mods` folder on the server **and each client**. For singleplayer, install it in your client.
4. Restart Minecraft and the server. Replace any older version of this mod; do not keep both JARs.

No commands, permission manager or configuration file is required. Players use their normal tools and beacon/potion effects. Clients without the mod do not have matching local mining calculations; a server-only installation is not supported.

## Customize the blocks

The block tag is `deepslate_instamine:instant_mineable_deepslate`.

In a Minecraft 26.3 datapack, create:

```text
data/deepslate_instamine/tags/block/instant_mineable_deepslate.json
```

For example, to add cobbled deepslate:

```json
{
  "replace": false,
  "values": ["minecraft:cobbled_deepslate"]
}
```

Use `"replace": true` with an empty `values` array to disable the selected blocks. The tool must still be correct for the actual target. Accidentally tagging stone does not recurse, and an unbreakable or otherwise nonpositive-progress result is left unchanged.

Restart after installing the datapack so both sides receive the tags. Review the balance impact before adding ores or other blocks.

## What stays vanilla

The mod changes a mining-progress return value; it does not remove blocks itself, edit worlds on startup or register custom items. Minecraft's normal breaking pipeline continues to handle game-mode restrictions, drops, durability and break events. The mod makes no network requests and collects no telemetry.

Other mods that change the same progress calculation can interact with this one. Universal mod compatibility and actual multiplayer client behavior are not claimed; see [the test record](TESTING.md).

## Build

Install JDK 25 and set `JAVA_HOME`, then run:

```powershell
.\gradlew.bat --no-daemon build
```

On Linux/macOS:

```sh
./gradlew --no-daemon build
```

Installable output: `build/libs/deepslate-instamine-1.1.0.jar`. The `-sources.jar` is for developers, not installation.

The build pins Gradle 9.6.1 and Fabric Loom 1.17.21. The wrapper distribution has a SHA-256 pin. Minecraft, Fabric API and Loader are separate dependencies and are not bundled in the mod. See [third-party notices](THIRD_PARTY.md).

## Report a problem

Include your Minecraft/Fabric versions, other mining-related mods, tool/enchantments/effects, and reproduction steps with a disposable world. Redact player details, addresses, credentials and private paths from any log excerpts. Do not upload worlds or full server folders.

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
