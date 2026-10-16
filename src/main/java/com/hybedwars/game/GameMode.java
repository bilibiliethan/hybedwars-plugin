package com.hybedwars.game;

import java.util.List;
import java.util.Locale;

/**
 * 游戏模式。
 * <ul>
 *     <li>{@link #SOLO} 单人八队 —— 8 队 × 1 人 = 8 人</li>
 *     <li>{@link #DOUBLES} 双人八队 —— 8 队 × 2 人 = 16 人</li>
 *     <li>{@link #QUADS} 四人四队 —— 4 队 × 4 人 = 16 人</li>
 *     <li>{@link #MINI} 四队八人（快速局）—— 4 队 × 2 人 = 8 人</li>
 *     <li>{@link #EXPERIENCE} 经验四队八人 —— 4 队 × 8 人 = 32 人，资源自动转化为经验，商店用经验结算</li>
 * </ul>
 */
public enum GameMode {

    SOLO("单人八队", 8, 1, false),
    DOUBLES("双人八队", 8, 2, false),
    QUADS("四人四队", 4, 4, false),
    MINI("四队八人", 4, 2, false),
    EXPERIENCE("经验四队八人", 4, 8, true);

    private final String display;
    private final int teamCount;
    private final int teamSize;
    private final boolean experience;

    GameMode(String display, int teamCount, int teamSize, boolean experience) {
        this.display = display;
        this.teamCount = teamCount;
        this.teamSize = teamSize;
        this.experience = experience;
    }

    public String display() {
        return display;
    }

    public int teamCount() {
        return teamCount;
    }

    public int teamSize() {
        return teamSize;
    }

    /** 是否为经验模式（资源转化为经验、商店用经验结算）。 */
    public boolean experience() {
        return experience;
    }

    public int maxPlayers() {
        return teamCount * teamSize;
    }

    /** 开始时至少需要的玩家数量（每队至少一人）。 */
    public int minPlayers() {
        return Math.max(2, teamCount);
    }

    public List<TeamColor> teams() {
        return TeamColor.first(teamCount);
    }

    public String descriptor() {
        return display + "（" + teamCount + " 队 × " + teamSize + " 人，共 " + maxPlayers() + " 人）"
                + (experience ? " · 经验模式" : "");
    }

    public static GameMode parse(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        for (GameMode mode : values()) {
            if (mode.name().toLowerCase(Locale.ROOT).equals(value) || mode.display.equals(input.trim())) {
                return mode;
            }
        }
        return switch (value) {
            case "s", "1", "单人", "solo8", "8x1" -> SOLO;
            case "d", "duo", "2", "双人", "8x2" -> DOUBLES;
            case "q", "quad", "4v4v4v4", "4", "四人", "4x4" -> QUADS;
            case "m", "mini", "体验", "四队八人", "4x2", "mini4" -> MINI;
            case "xp", "exp", "experience", "exp8", "经验", "经验模式", "经验四队八人", "4x2exp", "4x2xp" -> EXPERIENCE;
            default -> null;
        };
    }

    public static String commandHint() {
        StringBuilder sb = new StringBuilder();
        for (GameMode mode : values()) {
            if (sb.length() > 0) {
                sb.append("&7 | ");
            }
            sb.append("&e").append(mode.name().toLowerCase(Locale.ROOT)).append("&7(").append(mode.display()).append(")");
        }
        return sb.toString();
    }
}
