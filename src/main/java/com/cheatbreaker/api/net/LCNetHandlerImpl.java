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

import com.moonsworth.client.nethandler.client.LCPacketEmoteBroadcast;
import com.moonsworth.client.nethandler.client.LCPacketStaffModStatus;
import com.moonsworth.client.nethandler.client.LCPacketVersionNumber;
import com.moonsworth.client.nethandler.shared.LCPacketWaypointAdd;
import com.moonsworth.client.nethandler.shared.LCPacketWaypointRemove;
import org.bukkit.entity.Player;

public class LCNetHandlerImpl extends LCNetHandler
{

    @Override
    public void handleAddWaypoint(LCPacketWaypointAdd lcPacketAddWaypoint) {}

    @Override
    public void handleRemoveWaypoint(LCPacketWaypointRemove lcPacketRemoveWaypoint) {}

    @Override
    public void handleEmote(LCPacketEmoteBroadcast lcPacketEmoteBroadcast) {}

    @Override
    public void handlePacketVersionNumber(LCPacketVersionNumber lcPacketVersionNumber) {}

    @Override
    public void handleStaffModStatus(LCPacketStaffModStatus lcPacketStaffModStatus) {
        Player p = lcPacketStaffModStatus.getAttachment();

        p.sendMessage("Enabled " + lcPacketStaffModStatus.getEnabled());
    }
}
