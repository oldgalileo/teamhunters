package io.getcoffee.hunters;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.Arrays;
import java.util.Optional;

public class PlayerChangedWorldEventListener implements Listener {

    private final Hunters instance;

    public PlayerChangedWorldEventListener(Hunters instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent e) {
        Bukkit.getScheduler().runTaskLater(this.instance, () -> {
            this.instance.playerTargetMap.put(e.getPlayer().getName(), e.getPlayer().getLocation());
        }, 11);
    }

}
