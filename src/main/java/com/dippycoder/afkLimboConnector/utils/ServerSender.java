package com.dippycoder.afkLimboConnector.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Handles sending players to other servers using BungeeCord.
 * Supports a priority list of servers — if the player is still online after
 * the configured delay, the next server in the list is tried.
 */
public class ServerSender {

    private final JavaPlugin plugin;

    public ServerSender(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Attempts to send the player to the first server in the list.
     * If the player is still online after delayTicks, tries the next one, and so on.
     */
    public void sendToServerWithFallback(Player player, List<String> servers, int delayTicks) {
        if (servers == null || servers.isEmpty()) {
            plugin.getLogger().warning("No limbo servers configured — cannot send " + player.getName());
            return;
        }
        tryServer(player, servers, 0, delayTicks);
    }

    private void tryServer(Player player, List<String> servers, int index, int delayTicks) {
        if (index >= servers.size()) {
            plugin.getLogger().warning("All limbo servers exhausted for " + player.getName() + " — player remains online.");
            return;
        }

        String server = servers.get(index);
        sendRaw(player, server);
        plugin.getLogger().info("Trying to send " + player.getName() + " to " + server + " (attempt " + (index + 1) + "/" + servers.size() + ")");

        // After delayTicks, check if the player is still online.
        // If yes → the server was likely full/offline, try next.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (Bukkit.getPlayer(player.getUniqueId()) != null) {
                // Player still here — previous server didn't take them
                plugin.getLogger().info(player.getName() + " still online after trying " + server + ", trying next...");
                tryServer(player, servers, index + 1, delayTicks);
            }
            // else: player disconnected from this server (switched) — success
        }, delayTicks);
    }

    /** Single direct send, no fallback. */
    public void sendToServer(Player player, String serverName) {
        sendRaw(player, serverName);
    }

    private void sendRaw(Player player, String serverName) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send " + player.getName() + " to " + serverName + ": " + e.getMessage());
        }
    }
}