package com.dippycoder.afkLimboConnector.managers;

import com.dippycoder.afkLimboConnector.utils.ServerSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class AfkManager {

    private final JavaPlugin plugin;
    private final ServerSender serverSender;

    private final HashMap<UUID, Long> lastActivity = new HashMap<>();
    private final HashMap<UUID, Boolean> wasAfk = new HashMap<>();

    // Config values
    private String limboServer;
    private long warningTime;
    private long actionTime;
    private String actionType;
    private boolean bypassEnabled;
    private String bypassPermission;
    private boolean logActions;

    // Messages
    private String msgWarning;
    private String msgTimeout;
    private String msgMove;
    private String msgKick;
    private String msgNoPermission;
    private String msgUsage;
    private String msgReload;

    public AfkManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.serverSender = new ServerSender(plugin);
        reloadConfigValues();
    }

    public void reloadConfigValues() {
        this.limboServer = plugin.getConfig().getString("limbo-server", "limbo");
        this.warningTime = plugin.getConfig().getLong("warning-time", 180);
        this.actionTime = plugin.getConfig().getLong("action-time", 210);
        this.actionType = plugin.getConfig().getString("action-type", "MOVE").toUpperCase();
        this.bypassEnabled = plugin.getConfig().getBoolean("bypass.enabled", true);
        this.bypassPermission = plugin.getConfig().getString("bypass.permission", "afklimbo.bypass");
        this.logActions = plugin.getConfig().getBoolean("log-actions", true);

        this.msgWarning = plugin.getConfig().getString("messages.warning", "§8[§b§lAFK§r§8] §eYou have been inactive! You will be moved to limbo soon.");
        this.msgTimeout = plugin.getConfig().getString("messages.timeout", "§8[§b§lAFK§r§8] §cYou are being moved to limbo...");
        this.msgMove = plugin.getConfig().getString("messages.move", "§8[§b§lAFK§r§8] §aYou are no longer AFK.");
        this.msgKick = plugin.getConfig().getString("messages.kick", "§8[§b§lAFK§r§8] §cYou were kicked for being AFK too long!");
        this.msgNoPermission = plugin.getConfig().getString("messages.no-permission", "§8[§b§lAFK§r§8] §cYou don't have permission.");
        this.msgUsage = plugin.getConfig().getString("messages.usage", "§8[§b§lAFK§r§8] §eUsage: /afklimbo reload");
        this.msgReload = plugin.getConfig().getString("messages.reload", "§8[§b§lAFK§r§8] §aAfkLimboConnector configuration reloaded!");
    }

    public void updateActivity(Player player) {
        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());

        if (wasAfk.getOrDefault(uuid, false)) {
            wasAfk.put(uuid, false);
            player.sendMessage(msgMove);
        }
    }

    public void startAfkChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkPlayers();
            }
        }.runTaskTimer(plugin, 20, 20);
    }

    private void checkPlayers() {
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            if (bypassEnabled && player.hasPermission(bypassPermission)) {
                continue;
            }

            long lastActive = lastActivity.getOrDefault(uuid, now);
            long inactiveSeconds = (now - lastActive) / 1000;

            if (inactiveSeconds == warningTime) {
                player.sendMessage(msgWarning);
                wasAfk.put(uuid, true);
            }

            if (inactiveSeconds >= actionTime) {
                wasAfk.put(uuid, false);

                if (actionType.equals("KICK")) {
                    player.kickPlayer(msgKick);
                    if (logActions)
                        plugin.getLogger().info(player.getName() + " was kicked for being AFK too long.");
                } else {
                    player.sendMessage(msgTimeout);
                    serverSender.sendToServer(player, limboServer);
                    if (logActions)
                        plugin.getLogger().info(player.getName() + " was AFK too long - moved to server: " + limboServer);
                }

                lastActivity.put(uuid, now);
            }
        }
    }

    public void resetPlayer(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        wasAfk.put(player.getUniqueId(), false);
    }

    public void removePlayer(Player player) {
        lastActivity.remove(player.getUniqueId());
        wasAfk.remove(player.getUniqueId());
    }
}