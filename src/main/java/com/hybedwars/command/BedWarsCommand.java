package com.hybedwars.command;

import com.hybedwars.HyBedWars;
import com.hybedwars.config.Messages;
import com.hybedwars.config.Settings;
import com.hybedwars.game.Arena;
import com.hybedwars.game.GameMode;
import com.hybedwars.game.Team;
import com.hybedwars.game.TeamColor;
import com.hybedwars.generator.Generator;
import com.hybedwars.listener.PlayerListener;
import com.hybedwars.stats.PlayerStats;
import com.hybedwars.stats.StatsManager;
import com.hybedwars.util.Chat;
import com.hybedwars.util.Loc;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** /bw 命令与地图搭建工具。 */
public class BedWarsCommand implements CommandExecutor, TabCompleter {

    private final HyBedWars plugin;

    public BedWarsCommand(HyBedWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help", "?" -> sendHelp(sender);
            case "join", "j" -> join(sender, args);
            case "leave", "quit", "l" -> leave(sender);
            case "list", "arenas" -> list(sender);
            case "stats", "stat" -> stats(sender, args);
            case "top", "leaderboard" -> top(sender, args);
            case "start" -> start(sender, args);
            case "stop", "end" -> stop(sender, args);
            case "reload", "rl" -> reload(sender);
            case "arena", "map", "setup" -> arena(sender, args);
            default -> Chat.send(sender, "general.unknown-command");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        Chat.raw(sender, Messages.get("command.help-header"));
        Chat.raw(sender, Messages.getList("command.help-player"));
        if (sender.hasPermission("hybedwars.admin")) {
            Chat.raw(sender, Messages.getList("command.help-admin"));
        }
        Chat.raw(sender, Messages.get("command.modes"));
    }

    // ==========================================================
    //  玩家命令
    // ==========================================================

