package com.hybedwars.listener;

import com.hybedwars.HyBedWars;
import com.hybedwars.config.Settings;
import com.hybedwars.game.Arena;
import com.hybedwars.game.GamePlayer;
import com.hybedwars.game.TeamColor;
import com.hybedwars.item.Items;
import com.hybedwars.util.Chat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/** 特殊道具行为：火球、搭桥蛋、床虱、梦境守卫、魔法牛奶、末影珍珠等。 */
public class ItemListener implements Listener {

    private final HyBedWars plugin;

    public ItemListener(HyBedWars plugin) {
        this.plugin = plugin;
    }

    // ==========================================================
    //  右键使用
    // ==========================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        Arena arena = plugin.gameManager().arenaOf(player);
        if (arena == null) {
            return;
        }
        GamePlayer gp = arena.player(player);
        if (gp == null || !gp.isAlive() || arena.status() != Arena.Status.RUNNING) {
            return;
        }
        if (!event.getAction().isRightClick()) {
            return;
        }

        if (Items.is(item, "fireball")) {
            event.setCancelled(true);
            launchFireball(player, gp, arena);
            return;
        }
        if (Items.is(item, "magic_milk")) {
            event.setCancelled(true);
            gp.giveMagicMilk(30);
            consume(player, item);
            Chat.raw(player, "&a你获得了 30 秒的陷阱免疫。");
            return;
        }
        if (Items.is(item, "rescue_platform")) {
            event.setCancelled(true);
            if (player.isOnGround() && player.getFallDistance() <= 0.0f) {
                Chat.raw(player, "&c只能在空中（坠落时）使用自救平台。");
                return;
            }
            if (!gp.canUseRescue()) {
                Chat.raw(player, "&c自救平台冷却中…");
                return;
            }
            gp.markRescueUsed();
            placeRescuePlatform(player, arena);
            consume(player, item);
            Chat.raw(player, "&a自救平台已生成！");
            return;
        }
        if (Items.is(item, "dream_defender")) {
            event.setCancelled(true);
            spawnDefender(player, gp, arena);
            consume(player, item);
            return;
        }
        if (item.getType() == Material.ENDER_PEARL) {
            if (!gp.canUsePearl()) {
                event.setCancelled(true);
                Chat.raw(player, "&c末影珍珠冷却中（" + Settings.pearlCooldown + " 秒）");
                return;
            }
            gp.markPearlUsed();
        }
    }

    private void launchFireball(Player player, GamePlayer gp, Arena arena) {
        if (!gp.canUseFireball()) {
            return;
        }
        gp.markFireballUsed();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize().multiply(Settings.fireballSpeed);
        Fireball fireball = player.getWorld().spawn(eye.add(direction.clone().normalize()), Fireball.class);
        fireball.setDirection(direction);
        fireball.setYield((float) Settings.fireballPower);
        fireball.setIsIncendiary(false);
        fireball.setShooter(player);
        consume(player, player.getInventory().getItemInMainHand());
    }

    private void spawnDefender(Player player, GamePlayer gp, Arena arena) {
        Location location = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize());
        IronGolem golem = player.getWorld().spawn(location, IronGolem.class);
        golem.setPlayerCreated(true);
        golem.setCustomName(Chat.color((gp.teamColor() == null ? "" : gp.teamColor().code())
                + "梦境守卫"));
        golem.setCustomNameVisible(false);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (golem.isValid()) {
                golem.remove();
            }
        }, 20L * 120);
        Chat.raw(player, "&a已召唤梦境守卫。");
    }

    // ==========================================================
    //  投掷物
    // ==========================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player player)) {
            return;
        }
        Arena arena = plugin.gameManager().arenaOf(player);
        if (arena == null) {
            return;
        }
        GamePlayer gp = arena.player(player);
        if (gp == null) {
            return;
        }
        if (projectile instanceof Egg egg && Items.is(egg.getItem(), "bridge_egg")) {
            buildBridge(egg, player, gp, arena);
            egg.remove();
            return;
        }
        if (projectile instanceof Snowball snowball && Items.is(snowball.getItem(), "bed_bug")) {
            spawnBedBug(snowball, player, gp, arena);
            snowball.remove();
        }
    }

    private void buildBridge(Egg egg, Player player, GamePlayer gp, Arena arena) {
        Location hit = egg.getLocation();
        Location from = player.getLocation();
        Vector back = from.toVector().subtract(hit.toVector());
        back.setY(0);
        if (back.lengthSquared() < 0.01) {
            Vector direction = player.getLocation().getDirection();
            direction.setY(0);
            back = direction.multiply(-1);
        }
        back.normalize();
        Vector side = new Vector(-back.getZ(), 0, back.getX()).normalize();
        Material wool = gp.teamColor() == null ? Material.WHITE_WOOL : gp.teamColor().wool();

        int bottomY = Math.max(hit.getBlockY() - 1, Settings.voidY);
        int length = (int) Math.min(48, Math.hypot(hit.getX() - from.getX(), hit.getZ() - from.getZ()));
        int placed = 0;
        for (int i = 0; i <= length; i++) {
            Location center = new Location(hit.getWorld(),
                    hit.getX() + back.getX() * i, bottomY, hit.getZ() + back.getZ() * i);
            for (int offset = -1; offset <= 1; offset++) {
                Location target = new Location(hit.getWorld(),
                        center.getX() + side.getX() * offset, bottomY, center.getZ() + side.getZ() * offset);
                Block block = target.getBlock();
                if (block.getType().isAir() || block.getType() == Material.WATER) {
                    block.setType(wool, false);
                    arena.addPlaced(block);
                    placed++;
                }
            }
        }
        if (placed == 0) {
            Chat.raw(player, "&c搭桥失败：落点附近没有空间。");
        }
    }

    private void placeRescuePlatform(Player player, Arena arena) {
        Location loc = player.getLocation();
        int baseY = loc.getBlockY() - 1;
        int placed = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block target = loc.getWorld().getBlockAt(
                        loc.getBlockX() + dx, baseY, loc.getBlockZ() + dz);
                if (target.getType().isAir() || target.getType() == Material.WATER) {
                    target.setType(Material.STONE, false);
                    arena.addPlaced(target);
                    placed++;
                }
            }
        }
        if (placed == 0) {
            Chat.raw(player, "&c脚下没有空间放置平台。");
        }
    }

    private void spawnBedBug(Snowball snowball, Player player, GamePlayer gp, Arena arena) {
        Location location = snowball.getLocation();
        Silverfish silverfish = location.getWorld().spawn(location, Silverfish.class);
        silverfish.setCustomName(Chat.color("&c床虱"));
        Player target = findNearestEnemy(arena, gp, location);
        if (target != null) {
            silverfish.setTarget(target);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (silverfish.isValid()) {
                silverfish.remove();
            }
        }, 20L * 30);
    }

    private Player findNearestEnemy(Arena arena, GamePlayer self, Location location) {
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player candidate : arena.onlinePlayers()) {
            GamePlayer gp = arena.player(candidate);
            if (gp == null || !gp.isAlive() || gp.teamColor() == self.teamColor()) {
                continue;
            }
            if (!candidate.getWorld().equals(location.getWorld())) {
                continue;
            }
            double distance = candidate.getLocation().distanceSquared(location);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    // ==========================================================
    //  海绵吸水
    // ==========================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.SPONGE) {
            return;
        }
        Arena arena = plugin.gameManager().arenaOf(event.getPlayer());
        if (arena == null) {
            return;
        }
        Block center = event.getBlockPlaced();
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    Block block = center.getRelative(x, y, z);
                    if (block.getType() == Material.WATER) {
                        block.setType(Material.AIR, false);
                        arena.removeWater(block);
                    }
                }
            }
        }
    }

    private void consume(Player player, ItemStack item) {
        if (item == null || player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }
        if (item.getAmount() <= 1) {
            player.getInventory().removeItem(item);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }

    /** 队伍颜色辅助（供扩展使用）。 */
    public static Material woolOf(TeamColor color) {
        return color == null ? Material.WHITE_WOOL : color.wool();
    }

    /** 未使用但保留，便于后续扩展实体过滤。 */
    public static boolean isOwnedEntity(Entity entity) {
        return entity instanceof Silverfish || entity instanceof IronGolem;
    }
}
