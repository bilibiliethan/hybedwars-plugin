package com.hybedwars.game;

import org.bukkit.DyeColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 队伍颜色。所有方块材质均由染料颜色推导，避免硬编码。 */
public enum TeamColor {

    RED("红队", "&c", DyeColor.RED),
    BLUE("蓝队", "&9", DyeColor.BLUE),
    GREEN("绿队", "&a", DyeColor.GREEN),
    YELLOW("黄队", "&e", DyeColor.YELLOW),
    AQUA("青队", "&b", DyeColor.LIGHT_BLUE),
    WHITE("白队", "&f", DyeColor.WHITE),
    PINK("粉队", "&d", DyeColor.PINK),
    GRAY("灰队", "&7", DyeColor.GRAY);

    public static final List<TeamColor> STANDARD_ORDER = List.of(
            RED, BLUE, GREEN, YELLOW, AQUA, WHITE, PINK, GRAY);

    private final String display;
    private final String code;
    private final DyeColor dye;

    TeamColor(String display, String code, DyeColor dye) {
        this.display = display;
        this.code = code;
        this.dye = dye;
    }

    public String display() {
        return display;
    }

    /** 例如 "&c"。 */
    public String code() {
        return code;
    }

    public String colored() {
        return code + display;
    }

    public DyeColor dye() {
        return dye;
    }

    public org.bukkit.ChatColor chat() {
        return org.bukkit.ChatColor.getByChar(code.charAt(1));
    }

    public org.bukkit.Color bukkitColor() {
        return dye.getColor();
    }

    public org.bukkit.Material wool() {
        return org.bukkit.Material.valueOf(dye.name() + "_WOOL");
    }

    public org.bukkit.Material terracotta() {
        return org.bukkit.Material.valueOf(dye.name() + "_TERRACOTTA");
    }

    public org.bukkit.Material stainedGlass() {
        return org.bukkit.Material.valueOf(dye.name() + "_STAINED_GLASS");
    }

    public org.bukkit.Material bed() {
        return org.bukkit.Material.valueOf(dye.name() + "_BED");
    }

    /** 取前 n 个队伍（4 队模式会自动变成 红/蓝/绿/黄）。 */
    public static List<TeamColor> first(int count) {
        List<TeamColor> out = new ArrayList<>();
        for (TeamColor color : STANDARD_ORDER) {
            if (out.size() >= count) {
                break;
            }
            out.add(color);
        }
        return out;
    }

    public static TeamColor parse(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim().toUpperCase(Locale.ROOT);
        for (TeamColor color : values()) {
            if (color.name().equals(value)) {
                return color;
            }
        }
        // 支持中文 / 颜色代码
        for (TeamColor color : values()) {
            if (color.display.equals(input.trim()) || color.code.equalsIgnoreCase(input.trim())) {
                return color;
            }
        }
        return switch (value) {
            case "LIGHT_BLUE", "AQUA", "青" -> AQUA;
            case "PURPLE", "MAGENTA", "粉" -> PINK;
            case "GREY", "灰" -> GRAY;
            default -> null;
        };
    }
}
