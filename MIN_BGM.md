# min_bgm Region Music

## Overview

The plugin assigns a resource-pack sound to an X/Z region. When a player enters
that region, the plugin requests playback of the assigned sound. When the player
leaves, changes region, or the region's sound changes, the previous custom sound
is stopped before the next one is requested.

Regions are scoped to the world where they were created. Their bounds include
both corner coordinates. If regions overlap, the smallest matching region takes
priority. The Y coordinate is not considered.

## Requirements

- The resource pack must define the sound in `assets/<namespace>/sounds.json` and
  provide its audio file.
- Players must have the resource pack enabled to hear custom sounds.
- Commands must be run by an in-game player with the
  `papermcplugintemplate.regionmusic` permission. This permission defaults to OP.
- Region names must be 1-32 characters containing only ASCII letters, numbers,
  `_`, or `-`.

The plugin does not distribute or force-accept a resource pack, and it cannot
guarantee that a client actually played or heard a requested sound. It also does
not disable Minecraft's built-in background music; that requires a resource pack
that replaces the relevant vanilla tracks or a client-side mod.

## Commands

All commands use `/regionmusic` and can only be run by an in-game player.

### Add a region

```text
/regionmusic add <x1> <z1> <x2> <z2> <name>
```

Creates a named region in the command sender's current world. The coordinate
corners can be provided in either order.

Example:

```text
/regionmusic add 100 200 150 250 forest
```

### Assign or change a region's song

```text
/regionmusic song <name> <namespace:sound>
```

The sound key must match an entry registered by the resource pack's
`sounds.json`. Assigning a new sound updates players currently inside the region.

Example:

```text
/regionmusic song forest min_bgm:forest_theme
```

### Move a region

```text
/regionmusic move <name> <x1> <z1> <x2> <z2>
```

Changes the bounds while keeping the region's world and assigned sound.

### Delete a region

```text
/regionmusic delete <name>
```

Deletes the region and its song assignment. Any active custom song from that
region is stopped for affected online players.

## Storage

Region names, world names, coordinates, and sound keys are stored in
`plugins/PapermcPluginTemplate/regions.yml` under the server directory. The file
is created or updated when regions are changed.

## Logs

Plugin logs use these prefixes in the server console and `logs/latest.log`:

- `[min_bgm INFO]` for startup, region operations, configuration loading, and
  playback/stop requests.
- `[min_bgm WARNING]` for invalid commands, rejected settings, or invalid saved
  region entries.
- `[min_bgm SEVERE]` for plugin initialization or region file save failures.

Playback log entries confirm that the server requested a sound for a player;
they do not confirm resource-pack acceptance or successful client playback.

## Development server and tests

From the repository root, build and run the local Paper server with:

```bash
./gradlew :app:runDevServer
```

Run the project's automated tests with:

```bash
./gradlew :app:test
```