package com.dippycoder.afkLimboConnector.managers;

import com.dippycoder.afkLimboConnector.utils.ServerSender;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class AfkManager {

    private final JavaPlugin plugin;
    private final ServerSender serverSender;

    private final HashMap<UUID, Long> lastActivity = new HashMap<>();
    private final HashMap<UUID, Boolean> wasAfk = new HashMap<>();
    // Players who got the warning but haven't been sent yet — gate for un-AFK sound
    private final Set<UUID> warnedNotSent = new HashSet<>();
    // Repeating warning sound tasks (CONSTANT mode)
    private final HashMap<UUID, BukkitTask> warningSoundTasks = new HashMap<>();

    // --- Config: general ---
    private List<String> defaultLimboServers;
    private int limboFallbackDelayTicks;
    private long warningTime;
    private long actionTime;
    private String actionType;
    private boolean bypassEnabled;
    private String bypassPermission;
    private boolean logActions;

    // --- Config: perm-based limbos ---
    private boolean permLimbosEnabled;
    private List<PermLimbo> permLimbos; // ordered, first match wins

    // --- Config: sounds ---
    private boolean warningSoundEnabled;
    private Sound warningSoundType;
    private float warningSoundVolume;
    private float warningSoundPitch;
    private String warningSoundMode;       // SINGLE | CONSTANT
    private int warningSoundIntervalTicks;

    private boolean unafkSoundEnabled;
    private Sound unafkSoundType;
    private float unafkSoundVolume;
    private float unafkSoundPitch;

    // --- Config: messages ---
    private String msgWarning;
    private String msgTimeout;
    private String msgMove;
    private String msgKick;
    private String msgNoPermission;
    private String msgUsage;
    private String msgReload;

    // -------------------------------------------------------------------------

    public AfkManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.serverSender = new ServerSender(plugin);
        reloadConfigValues();
    }

    public void reloadConfigValues() {
        // Multi-limbo: prefer list key, fall back to legacy single key
        if (plugin.getConfig().isList("limbo-servers")) {
            this.defaultLimboServers = plugin.getConfig().getStringList("limbo-servers");
        } else {
            String single = plugin.getConfig().getString("limbo-server", "limbo");
            this.defaultLimboServers = Collections.singletonList(single);
        }
        if (this.defaultLimboServers.isEmpty()) this.defaultLimboServers = Collections.singletonList("limbo");

        int fallbackSeconds = plugin.getConfig().getInt("limbo-fallback-delay", 5);
        this.limboFallbackDelayTicks = fallbackSeconds * 20;

        this.warningTime      = plugin.getConfig().getLong("warning-time", 180);
        this.actionTime       = plugin.getConfig().getLong("action-time", 210);
        this.actionType       = plugin.getConfig().getString("action-type", "MOVE").toUpperCase();
        this.bypassEnabled    = plugin.getConfig().getBoolean("bypass.enabled", true);
        this.bypassPermission = plugin.getConfig().getString("bypass.permission", "afklimbo.bypass");
        this.logActions       = plugin.getConfig().getBoolean("log-actions", true);

        // Perm-based limbos
        this.permLimbosEnabled = plugin.getConfig().getBoolean("perm-limbos.enabled", false);
        this.permLimbos = new ArrayList<>();
        if (permLimbosEnabled) {
            List<Map<?, ?>> entries = plugin.getConfig().getMapList("perm-limbos.entries");
            for (Map<?, ?> entry : entries) {
                String perm = (String) entry.get("permission");
                Object raw = entry.get("servers");
                List<String> servers = new ArrayList<>();
                if (raw instanceof List<?> list) {
                    for (Object s : list) servers.add(s.toString());
                }
                if (perm != null && !servers.isEmpty()) {
                    permLimbos.add(new PermLimbo(perm, servers));
                }
            }
        }

        // Sounds — warning
        this.warningSoundEnabled       = plugin.getConfig().getBoolean("sounds.warning.enabled", true);
        this.warningSoundMode          = plugin.getConfig().getString("sounds.warning.mode", "SINGLE").toUpperCase();
        this.warningSoundVolume        = (float) plugin.getConfig().getDouble("sounds.warning.volume", 1.0);
        this.warningSoundPitch         = (float) plugin.getConfig().getDouble("sounds.warning.pitch", 1.0);
        this.warningSoundIntervalTicks = plugin.getConfig().getInt("sounds.warning.interval-ticks", 40);
        this.warningSoundType          = parseSound(
                plugin.getConfig().getString("sounds.warning.sound", "BLOCK_NOTE_BLOCK_PLING"),
                Sound.BLOCK_NOTE_BLOCK_PLING);

        // Sounds — un-AFK
        this.unafkSoundEnabled = plugin.getConfig().getBoolean("sounds.unafk.enabled", true);
        this.unafkSoundVolume  = (float) plugin.getConfig().getDouble("sounds.unafk.volume", 1.0);
        this.unafkSoundPitch   = (float) plugin.getConfig().getDouble("sounds.unafk.pitch", 1.2);
        this.unafkSoundType    = parseSound(
                plugin.getConfig().getString("sounds.unafk.sound", "ENTITY_PLAYER_LEVELUP"),
                Sound.ENTITY_PLAYER_LEVELUP);

        // Messages
        this.msgWarning      = plugin.getConfig().getString("messages.warning",       "§8[§b§lAFK§r§8] §eYou have been inactive! You will be moved to limbo soon.");
        this.msgTimeout      = plugin.getConfig().getString("messages.timeout",       "§8[§b§lAFK§r§8] §cYou are being moved to limbo...");
        this.msgMove         = plugin.getConfig().getString("messages.move",          "§8[§b§lAFK§r§8] §aYou are no longer AFK.");
        this.msgKick         = plugin.getConfig().getString("messages.kick",          "§8[§b§lAFK§r§8] §cYou were kicked for being AFK too long!");
        this.msgNoPermission = plugin.getConfig().getString("messages.no-permission", "§8[§b§lAFK§r§8] §cYou don't have permission.");
        this.msgUsage        = plugin.getConfig().getString("messages.usage",         "§8[§b§lAFK§r§8] §eUsage: /afklimbo reload");
        this.msgReload       = plugin.getConfig().getString("messages.reload",        "§8[§b§lAFK§r§8] §aAfkLimboConnector configuration reloaded!");
    }

    // -------------------------------------------------------------------------
    // Activity tracking
    // -------------------------------------------------------------------------

    public void updateActivity(Player player) {
        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());

        if (wasAfk.getOrDefault(uuid, false)) {
            wasAfk.put(uuid, false);
            player.sendMessage(msgMove);

            // Un-AFK sound: only if they were warned but not yet sent to limbo
            if (warnedNotSent.contains(uuid)) {
                stopWarningSoundTask(uuid);
                warnedNotSent.remove(uuid);
                if (unafkSoundEnabled) {
                    player.playSound(player.getLocation(), unafkSoundType, unafkSoundVolume, unafkSoundPitch);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // AFK checker loop
    // -------------------------------------------------------------------------

    public void startAfkChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkPlayers();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void checkPlayers() {
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            if (bypassEnabled && player.hasPermission(bypassPermission)) continue;

            long lastActive      = lastActivity.getOrDefault(uuid, now);
            long inactiveSeconds = (now - lastActive) / 1000;

            // Warning threshold — fire exactly once at the boundary second
            if (inactiveSeconds == warningTime) {
                wasAfk.put(uuid, true);
                warnedNotSent.add(uuid);
                player.sendMessage(msgWarning);
                playWarningSound(player);
            }

            // Action threshold
            if (inactiveSeconds >= actionTime) {
                wasAfk.put(uuid, false);
                stopWarningSoundTask(uuid);
                warnedNotSent.remove(uuid); // sent — no un-AFK sound if they move later

                if (actionType.equals("KICK")) {
                    player.kickPlayer(msgKick);
                    if (logActions)
                        plugin.getLogger().info(player.getName() + " was kicked for being AFK.");
                } else {
                    player.sendMessage(msgTimeout);
                    List<String> servers = resolveServersForPlayer(player);
                    serverSender.sendToServerWithFallback(player, servers, limboFallbackDelayTicks);
                    if (logActions)
                        plugin.getLogger().info(player.getName() + " is AFK — sending to limbo (first: " + servers.get(0) + ")");
                }

                lastActivity.put(uuid, now);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Server resolution
    // -------------------------------------------------------------------------

    /**
     * Returns the limbo server list for this player.
     * Checks perm-limbos first (first matching entry wins), then falls back to default list.
     */
    private List<String> resolveServersForPlayer(Player player) {
        if (permLimbosEnabled) {
            for (PermLimbo entry : permLimbos) {
                if (player.hasPermission(entry.permission())) {
                    return entry.servers();
                }
            }
        }
        return defaultLimboServers;
    }

    // -------------------------------------------------------------------------
    // Sound helpers
    // -------------------------------------------------------------------------

    private void playWarningSound(Player player) {
        if (!warningSoundEnabled) return;

        if (warningSoundMode.equals("CONSTANT")) {
            stopWarningSoundTask(player.getUniqueId()); // cancel any leftover

            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    Player p = Bukkit.getPlayer(player.getUniqueId());
                    if (p == null || !wasAfk.getOrDefault(p.getUniqueId(), false)) {
                        cancel();
                        warningSoundTasks.remove(player.getUniqueId());
                        return;
                    }
                    p.playSound(p.getLocation(), warningSoundType, warningSoundVolume, warningSoundPitch);
                }
            }.runTaskTimer(plugin, 0L, warningSoundIntervalTicks);

            warningSoundTasks.put(player.getUniqueId(), task);
        } else {
            // SINGLE — play once
            player.playSound(player.getLocation(), warningSoundType, warningSoundVolume, warningSoundPitch);
        }
    }

    private void stopWarningSoundTask(UUID uuid) {
        BukkitTask task = warningSoundTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    // -------------------------------------------------------------------------
    // Player lifecycle
    // -------------------------------------------------------------------------

    public void resetPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());
        wasAfk.put(uuid, false);
        warnedNotSent.remove(uuid);
        stopWarningSoundTask(uuid);
    }

    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        lastActivity.remove(uuid);
        wasAfk.remove(uuid);
        warnedNotSent.remove(uuid);
        stopWarningSoundTask(uuid);
    }

    // -------------------------------------------------------------------------
    // Util
    // -------------------------------------------------------------------------

    private Sound parseSound(String name, Sound fallback) {
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown sound '" + name + "', using " + fallback.name());
            return fallback;
        }
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    private record PermLimbo(String permission, List<String> servers) {}
}