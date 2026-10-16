package com.hybedwars.item;

import com.hybedwars.HyBedWars;
import com.hybedwars.game.TeamColor;
import com.hybedwars.util.Compat;
import com.hybedwars.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

/** 特殊道具工厂。所有自定义道具都用 PDC 打上 "special" 标记。 */
public final class Items {

    public static final String TAG = "special";

    private static NamespacedKey key;

    private Items() {
    }

    private static NamespacedKey key() {
        if (key == null) {
            key = new NamespacedKey(HyBedWars.get(), TAG);
        }
        return key;
    }

    private static void tag(ItemStack stack, String id) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key(), PersistentDataType.STRING, id);
            stack.setItemMeta(meta);
        }
    }

    /** 判断物品是否为指定 id 的自定义道具。 */
    public static boolean is(ItemStack stack, String id) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        String value = meta.getPersistentDataContainer().get(key(), PersistentDataType.STRING);
        return id.equals(value);
    }

    public static String tagOf(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta == null ? null : meta.getPersistentDataContainer().get(key(), PersistentDataType.STRING);
    }

    // ---------------- 武器 / 工具 ----------------

    public static ItemStack sword(int tier, int sharpness, TeamColor color) {
        Material material = switch (tier) {
            case 3 -> Material.DIAMOND_SWORD;
            case 2 -> Material.IRON_SWORD;
            case 1 -> Material.STONE_SWORD;
            default -> Material.WOODEN_SWORD;
        };
        ItemBuilder builder = ItemBuilder.of(material).unbreakable(true).hideAttributes();
        if (sharpness > 0) {
            builder.enchant(Compat.ench("sharpness"), sharpness);
        }
        return builder.build();
    }

    public static ItemStack pickaxe(int tier) {
        Material material = switch (tier) {
            case 3, 4 -> Material.DIAMOND_PICKAXE;
            case 2 -> Material.IRON_PICKAXE;
            default -> Material.WOODEN_PICKAXE;
        };
        ItemBuilder builder = ItemBuilder.of(material).unbreakable(true).hideAttributes();
        if (tier >= 4) {
            builder.enchant(Compat.ench("efficiency"), 1);
        }
        return builder.build();
    }

    public static ItemStack axe(int tier) {
        Material material = switch (tier) {
            case 4 -> Material.DIAMOND_AXE;
            case 3 -> Material.IRON_AXE;
            case 2 -> Material.STONE_AXE;
            default -> Material.WOODEN_AXE;
        };
        return ItemBuilder.of(material).unbreakable(true).hideAttributes().build();
    }

    public static ItemStack shears() {
        return ItemBuilder.of(Material.SHEARS).unbreakable(true).build();
    }

    public static ItemStack knockbackStick() {
        return ItemBuilder.of(Material.STICK)
                .name("&a击退木棍")
                .enchant(Compat.ench("knockback"), 1)
                .hideAttributes()
                .build();
    }

    public static ItemStack bow(int tier) {
        ItemBuilder builder = ItemBuilder.of(Material.BOW).unbreakable(true);
        if (tier >= 2) {
            builder.enchant(Compat.ench("power"), 1);
        }
        if (tier >= 3) {
            builder.enchant(Compat.ench("punch"), 1);
        }
        return builder.build();
    }

    public static ItemStack arrows(int amount) {
        return ItemBuilder.of(Material.ARROW, amount).build();
    }

    public static ItemStack goldenApple() {
        return ItemBuilder.of(Material.GOLDEN_APPLE).build();
    }

    // ---------------- 功能道具 ----------------

    public static ItemStack fireball() {
        ItemStack stack = ItemBuilder.of(Material.FIRE_CHARGE)
                .name("&6火球")
                .lore("&7右键发射，炸开一条通路")
                .build();
        tag(stack, "fireball");
        return stack;
    }

    public static ItemStack tnt() {
        ItemStack stack = ItemBuilder.of(Material.TNT)
                .name("&cTNT")
                .lore("&7放置后立即引爆")
                .build();
        tag(stack, "tnt");
        return stack;
    }

    public static ItemStack enderPearl() {
        return ItemBuilder.of(Material.ENDER_PEARL)
                .name("&5末影珍珠")
                .build();
    }

    public static ItemStack waterBucket() {
        ItemStack stack = ItemBuilder.of(Material.WATER_BUCKET)
                .name("&b水桶")
                .lore("&7用于降低摔落伤害")
                .build();
        tag(stack, "water");
        return stack;
    }

    public static ItemStack sponge() {
        return ItemBuilder.of(Material.SPONGE)
                .name("&e海绵")
                .lore("&7吸收周围的水")
                .build();
    }

    public static ItemStack ladder(int amount) {
        return ItemBuilder.of(Material.LADDER, amount).build();
    }

    public static ItemStack bridgeEgg() {
        ItemStack stack = ItemBuilder.of(Material.EGG)
                .name("&a搭桥蛋")
                .lore("&7投掷后自动铺出一条羊毛桥")
                .build();
        tag(stack, "bridge_egg");
        return stack;
    }

    public static ItemStack bedBug() {
        ItemStack stack = ItemBuilder.of(Material.SNOWBALL)
                .name("&c床虱")
                .lore("&7投掷后召唤一只蠹虫")
                .build();
        tag(stack, "bed_bug");
        return stack;
    }

    public static ItemStack dreamDefender(TeamColor color) {
        ItemStack stack = ItemBuilder.of(Material.IRON_GOLEM_SPAWN_EGG)
                .name("&7梦境守卫")
                .lore("&7召唤一只保护基地的铁傀儡")
                .build();
        tag(stack, "dream_defender");
        return stack;
    }

    public static ItemStack popupTower() {
        ItemStack stack = ItemBuilder.of(Material.CHEST)
                .name("&6紧凑弹出塔")
                .lore("&7放置后原地升起一座防御塔")
                .build();
        tag(stack, "popup_tower");
        return stack;
    }

    public static ItemStack magicMilk() {
        ItemStack stack = ItemBuilder.of(Material.MILK_BUCKET)
                .name("&f魔法牛奶")
                .lore("&730 秒内不会触发敌方陷阱")
                .build();
        tag(stack, "magic_milk");
        return stack;
    }

    public static ItemStack rescuePlatform() {
        ItemStack stack = ItemBuilder.of(Material.STONE)
                .name("&f自救平台")
                .lore("&7坠落（空中）时右键，在脚下生成石板")
                .build();
        tag(stack, "rescue_platform");
        return stack;
    }

    public static ItemStack potion(String effectKey, int amplifier, int durationSeconds, String name, String color) {
        ItemStack stack = new ItemStack(Material.POTION);
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof PotionMeta potionMeta) {
            org.bukkit.potion.PotionEffectType type = Compat.effectOrNull(effectKey);
            if (type != null) {
                potionMeta.addCustomEffect(new PotionEffect(type, durationSeconds * 20,
                        Math.max(0, amplifier), false, true, true), true);
                try {
                    potionMeta.setColor(type.getColor());
                } catch (Throwable ignored) {
                    // 某些效果没有颜色
                }
            }
        }
        if (meta != null) {
            meta.setDisplayName(com.hybedwars.util.Chat.color(name));
            meta.setLore(java.util.List.of(com.hybedwars.util.Chat.color("&7饮用后获得效果")));
            stack.setItemMeta(meta);
        }
        tag(stack, "potion_" + effectKey);
        return stack;
    }
}
