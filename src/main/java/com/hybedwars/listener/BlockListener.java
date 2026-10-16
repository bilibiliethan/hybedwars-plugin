package com.hybedwars.listener;

import com.hybedwars.HyBedWars;
import com.hybedwars.config.Messages;
import com.hybedwars.config.Settings;
import com.hybedwars.game.Arena;
import com.hybedwars.game.GamePlayer;
import com.hybedwars.game.Team;
import com.hybedwars.game.TeamColor;
import com.hybedwars.item.Items;
import com.hybedwars.util.Chat;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 方块放置 / 破坏、床机制、爆炸保护。 */
public class BlockListener implements Listener {

    private final HyBedWars plugin;

    public BlockListener(HyBedWars plugin) {
        this.plugin = plugin;
    }

    private Arena arenaAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        for (Arena arena : plugin.arenaManager().all()) {
            if (location.getWorld().getName().equals(arena.worldName())) {
                return arena;
            }
        }
        return null;
    }

    // ==========================================================
    //  破坏
    // ==========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Arena arena = arenaAt(block.getLocation());
        if (arena == null) {
            return;
        }
        if (arena.isEditMode() && player.hasPermission("hybedwars.admin")) {
            return;
        }
        if (arena.status() != Arena.Status.RUNNING) {
            event.setCancelled(true);
            return;
        }
        GamePlayer gp = arena.player(player);
        if (gp == null || !gp.isAlive()) {
            event.setCancelled(true);
            return;
        }

        Team bedTeam = arena.bedOwner(block);
        if (bedTeam != null) {
            handleBedBreak(event, arena, bedTeam, player);
            return;
        }
        if (arena.isPlaced(block)) {
            arena.removePlaced(block);
            arena.removeWater(block);
            event.setDropItems(false);
            event.setExpToDrop(0);
            return;
        }
        event.setCancelled(true);
        Chat.send(player, "game.cannot-break-here");
    }

    private void handleBedBreak(BlockBreakEvent event, Arena arena, Team bedTeam, Player player) {
        event.setDropItems(false);
        event.setExpToDrop(0);
        TeamColor breakerTeam = arena.teamColorOf(player);
        if (!bedTeam.bedAlive()) {
            event.setCancelled(true);
            return;
        }
        if (breakerTeam == bedTeam.color()) {
            event.setCancelled(true);
            Chat.raw(player, "&c你不能破坏自己队伍的床。");
            return;
        }
        // 移除两格床
        bedTeam.removeBedBlocks();
        arena.destroyBed(bedTeam, player);
    }

    // ==========================================================
    //  放置
    // ==========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Arena arena = arenaAt(block.getLocation());
        if (arena == null) {
            return;
        }
        if (arena.isEditMode() && player.hasPermission("hybedwars.admin")) {
            return;
        }
        if (arena.status() != Arena.Status.RUNNING) {
            event.setCancelled(true);
            return;
        }
        GamePlayer gp = arena.player(player);
        if (gp == null || !gp.isAlive() || player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }
        if (!arena.insideArena(block.getLocation())) {
            event.setCancelled(true);
            Chat.send(player, "game.cannot-build-here");
            return;
        }
        if (block.getY() > Settings.maxBuildY) {
            event.setCancelled(true);
            Chat.send(player, "game.build-limit");
            return;
        }
        if (arena.nearEnemyBed(block.getLocation(), gp.teamColor())) {
            event.setCancelled(true);
            Chat.send(player, "game.cannot-place-near-bed");
            return;
        }

        ItemStack hand = event.getItemInHand();

        // 弹出塔
        if (Items.is(hand, "popup_tower")) {
            event.setCancelled(true);
            buildPopupTower(arena, gp, block, player);
            consume(player, hand);
            return;
        }
        // 魔法牛奶
        if (Items.is(hand, "magic_milk")) {
            event.setCancelled(true);
            return;
        }
        // TNT 自动点燃
        if (Items.is(hand, "tnt") || block.getType() == Material.TNT) {
            event.setCancelled(true);
            Block target = block;
            target.setType(Material.AIR, false);
            TNTPrimed tnt = block.getWorld().spawn(target.getLocation().add(0.5, 0, 0.5), TNTPrimed.class);
            tnt.setFuseTicks((int) Math.round(Settings.tntFuse * 20));
            tnt.setSource(player);
            consume(player, hand);
            return;
        }

        arena.addPlaced(block);
    }

    private void buildPopupTower(Arena arena, GamePlayer gp, Block base, Player player) {
        Material wool = gp.teamColor() == null ? Material.WHITE_WOOL : gp.teamColor().wool();
        Location origin = base.getLocation();
        int height = 4;
        for (int y = 0; y < height; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                        continue;
                    }
                    Block target = origin.clone().add(x, y, z).getBlock();
                    if (target.getType().isAir()) {
                        target.setType(wool, false);
                        arena.addPlaced(target);
                    }
                }
            }
        }
        Block center = origin.clone().add(0, height, 0).getBlock();
        center.setType(Material.LADDER, false);
        arena.addPlaced(center);
        Chat.raw(player, "&a已放置紧凑弹出塔。");
    }

    private void consume(Player player, ItemStack item) {
        if (item == null) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        int amount = item.getAmount();
        if (amount <= 1) {
            player.getInventory().removeItem(item);
        } else {
            item.setAmount(amount - 1);
        }
    }

    // ==========================================================
    //  爆炸
    // ==========================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        filterExplosion(event.blockList(), event.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        filterExplosion(event.blockList(), event.getBlock().getLocation());
    }

    private void filterExplosion(List<Block> blocks, Location center) {
        Arena arena = arenaAt(center);
        if (arena == null) {
            return;
        }
        List<Block> toRemove = new ArrayList<>();
        for (Block block : blocks) {
            Team bedTeam = arena.bedOwner(block);
            if (bedTeam != null) {
                toRemove.add(block);
                continue;
            }
            String name = block.getType().name();
            if (name.endsWith("_STAINED_GLASS") && arena.isPlaced(block)) {
                // 防爆玻璃
                toRemove.add(block);
                continue;
            }
            if (!arena.isPlaced(block)) {
                toRemove.add(block);
            }
        }
        blocks.removeAll(toRemove);
        for (Block block : blocks) {
            arena.removePlaced(block);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        Arena arena = arenaAt(event.getBlock().getLocation());
        if (arena == null) {
            return;
        }
        if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD
                || event.getCause() == BlockIgniteEvent.IgniteCause.LAVA) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        Arena arena = arenaAt(event.getBlock().getLocation());
        if (arena != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() == Material.FIRE) {
            Arena arena = arenaAt(event.getBlock().getLocation());
            if (arena != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent event) {
        Arena arena = arenaAt(event.getBlock().getLocation());
        if (arena != null && !arena.isPlaced(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFromTo(BlockFromToEvent event) {
        Arena arena = arenaAt(event.getBlock().getLocation());
        if (arena == null) {
            return;
        }
        Material type = event.getBlock().getType();
        if (type == Material.WATER || type == Material.LAVA) {
            if (event.getBlock().getY() <= Settings.voidY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Arena arena = arenaAt(event.getBlock().getLocation());
        if (arena == null) {
            return;
        }
        if (arena.status() != Arena.Status.RUNNING) {
            event.setCancelled(true);
            return;
        }
        ItemStack hand = player.getInventory().getItem(event.getHand());
        if (hand != null && hand.getType() == Material.LAVA_BUCKET) {
            event.setCancelled(true);
            Chat.raw(player, "&c对局中不允许放置岩浆。");
            return;
        }
        Block target = event.getBlock().getRelative(event.getBlockFace());
        arena.trackWater(target);
        arena.addPlaced(target);
    }

    @EventHandler(ignoreCancelled = true)
    public void onStructureGrow(org.bukkit.event.world.StructureGrowEvent event) {
        Arena arena = arenaAt(event.getLocation());
        if (arena != null) {
            event.setCancelled(true);
        }
    }
}
