package io.getcoffee.hunters;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.Arrays;

public class PlayerChangedWorldEventListener implements Listener {

    @EventHandler
    public void onPlayerChangedWorldEvent(PlayerChangedWorldEvent e) {
        Arrays.stream(e.getPlayer().getInventory().getContents())
                .filter(item -> item != null && item.getType() == Material.COMPASS)
                .forEach(item -> {
                    var compassMeta = (CompassMeta) item.getItemMeta();
                    if(!compassMeta.hasLodestone()) {
                        return;
                    }
                    compassMeta.setLodestoneTracked(false);
                    compassMeta.setLodestone(e.getPlayer().getLocation());
                    item.setItemMeta(compassMeta);
                });
    }

}
