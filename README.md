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

**Config:**
<details>
<summary>Spoiler</summary>


```
# AfkLimboConnector Configuration

# Time (in seconds) before sending warning
warning-time: 20

# Time (in seconds) before executing action (kick or move)
action-time: 40

# What happens when player is AFK too long? (MOVE or KICK)
action-type: "MOVE"

# Server to send AFK players to (only used if action-type = MOVE)
limbo-server: "limbo"

# Should log to console when an action happens?
log-actions: true

# Bypass settings 
bypass:
  enabled: false
  permission: "afklimbo.bypass"

# Messages (you can use color codes with §)
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

