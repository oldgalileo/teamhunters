package io.getcoffee.hunters;

import io.getcoffee.hunters.Hunters.Pair;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PlayerChangedWorldEventListener implements Listener {

    private final Hunters instance;

    public PlayerChangedWorldEventListener(Hunters instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent e) {
        this.instance.playerWordPortalMap.put(new Pair<>(e.getPlayer().getName(), e.getPlayer().getWorld().getName()), e.getPlayer().getLocation());
    }

}
