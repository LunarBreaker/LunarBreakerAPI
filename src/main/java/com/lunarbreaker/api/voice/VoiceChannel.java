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

package com.lunarbreaker.api.voice;

import com.lunarbreaker.api.LunarBreakerAPI;
import com.cheatbreaker.nethandler.server.CBPacketDeleteVoiceChannel;
import com.cheatbreaker.nethandler.server.CBPacketVoiceChannelUpdate;
import com.lunarclient.bukkitapi.nethandler.server.LCPacketVoiceChannelRemove;
import com.lunarclient.bukkitapi.nethandler.server.LCPacketVoiceChannelUpdate;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class VoiceChannel
{
    private final String name;

    private final UUID uuid;

    private final List<Player> playersInChannel = new ArrayList<>();

    private final List<Player> playersListening = new ArrayList<>();

    public VoiceChannel(String name)
    {
        this.name = name;
        this.uuid = UUID.randomUUID();
    }

    public void addPlayer(Player player)
    {
        if (hasPlayer(player)) return;

        for (Player player1 : playersInChannel)
        {
            LunarBreakerAPI.getInstance().sendPacket(player1, new LCPacketVoiceChannelUpdate(0, uuid, player.getUniqueId(), player.getDisplayName()));
            LunarBreakerAPI.getInstance().sendPacket(player1, new CBPacketVoiceChannelUpdate(0, uuid, player.getUniqueId(), player.getDisplayName()));
        }

        playersInChannel.add(player);
        LunarBreakerAPI.getInstance().sendVoiceChannel(player, this);
    }

    public boolean removePlayer(Player player)
    {
        if (!hasPlayer(player)) return false;

        for (Player player1 : playersInChannel)
        {
            if (player1 == player) continue;
            LunarBreakerAPI.getInstance().sendPacket(player1, new LCPacketVoiceChannelUpdate(1, uuid, player.getUniqueId(), player.getDisplayName()));
            LunarBreakerAPI.getInstance().sendPacket(player1, new CBPacketVoiceChannelUpdate(1, uuid, player.getUniqueId(), player.getDisplayName()));
        }

        LunarBreakerAPI.getInstance().sendPacket(player, new LCPacketVoiceChannelRemove(uuid));
        LunarBreakerAPI.getInstance().sendPacket(player, new CBPacketDeleteVoiceChannel(uuid));
        LunarBreakerAPI.getInstance().getPlayerActiveChannels().remove(player.getUniqueId());

        playersListening.removeIf(player1 -> player1 == player);
        return playersInChannel.removeIf(player1 -> player1 == player);
    }

    private boolean addListening(Player player)
    {
        if (!hasPlayer(player) || isListening(player)) return false;

        playersListening.add(player);

        for (Player player1 : playersInChannel)
        {
            LunarBreakerAPI.getInstance().sendPacket(player1, new LCPacketVoiceChannelUpdate(2, uuid, player.getUniqueId(), player.getDisplayName()));
            LunarBreakerAPI.getInstance().sendPacket(player1, new CBPacketVoiceChannelUpdate(2, uuid, player.getUniqueId(), player.getDisplayName()));
        }

        return true;
    }

    private boolean removeListening(Player player)
    {
        if (!isListening(player)) return false;

        for (Player player1 : playersInChannel)
        {
            if (player1 == player) continue;
            LunarBreakerAPI.getInstance().sendPacket(player1, new LCPacketVoiceChannelUpdate(3, uuid, player.getUniqueId(), player.getDisplayName()));
            LunarBreakerAPI.getInstance().sendPacket(player1, new CBPacketVoiceChannelUpdate(3, uuid, player.getUniqueId(), player.getDisplayName()));
        }

        return playersListening.removeIf(player1 -> player1 == player);
    }

    public void setActive(Player player)
    {
        LunarBreakerAPI api = LunarBreakerAPI.getInstance();
        Optional.ofNullable(api.getPlayerActiveChannels().get(player.getUniqueId())).ifPresent(c -> {
            if (c != this) c.removeListening(player);
        });
        if (addListening(player)) {
            api.getPlayerActiveChannels().put(player.getUniqueId(), this);
        }
    }

    public boolean validatePlayers()
    {
        return playersInChannel.removeIf(Objects::isNull) || playersListening.removeIf(player -> !playersInChannel.contains(player));
    }

    public boolean hasPlayer(Player player)
    {
        return playersInChannel.contains(player);
    }

    public boolean isListening(Player player)
    {
        return playersListening.contains(player);
    }

    /**
     * Convert this to the map that will be sent over the net channel
     */
    public Map<UUID, String> toPlayersMap()
    {
        return playersInChannel.stream()
                .collect(Collectors.toMap(
                        Player::getUniqueId,
                        Player::getDisplayName
                ));
    }

    /**
     * Convert this to the map that will be sent over the net channel
     */
    public Map<UUID, String> toListeningMap()
    {
        return playersListening.stream()
                .collect(Collectors.toMap(
                        Player::getUniqueId,
                        Player::getDisplayName
                ));
    }
}
