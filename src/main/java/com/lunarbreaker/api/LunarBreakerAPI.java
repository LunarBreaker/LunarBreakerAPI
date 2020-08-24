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

package com.lunarbreaker.api;

import com.cheatbreaker.nethandler.CBPacket;
import com.cheatbreaker.nethandler.obj.ServerRule;
import com.cheatbreaker.nethandler.server.*;
import com.cheatbreaker.nethandler.shared.CBPacketAddWaypoint;
import com.cheatbreaker.nethandler.shared.CBPacketRemoveWaypoint;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.lunarbreaker.api.command.CBCCommand;
import com.lunarbreaker.api.command.CBCommand;
import com.lunarbreaker.api.command.LCCommand;
import com.lunarbreaker.api.event.PlayerRegisterCBEvent;
import com.lunarbreaker.api.event.PlayerRegisterLCEvent;
import com.lunarbreaker.api.event.PlayerUnregisterCBEvent;
import com.lunarbreaker.api.event.PlayerUnregisterLCEvent;
import com.lunarbreaker.api.net.CBNetHandler;
import com.lunarbreaker.api.net.CBNetHandlerImpl;
import com.lunarbreaker.api.net.LCNetHandler;
import com.lunarbreaker.api.net.event.CBPacketReceivedEvent;
import com.lunarbreaker.api.net.event.CBPacketSentEvent;
import com.lunarbreaker.api.net.event.LCPacketReceivedEvent;
import com.lunarbreaker.api.net.event.LCPacketSentEvent;
import com.lunarbreaker.api.object.*;
import com.lunarbreaker.api.voice.VoiceChannel;
import com.lunarclient.bukkitapi.nethandler.LCPacket;
import com.lunarclient.bukkitapi.nethandler.client.*;
import com.lunarclient.bukkitapi.nethandler.server.LCPacketVoiceChannel;
import com.lunarclient.bukkitapi.nethandler.server.LCPacketVoiceChannelRemove;
import com.lunarclient.bukkitapi.nethandler.shared.LCPacketEmoteBroadcast;
import com.lunarclient.bukkitapi.nethandler.shared.LCPacketWaypointAdd;
import com.lunarclient.bukkitapi.nethandler.shared.LCPacketWaypointRemove;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class LunarBreakerAPI extends JavaPlugin implements Listener {

    private static final String CB_MESSAGE_CHANNEL = "CB-Client";

    private static final String LUNAR_MESSAGE_CHANNEL = "Lunar-Client";

    @Getter private static LunarBreakerAPI instance;
    private final Set<UUID> playersRunningCheatBreaker = new HashSet<>();
    private final Set<UUID> playersRunningLunarClient = new HashSet<>();

    private final Set<UUID> playersNotRegistered = new HashSet<>();

    @Setter private CBNetHandler cbNetHandlerServer = new CBNetHandlerImpl();

    @Setter private LCNetHandler lcNetHandlerServer = new LCNetHandler();

    private boolean voiceEnabled;

    @Getter private List<VoiceChannel> voiceChannels = new ArrayList<>();

    @Getter private final Map<UUID, VoiceChannel> playerActiveChannels = new HashMap<>();

    private final Map<UUID, List<CBPacket>> cbPacketQueue = new HashMap<>();

    private final Map<UUID, List<LCPacket>> lcPacketQueue = new HashMap<>();

    private final Map<UUID, List<UUID>> muteMap = new HashMap<>();

    private final Map<UUID, Function<World, String>> worldIdentifiers = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        Messenger messenger = getServer().getMessenger();

        messenger.registerOutgoingPluginChannel(this, CB_MESSAGE_CHANNEL);

        messenger.registerOutgoingPluginChannel(this, LUNAR_MESSAGE_CHANNEL);

        messenger.registerIncomingPluginChannel(this, CB_MESSAGE_CHANNEL, (channel, player, bytes) -> {
            CBPacket packet = CBPacket.handle(cbNetHandlerServer, bytes, player);
            CBPacketReceivedEvent event;
            Bukkit.getPluginManager().callEvent(event = new CBPacketReceivedEvent(player, packet));
            if (!event.isCancelled()) {
                packet.process(this.cbNetHandlerServer);
            }
        });

        messenger.registerIncomingPluginChannel(this, LUNAR_MESSAGE_CHANNEL, (channel, player, bytes) -> {
            LCPacket packet = LCPacket.handle(bytes, player);
            LCPacketReceivedEvent event;
            Bukkit.getPluginManager().callEvent(event = new LCPacketReceivedEvent(player, packet));
            if (!event.isCancelled()) {
                packet.process(this.lcNetHandlerServer);
            }
        });

        getCommand("lc").setExecutor(new LCCommand());
        getCommand("cb").setExecutor(new CBCommand());
        getCommand("cbc").setExecutor(new CBCCommand());

        getServer().getPluginManager().registerEvents(
                new Listener() {
                    @EventHandler
                    public void onRegister(PlayerRegisterChannelEvent event) {
                        if (!event.getChannel().equals(CB_MESSAGE_CHANNEL) && !event.getChannel().equals(LUNAR_MESSAGE_CHANNEL)) {
                            return;
                        }

                        playersNotRegistered.remove(event.getPlayer().getUniqueId());

                        if(event.getChannel().equals(CB_MESSAGE_CHANNEL)) {
                            playersRunningCheatBreaker.add(event.getPlayer().getUniqueId());
                        }

                        if(event.getChannel().equals(LUNAR_MESSAGE_CHANNEL)) {
                            playersRunningLunarClient.add(event.getPlayer().getUniqueId());
                        }

                        muteMap.put(event.getPlayer().getUniqueId(), new ArrayList<>());

                        if (voiceEnabled) {
                            changeServerRule(event.getPlayer(), com.lunarclient.bukkitapi.nethandler.client.obj.ServerRule.VOICE_ENABLED, true);
                            changeServerRule(event.getPlayer(), ServerRule.VOICE_ENABLED, true);
                        }

                        if(event.getChannel().equals(CB_MESSAGE_CHANNEL)) {
                            if (cbPacketQueue.containsKey(event.getPlayer().getUniqueId())) {
                                cbPacketQueue.get(event.getPlayer().getUniqueId()).forEach(p -> sendPacket(event.getPlayer(), p));

                                cbPacketQueue.remove(event.getPlayer().getUniqueId());
                            }
                        }

                        if(event.getChannel().equals(LUNAR_MESSAGE_CHANNEL)) {
                            if (lcPacketQueue.containsKey(event.getPlayer().getUniqueId())) {
                                lcPacketQueue.get(event.getPlayer().getUniqueId()).forEach(p -> sendPacket(event.getPlayer(), p));

                                lcPacketQueue.remove(event.getPlayer().getUniqueId());
                            }
                        }

                        if(event.getChannel().equals(LUNAR_MESSAGE_CHANNEL)) {
                            getServer().getPluginManager().callEvent(new PlayerRegisterLCEvent(event.getPlayer()));
                        }

                        if(event.getChannel().equals(CB_MESSAGE_CHANNEL)) {
                            getServer().getPluginManager().callEvent(new PlayerRegisterCBEvent(event.getPlayer()));
                        }
                        updateWorld(event.getPlayer());
                    }

                    @EventHandler
                    public void onUnregister(PlayerUnregisterChannelEvent event) {
                        System.out.println(event.getChannel());
                        if (event.getChannel().equals(CB_MESSAGE_CHANNEL)) {
                            playersRunningCheatBreaker.remove(event.getPlayer().getUniqueId());
                            playerActiveChannels.remove(event.getPlayer().getUniqueId());
                            muteMap.remove(event.getPlayer().getUniqueId());

                            getServer().getPluginManager().callEvent(new PlayerUnregisterCBEvent(event.getPlayer()));
                        }
                        if (event.getChannel().equals(LUNAR_MESSAGE_CHANNEL)) {
                            playersRunningLunarClient.remove(event.getPlayer().getUniqueId());
                            playerActiveChannels.remove(event.getPlayer().getUniqueId());
                            muteMap.remove(event.getPlayer().getUniqueId());

                            getServer().getPluginManager().callEvent(new PlayerUnregisterLCEvent(event.getPlayer()));
                        }
                    }

                    @EventHandler
                    public void onUnregister(PlayerQuitEvent event) {
                        getPlayerChannels(event.getPlayer()).forEach(channel -> channel.removePlayer(event.getPlayer()));

                        playersRunningCheatBreaker.remove(event.getPlayer().getUniqueId());
                        playersRunningLunarClient.remove(event.getPlayer().getUniqueId());

                        playersNotRegistered.remove(event.getPlayer().getUniqueId());
                        playerActiveChannels.remove(event.getPlayer().getUniqueId());
                        muteMap.remove(event.getPlayer().getUniqueId());
                    }

                    @EventHandler(priority = EventPriority.LOWEST)
                    public void onJoin(PlayerJoinEvent event) {
                        Bukkit.getScheduler().runTaskLater(instance, () -> {
                            if(!isRunningCheatBreaker(event.getPlayer())) {
                                cbPacketQueue.remove(event.getPlayer().getUniqueId());
                            }
                            if(!isRunningLunarClient(event.getPlayer())) {
                                lcPacketQueue.remove(event.getPlayer().getUniqueId());
                            }
                        }, 2 * 20L);
                    }

                    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
                    public void onWorldChange(PlayerChangedWorldEvent event) {
                        updateWorld(event.getPlayer());
                    }

                    private void updateWorld(Player player) {
                        String worldIdentifier = getWorldIdentifier(player.getWorld());

                        sendPacket(player, new LCPacketUpdateWorld(worldIdentifier));
                        sendPacket(player, new CBPacketUpdateWorld(worldIdentifier));
                    }

                }
        , this);
    }

    public String getWorldIdentifier(World world) {
        String worldIdentifier = world.getUID().toString();

        if (worldIdentifiers.containsKey(world.getUID())) {
            worldIdentifier = worldIdentifiers.get(world.getUID()).apply(world);
        }

        return worldIdentifier;
    }

    public void registerWorldIdentifier(World world, Function<World, String> identifier) {
        worldIdentifiers.put(world.getUID(), identifier);
    }

    public boolean isRunningCheatBreaker(Player player) {
        return isRunningCheatBreaker(player.getUniqueId());
    }

    public boolean isRunningCheatBreaker(UUID playerUuid) {
        return playersRunningCheatBreaker.contains(playerUuid);
    }

    public boolean isRunningLunarClient(Player player) {
        return isRunningLunarClient(player.getUniqueId());
    }

    public boolean isRunningLunarClient(UUID playerUuid) {
        return playersRunningLunarClient.contains(playerUuid);
    }

    public Set<Player> getPlayersRunningCheatBreaker() {
        return ImmutableSet.copyOf(playersRunningCheatBreaker.stream().map(Bukkit::getPlayer).collect(Collectors.toSet()));
    }

    public Set<Player> getPlayersRunningLunarClient() {
        return ImmutableSet.copyOf(playersRunningLunarClient.stream().map(Bukkit::getPlayer).collect(Collectors.toSet()));
    }

    public void sendNotification(Player player, CBNotification notification) {
        sendPacket(player, new LCPacketNotification(
                notification.getMessage(),
                notification.getDurationMs(),
                notification.getLevel().name()
        ));
        sendPacket(player, new CBPacketNotification(
                notification.getMessage(),
                notification.getDurationMs(),
                notification.getLevel().name()
        ));
    }

    public void sendNotificationOrFallback(Player player, CBNotification notification, Runnable fallback) {
        if (isRunningCheatBreaker(player) || isRunningLunarClient(player) ) {
            sendNotification(player, notification);
        } else {
            fallback.run();
        }
    }

    public void setStaffModuleState(Player player, StaffModule module, boolean state) {
        sendPacket(player, new LCPacketStaffModState(module.name(), state));
        sendPacket(player, new CBPacketStaffModState(module.name(), state));
    }

    public void setMinimapStatus(Player player, MinimapStatus status) {
        sendPacket(player, new LCPacketServerRule(com.lunarclient.bukkitapi.nethandler.client.obj.ServerRule.MINIMAP_STATUS, status.name()));
        sendPacket(player, new CBPacketServerRule(ServerRule.MINIMAP_STATUS, status.name()));
    }

    public void setServerName(Player p, String name) {
        sendPacket(p, new LCPacketServerUpdate(name));
        sendPacket(p, new CBPacketServerUpdate(name));
    }

    public void performEmote(Player p, int emoteId) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            sendPacket(p, new LCPacketEmoteBroadcast(other.getUniqueId(), emoteId));
        }
    }

    public void setCompetitiveGame(Player player, boolean isCompetitive) {
        sendPacket(player, new LCPacketServerRule(com.lunarclient.bukkitapi.nethandler.client.obj.ServerRule.COMPETITIVE_GAME, isCompetitive));
        sendPacket(player, new CBPacketServerRule(ServerRule.COMPETITIVE_GAMEMODE, isCompetitive));
    }

    public void giveAllStaffModules(Player player) {
        for (StaffModule module : StaffModule.values()) {
            LunarBreakerAPI.getInstance().setStaffModuleState(player, module, true);
        }
    }

    public void disableAllStaffModules(Player player) {
        for (StaffModule module : StaffModule.values()) {
            LunarBreakerAPI.getInstance().setStaffModuleState(player, module, false);
        }
    }

    public void sendTeammates(Player player, CBPacketTeammates packet) {
        validatePlayers(player, packet);
        sendPacket(player, packet);
    }

    public void sendTeammates(Player player, LCPacketTeammates packet) {
        validatePlayers(player, packet);
        sendPacket(player, packet);
    }

    public void validatePlayers(Player sendingTo, CBPacketTeammates packet) {
        packet.getPlayers().entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) != null && !Bukkit.getPlayer(entry.getKey()).getWorld().equals(sendingTo.getWorld()));
    }

    public void validatePlayers(Player sendingTo, LCPacketTeammates packet) {
        packet.getPlayers().entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) != null && !Bukkit.getPlayer(entry.getKey()).getWorld().equals(sendingTo.getWorld()));
    }

    public void addHologram(Player player, UUID id, Vector position, String[] lines) {
        sendPacket(player, new LCPacketHologram(id, position.getX(), position.getY(), position.getZ(), Arrays.asList(lines)));
        sendPacket(player, new CBPacketAddHologram(id, position.getX(), position.getY(), position.getZ(), Arrays.asList(lines)));
    }

    public void updateHologram(Player player, UUID id, String[] lines) {
        sendPacket(player, new LCPacketHologramUpdate(id, Arrays.asList(lines)));
        sendPacket(player, new CBPacketUpdateHologram(id, Arrays.asList(lines)));
    }

    public void removeHologram(Player player, UUID id) {
        sendPacket(player, new LCPacketHologramRemove(id));
        sendPacket(player, new CBPacketRemoveHologram(id));
    }

    public void overrideNametag(Player target, List<String> nametag, Player viewer) {
        sendPacket(viewer, new LCPacketNametagsOverride(target.getUniqueId(), nametag));
        sendPacket(viewer, new CBPacketOverrideNametags(target.getUniqueId(), nametag));
    }

    public void resetNametag(Player target, Player viewer) {
        sendPacket(viewer, new LCPacketNametagsOverride(target.getUniqueId(), null));
        sendPacket(viewer, new CBPacketOverrideNametags(target.getUniqueId(), null));
    }

    public void hideNametag(Player target, Player viewer) {
        sendPacket(viewer, new LCPacketNametagsOverride(target.getUniqueId(), ImmutableList.of()));
        sendPacket(viewer, new CBPacketOverrideNametags(target.getUniqueId(), ImmutableList.of()));
    }

    public void sendTitle(Player player, TitleType type, String message, Duration displayTime) {
        sendTitle(player, type, message, Duration.ofMillis(500), displayTime, Duration.ofMillis(500));
    }

    public void sendTitle(Player player, TitleType type, String message, Duration displayTime, float scale) {
        sendTitle(player, type, message, Duration.ofMillis(500), displayTime, Duration.ofMillis(500), scale);
    }

    public void sendTitle(Player player, TitleType type, String message, Duration fadeInTime, Duration displayTime, Duration fadeOutTime) {
        sendTitle(player, type, message, fadeInTime, displayTime, fadeOutTime, 1F);
    }

    public void sendTitle(Player player, TitleType type, String message, Duration fadeInTime, Duration displayTime, Duration fadeOutTime, float scale) {
        sendPacket(player, new LCPacketTitle(type.name().toLowerCase(), message, scale, displayTime.toMillis(), fadeInTime.toMillis(), fadeOutTime.toMillis()));
        sendPacket(player, new CBPacketTitle(type.name().toLowerCase(), message, scale, displayTime.toMillis(), fadeInTime.toMillis(), fadeOutTime.toMillis()));
    }

    public void sendWaypoint(Player player, CBWaypoint waypoint) {
        sendPacket(player, new LCPacketWaypointAdd(
                waypoint.getName(),
                waypoint.getWorld(),
                waypoint.getColor(),
                waypoint.getX(),
                waypoint.getY(),
                waypoint.getZ(),
                waypoint.isForced(),
                waypoint.isVisible()
        ));
        sendPacket(player, new CBPacketAddWaypoint(
                waypoint.getName(),
                waypoint.getWorld(),
                waypoint.getColor(),
                waypoint.getX(),
                waypoint.getY(),
                waypoint.getZ(),
                waypoint.isForced(),
                waypoint.isVisible()
        ));
    }

    public void changeServerRule(Player p, ServerRule serverRule, boolean state) {
        sendPacket(p, new CBPacketServerRule(serverRule, state));
    }

    public void changeServerRule(Player p, com.lunarclient.bukkitapi.nethandler.client.obj.ServerRule serverRule, boolean state) {
        sendPacket(p, new LCPacketServerRule(serverRule, state));
    }

    public void removeWaypoint(Player player, CBWaypoint waypoint) {
        sendPacket(player, new LCPacketWaypointRemove(
                waypoint.getName(),
                waypoint.getWorld()
        ));
        sendPacket(player, new CBPacketRemoveWaypoint(
                waypoint.getName(),
                waypoint.getWorld()
        ));
    }

    public void sendCooldown(Player player, CBCooldown cooldown) {
        sendPacket(player, new LCPacketCooldown(cooldown.getMessage(), cooldown.getDurationMs(), cooldown.getIcon().getId()));
        sendPacket(player, new CBPacketCooldown(cooldown.getMessage(), cooldown.getDurationMs(), cooldown.getIcon().getId()));
    }

    public void clearCooldown(Player player, CBCooldown cooldown) {
        sendPacket(player, new LCPacketCooldown(cooldown.getMessage(), 0L, cooldown.getIcon().getId()));
        sendPacket(player, new CBPacketCooldown(cooldown.getMessage(), 0L, cooldown.getIcon().getId()));
    }

    public void voiceEnabled(boolean enabled) {
        voiceEnabled = enabled;
    }

    public void createVoiceChannels(VoiceChannel... voiceChannels) {
        this.voiceChannels.addAll(Arrays.asList(voiceChannels));
        for (VoiceChannel channel : voiceChannels) {
            for (Player player : channel.getPlayersInChannel()) {
                sendVoiceChannel(player, channel);
            }
        }
    }

    public void deleteVoiceChannel(VoiceChannel channel) {
        this.voiceChannels.removeIf(c -> {
            boolean remove = c == channel;
            if (remove) {
                channel.validatePlayers();
                for (Player player : channel.getPlayersInChannel()) {
                    sendPacket(player, new LCPacketVoiceChannelRemove(channel.getUuid()));
                    sendPacket(player, new CBPacketDeleteVoiceChannel(channel.getUuid()));
                    if (getPlayerActiveChannels().get(player.getUniqueId()) == channel) {
                        getPlayerActiveChannels().remove(player.getUniqueId());
                    }
                }
            }
            return remove;
        });
    }

    public void deleteVoiceChannel(UUID channelUUID) {
        getChannel(channelUUID).ifPresent(this::deleteVoiceChannel);
    }

    public List<VoiceChannel> getPlayerChannels(Player player) {
        return this.voiceChannels.stream().filter(channel -> channel.hasPlayer(player)).collect(Collectors.toList());
    }

    public void sendVoiceChannel(Player player, VoiceChannel channel) {
        channel.validatePlayers();
        sendPacket(player, new LCPacketVoiceChannel(channel.getUuid(), channel.getName(), channel.toPlayersMap(), channel.toListeningMap()));
        sendPacket(player, new CBPacketVoiceChannel(channel.getUuid(), channel.getName(), channel.toPlayersMap(), channel.toListeningMap()));
    }

    public void setActiveChannel(Player player, UUID uuid) {
        getChannel(uuid).ifPresent(channel -> setActiveChannel(player, channel));
    }

    public Optional<VoiceChannel> getChannel(UUID uuid) {
        return voiceChannels.stream().filter(channel -> channel.getUuid().equals(uuid)).findFirst();
    }

    public void setActiveChannel(Player player, VoiceChannel channel) {
        channel.setActive(player);
    }

    public void toggleVoiceMute(Player player, UUID other) {
        if (!muteMap.get(player.getUniqueId()).removeIf(uuid -> uuid.equals(other))) {
            muteMap.get(player.getUniqueId()).add(other);
        }
    }

    public boolean playerHasPlayerMuted(Player player, Player other) {
        return muteMap.get(other.getUniqueId()).contains(player.getUniqueId());
    }

    /*
    *  This is a boolean to indicate whether or not a CB message was sent.
    *  An example use-case is when you want to send a CheatBreaker
    *  notification if a player is running CheatBreaker, and a chat
    *  message if not.
    */
    public boolean sendPacket(Player player, CBPacket packet) {
        if (isRunningCheatBreaker(player)) {
            player.sendPluginMessage(this, CB_MESSAGE_CHANNEL, CBPacket.getPacketData(packet));
            Bukkit.getPluginManager().callEvent(new CBPacketSentEvent(player, packet));
            return true;
        }else if (isRunningLunarClient(player)) {
            return false;
        } else if (!playersNotRegistered.contains(player.getUniqueId())) {
            cbPacketQueue.putIfAbsent(player.getUniqueId(), new ArrayList<>());
            cbPacketQueue.get(player.getUniqueId()).add(packet);
            return false;
        }
        return false;
    }

    public boolean sendPacket(Player player, LCPacket packet) {
        if (isRunningLunarClient(player)) {
            player.sendPluginMessage(this, LUNAR_MESSAGE_CHANNEL, LCPacket.getPacketData(packet));
            Bukkit.getPluginManager().callEvent(new LCPacketSentEvent(player, packet));
            return true;
        } else if ((isRunningCheatBreaker(player))) {
            return false;
        } else if (!playersNotRegistered.contains(player.getUniqueId())) {
            lcPacketQueue.putIfAbsent(player.getUniqueId(), new ArrayList<>());
            lcPacketQueue.get(player.getUniqueId()).add(packet);
            return false;
        }
        return false;
    }

}