# AFK LIMBO CONNECTOR

Is a minecraft plugin which works with [Velocity](https://papermc.io/downloads/velocity). But it is a [PaperMC](https://papermc.io/downloads/paper) plugin.

**Why does it only work for PaperMC and not Velocity?**
-> It does need Velocity for the sub-servers but the plugin needs to be on the PaperMC server because Velocity does not have Activity Listeners.

**Features:**
- kick / move feature when afk
- warning and action when beeing afk
- bypass permission
- config reload command
- all messages are editable
- editable timings
- afk logging in console
- multi-limbo support (with priority)
- permission-based limbo system (if enabled)
- sounds for warning and un-afking (optional, configurable)

**Config:**
<details>
<summary>Spoiler</summary>


```
# AfkLimboConnector Configuration
# Made by DippyCoder

# Time (in seconds) before sending warning
warning-time: 40

# Time (in seconds) before executing action (kick or move)
action-time: 60

# What happens when player is AFK too long? (MOVE or KICK)
action-type: "MOVE"

# ---------------------------------------------------------------------------
# Multi-Limbo (priority list)
# ---------------------------------------------------------------------------
# Servers are tried in order. If the player is still online after
# limbo-fallback-delay seconds, the next server in the list is tried.
# You can also keep a single entry if you only have one limbo.
limbo-servers:
  - "limbo"
  #- "limbo-1"
  #- "limbo-2"

# How many seconds to wait before trying the next server (default: 5)
limbo-fallback-delay: 5

# ---------------------------------------------------------------------------
# Permission-based limbos
# ---------------------------------------------------------------------------
# When enabled, players with a matching permission are sent to a specific
# server list instead of the default limbo-servers list.
# Entries are checked top-to-bottom — first match wins.
# Can be disabled entirely by setting enabled: false.
perm-limbos:
  enabled: false
  entries:
    - permission: "afklimbo.vip"
      servers:
        - "limbo-vip"
        - "limbo"
    - permission: "afklimbo.premium"
      servers:
        - "limbo-premium"
        - "limbo"

# ---------------------------------------------------------------------------
# Sounds
# ---------------------------------------------------------------------------
sounds:
  #Find all sounds at https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html

  # Warning sound — plays when the AFK warning message is sent
  warning:
    enabled: true
    sound: "BLOCK_NOTE_BLOCK_BELL"
    volume: 1.0
    pitch: 1.0
    # SINGLE: plays once when the warning triggers
    # CONSTANT: repeats every interval-ticks until the player moves or is sent
    mode: "CONSTANT"
    # Only used in CONSTANT mode (in ticks, 20 = 1 second)
    interval-ticks: 3

  # Un-AFK sound — plays when a warned player moves again BEFORE being sent
  # Does NOT play if the player was already sent to limbo
  unafk:
    enabled: true
    sound: "BLOCK_LEVER_CLICK"
    volume: 1.0
    pitch: 1.2

# ---------------------------------------------------------------------------
# Bypass
# ---------------------------------------------------------------------------
bypass:
  enabled: false
  permission: "afklimbo.bypass"

# Should log to console when an action happens?
log-actions: true

# ---------------------------------------------------------------------------
# Messages (§ color codes supported)
# ---------------------------------------------------------------------------
messages:
  warning: "§8[§b§lAFK§r§8] §eYou have been inactive! You will be moved to limbo soon."
  timeout: "§8[§b§lAFK§r§8] §cYou are being moved to limbo..."
  move: "§8[§b§lAFK§r§8] §aYou are no longer AFK."
  kick: "§8[§b§lAFK§r§8] §cYou were kicked for being AFK too long!"
  no-permission: "§8[§b§lAFK§r§8] §cYou don't have permission to use this command!"
  usage: "§8[§b§lAFK§r§8] §eUsage: /afklimbo reload"
  reload: "§8[§b§lAFK§r§8] §aAfkLimboConnector configuration reloaded!"

```


</details>