    private void join(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, "general.player-only");
            return;
        }
        if (!player.hasPermission("hybedwars.play")) {
            Chat.send(player, "general.no-permission");
            return;
        }
        GameMode mode;
        if (args.length >= 2) {
            mode = GameMode.parse(args[1]);
            if (mode == null) {
                Chat.raw(player, Messages.get("command.modes"));
                return;
            }
        } else {
            mode = GameMode.SOLO;
        }
        plugin.gameManager().join(player, mode);
    }

    private void leave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, "general.player-only");
            return;
        }
        plugin.gameManager().leave(player);
    }

    private void list(CommandSender sender) {
        if (plugin.arenaManager().isEmpty()) {
            Chat.raw(sender, "&7当前没有任何地图，请使用 &e/bw arena create <名称> <模式> &7创建。");
            return;
        }
        Chat.raw(sender, "&8&m--------&r &c&l地图列表 &8&m--------");
        for (Arena arena : plugin.arenaManager().all()) {
            Chat.raw(sender, "&e" + arena.name() + " &7| " + arena.mode().display()
                    + " &7| 状态：&a" + translate(arena.status())
                    + " &7| 人数：&e" + arena.onlinePlayers().size() + "/" + arena.mode().maxPlayers()
                    + (arena.experienceMode() ? " &a[经验模式]" : "")
                    + (arena.isEditMode() ? " &c[编辑中]" : ""));
        }
    }

    private String translate(Arena.Status status) {
        return switch (status) {
            case WAITING -> "等待中";
            case STARTING -> "倒计时";
            case RUNNING -> "游戏中";
            case ENDING -> "结算中";
        };
    }

    private void stats(CommandSender sender, String[] args) {
        PlayerStats stats;
        if (args.length >= 2) {
            stats = plugin.statsManager().getByName(args[1]);
            if (stats == null) {
                Chat.raw(sender, "&c找不到玩家 &e" + args[1] + " &c的战绩。");
                return;
            }
        } else if (sender instanceof Player player) {
            stats = plugin.statsManager().getOrCreate(player);
        } else {
            Chat.raw(sender, "&c请在控制台指定玩家名：/bw stats <玩家>");
            return;
        }
        PlayerListener.showStats(sender, stats);
    }

    private void top(CommandSender sender, String[] args) {
        StatsManager.SortKey key = args.length >= 2 ? StatsManager.SortKey.parse(args[1]) : StatsManager.SortKey.KILLS;
        List<PlayerStats> list = plugin.statsManager().top(key, 10);
        Chat.raw(sender, Messages.get("stats.top-header", "%type%", key.display()));
        if (list.isEmpty()) {
            Chat.raw(sender, Messages.get("stats.top-empty"));
            return;
        }
        int rank = 1;
        for (PlayerStats stats : list) {
            Chat.raw(sender, Messages.get("stats.top-line",
                    "%rank%", String.valueOf(rank++),
                    "%color%", "&e",
                    "%player%", stats.name(),
                    "%value%", String.valueOf(key.value(stats))));
        }
    }

    // ==========================================================
    //  管理命令
    // ==========================================================

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("hybedwars.admin")) {
            Chat.send(sender, "general.no-permission");
            return false;
        }
        return true;
    }

    private void start(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        Arena arena = args.length >= 2 ? plugin.arenaManager().get(args[1]) : null;
        if (arena == null) {
            if (sender instanceof Player player) {
                arena = plugin.gameManager().arenaOf(player);
            }
        }
        if (arena == null) {
            Chat.raw(sender, "&c请指定地图：/bw start <地图>");
            return;
        }
        arena.forceStart();
        Chat.raw(sender, "&a已强制开始 &e" + arena.name() + "&a。");
    }

    private void stop(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        Arena arena = args.length >= 2 ? plugin.arenaManager().get(args[1]) : null;
        if (arena == null) {
            if (sender instanceof Player player) {
                arena = plugin.gameManager().arenaOf(player);
            }
        }
        if (arena == null) {
            Chat.raw(sender, "&c请指定地图：/bw stop <地图>");
            return;
        }
        arena.finishGame(null);
        Chat.raw(sender, "&a已结束 &e" + arena.name() + " &a的对局。");
    }

    private void reload(CommandSender sender) {
        if (!requireAdmin(sender)) {
            return;
        }
        Settings.load();
        Messages.load();
        plugin.shopManager().load();
        plugin.arenaManager().load();
        Chat.send(sender, "general.reloaded");
    }

    // ==========================================================
    //  地图搭建
    // ==========================================================

    private void arena(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            Chat.raw(sender, "&c用法：/bw arena <create|delete|edit|done|setworld|setlobby|setspectator|setshop|setupgrades|setspawn|setbed|setgen|adddiamond|addemerald|cleargen|toggleexp|pos1|pos2|save|info>");
            Chat.raw(sender, "&7资源点指令可加可选速度：&e/bw arena setgen <队> [间隔秒] &7/ &e/bw arena adddiamond [间隔秒] &7/ &e/bw arena addemerald [间隔秒]");
            Chat.raw(sender, "&7间隔为「每隔多少秒生成一个」，例如 &eadddiamond 5 &7= 每 5 秒一个钻石；不填则用 config.yml 默认速率。");
            return;
        }
        if (!(sender instanceof Player player)) {
            Chat.raw(sender, "&c地图搭建命令只能由玩家执行。");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);

        switch (action) {
            case "create" -> {
                if (args.length < 4) {
                    Chat.raw(sender, "&c用法：/bw arena create <名称> <模式>");
                    Chat.raw(sender, "&7模式：" + GameMode.commandHint());
                    return;
                }
                GameMode mode = GameMode.parse(args[3]);
                if (mode == null) {
                    Chat.raw(sender, "&c无效的模式。");
                    Chat.raw(sender, "&7模式：" + GameMode.commandHint());
                    return;
                }
                if (plugin.arenaManager().get(args[2]) != null) {
                    Chat.send(sender, "arena.exists", "%arena%", args[2]);
                    return;
                }
                plugin.arenaManager().create(args[2], mode);
                Chat.send(sender, "arena.created", "%arena%", args[2], "%mode%", mode.descriptor());
                Chat.raw(sender, "&7下一步：&e/bw arena edit " + args[2] + " &7进入地图世界建造。");
            }
            case "delete" -> {
                if (args.length < 3) {
                    Chat.raw(sender, "&c用法：/bw arena delete <名称>");
                    return;
                }
                if (!plugin.arenaManager().delete(args[2])) {
                    Chat.send(sender, "arena.not-found", "%arena%", args[2]);
                    return;
                }
                Chat.send(sender, "arena.deleted", "%arena%", args[2]);
            }
            case "edit" -> {
                Arena arena = requireArena(sender, args, 2);
                if (arena == null) {
                    return;
                }
                if (!arena.beginEdit()) {
                    Chat.raw(sender, "&c地图世界加载失败，请检查控制台。");
                    return;
                }
                arena.teleportToEdit(player);
                if (arena.usedPlaceholder()) {
                    Chat.raw(sender, "&e未找到地图模板 &fplugins/HyBedWars/maps/" + arena.name()
                            + "/&e，已生成「占位平台」世界（实心草地，不是虚空）用于编辑与测试。");
                    Chat.raw(sender, "&7若要使用真实地图，请把完整的世界文件夹放到该目录后重新 &e/bw arena edit&7。");
                }
                Chat.raw(sender, "&a已进入编辑模式。建造完成后使用 &e/bw arena done &a保存并退出。");
            }
            case "done" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    Chat.send(sender, "arena.not-editing");
                    return;
                }
                arena.endEdit();
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                if (Settings.lobby != null) {
                    player.teleport(Settings.lobby);
                }
                plugin.arenaManager().save();
                Chat.raw(sender, "&a地图 &e" + arena.name() + " &a已保存。");
            }
            case "setworld" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    Chat.send(sender, "arena.not-editing");
                    return;
                }
                arena.setWorldName(player.getWorld().getName());
                Chat.send(sender, "arena.world-set", "%arena%", arena.name(),
                        "%world%", player.getWorld().getName());
            }
            case "setlobby" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    return;
                }
                arena.setWaitingSpawn(player.getLocation());
                Chat.send(sender, "arena.lobby-set");
            }
            case "setspectator" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    return;
                }
                arena.setSpectatorSpawn(player.getLocation());
                Chat.send(sender, "arena.spectator-set");
            }
            case "setshop" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    return;
                }
                arena.setShopNpcLocation(player.getLocation());
                Chat.send(sender, "arena.shop-set");
            }
            case "setupgrades" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    return;
                }
                arena.setUpgradeNpcLocation(player.getLocation());
                Chat.send(sender, "arena.upgrade-set");
            }
            case "setspawn" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    return;
                }
                TeamColor color = teamArg(sender, args, 2);
                if (color == null) {
                    return;
                }
                Team team = arena.team(color);
                if (team == null) {
                    Chat.raw(sender, "&c该地图没有 " + color.display() + " 队伍。");
                    return;
                }
                team.setSpawn(player.getLocation());
                Chat.send(sender, "arena.spawn-set", "%team%", color.display());
            }
            case "setbed" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    return;
                }
                TeamColor color = teamArg(sender, args, 2);
                if (color == null) {
                    return;
                }
                Team team = arena.team(color);
                if (team == null) {
                    return;
                }
                Block bed = findBed(player);
                if (bed == null) {
                    Chat.raw(sender, "&c请站在床上或对着床执行该命令。");
                    return;
                }
                if (bed.getBlockData() instanceof Bed bedData) {
                    org.bukkit.block.BlockFace facing = bedData.getFacing();
                    Block foot = bedData.getPart() == Bed.Part.FOOT
                            ? bed : bed.getRelative(facing.getOppositeFace());
                    team.setBedFoot(foot.getLocation());
                    team.setBedFacing(facing);
                } else {
                    team.setBedFoot(bed.getLocation());
                }
                Chat.send(sender, "arena.bed-set", "%team%", color.display());
            }
            case "setgen" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    return;
                }
                TeamColor color = teamArg(sender, args, 2);
                if (color == null) {
                    return;
                }
                Team team = arena.team(color);
                if (team == null) {
                    return;
                }
                double rate = parseRate(sender, args, 3);
                if (rate < 0) {
                    return;
                }
                team.setGenerator(player.getLocation());
                team.setGeneratorRate(rate);
                if (rate > 0) {
                    Chat.send(sender, "arena.gen-rate",
                            "%team%", color.display(), "%rate%", formatRate(rate));
                } else {
                    Chat.send(sender, "arena.gen-set", "%team%", color.display());
                }
            }
            case "adddiamond" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    return;
                }
                double rate = parseRate(sender, args, 2);
                if (rate < 0) {
                    return;
                }
                arena.addSavedGenerator(Generator.Type.DIAMOND, player.getLocation(), rate);
                if (rate > 0) {
                    Chat.send(sender, "arena.diamond-rate",
                            "%count%", String.valueOf(arena.savedGeneratorCount(Generator.Type.DIAMOND)),
                            "%rate%", formatRate(rate));
                } else {
                    Chat.send(sender, "arena.diamond-added",
                            "%count%", String.valueOf(arena.savedGeneratorCount(Generator.Type.DIAMOND)));
                }
            }
            case "addemerald" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    return;
                }
                double rate = parseRate(sender, args, 2);
                if (rate < 0) {
                    return;
                }
                arena.addSavedGenerator(Generator.Type.EMERALD, player.getLocation(), rate);
                if (rate > 0) {
                    Chat.send(sender, "arena.emerald-rate",
                            "%count%", String.valueOf(arena.savedGeneratorCount(Generator.Type.EMERALD)),
                            "%rate%", formatRate(rate));
                } else {
                    Chat.send(sender, "arena.emerald-added",
                            "%count%", String.valueOf(arena.savedGeneratorCount(Generator.Type.EMERALD)));
                }
            }
            case "toggleexp", "expmode" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    Chat.send(sender, "arena.not-editing");
                    return;
                }
                boolean enabled = !arena.experienceMode();
                arena.setExperienceEconomy(enabled);
                plugin.arenaManager().save();
                Chat.send(sender, enabled ? "experience.enabled" : "experience.disabled",
                        "%arena%", arena.name());
                Chat.send(sender, "experience.info");
            }
            case "cleargen" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    return;
                }
                arena.savedGenerators().clear();
                Chat.raw(sender, "&a已清空所有公共生成器。");
            }
            case "pos1" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    return;
                }
                arena.setCorners(player.getLocation(), arena.corner2());
                Chat.raw(sender, "&a区域点 1 已设置：" + Loc.shortString(player.getLocation()));
            }
            case "pos2" -> {
                Arena arena = editingArena();
                if (arena == null) {
                    return;
                }
                arena.setCorners(arena.corner1(), player.getLocation());
                Chat.raw(sender, "&a区域点 2 已设置：" + Loc.shortString(player.getLocation()));
            }
            case "save" -> {
                plugin.arenaManager().save();
                Chat.raw(sender, "&a所有地图已保存到 arenas.yml。");
            }
            case "info" -> {
                Arena arena = args.length >= 3
                        ? plugin.arenaManager().get(args[2]) : editingArena();
                if (arena == null) {
                    Chat.send(sender, "arena.not-found", "%arena%", args.length >= 3 ? args[2] : "?");
                    return;
                }
                Chat.raw(sender, Messages.get("arena.info-header"));
                Chat.raw(sender, Messages.get("arena.info",
                        "%arena%", arena.name(),
                        "%mode%", arena.mode().descriptor(),
                        "%world%", String.valueOf(arena.worldName())));
                Chat.raw(sender, arena.experienceMode()
                        ? "&a经验模式：&2已开启 &7(资源自动转化为经验，商店用经验结算)"
                        : "&7经验模式：&c未开启");
                Chat.raw(sender, Messages.get("experience.info"));
                for (TeamColor color : arena.mode().teams()) {
                    Team team = arena.team(color);
                    if (team == null) {
                        continue;
                    }
                    Chat.raw(sender, Messages.get("arena.info-team",
                            "%color%", color.code(),
                            "%team%", color.display(),
                            "%spawn%", Loc.shortString(team.spawn()),
                            "%bed%", Loc.shortString(team.bedFoot()),
                            "%gen%", Loc.shortString(team.generator())));
                }
                List<String> missing = arena.missingParts();
                Chat.raw(sender, missing.isEmpty()
                        ? "&a该地图配置完整，可以开始游戏。"
                        : Messages.get("arena.incomplete", "%arena%", arena.name(),
                        "%missing%", String.join("、", missing)));
            }
            default -> Chat.raw(sender, "&c未知的地图子命令。");
        }
    }

    private Arena requireArena(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            Chat.raw(sender, "&c请指定地图名称。");
            return null;
        }
        Arena arena = plugin.arenaManager().get(args[index]);
        if (arena == null) {
            Chat.send(sender, "arena.not-found", "%arena%", args[index]);
        }
        return arena;
    }

    private Arena editingArena() {
        for (Arena arena : plugin.arenaManager().all()) {
            if (arena.isEditMode()) {
                return arena;
            }
        }
        return null;
    }

    private TeamColor teamArg(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            Chat.raw(sender, "&c请指定队伍颜色，例如 red / blue / green / yellow。");
            return null;
        }
        TeamColor color = TeamColor.parse(args[index]);
        if (color == null) {
            Chat.raw(sender, "&c无效的队伍颜色。可用：" + Arrays.toString(TeamColor.values()));
        }
        return color;
    }

    /**
     * 解析资源点刷新间隔（秒/个）。index 处若不存在或不是正数，返回 0（使用默认速率）；
     * 若存在但是非法数字或 <= 0，返回 -1 表示参数错误（已提示玩家）。
     */
    private double parseRate(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            return 0;
        }
        try {
            double value = Double.parseDouble(args[index]);
            if (value <= 0) {
                Chat.send(sender, "arena.invalid-rate");
                return -1;
            }
            return value;
        } catch (NumberFormatException e) {
            Chat.send(sender, "arena.invalid-rate");
            return -1;
        }
    }

    /** 把刷新间隔格式化为易读字符串（整数不显示小数）。 */
    private String formatRate(double rate) {
        if (rate == Math.floor(rate) && !Double.isInfinite(rate)) {
            return String.valueOf((long) rate);
        }
        return String.valueOf(rate);
    }

    private Block findBed(Player player) {
        Block target = player.getTargetBlockExact(6);
        if (target != null && target.getType().name().endsWith("_BED")) {
            return target;
        }
        Block below = player.getLocation().subtract(0, 1, 0).getBlock();
        if (below.getType().name().endsWith("_BED")) {
            return below;
        }
        Location center = player.getLocation();
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getType().name().endsWith("_BED")) {
                        return block;
                    }
                }
            }
        }
        return null;
    }

    // ==========================================================
    //  Tab 补全
    // ==========================================================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            addMatches(out, args[0], "help", "join", "leave", "list", "stats", "top");
            if (sender.hasPermission("hybedwars.admin")) {
                addMatches(out, args[0], "start", "stop", "reload", "arena");
            }
            return out;
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "join", "j" -> {
                    for (GameMode mode : GameMode.values()) {
                        out.add(mode.name().toLowerCase(Locale.ROOT));
                    }
                }
                case "stats", "stat" -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        out.add(player.getName());
                    }
                }
                case "top" -> {
                    for (StatsManager.SortKey key : StatsManager.SortKey.values()) {
                        out.add(key.name().toLowerCase(Locale.ROOT));
                    }
                }
                case "start", "stop" -> out.addAll(plugin.arenaManager().names());
                case "arena", "map", "setup" -> addMatches(out, args[1], "create", "delete", "edit", "done",
                        "setworld", "setlobby", "setspectator", "setshop", "setupgrades", "setspawn",
                        "setbed", "setgen", "adddiamond", "addemerald", "cleargen", "toggleexp",
                        "pos1", "pos2", "save", "info");
                default -> {
                }
            }
            return out;
        }
        if (args.length == 3) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "delete", "edit", "info" -> out.addAll(plugin.arenaManager().names());
                case "setspawn", "setbed", "setgen" -> {
                    Arena arena = editingArena();
                    if (arena != null) {
                        for (TeamColor color : arena.mode().teams()) {
                            out.add(color.name().toLowerCase(Locale.ROOT));
                        }
                    }
                }
                default -> {
                }
            }
            return out;
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("create")) {
            for (GameMode mode : GameMode.values()) {
                out.add(mode.name().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    private void addMatches(List<String> out, String prefix, String... values) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(value);
            }
        }
    }
}
