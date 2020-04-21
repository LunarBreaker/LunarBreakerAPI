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

package com.cheatbreaker.api.net;

import com.cheatbreaker.api.CheatBreakerAPI;
import com.cheatbreaker.api.voice.VoiceChannel;
import com.moonsworth.client.nethandler.client.LCPacketClientVoice;
import com.moonsworth.client.nethandler.client.LCPacketVersionNumber;
import com.moonsworth.client.nethandler.client.LCPacketVoiceChannelSwitch;
import com.moonsworth.client.nethandler.client.LCPacketVoiceMute;
import com.moonsworth.client.nethandler.server.ILCNetHandlerServer;
import com.moonsworth.client.nethandler.server.LCPacketVoice;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class LCNetHandler implements ILCNetHandlerServer {

    @Override
    public void handleVoice(LCPacketClientVoice packet) {
        Player player = packet.getAttachment();
        VoiceChannel channel = CheatBreakerAPI.getInstance().getPlayerActiveChannels().get(player.getUniqueId());
        if (channel == null) return;

        channel.getPlayersListening().stream().filter(p -> p != player && !CheatBreakerAPI.getInstance().playerHasPlayerMuted(p, p)
                && !CheatBreakerAPI.getInstance().playerHasPlayerMuted(player, p)).forEach(other ->
                CheatBreakerAPI.getInstance().sendPacket(other, new LCPacketVoice(player.getUniqueId(), packet.getData())));
    }

    @Override
    public void handleVoiceChannelSwitch(LCPacketVoiceChannelSwitch packet) {
        Player player = packet.getAttachment();
        CheatBreakerAPI.getInstance().setActiveChannel(player, packet.getSwitchingTo());
    }

    @Override
    public void handleVoiceMute(LCPacketVoiceMute packet) {
        Player player = packet.getAttachment();
        UUID muting = packet.getMuting();

        VoiceChannel channel = CheatBreakerAPI.getInstance().getPlayerActiveChannels().get(player.getUniqueId());
        if (channel == null) return;

        CheatBreakerAPI.getInstance().toggleVoiceMute(player, muting);
    }
}
