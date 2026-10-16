package com.hybedwars.shop;

import org.bukkit.Material;

/** 商店分类。 */
public enum ShopCategory {

    BLOCKS("方块", Material.TERRACOTTA, 20),
    MELEE("近战武器", Material.IRON_SWORD, 21),
    ARMOR("护甲", Material.IRON_CHESTPLATE, 22),
    TOOLS("工具", Material.STONE_PICKAXE, 23),
    RANGED("远程武器", Material.BOW, 24),
    POTIONS("药水", Material.POTION, 25),
    UTILITY("功能道具", Material.TNT, 26);

    private final String display;
    private final Material icon;
    private final int slot;

    ShopCategory(String display, Material icon, int slot) {
        this.display = display;
        this.icon = icon;
        this.slot = slot;
    }

    public String display() {
        return display;
    }

    public Material icon() {
        return icon;
    }

    public int slot() {
        return slot;
    }

    public static ShopCategory parse(String input) {
        if (input == null) {
            return null;
        }
        for (ShopCategory category : values()) {
            if (category.name().equalsIgnoreCase(input) || category.display.equals(input.trim())) {
                return category;
            }
        }
        return null;
    }
}
