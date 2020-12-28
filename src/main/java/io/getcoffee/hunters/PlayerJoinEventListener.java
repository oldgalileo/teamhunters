package io.getcoffee.hunters;

import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinEventListener implements Listener {

    private final Hunters instance;

    public PlayerJoinEventListener(Hunters instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (instance.isRunning()) {
            instance.startPlayer(e.getPlayer());
        } else {
            if(!instance.pauseLocation.containsKey(e.getPlayer().getName())) {
                instance.pausePlayer(e.getPlayer());
            }
        }
    }

}
