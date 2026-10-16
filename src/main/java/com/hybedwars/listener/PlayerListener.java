package com.hybedwars.listener;

import com.hybedwars.HyBedWars;
import com.hybedwars.config.Settings;
import com.hybedwars.game.Arena;
import com.hybedwars.game.GamePlayer;
import com.hybedwars.stats.PlayerStats;
import com.hybedwars.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/** 玩家生命周期与基础行为限制。 */
public class PlayerListener implements Listener {

    private final HyBedWars plugin;

    public PlayerListener(HyBedWars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.statsManager().getOrCreate(player);
        if (Settings.forceLobby && plugin.gameManager().arenaOf(player) == null) {
            Location dest = Settings.lobby;
            if (dest == null && plugin.lobby() != null && plugin.lobby().world() != null) {
                dest = plugin.lobby().world().getSpawnLocation().clone().add(0.5, 0, 0.5);
            }
            if (dest != null) {
                player.teleport(dest);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.gameManager().arenaOf(player);
        if (arena != null) {
            arena.quit(player, false);
        }
        plugin.scoreboardManager().remove(player);
        plugin.statsManager().getOrCreate(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.gameManager().arenaOf(player);
        if (arena == null) {
            if (Settings.lobby != null) {
                event.setRespawnLocation(Settings.lobby);
            }
            return;
        }
        GamePlayer gp = arena.player(player);
        if (gp == null) {
            return;
        }
        Location target = arena.spectatorSpawn();
        if (target == null) {
            target = arena.world() != null ? arena.world().getSpawnLocation() : Settings.lobby;
        }
        if (target != null) {
            event.setRespawnLocation(target);
        }
        boolean respawning = gp.isRespawning();
        Location finalTarget = target;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (respawning) {
                player.setGameMode(GameMode.SPECTATOR);
                player.setAllowFlight(true);
                if (finalTarget != null) {
                    player.teleport(finalTarget);
                }
            } else {
                player.setGameMode(GameMode.SPECTATOR);
                player.setAllowFlight(true);
            }
        });
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (plugin.gameManager().isPlaying(player)) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.gameManager().arenaOf(player);
        if (arena == null) {
            return;
        }
        if (arena.status() != Arena.Status.RUNNING) {
            event.setCancelled(true);
            return;
        }
        GamePlayer gp = arena.player(player);
        if (gp == null || !gp.isAlive()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.gameManager().arenaOf(player);
        if (arena != null) {
            event.setCancelled(true);
        }
    }

    /**
     * 经验模式：捡到的铁、金、钻石、绿宝石不会进入背包，
     * 而是按配置比例直接转化为经验值。
     */
    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Arena arena = plugin.gameManager().arenaOf(player);
        if (arena == null || !arena.experienceMode()) {
            return;
        }
        GamePlayer gp = arena.player(player);
        if (gp == null) {
            return;
        }
        Item dropped = event.getItem();
        ItemStack stack = dropped.getItemStack();
        if (Settings.xpValueOf(stack.getType()) <= 0) {
            return;
        }
        event.setCancelled(true);
        arena.grantResourceExperience(gp, stack.getType(), stack.getAmount());
        dropped.remove();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.gameManager().arenaOf(player);
        if (arena == null) {
            return;
        }
        if (Settings.blockedCommands.isEmpty()) {
            return;
        }
        String message = event.getMessage().toLowerCase(Locale.ROOT);
        for (String allowed : Settings.allowedCommands) {
            if (message.startsWith(allowed)) {
                return;
            }
        }
        for (String blocked : Settings.blockedCommands) {
            if (message.startsWith(blocked)) {
                event.setCancelled(true);
                Chat.raw(player, "&c对局中不允许使用该命令。");
                return;
            }
        }
    }

    /** 供命令系统显示的战绩输出。 */
    public static void showStats(org.bukkit.command.CommandSender sender, PlayerStats stats) {
        sender.sendMessage(Chat.color("&8&m--------&r &c&l" + stats.name() + " &7的战绩 &8&m--------"));
        sender.sendMessage(Chat.color("&7星级：&e⭐" + stats.level() + " &7经验：&e" + stats.xp()));
        sender.sendMessage(Chat.color("&7胜利：&a" + stats.wins() + " &7失败：&c" + stats.losses()
                + " &7胜率：&e" + stats.winRate() + "%"));
        sender.sendMessage(Chat.color("&7击杀：&e" + stats.kills() + " &7最终击杀：&e" + stats.finalKills()));
        sender.sendMessage(Chat.color("&7破坏床数：&e" + stats.bedsBroken() + " &7死亡：&e" + stats.deaths()));
        sender.sendMessage(Chat.color("&7游戏场数：&e" + stats.games() + " &7K/D：&e" + stats.kdr()));
    }
}
