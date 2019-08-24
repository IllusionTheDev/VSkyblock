package com.github.Viduality.VSkyblock.Utilitys;

import com.github.Viduality.VSkyblock.VSkyblock;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.FileUtils;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class WorldManager {

    private VSkyblock plugin = VSkyblock.getInstance();
    private File worlds = new File(plugin.getDataFolder(), "Worlds.yml");
    private FileConfiguration worldsConfig = new YamlConfiguration();


    public void createIsland(String island) {

        //Check if the world doesn't already exists
        if (!getAllWorlds().contains(island)) {
            File dir = new File(plugin.getServer().getWorldContainer().getAbsolutePath() + "/" + island);
            if (!dir.exists()) {
                dir.mkdirs();

                File source = new File(plugin.getServer().getWorldContainer().getAbsolutePath() + "/VSkyblockMasterIsland");

                try {
                    FileUtils.copyDirectory(source, dir);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                File files[] = dir.listFiles();
                for (File file : files) {
                    if (file.getName().equalsIgnoreCase("uid.dat") || file.getName().equalsIgnoreCase("session.lock")) {
                        file.delete();
                    }
                }

                WorldCreator wc = new WorldCreator(island);
                wc.generator("VSkyblock");
                wc.environment(World.Environment.NORMAL);
                wc.type(WorldType.FLAT);
                wc.generateStructures(false);
                World newIsland = wc.createWorld();
                plugin.getServer().getWorlds().add(newIsland);
                plugin.getServer().getWorld(island).setSpawnLocation(0, 67, 0);
                addWorld(island);
                System.out.println(plugin.getServer().getWorlds());
            } else {
                System.out.println("Folder already exists!");
            }
        } else {
            System.out.println(ChatColor.RED + "Tried to create save a world but VSkyblock already knows about it!");
        }
    }

    public boolean unloadWorld(String world) {
        if (plugin.getServer().getWorlds().contains(plugin.getServer().getWorld(world))) {
            if (plugin.getServer().getWorld(world).getPlayers() != null) {
                for (Player player : plugin.getServer().getWorld(world).getPlayers()) {
                    player.teleport(plugin.getServer().getWorld(plugin.getConfig().getString("SpawnWorld")).getSpawnLocation());
                }
            }
            plugin.getServer().unloadWorld(world, true);
            return true;
        } else {
            System.out.println(ChatColor.RED + "Tried to unload a world VSkyblock does not know about. :(");
            return false;
        }
    }

    public boolean loadWorld(String world) {
        if (getAllWorlds().contains(world)) {
            if (getUnloadedWorlds().contains(world)) {
                WorldCreator wc = new WorldCreator(world);
                wc.generator("VSkyblock");
                wc.environment(World.Environment.NORMAL);
                wc.type(WorldType.FLAT);
                wc.generateStructures(false);
                World loadedworld = wc.createWorld();
                plugin.getServer().getWorlds().add(loadedworld);
                return true;
            } else {
                if (getLoadedWorlds().contains(world)) {
                    return true;
                }
            }
        } else {
            System.out.println(ChatColor.RED + "VSkyblock does not know about this world!");
            return false;
        }
        return false;
    }

    public boolean deleteWorld(String world) {
        if (getAllWorlds().contains(world)) {
            if (loadWorld(world)) {
                World delete = plugin.getServer().getWorld(world);
                if (unloadWorld(world)) {
                    File deleteFolder = delete.getWorldFolder();

                    if(deleteFolder.exists()) {
                        File files[] = deleteFolder.listFiles();

                        for (File file : files) {
                            if (file.isDirectory()) {
                                for (File file2 : file.listFiles()) {
                                    file2.delete();
                                }
                            }
                            file.delete();
                        }
                    }
                    deleteWorldfromConfig(world);
                    return(deleteFolder.delete());
                } else {
                    System.out.println(ChatColor.RED + "Could not delete world " + world);
                    return false;
                }
            } else {
                System.out.println(ChatColor.RED + "Could not delete world " + world);
                return false;
            }
        } else {
            System.out.println(ChatColor.RED + "VSkyblock does not know about this world!");
            return false;
        }
    }

    public Location getSpawnLocation(String world) {
        ConfigShorts.loadWorldConfig();
        List<String> worlds = getAllWorlds();
        if (worlds.contains(world)) {
            if (getUnloadedWorlds().contains(world)) {
                loadWorld(world);
            }
            World world1 = plugin.getServer().getWorld(world);
            double x = plugin.getConfig().getDouble("Worlds." + world + ".spawnLocation.x");
            double y = plugin.getConfig().getDouble("Worlds." + world + ".spawnLocation.y");
            double z = plugin.getConfig().getDouble("Worlds." + world + ".spawnLocation.z");
            float yaw = (float) plugin.getConfig().getDouble("Worlds." + world + ".spawnLocation.yaw");
            float pitch = (float) plugin.getConfig().getDouble("Worlds." + world + ".spawnLocation.pitch");
            ConfigShorts.loaddefConfig();
            return new Location(world1, x, y, z, yaw, pitch);
        } else {
            System.out.println(ChatColor.RED + "Could not find a spawn location for world " + world + "!");
            return null;
        }
    }


    public List<String> getUnloadedWorlds() {
        List<World> loadedWorlds = plugin.getServer().getWorlds();
        List<String> worlds = new ArrayList<>();
        for (World world : loadedWorlds) {
            worlds.add(world.getName());
        }
        List<String> allWorlds = getAllWorlds();
        List<String> unloadedworlds = new ArrayList<>();
        for (String currentworld : allWorlds) {
            if (!worlds.contains(currentworld)) {
                unloadedworlds.add(currentworld);
            }
        }
        return unloadedworlds;
    }


    public List<String> getLoadedWorlds() {

        List<World> loadedWorlds = plugin.getServer().getWorlds();
        List<String> worlds = new ArrayList<>();
        for (World world : loadedWorlds) {
            worlds.add(world.getName());
        }
        return worlds;
    }

    public List<String> getAllWorlds() {
        ConfigShorts.loadWorldConfig();
        Set<String> allworlds = plugin.getConfig().getConfigurationSection("Worlds").getKeys(false);
        ConfigShorts.loaddefConfig();
        return new ArrayList<>(allworlds);
    }

    public String getSpawnWorld() {
        return plugin.getServer().getWorlds().get(0).getName();
    }


    public void addWorld(String world) {
        try {
            InputStream templateStream = plugin.getResource("WorldTemplate.yml");
            StringBuilder out = new StringBuilder();
            final char[] buffer = new char[0x10000];

            Reader in = new InputStreamReader(templateStream);
            int read;
            do {
                read = in.read(buffer, 0, buffer.length);
                if (read>0) {
                    out.append(buffer, 0, read);
                }
            } while (read>=0);
            String template = out.toString();
            in.close();
            template = template.replace("WorldName", world);


            // input the file content to the StringBuffer "input"
            BufferedReader file = new BufferedReader(new FileReader(plugin.getDataFolder() + "/Worlds.yml"));
            String line;
            StringBuffer inputBuffer = new StringBuffer();

            while ((line = file.readLine()) != null) {
                inputBuffer.append(line);
                inputBuffer.append('\n');
            }
            inputBuffer.append(template);
            file.close();

            String worldsFile = inputBuffer.toString();

            FileOutputStream fileOut = new FileOutputStream(plugin.getDataFolder() + "/Worlds.yml");
            fileOut.write(worldsFile.getBytes());
            fileOut.close();

        } catch (Exception e) {
            System.out.println("Problem reading file.");
            e.printStackTrace();
        }
    }


    private void deleteWorldfromConfig(String world) {

        ConfigShorts.loadWorldConfig();

        String inputStr = null;
        String worldinfoString = null;

        try {
            // input the file content to the StringBuffer "input"
            BufferedReader file = new BufferedReader(new FileReader(plugin.getDataFolder() + "/Worlds.yml"));
            String line;
            StringBuffer inputBuffer = new StringBuffer();
            StringBuffer worldinfo = new StringBuffer();
            int i = 0;

            Object[] test = plugin.getConfig().getConfigurationSection("Worlds." + world).getKeys(true).toArray();
            String lastPart = null;
            for (Object out : test) {
                lastPart = out.toString();
            }

            while ((line = file.readLine()) != null) {
                inputBuffer.append(line);
                inputBuffer.append('\n');
                if (line.contains(world)) {
                    i = 1;
                }
                if (i == 1) {
                    worldinfo.append(line);
                    worldinfo.append('\n');
                    if (line.contains(lastPart)) {
                        i = 0;
                    }
                }
            }
            inputStr = inputBuffer.toString();
            worldinfoString = worldinfo.toString();
            file.close();

            inputStr = inputStr.replace(worldinfoString, "");


            FileOutputStream fileOut = new FileOutputStream(plugin.getDataFolder() + "/Worlds.yml");
            fileOut.write(inputStr.getBytes());
            fileOut.close();

            ConfigShorts.loaddefConfig();

        } catch (Exception e) {
            System.out.println("Problem reading file.");
            e.printStackTrace();
        }
    }

    public void setOption(String world, String string, String option) {

        ConfigShorts.loadWorldConfig();

        String inputStr = null;
        String worldinfoString = null;

        try {
            // input the file content to the StringBuffer "input"
            BufferedReader file = new BufferedReader(new FileReader(plugin.getDataFolder() + "/Worlds.yml"));
            String line;
            StringBuffer inputBuffer = new StringBuffer();
            StringBuffer worldinfo = new StringBuffer();
            int i = 0;

            while ((line = file.readLine()) != null) {
                inputBuffer.append(line);
                inputBuffer.append('\n');
                if (line.contains(world)) {
                    i = 1;
                }
                if (i == 1) {
                    worldinfo.append(line);
                    worldinfo.append('\n');
                    if (line.contains(string)) {
                        i = 0;
                    }
                }
            }
            inputStr = inputBuffer.toString();
            worldinfoString = worldinfo.toString();
            String newWorldInfo = worldinfoString;
            file.close();

            if (newWorldInfo.contains(string)) {
                String oldString = plugin.getConfig().getString("Worlds." + world + "." + string);
                String replace1 = string + ":";
                String with1 = string + ": " + option;
                String OldLine = string + ": " + oldString;
                String NewLine = string + ": " + option;
                if (oldString == null) {
                    newWorldInfo = newWorldInfo.replace(replace1, with1);
                } else {
                    newWorldInfo = newWorldInfo.replace(OldLine, NewLine);
                }
            } else {
                System.out.println("Keine Zeile in der Config gefunden!");
            }


            inputStr = inputStr.replace(worldinfoString, newWorldInfo);


            FileOutputStream fileOut = new FileOutputStream(plugin.getDataFolder() + "/Worlds.yml");
            fileOut.write(inputStr.getBytes());
            fileOut.close();

            ConfigShorts.loaddefConfig();

        } catch (Exception e) {
            System.out.println("Problem reading file.");
            e.printStackTrace();
        }
    }


    public void setSpawnLocation(Location loc) {

        ConfigShorts.loadWorldConfig();
        List<String> worlds = getAllWorlds();

        if (worlds.contains(loc.getWorld().getName())) {
            String world = loc.getWorld().getName();
            double x = loc.getX();
            double y = loc.getY();
            double z = loc.getZ();
            float yaw = loc.getYaw();
            float pitch = loc.getPitch();



            List<String> locInfo = Arrays.asList("x", "y", "z", "yaw", "pitch");
            List<String> locValues = Arrays.asList(String.valueOf(x), String.valueOf(y), String.valueOf(z), String.valueOf(yaw), String.valueOf(pitch));


            String inputStr = null;
            String worldinfoString = null;

            try {
                // input the file content to the StringBuffer "input"
                BufferedReader file = new BufferedReader(new FileReader(plugin.getDataFolder() + "/Worlds.yml"));
                String line;
                StringBuffer inputBuffer = new StringBuffer();
                StringBuffer worldinfo = new StringBuffer();
                int i = 0;

                while ((line = file.readLine()) != null) {
                    inputBuffer.append(line);
                    inputBuffer.append('\n');
                    if (line.contains(world)) {
                        i = 1;
                    }
                    if (i == 1) {
                        worldinfo.append(line);
                        worldinfo.append('\n');
                        if (line.contains("yaw")) {
                            i = 0;
                        }
                    }
                }
                inputStr = inputBuffer.toString();
                worldinfoString = worldinfo.toString();
                String newWorldInfo = worldinfoString;
                file.close();

                for (int a = 0; a < locInfo.size(); a++) {
                    String currentData = locInfo.get(a);
                    if (newWorldInfo.contains(currentData)) {
                        String oldString = plugin.getConfig().getString("Worlds." + world + ".spawnLocation" + "." + currentData);
                        String replace1 = currentData + ":";
                        String with1 = currentData + ": " + locValues.get(a);
                        String OldLine = currentData + ": " + oldString;
                        String NewLine = currentData + ": " + locValues.get(a);
                        if (oldString == null) {
                            newWorldInfo = newWorldInfo.replace(replace1, with1);
                        } else {
                            newWorldInfo = newWorldInfo.replace(OldLine, NewLine);
                        }
                    } else {
                        System.out.println("Keine Zeile in der Config gefunden!");
                    }
                }


                inputStr = inputStr.replace(worldinfoString, newWorldInfo);


                FileOutputStream fileOut = new FileOutputStream(plugin.getDataFolder() + "/Worlds.yml");
                fileOut.write(inputStr.getBytes());
                fileOut.close();

                ConfigShorts.loaddefConfig();

            } catch (Exception e) {
                System.out.println("Problem reading file.");
                e.printStackTrace();
            }
        }
    }
}