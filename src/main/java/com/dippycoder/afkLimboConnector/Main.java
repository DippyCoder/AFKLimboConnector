package com.dippycoder.afkLimboConnector;

import com.dippycoder.afkLimboConnector.commands.AfkCommand;
import com.dippycoder.afkLimboConnector.listeners.PlayerListener;
import com.dippycoder.afkLimboConnector.managers.AfkManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private AfkManager afkManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.afkManager = new AfkManager(this);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(afkManager), this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getCommand("afklimbo").setExecutor(new AfkCommand(this));

        afkManager.startAfkChecker();
        sendEnableMessage();
    }

    @Override
    public void onDisable() {
        getLogger().info("AfkLimboConnector disabled.");
    }

    public AfkManager getAfkManager() {
        return afkManager;
    }

    public void sendEnableMessage() {
        getLogger().info("--- AfkLimboConnector ---");
        getLogger().info("");
        getLogger().info("Loaded AFKLC");
        getLogger().info("v.1.1.0 - by DippyCoder");
        getLogger().info("");
        getLogger().info("--- AfkLimboConnector ---");
    }
}