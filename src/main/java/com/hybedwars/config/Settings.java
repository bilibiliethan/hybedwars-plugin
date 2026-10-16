package com.hybedwars.config;

import com.hybedwars.HyBedWars;
import com.hybedwars.util.Loc;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** config.yml 的静态快照，启动与重载时刷新。 */
public final class Settings {

    // 通用
    public static int voidY = 0;
    public static Location lobby;
    public static boolean forceLobby = false;

    // 大厅
    public static String lobbyWorld = "";
    public static List<LobbyNpc> lobbyNpcs = new ArrayList<>();

    // 游戏流程
    public static int startCountdown = 30;
    public static int fullCountdown = 10;
    public static int respawnDelay = 5;
    public static int maxBuildY = 110;
    public static int bedProtectRadius = 5;
    public static int trapRadius = 8;
    public static int generatorItemLimit = 48;
    public static Set<String> blockedCommands = new LinkedHashSet<>();
    public static Set<String> allowedCommands = new LinkedHashSet<>();

    // 世界
    public static boolean copyTemplate = true;
    public static String templateFolder = "maps";
    public static boolean disableAutosave = true;

    // 生成器
    public static double[] ironRate = {1.5, 1.2, 0.9, 0.6};
    public static double[] goldRate = {8.0, 6.0, 4.0, 2.0};
    public static double[] diamondRate = {30.0, 20.0, 15.0};
    public static double[] emeraldRate = {60.0, 45.0, 30.0};
    public static int[] diamondUpgradeAt = {360, 720};
    public static int[] emeraldUpgradeAt = {720, 1440};

    // 事件
    public static int bedGoneSeconds = 1800;
    public static int gameEndSeconds = 2700;

    // 战斗
    public static double fireballSpeed = 1.35;
    public static double fireballPower = 2.2;
    public static boolean fireballBreakBlocks = true;
    public static double tntFuse = 2.0;
    public static int pearlCooldown = 3;
    public static boolean dropItemsOnDeath = true;
    public static int damageImmunity = 0;

    // 统计
    public static int statsSaveInterval = 300;
    public static int xpPerLevel = 5000;
    public static int xpWin = 100;
    public static int xpKill = 10;
    public static int xpFinalKill = 30;
    public static int xpBedBreak = 25;
    public static int xpPlay = 5;

    // 商店
    public static boolean shopRequireSneak = false;
    public static int bulkBuyAmount = 5;
    public static boolean refundOnFail = true;

    // 经验模式（捡到的铁/金/钻石/绿宝石自动转化为经验值，商店用经验结算）
    public static int xpValueIron = 1;
    public static int xpValueGold = 4;
    public static int xpValueDiamond = 12;
    public static int xpValueEmerald = 30;
    public static int xpKillReward = 10;
    public static int xpBedReward = 25;
    public static boolean xpPickupMessage = true;

    // 杂项
    public static boolean scoreboard = true;
    public static boolean teamNametagColor = true;
    public static boolean friendlyFire = false;
    public static boolean lobbyProtect = true;

    private Settings() {
    }

    /** 大厅 NPC：关联某个地图，并位于相对大厅世界的一组坐标。 */
    public static class LobbyNpc {
        public final String arena;
        public final Location location;

        public LobbyNpc(String arena, Location location) {
            this.arena = arena;
            this.location = location;
        }
    }

    /** 解析 lobby.npcs 中的一条 "arena=名称;location=x,y,z[,yaw,pitch]"。 */
    private static void parseLobbyNpc(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String arena = "";
        double x = 0, y = 0, z = 0;
        float yaw = 0f, pitch = 0f;
        boolean hasLoc = false;
        for (String part : raw.split(";")) {
            int idx = part.indexOf('=');
            if (idx < 0) {
                continue;
            }
            String key = part.substring(0, idx).trim().toLowerCase(java.util.Locale.ROOT);
            String val = part.substring(idx + 1).trim();
            if (key.equals("arena")) {
                arena = val;
            } else if (key.equals("location")) {
                String[] p = val.split(",");
                if (p.length >= 3) {
                    try {
                        x = Double.parseDouble(p[0]);
                        y = Double.parseDouble(p[1]);
                        z = Double.parseDouble(p[2]);
                        if (p.length > 3) yaw = Float.parseFloat(p[3]);
                        if (p.length > 4) pitch = Float.parseFloat(p[4]);
                        hasLoc = true;
                    } catch (NumberFormatException ignored) {
                        // 坐标解析失败
                    }
                }
            }
        }
        if (!arena.isEmpty() && hasLoc) {
            lobbyNpcs.add(new LobbyNpc(arena, new Location(null, x, y, z, yaw, pitch)));
        }
    }

