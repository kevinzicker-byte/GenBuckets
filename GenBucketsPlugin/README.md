# GenBuckets

Custom Paper plugin project for a factions server.

## Included features
- `/gen` opens the GUI
- `/gen help`
- `/gen reload`
- `/gen give <player> <bucket> [amount]`
- `/gen list`
- `/gen info <bucket>`
- `/gen debug`
- Chain gens
- Prevent double anchors
- Early-stop anchor replacement
- Config-driven bucket list
- Horizontal length and vertical min/max are easy to edit in `config.yml`
- Sand uses real sand only (no sandstone)

## Current bucket IDs
- cobble_vertical
- cobble_horizontal
- obby_vertical
- obby_horizontal
- sand_vertical
- netherrack_horizontal
- lava_vertical_down

## Build
This project is set up for Gradle + Java 21.

Typical build command:

```bash
./gradlew build
```

The jar should end up in:

```text
build/libs/GenBuckets-1.0.0.jar
```

## Notes
This container did not have the Paper API or Gradle wrapper available, so the source project is included ready to build, but the jar was not compiled here.
