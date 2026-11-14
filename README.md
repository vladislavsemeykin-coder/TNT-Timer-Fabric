# TNT Timer v1.1 — Minecraft Java 1.21.4 / Fabric

A **client-only** mod displaying a movable HUD with a separate TNT countdown for every primed TNT entity within **100 blocks** of the player.

## Features
- Countdown displayed in seconds with **one decimal place**, e.g. `9,2 с`.
- Distance per TNT and green/yellow/red countdown coloring.
- Normal compact HUD overlay; appears when at least one primed TNT entity is within range.
- **Right Shift** opens an editor with a sample HUD. **Left-click and drag** the panel. **Esc** saves and closes.
- Panel position persists at `config/tnttimer.properties`.
- Client-only: no server mod needed; cannot detect TNT entities the server hasn't sent to the client.
- Supports at most 10 listed timers at once, plus remaining TNT count.

## Build
Requires Java **21** and Gradle compatible with Fabric Loom 1.9. From the project root run:

```sh
gradle build
```

The mod JAR will be in `build/libs/`. Place it in `.minecraft/mods` alongside Fabric API for Minecraft 1.21.4.

## Technical detail
The timer uses the client-visible TNT entity fuse in ticks, divided by 20, and render tick delta to reduce stepping. It is an **estimate** rather than a server-authoritative real-time explosion clock: lag, server TPS changes, and entity tracking distance can affect the display. Vanilla TNT starts at about 4 seconds; 9.2 seconds is an example of longer custom fuses / sample preview.

## Notes
The project includes source files but has not been compiled or run in Minecraft in this environment.
