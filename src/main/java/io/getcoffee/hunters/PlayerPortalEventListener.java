package io.getcoffee.hunters;

import io.getcoffee.hunters.Hunters.Pair;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class PlayerPortalEventListener implements Listener {

    private final Hunters instance;

    public PlayerPortalEventListener(Hunters instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onPlayerPortalEvent(PlayerPortalEvent e) {
        this.instance.playerWordPortalMap.put(new Pair<>(e.getPlayer().getName(), e.getPlayer().getWorld().getName()), e.getFrom());
    }
}
