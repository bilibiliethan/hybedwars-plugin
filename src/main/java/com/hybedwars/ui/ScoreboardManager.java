package com.hybedwars.ui;

import com.hybedwars.HyBedWars;
import com.hybedwars.config.Settings;
import com.hybedwars.game.Arena;
import com.hybedwars.game.GameEvent;
import com.hybedwars.game.GamePlayer;
import com.hybedwars.game.Team;
import com.hybedwars.game.TeamColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** 侧边栏 + 队伍名牌颜色。每个玩家持有独立计分板，带指纹脏检查。 */
public class ScoreboardManager {

    private final HyBedWars plugin;
    private final Map<UUID, Board> boards = new HashMap<>();
    private final Map<UUID, String> fingerprints = new HashMap<>();

    public ScoreboardManager(HyBedWars plugin) {
        this.plugin = plugin;
    }

    public void update(Arena arena) {
        if (!Settings.scoreboard) {
            return;
        }
        String assignment = buildAssignmentFingerprint(arena);
        for (GamePlayer gp : new ArrayList<>(arena.gamePlayers())) {
            Player player = gp.bukkit();
            if (player == null || !player.isOnline()) {
                continue;
            }
            List<String> lines = buildLines(arena, gp);
            String fingerprint = assignment + "||" + String.join("\n", lines);
            if (fingerprint.equals(fingerprints.get(gp.uuid()))) {
                continue;
            }
            fingerprints.put(gp.uuid(), fingerprint);
            Board board = boards.computeIfAbsent(gp.uuid(), id -> new Board("&c&l起床战争"));
            board.setLines(lines);
            if (player.getScoreboard() != board.scoreboard()) {
                player.setScoreboard(board.scoreboard());
            }
            applyTeams(arena, board);
        }
    }

    private String buildAssignmentFingerprint(Arena arena) {
        StringBuilder sb = new StringBuilder();
        for (Player player : arena.onlinePlayers()) {
            sb.append(player.getUniqueId()).append(':')
                    .append(arena.teamColorOf(player)).append(';');
        }
        return sb.toString();
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
        fingerprints.remove(player.getUniqueId());
    }

    public void clear() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboard() != Bukkit.getScoreboardManager().getMainScoreboard()) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        boards.clear();
        fingerprints.clear();
    }

    private List<String> buildLines(Arena arena, GamePlayer gp) {
        List<String> lines = new ArrayList<>();
        lines.add("&8&m--------------------");
        lines.add("&c&l床 战 争 &8| &e" + arena.name());
        lines.add("&7" + arena.mode().display() + " &8| &7" + arena.mode().maxPlayers() + " 人上限");

        if (arena.status() == Arena.Status.RUNNING) {
            GameEvent next = arena.nextEvent();
            String eventPart = next == null
                    ? "&7下个事件: &c无"
                    : "&7下个: &e" + next.type().display() + " &7(" + time(arena.secondsUntilNextEvent()) + "&7)";
            lines.add("&7时间: &e" + time(arena.elapsedSeconds()) + " &8| " + eventPart);
        } else {
            int seconds = arena.countdown();
            lines.add("&7等待中 &8| &7人数: &e" + arena.onlinePlayers().size()
                    + "&7/&e" + arena.mode().maxPlayers());
            lines.add(seconds > 0
                    ? "&7开始倒计时: &c" + time(seconds)
                    : "&7等待更多玩家加入…");
        }
        lines.add("");

        for (TeamColor color : arena.mode().teams()) {
            Team team = arena.team(color);
            if (team == null) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(color.code()).append('■').append(' ').append(color.code()).append(color.display());
            if (team.isEliminated()) {
                sb.append(" &8已淘汰");
            } else {
                sb.append(" &f").append(team.aliveCount()).append("&7/").append(arena.mode().teamSize());
                sb.append(team.bedAlive() ? " &a✔" : " &c✖");
            }
            lines.add(sb.toString());
        }

        List<String> tail = new ArrayList<>();
        tail.add("");
        if (gp.teamColor() != null) {
            tail.add("&7你的队伍: " + gp.teamColor().code() + gp.teamColor().display());
        }
        if (arena.experienceMode()) {
            tail.add("&7经验: &a" + gp.experience());
        }
        tail.add("&7击杀: &e" + gp.kills() + " &8| &7最终: &e" + gp.finalKills());
        tail.add("&7破坏床: &e" + gp.bedsBroken());
        while (!tail.isEmpty() && lines.size() < 14) {
            lines.add(tail.remove(0));
        }
        if (lines.size() < 15) {
            lines.add("&8HyBedWars &7v1.0");
        }
        return lines;
    }

    private void applyTeams(Arena arena, Board board) {
        for (TeamColor color : TeamColor.STANDARD_ORDER) {
            List<String> entries = new ArrayList<>();
            for (Player player : arena.onlinePlayers()) {
                GamePlayer gp = arena.player(player);
                if (gp != null && gp.teamColor() == color) {
                    entries.add(player.getName());
                }
            }
            if (entries.isEmpty()) {
                board.resetTeam("bw_" + color.name().toLowerCase(Locale.ROOT));
                continue;
            }
            String prefix = Settings.teamNametagColor ? color.code() : "";
            board.applyTeam("bw_" + color.name().toLowerCase(Locale.ROOT), prefix, "", entries);
            org.bukkit.scoreboard.Team team = board.scoreboard()
                    .getTeam("bw_" + color.name().toLowerCase(Locale.ROOT));
            if (team != null) {
                team.setAllowFriendlyFire(false);
                team.setCanSeeFriendlyInvisibles(true);
            }
        }
    }

    public static String time(int seconds) {
        int safe = Math.max(0, seconds);
        int minutes = safe / 60;
        int rest = safe % 60;
        return minutes + ":" + (rest < 10 ? "0" : "") + rest;
    }
}
