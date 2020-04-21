package com.cheatbreaker.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;

/**
 * Called whenever a player registers the LC plugin channel
 */
public final class PlayerRegisterLCEvent extends Event
{
    @Getter private static HandlerList handlerList = new HandlerList();

    @Getter private final Player player;

    public PlayerRegisterLCEvent(Player player) {
        this.player = player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

}