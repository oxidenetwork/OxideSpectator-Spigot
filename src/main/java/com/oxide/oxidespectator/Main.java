package com.oxide.oxidespectator;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends JavaPlugin {
    private static Main plugin;
    private HashMap<UUID, Location> playerOldLocation = new HashMap<>();
    private File file;

    public static Boolean inArea(Location location, Area area) {
        return location.getX() > area.getX_min() && location.getX() < area.getX_max() && location.getZ() > area.getZ_min() && location.getZ() < area.getZ_max();
    }

    public void onEnable() {
        plugin = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        file = new File(getDataFolder() + File.separator + "players.dat");

        if (!file.exists()) {
            try {
                file.createNewFile();
                getConfig().options().copyDefaults(true);
                saveConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            YamlConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection configurationSection = fileConfiguration.getConfigurationSection("players");
            for (String playerUUID : Objects.requireNonNull(configurationSection, "Section null (players)").getKeys(false)) {
                ConfigurationSection section = configurationSection.getConfigurationSection(playerUUID);
                double x = Objects.requireNonNull(section).getDouble("x");
                double y = section.getDouble("y");
                double z = section.getDouble("z");
                World world = Bukkit.getWorld(Objects.requireNonNull(section.getString("world")));
                Location loc = new Location(world, x, y, z);
                playerOldLocation.put(UUID.fromString(playerUUID), loc);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!playerOldLocation.isEmpty()) {
                    for (UUID uuids : playerOldLocation.keySet()) {
                        Player player = Bukkit.getPlayer(uuids);
                        Area area = new Area(playerOldLocation.get(uuids), plugin.getConfig().getDouble("area.size"));
                        if (!Bukkit.getOnlinePlayers().contains(player) || inArea(player.getLocation(), area) || !player.getGameMode().equals(GameMode.SPECTATOR))
                            continue;

                        player.sendMessage("You hit the border teleporting you back");
                        player.teleport(playerOldLocation.get(uuids));
                    }
                }

            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void onDisable() {
        YamlConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
        fileConfiguration.set("players", null);

        if (!fileConfiguration.isConfigurationSection("players")) {
            fileConfiguration.createSection("players");
        }

        if (!playerOldLocation.isEmpty()) {
            Set<Map.Entry<UUID, Location>> playerEntry = playerOldLocation.entrySet();
            for (Map.Entry<UUID, Location> entry : playerEntry) {
                HashMap<String, Object> coordinates = new HashMap<>();
                coordinates.put("x", entry.getValue().getX());
                coordinates.put("y", entry.getValue().getY());
                coordinates.put("z", entry.getValue().getZ());
                coordinates.put("world", entry.getValue().getWorld().getName());
                fileConfiguration.set("players." + entry.getKey().toString(), coordinates);
            }
        }

        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("spectator")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("area") && (player.hasPermission("oxidespectator.area"))) {
                        double size = Double.parseDouble(args[1]);
                        plugin.getConfig().set("area.size", size);
                        plugin.saveConfig();
                        player.sendMessage("Main size is now " + size);
                        return true;
                    }
                } else {
                    if (player.getGameMode().equals(GameMode.SPECTATOR) && playerOldLocation.containsKey(player.getUniqueId())) {
                        player.teleport(playerOldLocation.get(player.getUniqueId()));
                        player.setGameMode(GameMode.SURVIVAL);
                        return true;
                    }
                    if (player.getGameMode().equals(GameMode.SURVIVAL)) {
                        playerOldLocation.put(player.getUniqueId(), player.getLocation());
                        player.setGameMode(GameMode.SPECTATOR);
                        return true;
                    }
                    Location loc = player.getLocation();
                    player.sendMessage("An error has occurred!");
                    playerOldLocation.remove(player.getUniqueId());
                    player.setGameMode(GameMode.SURVIVAL);
                    player.teleport(new Location(player.getWorld(), loc.getX(), (double) loc.getWorld().getHighestBlockYAt(loc), loc.getZ()));
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {
        if (command.getName().equalsIgnoreCase("spectator")) {
            List<String> l = new ArrayList<>();

            if (sender.hasPermission("oxidespectator.area")) {
                if (args.length == 1) {
                    l.add("area");
                }
            }

            return l;
        }
        return null;
    }
}

