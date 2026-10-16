package com.hybedwars.listener;

import com.hybedwars.HyBedWars;
import com.hybedwars.config.Settings;
import com.hybedwars.game.Arena;
import com.hybedwars.game.GamePlayer;
import com.hybedwars.game.Team;
import com.hybedwars.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.IronGolem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/** 战斗、死亡与击杀归属。 */
public class CombatListener implements Listener {

    private final HyBedWars plugin;

    public CombatListener(HyBedWars plugin) {
        this.plugin = plugin;
    }

    // ==========================================================
    //  伤害
    // ==========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (!(event.getEntity() instanceof Player victim)) {
            if (attacker != null) {
                Arena arena = plugin.gameManager().arenaOf(attacker);
                if (arena != null && arena.status() != Arena.Status.RUNNING) {
                    event.setCancelled(true);
                }
            }
            return;
        }
        Arena arena = plugin.gameManager().arenaOf(victim);
        if (arena == null) {
            return;
        }
        if (arena.status() != Arena.Status.RUNNING) {
            event.setCancelled(true);
            return;
        }
        if (attacker == null) {
            return;
        }
        GamePlayer victimGp = arena.player(victim);
        GamePlayer attackerGp = arena.player(attacker);
        if (victimGp == null || attackerGp == null) {
            return;
        }
        if (victimGp.teamColor() != null && victimGp.teamColor() == attackerGp.teamColor()) {
            event.setCancelled(true);
            return;
        }
        if (arena.elapsedSeconds() < Settings.damageImmunity) {
            event.setCancelled(true);
            return;
        }
        victimGp.setLastDamager(attacker.getUniqueId());
        if (event.getDamager() instanceof Player) {
            victimGp.setVoidDeath(false);
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Arena arena = plugin.gameManager().arenaOf(player);
        if (arena == null) {
            return;
        }
        if (arena.status() != Arena.Status.RUNNING) {
            event.setCancelled(true);
            return;
        }
        GamePlayer gp = arena.player(player);
        if (gp == null) {
            return;
        }
        if (!gp.isAlive() || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            gp.setVoidDeath(true);
            player.setHealth(0);
            return;
        }
        if (arena.elapsedSeconds() < Settings.damageImmunity) {
            event.setCancelled(true);
        }
    }

    // ==========================================================
    //  死亡
    // ==========================================================

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Arena arena = plugin.gameManager().arenaOf(player);
        if (arena == null) {
            return;
        }
        GamePlayer gp = arena.player(player);
        if (gp == null) {
            return;
        }
        event.setDeathMessage(null);
        event.setDroppedExp(0);

        Player killer = player.getKiller();
        if (killer == null && gp.lastDamager() != null && gp.wasRecentlyDamaged(10)) {
            Player candidate = Bukkit.getPlayer(gp.lastDamager());
            if (candidate != null && !candidate.equals(player)
                    && arena.teamColorOf(candidate) != gp.teamColor()) {
                killer = candidate;
            }
        }
        Team team = gp.team();
        boolean finalDeath = team == null || !team.bedAlive();
        if (finalDeath || !Settings.dropItemsOnDeath) {
            event.getDrops().clear();
        }
        arena.handleDeath(gp, killer);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Arena arena = null;
        if (entity.getWorld() != null) {
            for (Arena candidate : plugin.arenaManager().all()) {
                if (entity.getWorld().getName().equals(candidate.worldName())) {
                    arena = candidate;
                    break;
                }
            }
        }
        if (arena == null) {
            return;
        }
        if (entity instanceof Silverfish || entity instanceof IronGolem) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    /** 队伍升级广播辅助。 */
    public static void announce(Team team, String message) {
        for (Player player : team.onlinePlayers()) {
            player.sendMessage(Chat.color(message));
        }
    }
}
