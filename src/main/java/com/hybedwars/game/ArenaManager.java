package com.hybedwars.game;

import com.hybedwars.HyBedWars;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 地图存档管理（arenas.yml）。 */
public class ArenaManager {

    private final HyBedWars plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private File file;
    private FileConfiguration config;

    public ArenaManager(HyBedWars plugin) {
        this.plugin = plugin;
    }

    public void load() {
        arenas.clear();
        file = new File(plugin.getDataFolder(), "arenas.yml");
        if (!file.exists()) {
            try {
                if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                    plugin.getLogger().warning("无法创建插件数据目录");
                }
                if (file.createNewFile()) {
                    plugin.getLogger().info("已创建 arenas.yml");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 arenas.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("arenas");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            Arena arena = new Arena(plugin, key);
            try {
                arena.load(section);
                arenas.put(key.toLowerCase(Locale.ROOT), arena);
            } catch (Exception e) {
                plugin.getLogger().warning("加载地图 " + key + " 失败: " + e.getMessage());
            }
        }
        plugin.getLogger().info("已加载 " + arenas.size() + " 张地图。");
    }

    public void save() {
        if (config == null || file == null) {
            return;
        }
        config.set("arenas", null);
        for (Arena arena : arenas.values()) {
            ConfigurationSection section = config.createSection("arenas." + arena.name());
            arena.save(section);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("保存 arenas.yml 失败: " + e.getMessage());
        }
    }

    public Arena create(String name, GameMode mode) {
        Arena arena = new Arena(plugin, name);
        arena.setMode(mode);
        arenas.put(name.toLowerCase(Locale.ROOT), arena);
        save();
        return arena;
    }

    public boolean delete(String name) {
        Arena arena = arenas.remove(name.toLowerCase(Locale.ROOT));
        if (arena == null) {
            return false;
        }
        if (arena.worldName() != null) {
            com.hybedwars.util.WorldUtil.unload(arena.worldName());
            if (com.hybedwars.config.Settings.copyTemplate) {
                com.hybedwars.util.WorldUtil.deleteDirectory(
                        com.hybedwars.util.WorldUtil.worldFolder(arena.worldName()).toPath());
            }
        }
        File template = new File(plugin.getDataFolder(),
                com.hybedwars.config.Settings.templateFolder + File.separator + arena.name());
        if (template.isDirectory()) {
            com.hybedwars.util.WorldUtil.deleteDirectory(template.toPath());
        }
        save();
        return true;
    }

    public Arena get(String name) {
        if (name == null) {
            return null;
        }
        Arena exact = arenas.get(name.toLowerCase(Locale.ROOT));
        if (exact != null) {
            return exact;
        }
        for (Arena arena : arenas.values()) {
            if (arena.name().equalsIgnoreCase(name)) {
                return arena;
            }
        }
        return null;
    }

    public Collection<Arena> all() {
        return arenas.values();
    }

    public List<String> names() {
        List<String> list = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            list.add(arena.name());
        }
        return list;
    }

    public boolean isEmpty() {
        return arenas.isEmpty();
    }
}
