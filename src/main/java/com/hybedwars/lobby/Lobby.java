package com.hybedwars.lobby;

import com.hybedwars.HyBedWars;
import com.hybedwars.config.Settings;
import com.hybedwars.util.Chat;
import com.hybedwars.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 大厅：一个独立的可从地图模板导入的世界，里面放置 NPC，
 * 玩家点击 NPC 即可加入对应的竞技场。
 */
public class Lobby {

    private final HyBedWars plugin;
    private final NamespacedKey key;
    private World world;
    private final Map<UUID, String> npcArena = new LinkedHashMap<>();

    public Lobby(HyBedWars plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "lobby_npc");
    }

    public World world() {
        return world;
    }

    /** 加载大厅世界并生成 NPC。未配置 lobby.world 时不做任何事。 */
    public void load() {
        npcArena.clear();
        world = null;
        String worldName = Settings.lobbyWorld;
        if (worldName == null || worldName.isBlank()) {
            return;
        }
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            world = existing;
        } else {
            Path template = new File(plugin.getDataFolder(),
                    Settings.templateFolder + File.separator + worldName).toPath();
            if (Settings.copyTemplate && Files.isDirectory(template)) {
                Path target = WorldUtil.worldFolder(worldName).toPath();
                WorldUtil.deleteDirectory(target);
                if (!WorldUtil.copyDirectory(template, target)) {
                    plugin.getLogger().severe("大厅地图模板复制失败：" + template);
                }
            }
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            creator.generateStructures(false);
            world = Bukkit.createWorld(creator);
        }
        if (world == null) {
            plugin.getLogger().severe("无法加载大厅世界：" + worldName);
            return;
        }
        applyWorldSettings(world);
        spawnNpcs();
        plugin.getLogger().info("大厅世界已加载：" + worldName + "，NPC " + npcArena.size() + " 个。");
    }

    private void applyWorldSettings(World target) {
        if (Settings.disableAutosave) {
            target.setAutoSave(false);
        }
        target.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        target.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        target.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        target.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        target.setGameRule(GameRule.KEEP_INVENTORY, false);
        target.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        target.setGameRule(GameRule.NATURAL_REGENERATION, true);
        target.setGameRule(GameRule.DO_FIRE_TICK, false);
        target.setGameRule(GameRule.MOB_GRIEFING, false);
        target.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        target.setGameRule(GameRule.DO_INSOMNIA, false);
        target.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        target.setDifficulty(Difficulty.PEACEFUL);
        target.setStorm(false);
        target.setThundering(false);
        target.setTime(6000);
    }

    private void spawnNpcs() {
        for (Settings.LobbyNpc spec : Settings.lobbyNpcs) {
            if (spec.arena == null || spec.arena.isBlank() || spec.location == null) {
                continue;
            }
            Location location = spec.location.clone();
            location.setWorld(world);
            try {
                Villager villager = world.spawn(location, Villager.class);
                villager.setAI(false);
                villager.setInvulnerable(true);
                villager.setSilent(true);
                villager.setCollidable(false);
                villager.setPersistent(true);
                villager.setRemoveWhenFarAway(false);
                villager.setCustomName(Chat.color("&a&l加入 &e" + spec.arena + " &7(右键)"));
                villager.setCustomNameVisible(true);
                villager.getPersistentDataContainer().set(key,
                        org.bukkit.persistence.PersistentDataType.STRING, "lobby_npc:" + spec.arena);
                npcArena.put(villager.getUniqueId(), spec.arena);
            } catch (Throwable t) {
                plugin.getLogger().warning("无法生成大厅 NPC：" + t.getMessage());
            }
        }
    }

    /** 该实体是否为大厅 NPC；是则返回其关联的地图名，否则 null。 */
    public String npcArena(UUID entityId) {
        return npcArena.get(entityId);
    }

    /** 卸载大厅世界（不删除磁盘上的世界，保留下次启动复制用的模板）。 */
    public void unload() {
        npcArena.clear();
        if (world != null) {
            WorldUtil.unload(world.getName());
            world = null;
        }
    }
}
