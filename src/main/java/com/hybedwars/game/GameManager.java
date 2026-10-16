package com.hybedwars.game;

import com.hybedwars.HyBedWars;
import com.hybedwars.config.Messages;
import com.hybedwars.config.Settings;
import com.hybedwars.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 全局对局调度：主循环、匹配、进出游戏。 */
public class GameManager {

    private final HyBedWars plugin;
    private BukkitTask task;

    public GameManager(HyBedWars plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Arena arena : plugin.arenaManager().all()) {
            arena.despawnNpcs();
        }
    }

    private void tick() {
        for (Arena arena : new ArrayList<>(plugin.arenaManager().all())) {
            try {
                arena.tick();
            } catch (Throwable t) {
                plugin.getLogger().severe("地图 " + arena.name() + " 逻辑异常: " + t.getMessage());
            }
        }
    }

    // ==========================================================
    //  查询
    // ==========================================================

    public Arena arenaOf(Player player) {
        for (Arena arena : plugin.arenaManager().all()) {
            if (arena.contains(player)) {
                return arena;
            }
        }
        return null;
    }

    public GamePlayer findPlayer(Player player) {
        Arena arena = arenaOf(player);
        return arena == null ? null : arena.player(player);
    }

    public boolean isPlaying(Player player) {
        return arenaOf(player) != null;
    }

    // ==========================================================
    //  匹配
    // ==========================================================

    public Arena findAvailable(GameMode mode) {
        List<Arena> candidates = new ArrayList<>();
        for (Arena arena : plugin.arenaManager().all()) {
            if (arena.mode() != mode || arena.isEditMode()) {
                continue;
            }
            if (arena.status() == Arena.Status.RUNNING || arena.status() == Arena.Status.ENDING) {
                continue;
            }
            if (arena.onlinePlayers().size() >= mode.maxPlayers()) {
                continue;
            }
            candidates.add(arena);
        }
        // 优先人数最多的等待房，其次是已经开始的倒计时房
        candidates.sort(Comparator
                .comparingInt((Arena a) -> a.status() == Arena.Status.STARTING ? 1 : 0).reversed()
                .thenComparingInt((Arena a) -> -a.onlinePlayers().size()));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /** 加入一张该模式的可用地图。 */
    public boolean join(Player player, GameMode mode) {
        if (isPlaying(player)) {
            Chat.send(player, "join.already-playing");
            return false;
        }
        Arena arena = findAvailable(mode);
        if (arena == null) {
            Chat.send(player, "join.no-arena", "%mode%", mode.display());
            return false;
        }
        String error = arena.join(player, false);
        if (error != null) {
            Chat.send(player, "join.no-arena", "%mode%", mode.display());
            return false;
        }
        return true;
    }

    public boolean join(Player player, Arena arena) {
        if (isPlaying(player)) {
            Chat.send(player, "join.already-playing");
            return false;
        }
        String error = arena.join(player, false);
        if (error != null) {
            player.sendMessage(Chat.color(Messages.prefix() + error));
            return false;
        }
        return true;
    }

    public boolean leave(Player player) {
        Arena arena = arenaOf(player);
        if (arena == null) {
            Chat.send(player, "join.not-playing");
            return false;
        }
        arena.quit(player, true);
        Chat.send(player, "join.left");
        return true;
    }

    /** 服务器关闭时强制清理。 */
    public void shutdownAll() {
        for (Arena arena : plugin.arenaManager().all()) {
            arena.despawnNpcs();
            for (Player player : arena.onlinePlayers()) {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                player.getInventory().clear();
                if (Settings.lobby != null) {
                    player.teleport(Settings.lobby);
                }
            }
        }
    }
}
