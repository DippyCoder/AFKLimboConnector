package com.dippycoder.afkLimboConnector.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles sending players to other servers using BungeeCord.
 */
public class ServerSender {

    private final JavaPlugin plugin;

    public ServerSender(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendToServer(Player player, String serverName) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send player " + player.getName() + " to server " + serverName + ": " + e.getMessage());
        }
    }
}