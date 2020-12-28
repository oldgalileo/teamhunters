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
    private boolean started = false;

    public HashMap<String, Location> playerTargetMap = new HashMap<>();
    public HashMap<String, Integer> playerTeamMap = new HashMap<>();
    public String[] teamName = new String[2];
    public List<String>[] teamTargetMap = new ArrayList[2];

    private BukkitTask findNearestTargetTask;
    private BukkitTask updateCompassTask;

    public HashMap<String, Location> pauseLocation = new HashMap<>();
    public HashMap<Pair<String, String>, Location> playerWordPortalMap = new HashMap<>();

    @Override
    public void onEnable() {
        INSTANCE = this;
        loadConfig();
        getCommand("hunt_start").setExecutor(new CommandHuntStart(INSTANCE));
        getCommand("hunt_pause").setExecutor(new CommandHuntPause(INSTANCE));
        getServer().getPluginManager().registerEvents(new AsyncPlayerChatEventListener(INSTANCE), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnEventListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinEventListener(INSTANCE), this);
        getServer().getPluginManager().registerEvents(new PlayerChangedWorldEventListener(INSTANCE), this);
        getServer().getPluginManager().registerEvents(new PlayerPortalEventListener(INSTANCE), this);
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
            this.running = getConfig().getBoolean("running");
            this.started = getConfig().getBoolean("started");

            getLogger().info("STARTED: "+ this.running);
            getLogger().info("RUNNING: "+ this.running);

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
        getConfig().set("running", running);

        updateCompassTask = Bukkit.getScheduler().runTaskTimer(this, new UpdateCompassTask(), 10, 10);
        findNearestTargetTask = Bukkit.getScheduler().runTaskTimer(this, new FindNearestTargetTask(), 10, 10);

        for (Player p : getServer().getOnlinePlayers()) {
            startPlayer(p);
        }

        started = true;
        getConfig().set("started", started);
        saveConfig();
    }

    public void pause() {
        running = false;
        getConfig().set("running", running);
        saveConfig();

        updateCompassTask.cancel();
        findNearestTargetTask.cancel();

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
        if(!pauseLocation.containsKey(p.getName()) && !this.started) {
            p.teleport(getServer().getWorld("world").getHighestBlockAt(getServer().getWorld("world").getSpawnLocation().add(5 * team, 0, 0)).getLocation());
        } else if(pauseLocation.containsKey(p.getName())) {
            p.teleport(pauseLocation.get(p.getName()));
            pauseLocation.remove(p.getName());
            getConfig().set("paused." + p.getName(), null);
            saveConfig();
        }
        p.setGameMode(GameMode.SURVIVAL);
        p.setPlayerListName("[" + teamName[team] + "] " + p.getName());
        if(!p.getInventory().contains(Material.COMPASS))
            p.getInventory().addItem(new ItemStack(Material.COMPASS));
    }

    public void pausePlayer(Player p) {
        if (playerTeamMap.containsKey(p.getName()))
            pauseLocation.put(p.getName(), p.getLocation());
        p.setGameMode(GameMode.SPECTATOR);
        p.setPlayerListName(p.getName());
    }


    public Location getNearest(Player p) {
        var nearTargetLoc = getNearestTarget(p);
        if(nearTargetLoc != null)
            return nearTargetLoc;
        return getNearestTargetPortal(p);
    }

    public Location getNearestTarget(Player p) {
        int ourTeam = playerTeamMap.get(p.getName());
        int theirTeam = (ourTeam + 1) % 2;
        double distance = Double.POSITIVE_INFINITY; // To make sure the first

        Location targetLoc = null;
        for(Player target : getServer().getOnlinePlayers()) {
            if(ourTeam == playerTeamMap.get(target.getName())) continue;
            if(!teamTargetMap[theirTeam].contains(target.getName())) continue;
            if(target.getWorld() != p.getWorld()) continue;
            double distanceto = p.getLocation().distance(target.getLocation());
            if (distanceto > distance)
                continue;
            distance = distanceto;
            targetLoc = target.getLocation();
        }
        return targetLoc;
    }

    public Location getNearestTargetPortal(Player p) {
        int ourTeam = playerTeamMap.get(p.getName());
        int theirTeam = (ourTeam + 1) % 2;
        double distance = Double.POSITIVE_INFINITY; // To make sure the first

        Location targetLoc = null;
        for(Player target : getServer().getOnlinePlayers()) {
            if(ourTeam == playerTeamMap.get(target.getName())) continue;
            if(!teamTargetMap[theirTeam].contains(target.getName())) continue;
            if(target.getWorld() == p.getWorld()) continue;
            var loc = playerWordPortalMap.get(new Pair<>(target.getName(), p.getWorld().getName()));
            if(loc != null) {
                double distanceto = p.getLocation().distance(loc);
                if (distanceto > distance)
                    continue;
                distance = distanceto;
                targetLoc = loc;
            }
        }
        if(targetLoc == null) return playerWordPortalMap.get(new Pair<>(p.getName(), p.getWorld().getName()));
        return targetLoc;
    }

    public static class UpdateCompassTask implements Runnable {
        @Override
        public void run() {
            Hunters.INSTANCE.getServer().getOnlinePlayers().forEach(p ->
                    Optional.ofNullable(Hunters.INSTANCE.playerTargetMap.get(p.getName())).ifPresent(loc -> {
                        Arrays.stream(p.getInventory().getContents())
                                .filter(item -> item != null && item.getType() == Material.COMPASS)
                                .forEach(item -> {
                                    var compassMeta = (CompassMeta) item.getItemMeta();
                                    compassMeta.setDisplayName("Tracking: " + Hunters.INSTANCE.teamName[(Hunters.INSTANCE.playerTeamMap.get(p.getName()) + 1) % 2]);
                                    compassMeta.setLodestoneTracked(false);
                                    compassMeta.setLodestone(loc);
                                    compassMeta.setCustomModelData(Hunters.INSTANCE.getServer().getCurrentTick());
                                    item.setItemMeta(compassMeta);
                                });
                    })
            );
        }
    }

    public static class FindNearestTargetTask implements Runnable {

        @Override
        public void run() {
            Hunters.INSTANCE.getServer().getOnlinePlayers().forEach(p -> {
                Optional.ofNullable(Hunters.INSTANCE.getNearest(p)).ifPresent(loc -> {
                    Hunters.INSTANCE.playerTargetMap.put(p.getName(), loc);
                });
            });
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    public static class Pair<X, Y> extends AbstractMap.SimpleEntry {
        public Pair(X key, Y value) {
            super(key, value);
        }
    }
}
