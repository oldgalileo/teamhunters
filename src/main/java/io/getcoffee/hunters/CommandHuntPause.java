package io.getcoffee.hunters;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHuntPause implements CommandExecutor {

    private final Hunters instance;

    public CommandHuntPause(Hunters instance) {
        this.instance = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        instance.pause();
        instance.getServer().broadcastMessage("The hunt has been paused...");
        return false;
    }
}
