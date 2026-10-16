package com.hybedwars.game;

import com.hybedwars.HyBedWars;
import com.hybedwars.lobby.Lobby;
import com.hybedwars.config.Messages;
import com.hybedwars.config.Settings;
import com.hybedwars.generator.Generator;
import com.hybedwars.util.Chat;
import com.hybedwars.util.Loc;
import com.hybedwars.util.WorldUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 一张地图 / 一场对局的全部状态与逻辑。 */
public class Arena {

    public enum Status {
        /** 等待玩家 */
        WAITING,
        /** 人数达标，正在倒计时 */
        STARTING,
        /** 游戏进行中 */
        RUNNING,
        /** 结束演出中 */
        ENDING
    }

    private final HyBedWars plugin;
    private final String name;

    private GameMode mode = GameMode.SOLO;
    /** 本图是否使用经验经济（默认由模式决定，可用 /bw arena toggleexp 单独开关）。 */
    private boolean experienceEconomy;
    private Status status = Status.WAITING;
    private String worldName;
    private World world;

    private Location waitingSpawn;
    private Location spectatorSpawn;
    private Location shopNpcLocation;
    private Location upgradeNpcLocation;
    private Location corner1;
    private Location corner2;

    private final Map<TeamColor, Team> teams = new EnumMap<>(TeamColor.class);
    private final Map<UUID, GamePlayer> players = new LinkedHashMap<>();
    private final List<Generator> generators = new ArrayList<>();
    private final Set<Long> placedBlocks = new HashSet<>();
    private final List<GameEvent> events = new ArrayList<>();
    private final List<UUID> npcs = new ArrayList<>();
    private final Set<Long> temporaryWater = new LinkedHashSet<>();

    private int countdown;
    private int endTimer;
    private int ticks;
    private int eventIndex;
    private int globalDiamondTier = 1;
    private int globalEmeraldTier = 1;
    private TeamColor winningTeam;
    private boolean editMode;
    /** 本次编辑是否因缺少地图模板而使用了占位平台世界（非真实地图）。 */
    private boolean usedPlaceholder;

    public Arena(HyBedWars plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.experienceEconomy = mode.experience();
        resetTeams();
    }

    // ==========================================================
    //  基础属性
    // ==========================================================

    public String name() {
        return name;
    }

    public HyBedWars plugin() {
        return plugin;
    }

    public GameMode mode() {
        return mode;
    }

    public void setMode(GameMode mode) {
        this.mode = mode;
        this.experienceEconomy = mode.experience();
        resetTeams();
    }

    /**
     * 本图是否为经验模式。
     * <p>经验模式下：捡到的铁/金/钻石/绿宝石自动转化为经验值，商店全部用经验结算。
     */
    public boolean experienceMode() {
        return experienceEconomy;
    }

    public void setExperienceEconomy(boolean experienceEconomy) {
        this.experienceEconomy = experienceEconomy;
    }

    public Status status() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    public World world() {
        return world;
    }

    public String worldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public int countdown() {
        return countdown;
    }

    public int elapsedSeconds() {
        return ticks / 20;
    }

    public Location waitingSpawn() {
        return clone(waitingSpawn);
    }

    public void setWaitingSpawn(Location location) {
        this.waitingSpawn = Loc.toBlock(location);
    }

    public Location spectatorSpawn() {
        return clone(spectatorSpawn);
    }

    public void setSpectatorSpawn(Location location) {
        this.spectatorSpawn = Loc.toBlock(location);
    }

    public Location shopNpcLocation() {
        return clone(shopNpcLocation);
    }

    public void setShopNpcLocation(Location location) {
        this.shopNpcLocation = location == null ? null : location.clone();
    }

    public Location upgradeNpcLocation() {
        return clone(upgradeNpcLocation);
    }

    public void setUpgradeNpcLocation(Location location) {
        this.upgradeNpcLocation = location == null ? null : location.clone();
    }

    public Location corner1() {
        return clone(corner1);
    }

    public Location corner2() {
        return clone(corner2);
    }

    public void setCorners(Location first, Location second) {
        this.corner1 = Loc.toBlock(first);
        this.corner2 = Loc.toBlock(second);
    }

    public boolean isEditMode() {
        return editMode;
    }

    /** 本次编辑是否使用了占位平台世界（地图模板缺失时的兜底）。 */
    public boolean usedPlaceholder() {
        return usedPlaceholder;
    }

    private Location clone(Location location) {
        return location == null ? null : location.clone();
    }

    public TeamColor winner() {
        return winningTeam;
    }

    public List<Generator> generators() {
        return generators;
    }

    public List<GameEvent> events() {
        return events;
    }

    public Collection<GamePlayer> gamePlayers() {
        return players.values();
    }

