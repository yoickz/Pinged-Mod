**Pinged**

A lightweight and tactical client-side Fabric mod for Minecraft 1.21.11

This mod brings responsive in-world location and player pinging to any multiplayer server without requiring server-side plugins.

## Features

- **Location Pinging:** Ping any targeted block or terrain up to 512 blocks away with `V`. Plays a crisp chime, prints floored coordinates to your chat, and places an in-world pin with your player head and live distance.
- **Targeted Player Pinging:** Ping a player you are targeting to highlight them with a distinct red marker and announce `Pinged <Player>!` in chat.
- **Smart Ping Sharing:** Automatically share pings with teammates on your ping list (`/pinged add <player>`). Teammates with the mod see your visual in-world marker, while unmodded teammates receive clean coordinate messages in chat.
- **Screen Edge Clamping:** Markers remain visible when off-screen or behind your camera by clamping cleanly to your display borders, keeping objectives in sight across the map.
- **Configurable Durations:** Adjust how long pings stay on screen (5s, 10s, 15s, 20s, 30s, or 60s) directly through ModMenu or using `/pinged config`.
- **Clean Persistence:** Your friend list and duration preferences are saved automatically across game restarts.

## Commands

- `/pinged add <player>` - Add a player to your ping list so they receive your pings.
- `/pinged remove <player>` - Remove a player from your ping list.
- `/pinged list` - View all players currently on your ping list.
- `/pinged clear` - Clear all active pings from your screen immediately.
- `/pinged config` - Open the settings screen to adjust ping duration.

## Keybinds

- `V` - Ping targeted location or player (customizable in Options > Controls > Key Binds > Pinged).

## Requirements

- Minecraft 1.21.x
- [Fabric Loader](https://fabricmc.net/) (0.16.0 or higher)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [ModMenu](https://modrinth.com/mod/modmenu) to configure ping duration from the pause menu.
