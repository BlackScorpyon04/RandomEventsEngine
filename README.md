# Random Events Engine

Make your server lively and unpredictable with fun, chaotic random events 
that trigger automatically. Perfect for content creators, minigames, or 
spicing up survival worlds.

## Features

- 5 built-in events with more coming soon
- Fully configurable presets with adjustable frequency and event weights
- Bossbar countdown and event announcements
- Clean stop/start commands — no server restart needed
- Safe cleanup — mobs, blocks, items, and effects revert automatically

## Events

| Event | Description |
|-------|-------------|
| Hotbar Shuffle | Scrambles players' hotbar slots, optionally restores after a delay |
| Item Rain | Rains configurable items from the sky |
| Mob Ambush | Spawns hostile mobs around players |
| Floor Is Lava | Temporarily turns nearby ground into magma blocks |
| Potion Roulette | Gives everyone random positive or negative effects |

## Commands & Permissions

| Command | Description | Permission |
|---------|-------------|------------|
| `/re start [preset]` | Start the engine with chosen preset | `randomevents.admin` |
| `/re stop` | Stop the engine and clean up active events | `randomevents.admin` |
| `/re list` | List all available presets | everyone |
| `/re events` | List all available event IDs | everyone |
| `/re fire <id> [--force]` | Trigger an event manually | `randomevents.admin` |

## Quickstart

1. Drop the `.jar` into your `plugins` folder and restart your server
2. Edit `config.yml` to adjust event timing, items, and mob counts
3. Run `/re start` to begin the chaos

## Compatibility

- Minecraft: [version]
- API: Spigot/Bukkit [version]

## Configuration

```yaml
# Example config.yml structure
frequency: 30 # seconds between events
preset: default
events:
  hotbar_shuffle:
    weight: 1
    restore_delay: 10
```

## Roadmap

- Additional events coming soon
- Custom event API for developers
