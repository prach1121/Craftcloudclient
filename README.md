# Craftcloud Client

![Version](https://img.shields.io/badge/version-1.1.5-blue)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-green)
![Fabric](https://img.shields.io/badge/Fabric-0.18.2%2B-orange)
![License](https://img.shields.io/badge/license-Apache%202.0-red)

A client-side Fabric mod for Minecraft 1.21.11. HUD overlay, a config menu, and
a handful of QoL/PvP-visibility features. No packet manipulation, no
gameplay-affecting cheats — everything here is rendering, input, or local
tracking.

## Features

- HUD rows: FPS, TPS (client-side estimate), ping, clock, biome, light level,
  playtime, distance traveled, XP gained, combat timer, hostile mob count,
  world border distance — each independently toggleable and positionable
  (corner picker), with rolling ping/TPS graphs as an alternative to the
  text rows
- Config menu (Right Shift) with per-tab search
- Hide Crystal/Totem Particles — cosmetic only, drops the particle before
  render; does not touch damage or hitboxes
- Hide Players — rendering skip only; hitboxes, collision, and tab list are
  unaffected
- Auto Sprint, Chat Timestamps, Durability Warning, Low Health Warning
- Stats tracker (hits, placements, kills) with Today/Week/Month/Year/All-Time
  breakdown, and a kill-streak counter
- Waypoints + automatic death markers
- Performance tab: particle/cloud/FPS-cap toggles (standard client settings,
  nothing server-visible)

Config lives in `config/craftcloudclient*.json`.

## Requirements

- JDK 21
- Fabric Loader 0.18.2+ and Fabric API 0.141.4+1.21.11 for 1.21.11 (runtime)

## Building

```
./gradlew build        # Linux/macOS
gradlew.bat build       # Windows
```

Output jar: `build/libs/craftcloudclient-<version>.jar` (ignore the
`-sources` and `-dev` jars). Gradle will pull Minecraft, Yarn mappings,
Fabric Loader, and Fabric API on first run, so you need a network connection
for that step.

To work on it in an IDE, open the project folder in IntelliJ and let it
import the Gradle project.

## How "TPS" works

Vanilla doesn't send clients real server tick timings, so on a remote server
the TPS row is a client-side estimate of tick smoothness (labeled "est."),
not the actual server TPS. On singleplayer/LAN it reads a real 20.0.

## Mixins

`ClientWorldMixin` cancels crystal/totem particle spawns per the config
toggle. `EntityRenderer` is mixed into for the Hide Players culling check.
Both are wired through `craftcloudclient.mixins.json`; Loom handles the
annotation processing, no extra Gradle setup needed.

## Stats detection

- Hits: Fabric API's `AttackEntityCallback` on left-click attacks (does not
  confirm server acceptance)
- Placements (crystal/anchor/obsidian): stack count drop while right-click
  is held (won't register in creative)
- Kills: parsed from vanilla death messages in chat (won't work on servers
  with custom death messages)

## Project layout

```
src/main/java/com/craftcloudclient/client/
  CraftcloudClient.java        entrypoint, keybind + tick/HUD registration
  config/                      ModConfig, HudPosition
  hud/                         HudManager + individual HUD row modules
  gui/                         ConfigScreen, glass-themed widgets
  mixin/                       ClientWorldMixin, EntityRenderer mixin
  performance/                 PerformanceManager
  combat/                      CombatManager
  health/                      LowHealthWarningManager, HungerWarningManager
  items/                       DurabilityWarningManager
  chat/                        ChatTimestamps, GgSender
  update/                      UpdateChecker
  presence/                    Discord IPC / rich presence
  util/                        TickTimeTracker and friends
```

## License

Apache License 2.0 — see [LICENSE](LICENSE) for the full text.
