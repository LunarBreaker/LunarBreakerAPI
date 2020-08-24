package com.lunarbreaker.api.command;

import com.lunarbreaker.api.LunarBreakerAPI;
import com.lunarbreaker.api.object.CBNotification;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class CBCCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if(!sender.hasPermission("clientapi.check")) {
            sender.sendMessage(ChatColor.RED + "No permissions.");
            return false;
        }

        if(args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + s + " <time> <message>");
        }else {
            String message = StringUtils.join(args, ' ', 1, args.length);

            if(!StringUtils.isNumeric(args[0])) {
                sender.sendMessage(ChatColor.RED + "'" + args[0] + "'" + " is not a number.");
                return false;
            }

            if(Integer.parseInt(args[0]) > 30) {
                sender.sendMessage(ChatColor.RED + "You cannot make an announcement last for longer than 30 seconds");
                return false;
            }

            for(Player player : Bukkit.getOnlinePlayers()) {
                LunarBreakerAPI.getInstance().sendNotification(player, new CBNotification(message, Integer.parseInt(args[0]), TimeUnit.SECONDS));
            }
        }
        return false;
    }

}