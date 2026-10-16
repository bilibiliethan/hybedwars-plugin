package com.hybedwars.game;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/** 队伍升级（钻石升级）。 */
public enum UpgradeType {

    SHARPNESS("锋利之剑", "sharpness", Material.IRON_SWORD, new int[]{4},
            "全队剑获得锋利 I"),
    PROTECTION("护甲保护", "protection", Material.IRON_CHESTPLATE, new int[]{2, 4, 8, 16},
            "全队护甲获得保护附魔"),
    HASTE("急速挖掘", "haste", Material.GOLDEN_PICKAXE, new int[]{2, 4},
            "全队获得急迫效果"),
    FORGE("熔炉", "forge", Material.FURNACE, new int[]{4, 8, 12, 16},
            "基地资源生成速度提升"),
    HEAL_POOL("治疗池", "heal_pool", Material.BEACON, new int[]{2},
            "基地范围内持续回复生命"),

    TRAP_IT("陷阱：出其不意", "trap_it", Material.TRIPWIRE_HOOK, new int[]{1},
            "敌人进入基地时使其短暂失明与减速"),
    TRAP_COUNTER("陷阱：反攻", "trap_counter", Material.FEATHER, new int[]{2},
            "敌人进入基地时全队获得速度与跳跃提升"),
    TRAP_ALARM("陷阱：警报", "trap_alarm", Material.NOTE_BLOCK, new int[]{3},
            "敌人进入基地时暴露所有隐身敌人"),
    TRAP_FATIGUE("陷阱：疲劳", "trap_fatigue", Material.IRON_PICKAXE, new int[]{4},
            "敌人进入基地时获得挖掘疲劳");

    private final String display;
    private final String id;
    private final Material icon;
    private final int[] prices;
    private final String description;

    UpgradeType(String display, String id, Material icon, int[] prices, String description) {
        this.display = display;
        this.id = id;
        this.icon = icon;
        this.prices = prices;
        this.description = description;
    }

    public String display() {
        return display;
    }

    public String id() {
        return id;
    }

    public Material icon() {
        return icon;
    }

    public String description() {
        return description;
    }

    public int maxLevel() {
        return prices.length;
    }

    /** 从 level 升到 level+1 的价格（钻石）。level 从 0 开始。 */
    public int price(int currentLevel) {
        if (currentLevel < 0 || currentLevel >= prices.length) {
            return -1;
        }
        return prices[currentLevel];
    }

    public boolean isTrap() {
        return name().startsWith("TRAP_");
    }

    public static List<UpgradeType> traps() {
        return List.of(TRAP_IT, TRAP_COUNTER, TRAP_ALARM, TRAP_FATIGUE);
    }

    public static List<UpgradeType> normal() {
        return List.of(SHARPNESS, PROTECTION, HASTE, FORGE, HEAL_POOL);
    }

    public static UpgradeType parse(String input) {
        if (input == null) {
            return null;
        }
        for (UpgradeType type : values()) {
            if (type.id.equalsIgnoreCase(input) || type.name().equalsIgnoreCase(input)) {
                return type;
            }
        }
        return null;
    }

    // ---------- 具体效果 ----------

    /** 药水效果：等级 1 → amplifier 0。 */
    public PotionEffect effect(int items, int level) {
        PotionEffectType type;
        int amplifier;
        int duration;
        switch (this) {
            case HASTE -> {
                type = com.hybedwars.util.Compat.effect("haste");
                amplifier = Math.max(0, level - 1);
                duration = items;
            }
            case HEAL_POOL -> {
                type = com.hybedwars.util.Compat.effect("regeneration");
                amplifier = 0;
                duration = items;
            }
            case TRAP_IT -> {
                type = com.hybedwars.util.Compat.effect("blindness");
                amplifier = 0;
                duration = items;
            }
            case TRAP_FATIGUE -> {
                type = com.hybedwars.util.Compat.effect("mining_fatigue");
                amplifier = Math.max(0, level - 1);
                duration = items;
            }
            default -> {
                return null;
            }
        }
        return new PotionEffect(type, duration, amplifier, false, true, true);
    }
}
