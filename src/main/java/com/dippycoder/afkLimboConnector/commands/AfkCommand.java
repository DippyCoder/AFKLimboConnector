package com.dippycoder.afkLimboConnector.commands;

import com.dippycoder.afkLimboConnector.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AfkCommand implements CommandExecutor {

    private final Main plugin;

    public AfkCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getConfig().getString("messages.usage", "§8[§b§lAFK§r§8] §eUsage: /afklimbo reload"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (sender instanceof Player player && !player.hasPermission("afklimbo.reload")) {
                player.sendMessage(plugin.getConfig().getString("messages.no-permission", "§8[§b§lAFK§r§8] §cYou don't have permission."));
                return true;
            }

            plugin.reloadConfig();
            plugin.getAfkManager().reloadConfigValues();
            sender.sendMessage(plugin.getConfig().getString("messages.reload", "§8[§b§lAFK§r§8] §aAfkLimboConnector configuration reloaded!"));
            plugin.getLogger().info("AfkLimboConnector configuration reloaded by " + sender.getName());
            return true;
        }

        sender.sendMessage(plugin.getConfig().getString("messages.usage", "§8[§b§lAFK§r§8] §eUsage: /afklimbo reload"));
        return true;
    }
}