    public static void load() {
        HyBedWars plugin = HyBedWars.get();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        Messages.setPrefix(c.getString("prefix", "&8[&c&lBED&f&lWARS&8] &r"));

        voidY = c.getInt("game.void-y", 0);
        lobby = Loc.deserialize(c.getString("lobby.spawn", ""));
        forceLobby = c.getBoolean("lobby.force-on-join", false);
        lobbyWorld = c.getString("lobby.world", "");
        lobbyNpcs.clear();
        for (String raw : c.getStringList("lobby.npcs")) {
            parseLobbyNpc(raw);
        }

        startCountdown = c.getInt("game.start-countdown", 30);
        fullCountdown = c.getInt("game.full-countdown", 10);
        respawnDelay = c.getInt("game.respawn-delay", 5);
        maxBuildY = c.getInt("game.max-build-y", 110);
        bedProtectRadius = c.getInt("game.bed-protect-radius", 5);
        trapRadius = c.getInt("game.trap-radius", 8);
        generatorItemLimit = c.getInt("game.generator-item-limit", 48);

        blockedCommands.clear();
        for (String s : c.getStringList("game.blocked-commands")) {
            blockedCommands.add(s.toLowerCase());
        }
        allowedCommands.clear();
        for (String s : c.getStringList("game.allowed-commands")) {
            allowedCommands.add(s.toLowerCase());
        }

        copyTemplate = c.getBoolean("worlds.copy-template", true);
        templateFolder = c.getString("worlds.template-folder", "maps");
        disableAutosave = c.getBoolean("worlds.disable-autosave", true);

        ironRate = toDoubles(c.getDoubleList("generators.iron"), ironRate);
        goldRate = toDoubles(c.getDoubleList("generators.gold"), goldRate);
        diamondRate = toDoubles(c.getDoubleList("generators.diamond"), diamondRate);
        emeraldRate = toDoubles(c.getDoubleList("generators.emerald"), emeraldRate);
        diamondUpgradeAt = toInts(c.getIntegerList("generators.diamond-upgrade-at"), diamondUpgradeAt);
        emeraldUpgradeAt = toInts(c.getIntegerList("generators.emerald-upgrade-at"), emeraldUpgradeAt);

        bedGoneSeconds = c.getInt("events.bed-gone", 1800);
        gameEndSeconds = c.getInt("events.game-end", 2700);

        fireballSpeed = c.getDouble("combat.fireball-speed", 1.35);
        fireballPower = c.getDouble("combat.fireball-power", 2.2);
        fireballBreakBlocks = c.getBoolean("combat.fireball-break-blocks", true);
        tntFuse = c.getDouble("combat.tnt-fuse", 2.0);
        pearlCooldown = c.getInt("combat.pearl-cooldown", 3);
        dropItemsOnDeath = c.getBoolean("combat.drop-items-on-death", true);
        damageImmunity = c.getInt("combat.damage-immunity", 0);

        statsSaveInterval = c.getInt("stats.save-interval", 300);
        xpPerLevel = Math.max(1, c.getInt("stats.xp-per-level", 5000));
        xpWin = c.getInt("stats.xp.win", 100);
        xpKill = c.getInt("stats.xp.kill", 10);
        xpFinalKill = c.getInt("stats.xp.final-kill", 30);
        xpBedBreak = c.getInt("stats.xp.bed-break", 25);
        xpPlay = c.getInt("stats.xp.play", 5);

        shopRequireSneak = c.getBoolean("shop.require-sneak", false);
        bulkBuyAmount = Math.max(1, c.getInt("shop.bulk-buy-amount", 5));
        refundOnFail = c.getBoolean("shop.refund-on-fail", true);

        xpValueIron = Math.max(1, c.getInt("experience.values.iron", 1));
        xpValueGold = Math.max(1, c.getInt("experience.values.gold", 4));
        xpValueDiamond = Math.max(1, c.getInt("experience.values.diamond", 12));
        xpValueEmerald = Math.max(1, c.getInt("experience.values.emerald", 30));
        xpKillReward = Math.max(0, c.getInt("experience.kill-reward", 10));
        xpBedReward = Math.max(0, c.getInt("experience.bed-reward", 25));
        xpPickupMessage = c.getBoolean("experience.pickup-message", true);

        scoreboard = c.getBoolean("misc.scoreboard", true);
        teamNametagColor = c.getBoolean("misc.team-nametag-color", true);
        friendlyFire = c.getBoolean("misc.friendly-fire", false);
        lobbyProtect = c.getBoolean("misc.lobby-protect", true);
    }

    private static double[] toDoubles(List<Double> list, double[] fallback) {
        if (list == null || list.isEmpty()) {
            return fallback;
        }
        double[] out = new double[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    private static int[] toInts(List<Integer> list, int[] fallback) {
        if (list == null || list.isEmpty()) {
            return fallback;
        }
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    /** 按等级取生成速度（等级从 1 开始，超出范围取最后一档）。 */
    public static double rate(double[] table, int tier) {
        if (table.length == 0) {
            return 1.0;
        }
        int index = Math.max(0, Math.min(table.length - 1, tier - 1));
        return Math.max(0.05, table[index]);
    }

    public static List<Integer> tierUpgradeTimes(double[] table, int[] upgrades) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < table.length - 1; i++) {
            if (i < upgrades.length) {
                out.add(upgrades[i]);
            }
        }
        return out;
    }

    /** 该材料是否为经验模式下的"可转化资源"（铁/金/钻石/绿宝石）。 */
    public static boolean isExperienceResource(org.bukkit.Material material) {
        return xpValueOf(material) > 0;
    }

    /**
     * 经验模式下，捡到 1 个该材料可获得多少经验。
     *
     * @return 0 表示该材料不转化
     */
    public static int xpValueOf(org.bukkit.Material material) {
        if (material == null) {
            return 0;
        }
        return switch (material) {
            case IRON_INGOT -> xpValueIron;
            case GOLD_INGOT -> xpValueGold;
            case DIAMOND -> xpValueDiamond;
            case EMERALD -> xpValueEmerald;
            default -> 0;
        };
    }
}
