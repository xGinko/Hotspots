# Hotspots

Hotspots are temporary areas created by players (usually at spawn) that other players can teleport to using /hotspot join <name>.
The main purpose of them is to allow a player to challenge everyone else on the server to pvp at a location, without the other players
having to reset their bed spawn point or fly around looking for the challenging player.

### Note

This plugin was a commission for Alacity.net, a cracked anarchy server that has been running on the latest version of minecraft until October 2024.
It was very enjoyable to have worked with them until the end. Thank you daynios and Celestialis for letting me open-source this.

## Compatibility

Hotspots is currently compatible with any version from 1.20 and up, using java 21. 
Depending on its popularity, there might be future releases with backwards compatibility but there are no plans as of now.

## Commands

Admins can reload the plugin and show its version using /hotspots reload and /hotspots version.
Players have access to the following commands:
- /hotspot join <hotspotname/playername> - Joins an existing hotspot
- /hotspot create (bossbarcolor) (hotspotname) - Creates a new hotspot (uses input safe minimessage format)
- /hotspot end (admins can end other player's hotspots) - Ends the player's hotspot early
- /hotspot notifs (on/off) - Toggles hotspot notifications and bossbar

## Permissions

Permissions are pretty self-explanatory imo.

Default player permissions (These are TRUE by default)
hotspots.cmd.end
hotspots.cmd.join
hotspots.cmd.notifs

Non-default player permissions (These are only assigned to operators by default)
hotspots.cmd.create
hotspots.cmd.end.other
hotspots.cmd.reload
hotspots.cmd.version

Bypass permissions (Operators will have them by default)
hotspots.bypass.world-bounds
hotspots.bypass.playtime
hotspots.bypass.create.cooldown
hotspots.bypass.join.cooldown
hotspots.bypass.end.cooldown
hotspots.bypass.notifs.cooldown
hotspots.bypass.confirm.cooldown
