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

package com.lunarbreaker.api.net;

import com.lunarbreaker.api.LunarBreakerAPI;
import com.lunarbreaker.api.voice.VoiceChannel;
import com.lunarclient.bukkitapi.nethandler.client.LCPacketClientVoice;
import com.lunarclient.bukkitapi.nethandler.client.LCPacketVoiceChannelSwitch;
import com.lunarclient.bukkitapi.nethandler.client.LCPacketVoiceMute;
import com.lunarclient.bukkitapi.nethandler.server.LCNetHandlerServer;
import com.lunarclient.bukkitapi.nethandler.server.LCPacketStaffModStatus;
import com.lunarclient.bukkitapi.nethandler.server.LCPacketVoice;
import com.lunarclient.bukkitapi.nethandler.shared.LCPacketEmoteBroadcast;
import com.lunarclient.bukkitapi.nethandler.shared.LCPacketWaypointAdd;
import com.lunarclient.bukkitapi.nethandler.shared.LCPacketWaypointRemove;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.UUID;

public class LCNetHandler implements LCNetHandlerServer {
    
    @Override
    public void handleStaffModStatus(LCPacketStaffModStatus lcPacketStaffModStatus) {}

    @Override
    public void handleVoice(LCPacketClientVoice lcPacketClientVoice) {
        Player player = (Player) lcPacketClientVoice.getAttachment();
        VoiceChannel channel = LunarBreakerAPI.getInstance().getPlayerActiveChannels().get(player.getUniqueId());
        if (channel == null) return;

        channel.getPlayersListening().stream().filter(p -> p != player && !LunarBreakerAPI.getInstance().playerHasPlayerMuted(p, p)
                && !LunarBreakerAPI.getInstance().playerHasPlayerMuted(player, p)).forEach(other ->
                LunarBreakerAPI.getInstance().sendPacket(other, new LCPacketVoice(Collections.singleton(player.getUniqueId()), lcPacketClientVoice.getData())));
    }

    @Override
    public void handleVoiceMute(LCPacketVoiceMute lcPacketVoiceMute) {
        Player player = (Player) lcPacketVoiceMute.getAttachment();
        UUID muting = lcPacketVoiceMute.getMuting();

        VoiceChannel channel = LunarBreakerAPI.getInstance().getPlayerActiveChannels().get(player.getUniqueId());
        if (channel == null) return;

        LunarBreakerAPI.getInstance().toggleVoiceMute(player, muting);
    }

    @Override
    public void handleVoiceChannelSwitch(LCPacketVoiceChannelSwitch lcPacketVoiceChannelSwitch) {
        Player player = (Player) lcPacketVoiceChannelSwitch.getAttachment();
        LunarBreakerAPI.getInstance().setActiveChannel(player, lcPacketVoiceChannelSwitch.getSwitchingTo());
    }

    @Override
    public void handleAddWaypoint(LCPacketWaypointAdd lcPacketWaypointAdd) {}

    @Override
    public void handleRemoveWaypoint(LCPacketWaypointRemove lcPacketWaypointRemove) {}

    @Override
    public void handleEmote(LCPacketEmoteBroadcast lcPacketEmoteBroadcast) {}

}
