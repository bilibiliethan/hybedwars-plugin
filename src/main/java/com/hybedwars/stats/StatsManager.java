package com.hybedwars.stats;

import com.hybedwars.HyBedWars;
import com.hybedwars.game.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** 战绩存储（stats.yml），内存缓存 + 定时异步落盘。 */
public class StatsManager {

    public enum SortKey {
        KILLS("击杀", s -> s.kills() + s.finalKills()),
        WINS("胜利", PlayerStats::wins),
        BEDS("破坏床", PlayerStats::bedsBroken),
        LEVEL("星级", PlayerStats::level),
        XP("经验", PlayerStats::xp);

        private final String display;
        private final java.util.function.ToIntFunction<PlayerStats> getter;

        SortKey(String display, java.util.function.ToIntFunction<PlayerStats> getter) {
            this.display = display;
            this.getter = getter;
        }

        public String display() {
            return display;
        }

        public int value(PlayerStats stats) {
            return getter.applyAsInt(stats);
        }

        public static SortKey parse(String input) {
            if (input == null) {
                return KILLS;
            }
            String value = input.trim().toLowerCase(Locale.ROOT);
            for (SortKey key : values()) {
                if (key.name().toLowerCase(Locale.ROOT).equals(value) || key.display.equals(input.trim())) {
                    return key;
                }
            }
            return switch (value) {
                case "kill", "finalkill", "final" -> KILLS;
                case "win", "v" -> WINS;
                case "bed", "b" -> BEDS;
                case "star", "level", "lvl" -> LEVEL;
                case "exp" -> XP;
                default -> KILLS;
            };
        }
    }

    private final HyBedWars plugin;
    private final Map<UUID, PlayerStats> cache = new HashMap<>();
    private final Map<String, UUID> nameIndex = new HashMap<>();
    private File file;
    private BukkitTask saveTask;

    public StatsManager(HyBedWars plugin) {
        this.plugin = plugin;
    }

    public void load() {
        cache.clear();
        nameIndex.clear();
        file = new File(plugin.getDataFolder(), "stats.yml");
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    plugin.getLogger().info("已创建 stats.yml");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 stats.yml: " + e.getMessage());
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("players");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(key);
                String name = section.getString("name", key);
                PlayerStats stats = new PlayerStats(uuid, name);
                stats.setWins(section.getInt("wins"));
                stats.setLosses(section.getInt("losses"));
                stats.setKills(section.getInt("kills"));
                stats.setFinalKills(section.getInt("final-kills"));
                stats.setDeaths(section.getInt("deaths"));
                stats.setBedsBroken(section.getInt("beds-broken"));
                stats.setGames(section.getInt("games"));
                stats.setXp(section.getInt("xp"));
                cache.put(uuid, stats);
                nameIndex.put(name.toLowerCase(Locale.ROOT), uuid);
            } catch (IllegalArgumentException ignored) {
                // 忽略损坏的 UUID
            }
        }
        plugin.getLogger().info("已加载 " + cache.size() + " 名玩家的战绩。");
    }

    public void startAutoSave() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        long interval = Math.max(30, com.hybedwars.config.Settings.statsSaveInterval) * 20L;
        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::save, interval, interval);
    }

    public void stopAutoSave() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
    }

    /** 异步写盘。调用前会先在主线程快照数据。 */
    public void saveAsync() {
        save();
    }

    public synchronized void save() {
        if (file == null) {
            return;
        }
        FileConfiguration config = new YamlConfiguration();
        for (PlayerStats stats : cache.values()) {
            String path = "players." + stats.uuid();
            config.set(path + ".name", stats.name());
            config.set(path + ".wins", stats.wins());
            config.set(path + ".losses", stats.losses());
            config.set(path + ".kills", stats.kills());
            config.set(path + ".final-kills", stats.finalKills());
            config.set(path + ".deaths", stats.deaths());
            config.set(path + ".beds-broken", stats.bedsBroken());
            config.set(path + ".games", stats.games());
            config.set(path + ".xp", stats.xp());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("保存 stats.yml 失败: " + e.getMessage());
        }
    }

    // ==========================================================
    //  读取
    // ==========================================================

    public PlayerStats get(UUID uuid) {
        return uuid == null ? null : cache.get(uuid);
    }

    public PlayerStats getOrCreate(UUID uuid, String name) {
        PlayerStats stats = cache.get(uuid);
        if (stats == null) {
            stats = new PlayerStats(uuid, name == null ? uuid.toString().substring(0, 8) : name);
            cache.put(uuid, stats);
        }
        if (name != null) {
            stats.setName(name);
            nameIndex.put(name.toLowerCase(Locale.ROOT), uuid);
        }
        return stats;
    }

    public PlayerStats getOrCreate(Player player) {
        return getOrCreate(player.getUniqueId(), player.getName());
    }

    public PlayerStats getByName(String name) {
        if (name == null) {
            return null;
        }
        UUID uuid = nameIndex.get(name.toLowerCase(Locale.ROOT));
        if (uuid != null) {
            return cache.get(uuid);
        }
        for (PlayerStats stats : cache.values()) {
            if (stats.name().equalsIgnoreCase(name)) {
                return stats;
            }
        }
        return null;
    }

    public List<PlayerStats> top(SortKey key, int limit) {
        List<PlayerStats> list = new ArrayList<>(cache.values());
        list.sort(Comparator.comparingInt(key::value).reversed());
        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    // ==========================================================
    //  写入
    // ==========================================================

    public void recordPlayed(GamePlayer gp) {
        PlayerStats stats = getOrCreate(gp.uuid(), gp.name());
        stats.setGames(stats.games() + 1);
    }

    public void recordWin(GamePlayer gp) {
        PlayerStats stats = getOrCreate(gp.uuid(), gp.name());
        stats.setGames(stats.games() + 1);
        stats.setWins(stats.wins() + 1);
    }

    public void recordLoss(GamePlayer gp) {
        PlayerStats stats = getOrCreate(gp.uuid(), gp.name());
        stats.setGames(stats.games() + 1);
        stats.setLosses(stats.losses() + 1);
    }

    public void addKill(UUID uuid, int xp) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            stats.setKills(stats.kills() + 1);
            stats.addXp(xp);
        }
    }

    public void addFinalKill(UUID uuid, int xp) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            stats.setFinalKills(stats.finalKills() + 1);
            stats.addXp(xp);
        }
    }

    public void addBedBreak(UUID uuid, int xp) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            stats.setBedsBroken(stats.bedsBroken() + 1);
            stats.addXp(xp);
        }
    }

    public void addDeath(UUID uuid) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            stats.setDeaths(stats.deaths() + 1);
        }
    }

    public void addXp(UUID uuid, int amount) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            stats.addXp(amount);
        }
    }

    /** 只统计局数（平局）。 */
    public void recordGame(GamePlayer gp, boolean win, boolean counted) {
        if (!counted) {
            recordPlayed(gp);
        } else if (win) {
            recordWin(gp);
        } else {
            recordLoss(gp);
        }
    }
}
