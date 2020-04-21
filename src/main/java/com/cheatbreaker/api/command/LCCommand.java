package com.cheatbreaker.api.command;

import com.cheatbreaker.api.CheatBreakerAPI;
import net.evilblock.stark.Stark;
import net.evilblock.stark.core.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LCCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players.");
            return false;
        }

        Player p = (Player) sender;

        if(args.length == 0) {
            if(!CheatBreakerAPI.getInstance().isRunningLunarClient(p)) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c" + p.getDisguisedName() + " &cis NOT protected by Lunar Client."));
            }else {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a" + p.getDisguisedName() + " &ais currently protected by Lunar Client."));
            }
        }else {
            Player target = Bukkit.getPlayer(args[0]);

            if(target == null) {
                p.sendMessage(ChatColor.RED + "That player was not found.");
                return false;
            }

            if(!CheatBreakerAPI.getInstance().isRunningLunarClient(target)) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c" + target.getDisguisedName() + " &cis NOT protected by Lunar Client."));
            }else {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a" + target.getDisguisedName() + " &ais currently protected by Lunar Client."));
            }
        }
        return false;
    }
}
