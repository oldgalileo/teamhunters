package io.getcoffee.hunters;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class AsyncPlayerChatEventListener implements Listener {

    private final Hunters instance;

    public AsyncPlayerChatEventListener(Hunters instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        if(instance.isRunning() && instance.playerTeamMap.containsKey(e.getPlayer().getName())) {
            var prefix = "[" + instance.teamName[instance.playerTeamMap.get(e.getPlayer().getName())] + "]";
            e.setFormat(prefix + " " + e.getPlayer().getName() + ": " + e.getMessage());
        }
    }
}
