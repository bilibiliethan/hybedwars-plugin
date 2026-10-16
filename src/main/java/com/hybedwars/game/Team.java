package com.hybedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 一支队伍在地图中的全部状态。 */
public class Team {

    private final Arena arena;
    private final TeamColor color;

    private Location spawn;
    private Location bedFoot;
    private org.bukkit.block.BlockFace bedFacing = org.bukkit.block.BlockFace.NORTH;
    private Location generator;
    /** 队伍生成器（铁/金）的自定义刷新间隔（秒/个），0 = 默认。 */
    private double generatorRate = 0;

    private boolean bedAlive = true;
    private boolean eliminated = false;
    private boolean trapTriggeredThisVisit = false;

    private final Set<UUID> members = new LinkedHashSet<>();
    private final Map<UpgradeType, Integer> upgrades = new EnumMap<>(UpgradeType.class);
    private final Deque<UpgradeType> traps = new ArrayDeque<>();

    public Team(Arena arena, TeamColor color) {
        this.arena = arena;
        this.color = color;
    }

    // ---------- 基础属性 ----------

    public Arena arena() {
        return arena;
    }

    public TeamColor color() {
        return color;
    }

    public Location spawn() {
        return spawn == null ? null : spawn.clone();
    }

    public void setSpawn(Location spawn) {
        this.spawn = spawn == null ? null : com.hybedwars.util.Loc.toBlock(spawn);
    }

    public Location bedFoot() {
        return bedFoot == null ? null : bedFoot.clone();
    }

    public void setBedFoot(Location bedFoot) {
        this.bedFoot = bedFoot == null ? null : com.hybedwars.util.Loc.toBlock(bedFoot);
    }

    public org.bukkit.block.BlockFace bedFacing() {
        return bedFacing;
    }

    public void setBedFacing(org.bukkit.block.BlockFace facing) {
        this.bedFacing = facing == null ? org.bukkit.block.BlockFace.NORTH : facing;
    }

    public Location generator() {
        return generator == null ? null : generator.clone();
    }

    public void setGenerator(Location generator) {
        this.generator = generator == null ? null : generator.clone();
    }

    public double generatorRate() {
        return generatorRate;
    }

    public void setGeneratorRate(double generatorRate) {
        this.generatorRate = Math.max(0, generatorRate);
    }

    public boolean bedAlive() {
        return bedAlive;
    }

    public void setBedAlive(boolean bedAlive) {
        this.bedAlive = bedAlive;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    public boolean isConfigured() {
        return spawn != null && bedFoot != null && generator != null;
    }

    // ---------- 成员 ----------

    public Set<UUID> memberIds() {
        return members;
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean hasMember(UUID uuid) {
        return members.contains(uuid);
    }

    public int size() {
        return members.size();
    }

    public java.util.List<Player> onlinePlayers() {
        java.util.List<Player> list = new java.util.ArrayList<>();
        for (UUID uuid : members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                list.add(player);
            }
        }
        return list;
    }

    /** 仍然存活（非旁观）的成员数量。 */
    public int aliveCount() {
        int count = 0;
        for (Player player : onlinePlayers()) {
            GamePlayer gp = arena.player(player);
            if (gp != null && gp.isAlive()) {
                count++;
            }
        }
        return count;
    }

    // ---------- 升级 ----------

    public int upgradeLevel(UpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }

    public void setUpgradeLevel(UpgradeType type, int level) {
        if (level <= 0) {
            upgrades.remove(type);
        } else {
            upgrades.put(type, level);
        }
    }

    public boolean isMaxed(UpgradeType type) {
        return upgradeLevel(type) >= type.maxLevel();
    }

    public int nextPrice(UpgradeType type) {
        return type.price(upgradeLevel(type));
    }

    public Map<UpgradeType, Integer> upgrades() {
        return upgrades;
    }

    // ---------- 陷阱队列 ----------

    public Deque<UpgradeType> traps() {
        return traps;
    }

    public int trapCount() {
        return traps.size();
    }

    public void addTrap(UpgradeType trap) {
        traps.addLast(trap);
    }

    public UpgradeType popTrap() {
        return traps.pollFirst();
    }

    public boolean consumeTrapTrigger() {
        if (trapTriggeredThisVisit) {
            return false;
        }
        trapTriggeredThisVisit = true;
        return true;
    }

    public void resetTrapTrigger() {
        trapTriggeredThisVisit = false;
    }

    // ---------- 床 ----------

    /** 在配置的位置重建床（每局开始时调用，保证床一定是完整状态）。 */
    public void placeBed() {
        if (bedFoot == null || bedFoot.getWorld() == null) {
            return;
        }
        World world = bedFoot.getWorld();
        Material material = color.bed();
        Block footBlock = world.getBlockAt(bedFoot.getBlockX(), bedFoot.getBlockY(), bedFoot.getBlockZ());
        org.bukkit.block.BlockFace facing = bedFacing;

        // 让"脚"落在玩家设置的那一格：根据朝向反推
        Block headBlock = footBlock.getRelative(facing);

        try {
            Bed footData = (Bed) material.createBlockData();
            footData.setFacing(facing);
            footData.setPart(Bed.Part.FOOT);
            footBlock.setBlockData(footData, false);

            Bed headData = (Bed) material.createBlockData();
            headData.setFacing(facing);
            headData.setPart(Bed.Part.HEAD);
            headBlock.setBlockData(headData, false);
        } catch (Exception e) {
            footBlock.setType(material, false);
        }
        bedAlive = true;
        eliminated = false;
    }

    /** 移除床方块。 */
    public void removeBedBlocks() {
        if (bedFoot == null || bedFoot.getWorld() == null) {
            return;
        }
        World world = bedFoot.getWorld();
        for (org.bukkit.block.BlockFace face : new org.bukkit.block.BlockFace[]{
                org.bukkit.block.BlockFace.SELF, bedFacing}) {
            Block block = world.getBlockAt(bedFoot.getBlockX(), bedFoot.getBlockY(), bedFoot.getBlockZ());
            if (face != org.bukkit.block.BlockFace.SELF) {
                block = block.getRelative(face);
            }
            if (block.getType().name().endsWith("_BED")) {
                block.setType(Material.AIR, false);
            }
        }
    }

    /** 该方块是否属于本队的床。 */
    public boolean isBedBlock(Block block) {
        if (bedFoot == null) {
            return false;
        }
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        if (y != bedFoot.getBlockY()) {
            return false;
        }
        if (x == bedFoot.getBlockX() && z == bedFoot.getBlockZ()) {
            return true;
        }
        Block head = bedFoot.getBlock().getRelative(bedFacing);
        return head.getX() == x && head.getZ() == z && head.getY() == y;
    }

    /** 玩家是否位于该队基地内（出生点 / 床附近）。 */
    public boolean isInsideBase(Location location, int radius) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Location center = spawn != null ? spawn : bedFoot;
        if (center == null || !center.getWorld().equals(location.getWorld())) {
            return false;
        }
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        double dy = Math.abs(location.getY() - center.getY());
        return (dx * dx + dz * dz) <= (double) radius * radius && dy <= radius + 4;
    }

    public void reset() {
        bedAlive = true;
        eliminated = false;
        trapTriggeredThisVisit = false;
        members.clear();
        upgrades.clear();
        traps.clear();
    }
}
