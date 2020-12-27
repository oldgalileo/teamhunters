package io.getcoffee.hunters;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;

public class Hunters extends JavaPlugin {

    public static Hunters INSTANCE;
    private boolean running = false;

    public HashMap<String, Integer> playerTeamMap = new HashMap<>();
    public String[] teamName = new String[2];
    public List<String>[] teamTargetMap = new ArrayList[2];
    private HashMap<String, BukkitTask> compassTasks = new HashMap<>();

    public HashMap<String, Location> pauseLocation = new HashMap<>();

    @Override
    public void onEnable() {
        INSTANCE = this;
        loadConfig();
        getCommand("hunt_start").setExecutor(new CommandHuntStart(INSTANCE));
        getCommand("hunt_pause").setExecutor(new CommandHuntPause(INSTANCE));
        getServer().getPluginManager().registerEvents(new AsyncPlayerChatEventListener(INSTANCE), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(INSTANCE), this);
        getServer().setDefaultGameMode(GameMode.SPECTATOR);
        if (this.isRunning()) {
            this.start();
        }
    }

    public void loadConfig() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                getLogger().warning("congig.yml not found, yikes!");
                saveDefaultConfig();
            } else {
                getLogger().info("config.yml found, loading!");
                reloadConfig();
            }
            this.running = getConfig().getBoolean("started");
            getLogger().info("STARTED: "+ this.running);
            // First Team Setup
            teamName[0] = getConfig().getString("teams.first.name");
            getConfig().getStringList("teams.first.players").forEach(p -> playerTeamMap.put(p, 0));
            teamTargetMap[0] = getConfig().getStringList("teams.first.targets");
            // Second Team Setup
            teamName[1] = getConfig().getString("teams.second.name");
            getConfig().getStringList("teams.second.players").forEach(p -> playerTeamMap.put(p, 1));
            teamTargetMap[1] = getConfig().getStringList("teams.second.targets");

            getConfig().getConfigurationSection("paused").getValues(false).forEach((key, value) -> pauseLocation.put(key, (Location) value));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        getServer().broadcastMessage("Starting the hunt...");
        running = true;
        getConfig().set("started", running);
        saveConfig();

        for (Player p : getServer().getOnlinePlayers()) {
            startPlayer(p);
        }
    }

    public void pause() {
        running = false;
        getConfig().set("started", running);
        saveConfig();

        getConfig().createSection("paused");
        for (Player p : getServer().getOnlinePlayers()) {
            pausePlayer(p);
            getConfig().set("paused." + p.getName(), p.getLocation());
        }
        getServer().broadcastMessage("The hunt has been paused...");
        saveConfig();
    }

    public void startPlayer(Player p) {
        if(!playerTeamMap.containsKey(p.getName()))
            return;
        var team = playerTeamMap.get(p.getName());
        if(!pauseLocation.containsKey(p.getName())) {
            p.teleport(getServer().getWorld("world").getHighestBlockAt(getServer().getWorld("world").getSpawnLocation().add(5 * team, 0, 0)).getLocation());
        } else {
            p.teleport(pauseLocation.get(p.getName()));
            pauseLocation.remove(p.getName());
            getConfig().set("paused." + p.getName(), null);
            saveConfig();
        }
        p.setGameMode(GameMode.SURVIVAL);
        p.setPlayerListName("[" + teamName[team] + "] " + p.getName());
        addCompassTakeover(p);
        if(!p.getInventory().contains(Material.COMPASS))
            p.getInventory().addItem(new ItemStack(Material.COMPASS));
    }

    public void pausePlayer(Player p) {
        if (playerTeamMap.containsKey(p.getName()))
            pauseLocation.put(p.getName(), p.getLocation());
        p.setGameMode(GameMode.SPECTATOR);
        p.setPlayerListName(p.getName());
        cancelCompassTakeover(p);
    }

    public void addCompassTakeover(Player p) {
        this.compassTasks.put(p.getName(), Bukkit.getScheduler().runTaskTimer(Hunters.INSTANCE, () -> {
            Optional.ofNullable(Hunters.INSTANCE.getNearest(p)).ifPresent(loc -> {
                Arrays.stream(p.getInventory().getContents())
                        .filter(item -> item != null && item.getItemMeta() instanceof CompassMeta)
                        .forEach(item -> {
                            var compassMeta = (CompassMeta) item.getItemMeta();
                            compassMeta.setLodestoneTracked(false);
                            compassMeta.setLodestone(loc);
                            item.setItemMeta(compassMeta);
                        });
            });
        }, 10, 10));
    }

    public void cancelCompassTakeover(Player p) {
        Optional.ofNullable(this.compassTasks.get(p.getName())).ifPresent(BukkitTask::cancel);
        this.compassTasks.remove(p.getName());
    }

        public Location getNearest(Player player) {
        int ourTeam = Hunters.INSTANCE.playerTeamMap.get(player.getName());
        int theirTeam = (ourTeam + 1) % 2;
        double distance = Double.POSITIVE_INFINITY; // To make sure the first

        // player checked is closest
        Location target = null;
        for (Player p : Hunters.INSTANCE.getServer().getOnlinePlayers()) {
            if(ourTeam == Hunters.INSTANCE.playerTeamMap.get(p.getName())) continue;
            if(!Hunters.INSTANCE.teamTargetMap[theirTeam].contains(p.getName())) continue;
            if(p.getWorld() != player.getWorld()) continue;
            double distanceto = player.getLocation().distance(p.getLocation());
            if (distanceto > distance)
                continue;
            distance = distanceto;
            target = p.getLocation();
        }
        return target;
    }

    public boolean isRunning() {
        return this.running;
    }
}