    public List<Player> onlinePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                list.add(player);
            }
        }
        return list;
    }

    // ==========================================================
    //  队伍
    // ==========================================================

    public void resetTeams() {
        teams.clear();
        for (TeamColor color : mode.teams()) {
            teams.put(color, new Team(this, color));
        }
    }

    public Map<TeamColor, Team> teams() {
        return teams;
    }

    public Team team(TeamColor color) {
        return color == null ? null : teams.get(color);
    }

    public Team teamOf(Player player) {
        GamePlayer gp = player(player);
        return gp == null ? null : gp.team();
    }

    public TeamColor teamColorOf(Player player) {
        GamePlayer gp = player(player);
        return gp == null ? null : gp.teamColor();
    }

    public GamePlayer player(Player player) {
        return players.get(player.getUniqueId());
    }

    public GamePlayer player(UUID uuid) {
        return players.get(uuid);
    }

    public boolean contains(Player player) {
        return players.containsKey(player.getUniqueId());
    }

    /** 配置是否完整。 */
    public List<String> missingParts() {
        List<String> missing = new ArrayList<>();
        if (worldName == null || worldName.isBlank()) {
            missing.add("世界");
        }
        if (waitingSpawn == null) {
            missing.add("等待点");
        }
        if (spectatorSpawn == null) {
            missing.add("观战点");
        }
        for (TeamColor color : mode.teams()) {
            Team team = teams.get(color);
            if (team == null || team.spawn() == null) {
                missing.add(color.display() + "出生点");
            }
            if (team == null || team.bedFoot() == null) {
                missing.add(color.display() + "床");
            }
            if (team == null || team.generator() == null) {
                missing.add(color.display() + "资源点");
            }
        }
        return missing;
    }

    public int aliveTeams() {
        int count = 0;
        for (Team team : teams.values()) {
            if (!team.isEliminated()) {
                count++;
            }
        }
        return count;
    }

    // ==========================================================
    //  方块记录
    // ==========================================================

    public boolean isPlaced(Block block) {
        return placedBlocks.contains(Loc.blockKey(block.getX(), block.getY(), block.getZ()));
    }

    public void addPlaced(Block block) {
        placedBlocks.add(Loc.blockKey(block.getX(), block.getY(), block.getZ()));
    }

    public void removePlaced(Block block) {
        placedBlocks.remove(Loc.blockKey(block.getX(), block.getY(), block.getZ()));
    }

    public void trackWater(Block block) {
        temporaryWater.add(Loc.blockKey(block.getX(), block.getY(), block.getZ()));
    }

    public boolean isTemporaryWater(Block block) {
        return temporaryWater.contains(Loc.blockKey(block.getX(), block.getY(), block.getZ()));
    }

    public void removeWater(Block block) {
        temporaryWater.remove(Loc.blockKey(block.getX(), block.getY(), block.getZ()));
    }

    /** 是否在地图区域内（未设置区域时只判断世界）。 */
    public boolean insideArena(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (worldName != null && !location.getWorld().getName().equals(worldName)) {
            return false;
        }
        if (corner1 == null || corner2 == null) {
            return true;
        }
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY()) - 10;
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY()) + 40;
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ && y >= minY && y <= maxY;
    }

    /** 目标位置是否离敌方床太近。 */
    public boolean nearEnemyBed(Location location, TeamColor self) {
        if (Settings.bedProtectRadius <= 0 || location == null) {
            return false;
        }
        for (Team team : teams.values()) {
            if (team.color() == self || !team.bedAlive()) {
                continue;
            }
            Location bed = team.bedFoot();
            if (bed == null || !bed.getWorld().equals(location.getWorld())) {
                continue;
            }
            int dx = Math.abs(bed.getBlockX() - location.getBlockX());
            int dy = Math.abs(bed.getBlockY() - location.getBlockY());
            int dz = Math.abs(bed.getBlockZ() - location.getBlockZ());
            if (dx <= Settings.bedProtectRadius && dy <= Settings.bedProtectRadius
                    && dz <= Settings.bedProtectRadius) {
                return true;
            }
        }
        return false;
    }

    /** 判断方块属于哪一个队伍的床。 */
    public Team bedOwner(Block block) {
        if (!block.getType().name().endsWith("_BED")) {
            return null;
        }
        for (Team team : teams.values()) {
            if (team.isBedBlock(block)) {
                return team;
            }
        }
        return null;
    }

    // ==========================================================
    //  玩家进出
    // ==========================================================

    /** @return 加入失败的原因，成功返回 null */
    public String join(Player player, boolean silent) {
        if (editMode || status == Status.RUNNING || status == Status.ENDING) {
            return Messages.get("join.no-arena", "%mode%", mode.display());
        }
        if (players.size() >= mode.maxPlayers()) {
            return Messages.get("join.full");
        }
        GamePlayer existing = plugin.gameManager().findPlayer(player);
        if (existing != null && existing.arena() != this) {
            return Messages.get("join.already-playing");
        }
        GamePlayer gp = new GamePlayer(player, this);
        players.put(player.getUniqueId(), gp);

        Location target = waitingSpawn != null ? waitingSpawn.clone() : Settings.lobby;
        if (target != null) {
            player.teleport(target);
        }
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        broadcastRaw(Messages.get("join.team-joined",
                "%player%", player.getName(),
                "%color%", "&7",
                "%team%", "待分配",
                "%players%", String.valueOf(players.size()),
                "%max%", String.valueOf(mode.maxPlayers())));

        if (!silent) {
            Chat.send(player, "join.joined",
                    "%arena%", name,
                    "%players%", String.valueOf(players.size()),
                    "%max%", String.valueOf(mode.maxPlayers()));
        }
        plugin.scoreboardManager().update(this);
        checkCountdown();
        return null;
    }

    public void quit(Player player, boolean teleportLobby) {
        GamePlayer gp = players.remove(player.getUniqueId());
        if (gp != null) {
            gp.cancelRespawnTask();
            Team team = gp.team();
            if (team != null) {
                team.removeMember(player.getUniqueId());
            }
            broadcastRaw(Messages.get("join.team-quit", "%player%", player.getName()));
            if (status == Status.RUNNING) {
                plugin.statsManager().recordLoss(gp);
            }
        }
        if (teleportLobby && Settings.lobby != null) {
            player.teleport(Settings.lobby);
        }
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.getInventory().clear();
        player.setHealth(player.getMaxHealth());
        player.setFireTicks(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setGlowing(false);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        if (status == Status.STARTING && players.size() < mode.minPlayers()) {
            cancelCountdown();
        }
        if (status == Status.RUNNING) {
            Team quitTeam = gp == null ? null : gp.team();
            if (quitTeam != null && !quitTeam.isEliminated() && quitTeam.onlinePlayers().isEmpty()) {
                quitTeam.setEliminated(true);
                broadcastRaw(Messages.get("game.team-eliminated",
                        "%color%", quitTeam.color().code(), "%team%", quitTeam.color().display()));
            }
            checkWin();
        }
        if (players.isEmpty() && (status == Status.RUNNING || status == Status.ENDING)) {
            finishGame(null);
        }
        plugin.scoreboardManager().update(this);
    }

    // ==========================================================
    //  倒计时 / 开局
    // ==========================================================

    private void checkCountdown() {
        if (status == Status.WAITING && players.size() >= mode.minPlayers()) {
            status = Status.STARTING;
            countdown = players.size() >= mode.maxPlayers() ? Settings.fullCountdown : Settings.startCountdown;
            broadcastRaw(Messages.get(players.size() >= mode.maxPlayers()
                    ? "game.countdown-fast" : "game.countdown", "%time%", String.valueOf(countdown)));
        }
    }

    private void cancelCountdown() {
        status = Status.WAITING;
        countdown = 0;
        broadcastRaw(Messages.get("game.cancelled"));
    }

    public void forceStart() {
        if (status == Status.RUNNING) {
            return;
        }
        if (players.isEmpty()) {
            return;
        }
        startGame();
    }

    private boolean loadWorld() {
        if (worldName == null || worldName.isBlank()) {
            return false;
        }
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            world = existing;
            applyWorldSettings(existing);
            return true;
        }
        if (Settings.copyTemplate) {
            Path template = new File(plugin.getDataFolder(),
                    Settings.templateFolder + File.separator + name).toPath();
            Path target = WorldUtil.worldFolder(worldName).toPath();
            if (!Files.isDirectory(template)) {
                plugin.getLogger().severe("地图模板不存在: " + template);
                return false;
            }
            if (!WorldUtil.copyDirectory(template, target)) {
                return false;
            }
        }
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        World created = Bukkit.createWorld(creator);
        if (created == null) {
            plugin.getLogger().severe("无法加载世界: " + worldName);
            return false;
        }
        world = created;
        applyWorldSettings(created);
        return true;
    }

    private void applyWorldSettings(World target) {
        if (Settings.disableAutosave) {
            target.setAutoSave(false);
        }
        target.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        target.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        target.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        target.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        target.setGameRule(GameRule.KEEP_INVENTORY, false);
        target.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        target.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        target.setGameRule(GameRule.NATURAL_REGENERATION, true);
        target.setGameRule(GameRule.DO_FIRE_TICK, false);
        target.setGameRule(GameRule.MOB_GRIEFING, false);
        target.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        target.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        target.setGameRule(GameRule.DO_INSOMNIA, false);
        target.setGameRule(GameRule.DISABLE_RAIDS, true);
        target.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        target.setDifficulty(Difficulty.NORMAL);
        target.setStorm(false);
        target.setThundering(false);
        target.setTime(6000);
        if (waitingSpawn != null) {
            target.setSpawnLocation(waitingSpawn.getBlockX(), waitingSpawn.getBlockY(), waitingSpawn.getBlockZ());
        }
    }

    private void unloadWorld() {
        if (world != null) {
            Bukkit.unloadWorld(world, !Settings.copyTemplate);
            world = null;
        }
        if (Settings.copyTemplate && worldName != null) {
            WorldUtil.deleteDirectory(WorldUtil.worldFolder(worldName).toPath());
        }
    }

    // ==========================================================
    //  地图编辑模式
    // ==========================================================

    /** 进入编辑模式：加载（或复制出）一个可自由建造的世界。 */
    public boolean beginEdit() {
        if (worldName == null || worldName.isBlank()) {
            worldName = "bw_" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        }
        Path working = WorldUtil.worldFolder(worldName).toPath();
        boolean placeholder = false;
        if (Settings.copyTemplate) {
            Path template = new File(plugin.getDataFolder(),
                    Settings.templateFolder + File.separator + name).toPath();
            WorldUtil.unload(worldName);
            WorldUtil.deleteDirectory(working);
            if (Files.isDirectory(template)) {
                if (!WorldUtil.copyDirectory(template, working)) {
                    return false;
                }
            } else {
                placeholder = true;
                plugin.getLogger().severe("未找到地图模板：" + template
                        + "。将使用「占位平台」世界以便编辑。"
                        + "请把完整的地图世界文件夹放到该目录后重新执行 /bw arena edit 才能加载真实地图。");
            }
        }
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            world = existing;
        } else {
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            creator.generateStructures(false);
            if (!Files.isDirectory(working)) {
                // 缺少可用地图模板：生成实心草地平台占位世界，避免玩家坠入虚空
                creator.type(WorldType.FLAT);
                try {
                    creator.generatorSettings(
                            "{\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1},"
                                    + "{\"block\":\"minecraft:stone\",\"height\":3},"
                                    + "{\"block\":\"minecraft:grass_block\",\"height\":1}],"
                                    + "\"biome\":\"minecraft:plains\"}");
                } catch (Throwable ignored) {
                    // 使用默认超平坦
                }
            }
            world = Bukkit.createWorld(creator);
        }
        if (world == null) {
            return false;
        }
        applyWorldSettings(world);
        // 编辑时恢复成正常世界规则，方便建造
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        world.setGameRule(GameRule.DO_FIRE_TICK, true);
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 3);
        world.setTime(6000);
        editMode = true;
        status = Status.WAITING;
        usedPlaceholder = placeholder;
        return true;
    }

    /** 保存编辑结果并卸载世界。 */
    public void endEdit() {
        if (world != null) {
            world.save();
        }
        if (Settings.copyTemplate && worldName != null) {
            Path template = new File(plugin.getDataFolder(),
                    Settings.templateFolder + File.separator + name).toPath();
            Path working = WorldUtil.worldFolder(worldName).toPath();
            if (Files.isDirectory(working)) {
                WorldUtil.deleteDirectory(template);
                WorldUtil.copyDirectory(working, template);
            }
        }
        editMode = false;
        unloadWorld();
    }

    public void teleportToEdit(Player player) {
        if (world == null) {
            return;
        }
        Location target = waitingSpawn != null ? waitingSpawn.clone() : world.getSpawnLocation().clone().add(0.5, 0, 0.5);
        target.setWorld(world);
        player.teleport(target);
        player.setGameMode(org.bukkit.GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(false);
    }

    public void startGame() {
        if (!loadWorld()) {
            broadcastRaw(Messages.get("arena.world-load-failed"));
            status = Status.WAITING;
            return;
        }
        // 重置队伍
        for (Team team : teams.values()) {
            team.reset();
            team.placeBed();
        }
        assignTeams();

        // 生成器
        generators.clear();
        globalDiamondTier = 1;
        globalEmeraldTier = 1;
        for (Team team : teams.values()) {
            if (team.generator() != null) {
                Location location = team.generator();
                location.setWorld(world);
                Generator iron = new Generator(Generator.Type.IRON, location, team);
                Generator gold = new Generator(Generator.Type.GOLD, location, team);
                if (team.generatorRate() > 0) {
                    iron.setCustomRate(team.generatorRate());
                    gold.setCustomRate(team.generatorRate());
                }
                generators.add(iron);
                generators.add(gold);
            }
        }
        for (TeamColor color : mode.teams()) {
            Team team = teams.get(color);
            if (team != null && team.generator() != null) {
                Location location = team.generator();
                location.setWorld(world);
            }
        }
        // 公共生成器由存档记录
        for (SavedGenerator saved : savedGenerators) {
            Location location = saved.location().clone();
            location.setWorld(world);
            Generator generator = new Generator(saved.type(), location, null);
            generator.setTier(1);
            if (saved.rate() > 0) {
                generator.setCustomRate(saved.rate());
            }
            generators.add(generator);
        }

        buildEvents();
        eventIndex = 0;
        ticks = 0;
        placedBlocks.clear();
        temporaryWater.clear();
        winningTeam = null;

        // 出生点缺失检查（仅警告，不阻断开局）
        for (Team team : teams.values()) {
            if (team.spawn() == null) {
                plugin.getLogger().warning("[HyBedWars] 竞技场 " + name + " 的队伍 "
                        + team.color() + " 未设置出生点，玩家将落到地图默认出生点。请用 /bw arena edit 设置。");
            }
        }

        // 玩家
        for (GamePlayer gp : players.values()) {
            gp.reset();
            gp.setAlive(true);
            gp.setSpectator(false);
            Player player = gp.bukkit();
            if (player == null) {
                continue;
            }
            Team team = gp.team();
            if (team != null && team.spawn() != null) {
                Location spawn = team.spawn().clone();
                spawn.setWorld(world);
                player.teleport(spawn);
            } else if (world != null) {
                // 出生点未配置时回退到地图世界出生点，避免玩家卡在大厅
                player.teleport(world.getSpawnLocation().clone().add(0.5, 0, 0.5));
            }
            gp.giveSpawnKit();
            player.setGlowing(false);
            player.sendTitle(Chat.color(Messages.get("game.start-title")),
                    Chat.color(Messages.get("game.start-subtitle")), 5, 40, 10);
        }

        spawnNpcs();
        status = Status.RUNNING;
        broadcastRaw(Messages.get("game.started"));
        if (experienceEconomy) {
            broadcastRaw(Messages.get("game.experience-start"));
        }
    }

    /** 开局时按人数重新平衡队伍，同时尽量保留玩家上一局的队伍。 */
    private void assignTeams() {
        List<TeamColor> order = mode.teams();
        Map<UUID, TeamColor> previous = new LinkedHashMap<>();
        for (GamePlayer gp : players.values()) {
            if (gp.teamColor() != null && teams.containsKey(gp.teamColor())) {
                previous.put(gp.uuid(), gp.teamColor());
            }
        }
        for (Team team : teams.values()) {
            team.memberIds().clear();
        }
        // 先保留原队伍成员
        for (Map.Entry<UUID, TeamColor> entry : previous.entrySet()) {
            Team team = teams.get(entry.getValue());
            if (team != null && team.size() < mode.teamSize()) {
                team.addMember(entry.getKey());
            }
        }
        // 其余玩家补到人数最少的队伍
        for (GamePlayer gp : players.values()) {
            Team current = gp.team();
            if (current != null && current.hasMember(gp.uuid())) {
                continue;
            }
            TeamColor best = null;
            int bestSize = Integer.MAX_VALUE;
            for (TeamColor color : order) {
                Team team = teams.get(color);
                if (team == null || team.size() >= mode.teamSize()) {
                    continue;
                }
                if (team.size() < bestSize) {
                    bestSize = team.size();
                    best = color;
                }
            }
            if (best == null) {
                continue;
            }
            Team team = teams.get(best);
            team.addMember(gp.uuid());
            gp.setTeamColor(best);
        }
    }

    /** 手动选队。 */
    public boolean selectTeam(Player player, TeamColor color) {
        GamePlayer gp = player(player);
        if (gp == null) {
            return false;
        }
        Team team = teams.get(color);
        if (team == null || team.size() >= mode.teamSize()) {
            return false;
        }
        Team old = gp.team();
        if (old != null) {
            old.removeMember(gp.uuid());
        }
        team.addMember(gp.uuid());
        gp.setTeamColor(color);
        return true;
    }

    // ==========================================================
    //  事件时间轴
    // ==========================================================

    private void buildEvents() {
        events.clear();
        for (int i = 0; i < Settings.diamondRate.length - 1 && i < Settings.diamondUpgradeAt.length; i++) {
            events.add(new GameEvent(Settings.diamondUpgradeAt[i], GameEvent.Type.DIAMOND_UP));
        }
        for (int i = 0; i < Settings.emeraldRate.length - 1 && i < Settings.emeraldUpgradeAt.length; i++) {
            events.add(new GameEvent(Settings.emeraldUpgradeAt[i], GameEvent.Type.EMERALD_UP));
        }
        events.add(new GameEvent(Settings.bedGoneSeconds, GameEvent.Type.BED_GONE));
        events.add(new GameEvent(Settings.gameEndSeconds, GameEvent.Type.GAME_END));
        events.sort(java.util.Comparator.comparingInt(GameEvent::seconds));
    }

    public GameEvent nextEvent() {
        if (eventIndex < events.size()) {
            return events.get(eventIndex);
        }
        return null;
    }

    public int secondsUntilNextEvent() {
        GameEvent event = nextEvent();
        if (event == null) {
            return 0;
        }
        return Math.max(0, event.seconds() - elapsedSeconds());
    }

    private void processEvents() {
        while (eventIndex < events.size() && elapsedSeconds() >= events.get(eventIndex).seconds()) {
            GameEvent event = events.get(eventIndex);
            eventIndex++;
            handleEvent(event);
        }
    }

    private void handleEvent(GameEvent event) {
        switch (event.type()) {
            case DIAMOND_UP -> {
                globalDiamondTier++;
                for (Generator generator : generators) {
                    if (generator.type() == Generator.Type.DIAMOND) {
                        generator.setTier(globalDiamondTier);
                    }
                }
                broadcastRaw(Messages.get("game.event-diamond", "%tier%", String.valueOf(globalDiamondTier)));
            }
            case EMERALD_UP -> {
                globalEmeraldTier++;
                for (Generator generator : generators) {
                    if (generator.type() == Generator.Type.EMERALD) {
                        generator.setTier(globalEmeraldTier);
                    }
                }
                broadcastRaw(Messages.get("game.event-emerald", "%tier%", String.valueOf(globalEmeraldTier)));
            }
            case BED_GONE -> {
                for (Team team : teams.values()) {
                    team.setBedAlive(false);
                    team.removeBedBlocks();
                }
                broadcastRaw(Messages.get("game.bed-gone"));
            }
            case GAME_END -> {
                TeamColor best = null;
                int bestCount = -1;
                for (Team team : teams.values()) {
                    if (team.isEliminated()) {
                        continue;
                    }
                    int count = team.aliveCount();
                    if (count > bestCount) {
                        bestCount = count;
                        best = team.color();
                    }
                }
                broadcastRaw(Messages.get("game.game-end"));
                finishGame(best);
            }
            default -> {
            }
        }
        plugin.scoreboardManager().update(this);
    }

    // ==========================================================
    //  主循环
    // ==========================================================

    public void tick() {
        switch (status) {
            case STARTING -> {
                countdown--;
                if (countdown == 30 || countdown == 20 || countdown == 15 || countdown == 10
                        || countdown == 5 || (countdown <= 3 && countdown > 0)) {
                    broadcastRaw(Messages.get("game.countdown", "%time%", String.valueOf(countdown)));
                }
                if (countdown <= 0) {
                    startGame();
                }
            }
            case RUNNING -> {
                ticks++;
                for (Generator generator : generators) {
                    generator.tick();
                }
                processEvents();
                if (ticks % 20 == 0) {
                    tickSecond();
                }
                handleTemporaryWater();
            }
            case ENDING -> {
                endTimer--;
                if (endTimer <= 0) {
                    cleanup();
                }
            }
            default -> {
            }
        }
    }

    private void tickSecond() {
        int second = elapsedSeconds();
        for (Team team : teams.values()) {
            // 治疗池
            if (team.upgradeLevel(UpgradeType.HEAL_POOL) > 0 && !team.isEliminated()) {
                Location center = team.spawn();
                if (center != null && center.getWorld() != null) {
                    for (Player player : team.onlinePlayers()) {
                        GamePlayer gp = player(player);
                        if (gp != null && gp.isAlive() && player.getWorld().equals(center.getWorld())
                                && player.getLocation().distanceSquared(center) <= 100) {
                            if (player.getHealth() < player.getMaxHealth()) {
                                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 1.5));
                            }
                        }
                    }
                }
            }
        }
        // 虚空判定
        for (GamePlayer gp : new ArrayList<>(players.values())) {
            Player player = gp.bukkit();
            if (player == null) {
                continue;
            }
            if (player.getLocation().getY() > Settings.voidY) {
                continue;
            }
            if (!gp.isAlive() || gp.isSpectator()) {
                if (spectatorSpawn != null) {
                    Location target = spectatorSpawn.clone();
                    target.setWorld(world);
                    player.teleport(target);
                }
                continue;
            }
            gp.setVoidDeath(true);
            player.setHealth(0);
        }
        refreshEffects();
        checkTraps();
        plugin.scoreboardManager().update(this);
    }

    /** 陷阱触发判定：敌人进入基地范围时消耗队列中的第一个陷阱。 */
    private void checkTraps() {
        for (Team team : teams.values()) {
            if (team.isEliminated() || team.traps().isEmpty()) {
                continue;
            }
            List<GamePlayer> intruders = new ArrayList<>();
            for (GamePlayer gp : players.values()) {
                if (gp.teamColor() == team.color() || !gp.isAlive() || gp.isSpectator()) {
                    continue;
                }
                Player player = gp.bukkit();
                if (player == null || gp.hasMagicMilk()) {
                    continue;
                }
                if (team.isInsideBase(player.getLocation(), Settings.trapRadius)) {
                    intruders.add(gp);
                }
            }
            if (intruders.isEmpty()) {
                team.resetTrapTrigger();
                continue;
            }
            if (!team.consumeTrapTrigger()) {
                continue;
            }
            UpgradeType trap = team.popTrap();
            if (trap == null) {
                team.resetTrapTrigger();
                continue;
            }
            applyTrap(team, trap, intruders);
        }
    }

    private void applyTrap(Team team, UpgradeType trap, List<GamePlayer> intruders) {
        String trapName = trap.display();
        broadcastRaw(Messages.get("shop.trap-triggered", "%trap%", trapName));
        for (Player member : team.onlinePlayers()) {
            member.sendMessage(Chat.color(Messages.prefix() + Messages.get("shop.trap-alert",
                    "%trap%", trapName)));
        }
        switch (trap) {
            case TRAP_IT -> {
                for (GamePlayer gp : intruders) {
                    Player player = gp.bukkit();
                    if (player == null) {
                        continue;
                    }
                    addEffect(player, "blindness", 8, 0);
                    addEffect(player, "slowness", 8, 1);
                }
            }
            case TRAP_COUNTER -> {
                for (Player member : team.onlinePlayers()) {
                    addEffect(member, "speed", 15, 0);
                    addEffect(member, "jump_boost", 15, 1);
                }
            }
            case TRAP_ALARM -> {
                for (GamePlayer gp : intruders) {
                    Player player = gp.bukkit();
                    if (player != null) {
                        addEffect(player, "glowing", 15, 0);
                    }
                }
            }
            case TRAP_FATIGUE -> {
                for (GamePlayer gp : intruders) {
                    Player player = gp.bukkit();
                    if (player != null) {
                        addEffect(player, "mining_fatigue", 10, 0);
                    }
                }
            }
            default -> {
            }
        }
    }

    private void addEffect(Player player, String effectKey, int seconds, int amplifier) {
        org.bukkit.potion.PotionEffectType type = com.hybedwars.util.Compat.effectOrNull(effectKey);
        if (type != null) {
            player.addPotionEffect(new PotionEffect(type, seconds * 20, amplifier, false, true, true));
        }
    }

    /** 保证急迫等持续效果不丢失。 */
    private void refreshEffects() {
        for (GamePlayer gp : players.values()) {
            Player player = gp.bukkit();
            if (player == null || !gp.isAlive()) {
                continue;
            }
            Team team = gp.team();
            if (team == null) {
                continue;
            }
            int haste = team.upgradeLevel(UpgradeType.HASTE);
            if (haste > 0) {
                org.bukkit.potion.PotionEffectType type = com.hybedwars.util.Compat.effectOrNull("haste");
                if (type != null && (player.getPotionEffect(type) == null
                        || player.getPotionEffect(type).getDuration() < 200)) {
                    player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, haste - 1,
                            false, false, true));
                }
            }
        }
    }

    /** 水桶放置 8 秒后自动清除，避免地图被永久改造。 */
    private void handleTemporaryWater() {
        if (temporaryWater.isEmpty()) {
            return;
        }
        if (ticks % 20 != 0) {
            return;
        }
        Set<Long> toRemove = new LinkedHashSet<>();
        for (long key : temporaryWater) {
            Block block = blockFromKey(key);
            if (block == null) {
                toRemove.add(key);
                continue;
            }
            if (block.getType() != org.bukkit.Material.WATER
                    && block.getType() != org.bukkit.Material.BUBBLE_COLUMN) {
                toRemove.add(key);
                continue;
            }
            waterAge.merge(key, 1, Integer::sum);
            if (waterAge.getOrDefault(key, 0) >= 8) {
                block.setType(org.bukkit.Material.AIR, false);
                toRemove.add(key);
                waterAge.remove(key);
            }
        }
        temporaryWater.removeAll(toRemove);
    }

    private final Map<Long, Integer> waterAge = new LinkedHashMap<>();

    private Block blockFromKey(long key) {
        if (world == null) {
            return null;
        }
        int x = (int) (key >> 38);
        int z = (int) (key << 26 >> 38);
        int y = (int) ((key >> 26) & 0xFFF);
        if (x > 0x1FFFFFF) {
            x -= 0x4000000;
        }
        if (z > 0x1FFFFFF) {
            z -= 0x4000000;
        }
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return null;
        }
        return world.getBlockAt(x, y, z);
    }

    // ==========================================================
    //  经验模式
    // ==========================================================

    /**
     * 经验模式：把捡到的一份资源折算成经验值。
     *
     * @return 实际获得的经验值，0 表示该物品不转化
     */
    public int grantResourceExperience(GamePlayer gp, org.bukkit.Material material, int amount) {
        if (!experienceEconomy || gp == null) {
            return 0;
        }
        int unit = Settings.xpValueOf(material);
        if (unit <= 0) {
            return 0;
        }
        int value = unit * Math.max(1, amount);
        gp.addExperience(value);
        Player player = gp.bukkit();
        if (player != null && Settings.xpPickupMessage) {
            player.sendActionBar(Chat.comp(Messages.get("experience.pickup",
                    "%amount%", String.valueOf(value),
                    "%total%", String.valueOf(gp.experience()))));
        }
        return value;
    }

    /** 经验模式下的额外经验奖励（击杀 / 破坏床）。 */
    public void grantBonusExperience(GamePlayer gp, int amount, String messageKey) {
        if (!experienceEconomy || gp == null || amount <= 0) {
            return;
        }
        gp.addExperience(amount);
        Player player = gp.bukkit();
        if (player != null) {
            player.sendActionBar(Chat.comp(Messages.get(messageKey,
                    "%amount%", String.valueOf(amount),
                    "%total%", String.valueOf(gp.experience()))));
        }
    }

    // ==========================================================
    //  死亡 / 重生 / 淘汰
    // ==========================================================

    public void handleDeath(GamePlayer victim, Player killer) {
        Player player = victim.bukkit();
        Team team = victim.team();
        boolean finalDeath = team == null || !team.bedAlive();

        victim.setAlive(false);
        victim.addDeath();

        String killerName = killer != null ? killer.getName() : null;
        if (killer != null) {
            GamePlayer killerGp = player(killer);
            if (killerGp != null) {
                if (finalDeath) {
                    killerGp.addFinalKill();
                    plugin.statsManager().addFinalKill(killer.getUniqueId(), Settings.xpFinalKill);
                } else {
                    killerGp.addKill();
                    plugin.statsManager().addKill(killer.getUniqueId(), Settings.xpKill);
                }
                grantBonusExperience(killerGp, Settings.xpKillReward, "experience.kill-reward");
            }
        }
        plugin.statsManager().addDeath(victim.uuid());

        String key;
        if (victim.isVoidDeath() || killerName == null) {
            key = finalDeath ? "game.final-death-void" : "game.death-void";
        } else {
            key = finalDeath ? "game.final-death-message" : "game.death-message";
        }
        broadcastRaw(Messages.get(key,
                "%victim%", victim.name(),
                "%killer%", killerName == null ? "-" : killerName,
                "%color%", team == null ? "" : team.color().code(),
                "%team%", team == null ? "" : team.color().display()));

        if (finalDeath) {
            victim.setSpectator(true);
            victim.setRespawning(false);
            if (player != null) {
                player.sendMessage(Chat.color(Messages.prefix() + Messages.get("game.you-are-spectator")));
            }
            if (team != null) {
                team.removeMember(victim.uuid());
                if (team.onlinePlayers().isEmpty()) {
                    team.setEliminated(true);
                    broadcastRaw(Messages.get("game.team-eliminated",
                            "%color%", team.color().code(), "%team%", team.color().display()));
                }
            }
            plugin.scoreboardManager().update(this);
            checkWin();
        } else {
            victim.setRespawning(true);
            int delay = Settings.respawnDelay;
            Team finalTeam = team;
            int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> respawn(victim, finalTeam), delay * 20L).getTaskId();
            victim.setRespawnTaskId(taskId);
            if (player != null) {
                player.sendMessage(Chat.color(Messages.prefix()
                        + Messages.get("game.respawn-in", "%time%", String.valueOf(delay))));
            }
        }
    }

    /** 只判断是否应该整体结束。 */
    public void checkWin() {
        if (status != Status.RUNNING) {
            return;
        }
        List<Team> remaining = new ArrayList<>();
        for (Team team : teams.values()) {
            if (!team.isEliminated() && !team.onlinePlayers().isEmpty()) {
                remaining.add(team);
            }
        }
        if (remaining.size() <= 1) {
            finishGame(remaining.isEmpty() ? null : remaining.get(0).color());
        }
    }

    public void respawn(GamePlayer gp, Team team) {
        Player player = gp.bukkit();
        gp.setRespawnTaskId(-1);
        if (player == null || !players.containsKey(gp.uuid())) {
            return;
        }
        if (!gp.isAlive()) {
            if (team == null || !team.bedAlive()) {
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                if (spectatorSpawn != null) {
                    Location location = spectatorSpawn.clone();
                    location.setWorld(world);
                    player.teleport(location);
                }
                return;
            }
            gp.setAlive(true);
        }
        gp.setRespawning(false);
        gp.setVoidDeath(false);
        gp.giveSpawnKit();
        if (team != null && team.spawn() != null) {
            Location spawn = team.spawn();
            spawn.setWorld(world);
            player.teleport(spawn);
        }
        if (spectatorSpawn != null && player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        }
        player.sendTitle(Chat.color(Messages.get("game.respawned")), "", 2, 20, 5);
        plugin.scoreboardManager().update(this);
    }

    /** 进入观战状态（最终死亡）。 */
    public void makeSpectator(GamePlayer gp) {
        Player player = gp.bukkit();
        if (player == null) {
            return;
        }
        gp.setAlive(false);
        gp.setSpectator(true);
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        if (spectatorSpawn != null) {
            Location location = spectatorSpawn.clone();
            location.setWorld(world);
            player.teleport(location);
        }
    }

    // ==========================================================
    //  床
    // ==========================================================

    /** 处理床被破坏。 */
    public void destroyBed(Team team, Player breaker) {
        if (team == null || !team.bedAlive()) {
            return;
        }
        team.setBedAlive(false);
        team.removeBedBlocks();

        GamePlayer gp = player(breaker);
        String killerName = breaker.getName();
        if (gp != null) {
            gp.addBedBroken();
            grantBonusExperience(gp, Settings.xpBedReward, "experience.bed-reward");
        }
        plugin.statsManager().addBedBreak(breaker.getUniqueId(), Settings.xpBedBreak);

        broadcastRaw(Messages.get("game.bed-destroyed-broadcast",
                "%color%", team.color().code(),
                "%team%", team.color().display(),
                "%killer%", killerName));

        // 该队已经全员死亡则直接淘汰
        if (team.onlinePlayers().isEmpty() || team.aliveCount() == 0) {
            team.setEliminated(true);
            broadcastRaw(Messages.get("game.team-eliminated",
                    "%color%", team.color().code(), "%team%", team.color().display()));
        }
        plugin.scoreboardManager().update(this);
        checkWin();
    }

    // ==========================================================
    //  结束
    // ==========================================================

    public void finishGame(TeamColor winner) {
        if (status == Status.ENDING) {
            return;
        }
        if (status == Status.WAITING || status == Status.STARTING) {
            status = Status.WAITING;
            return;
        }
        this.winningTeam = winner;
        status = Status.ENDING;
        endTimer = 6;

        if (winner != null) {
            Team team = teams.get(winner);
            String display = team == null ? winner.display() : team.color().display();
            String color = team == null ? "" : team.color().code();
            broadcastRaw("&8&m----------------------------");
            broadcastRaw(Messages.get("game.winner-broadcast"));
            broadcastRaw(Messages.get("game.winner-team", "%color%", color, "%team%", display));
            broadcastRaw("&8&m----------------------------");
            for (GamePlayer gp : players.values()) {
                boolean same = gp.teamColor() == winner;
                if (same) {
                    plugin.statsManager().recordWin(gp);
                    plugin.statsManager().addXp(gp.uuid(), Settings.xpWin);
                } else {
                    plugin.statsManager().recordLoss(gp);
                }
                Player player = gp.bukkit();
                if (player == null) {
                    continue;
                }
                if (same) {
                    firework(player);
                    player.sendTitle(Chat.color("&6&l胜利！"), Chat.color("&e" + display), 5, 60, 15);
                } else {
                    player.sendTitle(Chat.color("&c&l失败"), Chat.color("&7" + display + " 获得了胜利"), 5, 60, 15);
                }
            }
        } else {
            broadcastRaw(Messages.get("game.draw"));
            for (GamePlayer gp : players.values()) {
                plugin.statsManager().recordPlayed(gp);
            }
        }
        if (experienceEconomy) {
            for (GamePlayer gp : players.values()) {
                Player player = gp.bukkit();
                if (player == null) {
                    continue;
                }
                player.sendMessage(Chat.color(Messages.prefix() + Messages.get("experience.summary",
                        "%amount%", String.valueOf(gp.experienceEarned()))));
            }
        }
        despawnNpcs();
        plugin.scoreboardManager().update(this);
    }

    private void firework(Player player) {
        try {
            org.bukkit.entity.Firework firework = player.getWorld().spawn(player.getLocation(),
                    org.bukkit.entity.Firework.class);
            org.bukkit.inventory.meta.FireworkMeta meta = firework.getFireworkMeta();
            meta.setPower(1);
            meta.addEffect(org.bukkit.FireworkEffect.builder()
                    .withColor(org.bukkit.Color.LIME, org.bukkit.Color.AQUA)
                    .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
                    .trail(true).flicker(true).build());
            firework.setFireworkMeta(meta);
        } catch (Throwable ignored) {
            // 忽略烟花失败
        }
    }

    private void cleanup() {
        despawnNpcs();
        for (GamePlayer gp : new ArrayList<>(players.values())) {
            gp.cancelRespawnTask();
            gp.removeLegacyPvp();
            Player player = gp.bukkit();
            if (player != null) {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                player.getInventory().clear();
                player.setFireTicks(0);
                player.setGlowing(false);
                for (PotionEffect effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                Location lobbySpawn = resolveLobbySpawn();
                if (lobbySpawn != null) {
                    player.teleport(lobbySpawn);
                } else {
                    World main = Bukkit.getWorlds().get(0);
                    if (main != null) {
                        player.teleport(main.getSpawnLocation());
                    }
                }
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        players.clear();
        generators.clear();
        placedBlocks.clear();
        temporaryWater.clear();
        waterAge.clear();
        resetTeams();
        status = Status.WAITING;
        countdown = 0;
        ticks = 0;
        eventIndex = 0;
        winningTeam = null;
        unloadWorld();
    }

    /** 解析大厅出生点：优先大厅世界出生点；无大厅则返回 null。 */
    private Location resolveLobbySpawn() {
        Lobby lobby = plugin.lobby();
        if (lobby != null && lobby.world() != null) {
            return lobby.world().getSpawnLocation().clone().add(0.5, 0, 0.5);
        }
        return null;
    }

    // ==========================================================
    //  NPC
    // ==========================================================

    public void spawnNpcs() {
        despawnNpcs();
        if (world == null) {
            return;
        }
        spawnNpc(shopNpcLocation, "&a&l商店 &7(右键打开)", false);
        spawnNpc(upgradeNpcLocation, "&b&l队伍升级 &7(右键打开)", true);
    }

    private void spawnNpc(Location location, String name, boolean upgrade) {
        if (location == null) {
            return;
        }
        Location spawn = location.clone();
        spawn.setWorld(world);
        try {
            Villager villager = world.spawn(spawn, Villager.class);
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setPersistent(true);
            villager.setRemoveWhenFarAway(false);
            villager.setCustomName(Chat.color(name));
            villager.setCustomNameVisible(true);
            npcs.add(villager.getUniqueId());
            npcTypes.put(villager.getUniqueId(), upgrade);
        } catch (Throwable ignored) {
            plugin.getLogger().warning("无法生成商店 NPC：" + ignored.getMessage());
        }
    }

    private final Map<UUID, Boolean> npcTypes = new LinkedHashMap<>();

    public void despawnNpcs() {
        for (UUID uuid : npcs) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
        npcs.clear();
        npcTypes.clear();
    }

    /** @return null 表示不是本图的 NPC，否则返回 true = 升级 NPC */
    public Boolean npcType(UUID entityId) {
        return npcTypes.get(entityId);
    }

    // ==========================================================
    //  广播
    // ==========================================================

    public void broadcastRaw(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String message = Chat.color(Messages.prefix() + text);
        for (Player player : onlinePlayers()) {
            player.sendMessage(message);
        }
    }

    public void broadcastPlain(String text) {
        String message = Chat.color(text);
        for (Player player : onlinePlayers()) {
            player.sendMessage(message);
        }
    }

    public void showTitleAll(String title, String subtitle) {
        Component t = Chat.comp(title);
        Component s = Chat.comp(subtitle);
        Title.Times times = Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500));
        for (Player player : onlinePlayers()) {
            player.showTitle(Title.title(t, s, times));
        }
    }

    // ==========================================================
    //  存档
    // ==========================================================

    /** 公共生成器（钻石 / 绿宝石），序列化保存。rate 为自定义刷新间隔（秒），0 = 默认。 */
    public record SavedGenerator(Generator.Type type, Location location, double rate) {
    }

    private final List<SavedGenerator> savedGenerators = new ArrayList<>();

    public List<SavedGenerator> savedGenerators() {
        return savedGenerators;
    }

    public void addSavedGenerator(Generator.Type type, Location location, double rate) {
        savedGenerators.add(new SavedGenerator(type, Loc.toBlock(location), Math.max(0, rate)));
    }

    public int savedGeneratorCount(Generator.Type type) {
        int count = 0;
        for (SavedGenerator saved : savedGenerators) {
            if (saved.type() == type) {
                count++;
            }
        }
        return count;
    }

    public void save(ConfigurationSection section) {
        section.set("mode", mode.name());
        section.set("experience-economy", experienceEconomy);
        section.set("world", worldName);
        section.set("waiting-spawn", Loc.serialize(waitingSpawn));
        section.set("spectator-spawn", Loc.serialize(spectatorSpawn));
        section.set("shop-npc", Loc.serialize(shopNpcLocation));
        section.set("upgrade-npc", Loc.serialize(upgradeNpcLocation));
        section.set("region.corner1", Loc.serialize(corner1));
        section.set("region.corner2", Loc.serialize(corner2));

        List<String> generatorsList = new ArrayList<>();
        for (SavedGenerator saved : savedGenerators) {
            generatorsList.add(saved.type().name() + ";" + Loc.serialize(saved.location()) + ";" + saved.rate());
        }
        section.set("global-generators", generatorsList);

        ConfigurationSection teamSection = section.getConfigurationSection("teams");
        if (teamSection == null) {
            teamSection = section.createSection("teams");
        }
        for (Team team : teams.values()) {
            ConfigurationSection ts = teamSection.getConfigurationSection(team.color().name());
            if (ts == null) {
                ts = teamSection.createSection(team.color().name());
            }
            ts.set("spawn", Loc.serialize(team.spawn()));
            ts.set("bed", Loc.serialize(team.bedFoot()));
            ts.set("bed-facing", team.bedFacing().name());
            ts.set("generator", Loc.serialize(team.generator()));
        }
    }

    public void load(ConfigurationSection section) {
        this.mode = GameMode.parse(section.getString("mode", "SOLO"));
        if (this.mode == null) {
            this.mode = GameMode.SOLO;
        }
        this.experienceEconomy = section.getBoolean("experience-economy", this.mode.experience());
        resetTeams();
        this.worldName = section.getString("world");
        this.waitingSpawn = Loc.deserialize(section.getString("waiting-spawn", ""));
        this.spectatorSpawn = Loc.deserialize(section.getString("spectator-spawn", ""));
        this.shopNpcLocation = Loc.deserialize(section.getString("shop-npc", ""));
        this.upgradeNpcLocation = Loc.deserialize(section.getString("upgrade-npc", ""));
        this.corner1 = Loc.deserialize(section.getString("region.corner1", ""));
        this.corner2 = Loc.deserialize(section.getString("region.corner2", ""));

        savedGenerators.clear();
        for (String raw : section.getStringList("global-generators")) {
            String[] parts = raw.split(";");
            if (parts.length < 2) {
                continue;
            }
            try {
                Generator.Type type = Generator.Type.valueOf(parts[0]);
                Location location = Loc.deserialize(parts[1]);
                double rate = parts.length >= 3 ? Double.parseDouble(parts[2]) : 0.0;
                if (location != null) {
                    savedGenerators.add(new SavedGenerator(type, location, Math.max(0, rate)));
                }
            } catch (IllegalArgumentException ignored) {
                // 忽略无效条目（含 NumberFormatException）
            }
        }

        ConfigurationSection teamSection = section.getConfigurationSection("teams");
        if (teamSection != null) {
            for (Team team : teams.values()) {
                ConfigurationSection ts = teamSection.getConfigurationSection(team.color().name());
                if (ts == null) {
                    continue;
                }
                team.setSpawn(Loc.deserialize(ts.getString("spawn", "")));
                team.setBedFoot(Loc.deserialize(ts.getString("bed", "")));
                team.setBedFacing(parseFacing(ts.getString("bed-facing", "NORTH")));
                team.setGenerator(Loc.deserialize(ts.getString("generator", "")));
            }
        }
    }

    private org.bukkit.block.BlockFace parseFacing(String input) {
        try {
            return org.bukkit.block.BlockFace.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (Throwable t) {
            return org.bukkit.block.BlockFace.NORTH;
        }
    }
}
