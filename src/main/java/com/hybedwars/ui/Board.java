package com.hybedwars.ui;

import com.hybedwars.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 一个玩家的侧边栏计分板，带脏检查避免无意义的封包。 */
public class Board {

    private static final int LINES = 15;

    private final Scoreboard scoreboard;
    private final Objective objective;
    private final List<String> entries = new ArrayList<>();
    private final Map<Integer, String> current = new HashMap<>();

    public Board(String title) {
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("hybw", Criteria.DUMMY, Chat.comp(title));
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        ChatColor[] colors = ChatColor.values();
        for (int i = 0; i < LINES; i++) {
            String entry = colors[i % colors.length].toString() + ChatColor.RESET;
            entries.add(entry);
            objective.getScore(entry).setScore(LINES - i);
        }
    }

    public Scoreboard scoreboard() {
        return scoreboard;
    }

    public Objective objective() {
        return objective;
    }

    /** 设置第 index 行（0 = 最上面）。text 为空则清空该行。 */
    public void setLine(int index, String text) {
        if (index < 0 || index >= LINES) {
            return;
        }
        String value = text == null ? "" : text;
        if (value.equals(current.get(index))) {
            return;
        }
        current.put(index, value);
        Team team = scoreboard.getTeam("line" + index);
        if (team == null) {
            team = scoreboard.registerNewTeam("line" + index);
            team.addEntry(entries.get(index));
        }
        team.prefix(Chat.comp(clamp(value)));
    }

    public void setLines(List<String> lines) {
        for (int i = 0; i < LINES; i++) {
            setLine(i, i < lines.size() ? lines.get(i) : "");
        }
    }

    public String line(int index) {
        return current.getOrDefault(index, "");
    }

    private String clamp(String text) {
        String stripped = Chat.strip(text);
        if (stripped.length() <= 30) {
            return text;
        }
        // 侧边栏可见宽度有限，超长时裁剪（按原始字符串长度近似处理）
        String colored = Chat.color(text);
        int excess = stripped.length() - 30;
        int cut = Math.min(colored.length(), excess * 2);
        return ChatColor.RESET + colored.substring(cut);
    }

    /** 队伍颜色名牌。 */
    public void applyTeam(String teamName, String prefix, String suffix, List<String> entries) {
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.prefix(Chat.comp(prefix));
        team.suffix(Chat.comp(suffix == null ? "" : suffix));
        for (String entry : entries) {
            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }
        }
    }

    public void resetTeam(String teamName) {
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            for (String entry : new ArrayList<>(team.getEntries())) {
                team.removeEntry(entry);
            }
            team.unregister();
        }
    }

    public void clear() {
        for (int i = 0; i < LINES; i++) {
            setLine(i, "");
        }
    }
}
