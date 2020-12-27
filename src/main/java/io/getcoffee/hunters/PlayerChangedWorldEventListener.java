package io.getcoffee.hunters;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.Arrays;

public class PlayerChangedWorldEventListener implements Listener {

    private final Hunters instance;

    public PlayerChangedWorldEventListener(Hunters instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent e) {
        if(this.instance.playerTeamMap.containsKey(e.getPlayer().getName())) {
            this.instance.playerTargetMap.put(e.getPlayer().getName(), e.getPlayer().getLocation());
        }
    }

}
