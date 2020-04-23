/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.cheatbreaker.api.command;

import com.cheatbreaker.api.CheatBreakerAPI;
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
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c" + p.getName() + " &cis NOT protected by Lunar Client."));
            }else {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a" + p.getName() + " &ais currently protected by Lunar Client."));
            }
        }else {
            Player target = Bukkit.getPlayer(args[0]);

            if(target == null) {
                p.sendMessage(ChatColor.RED + "That player was not found.");
                return false;
            }

            if(!CheatBreakerAPI.getInstance().isRunningLunarClient(target)) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c" + target.getName() + " &cis NOT protected by Lunar Client."));
            }else {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a" + target.getName() + " &ais currently protected by Lunar Client."));
            }
        }
        return false;
    }
}
