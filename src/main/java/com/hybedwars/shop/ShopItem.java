package com.hybedwars.shop;

import com.hybedwars.game.TeamColor;
import com.hybedwars.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** 商店中的一件商品。 */
public class ShopItem {

    /** 货币。 */
    public enum Currency {
        IRON("铁锭", Material.IRON_INGOT, "&f"),
        GOLD("金锭", Material.GOLD_INGOT, "&6"),
        DIAMOND("钻石", Material.DIAMOND, "&b"),
        EMERALD("绿宝石", Material.EMERALD, "&a");

        private final String display;
        private final Material material;
        private final String color;

        Currency(String display, Material material, String color) {
            this.display = display;
            this.material = material;
            this.color = color;
        }

        public String display() {
            return display;
        }

        public Material material() {
            return material;
        }

        public String color() {
            return color;
        }

        /** 经验模式下一个该货币单位折算的经验值。 */
        public int xpValue() {
            return switch (this) {
                case IRON -> com.hybedwars.config.Settings.xpValueIron;
                case GOLD -> com.hybedwars.config.Settings.xpValueGold;
                case DIAMOND -> com.hybedwars.config.Settings.xpValueDiamond;
                case EMERALD -> com.hybedwars.config.Settings.xpValueEmerald;
            };
        }
    }

    /** 商品类型，决定购买后发放什么。 */
    public enum Kind {
        BLOCK, TEAM_WOOL, TEAM_TERRACOTTA, TEAM_GLASS,
        SWORD, PICKAXE, AXE, SHEARS, ARMOR, BOW, ARROW,
        KNOCKBACK_STICK, GOLDEN_APPLE, POTION,
        FIREBALL, TNT, ENDER_PEARL, WATER_BUCKET, SPONGE, LADDER,
        BRIDGE_EGG, BED_BUG, DREAM_DEFENDER, POPUP_TOWER, MAGIC_MILK,
        RESCUE_PLATFORM,
        UPGRADES
    }

    private final String id;
    private final Kind kind;
    private final ShopCategory category;
    private final int slot;
    private final Material icon;
    private final String name;
    private final List<String> description;
    private final Currency currency;
    private final int price;
    private final int amount;
    private final int tier;
    private final String potionEffect;
    private final int potionDuration;
    private final int potionAmplifier;

    private boolean enabled = true;
    /** 由 shop.yml 覆盖的价格，-1 表示使用内置默认值。 */
    private int priceOverride = -1;

    public ShopItem(String id, Kind kind, ShopCategory category, int slot, Material icon,
                    String name, List<String> description, Currency currency, int price,
                    int amount, int tier, String potionEffect, int potionDuration, int potionAmplifier) {
        this.id = id;
        this.kind = kind;
        this.category = category;
        this.slot = slot;
        this.icon = icon;
        this.name = name;
        this.description = description;
        this.currency = currency;
        this.price = price;
        this.amount = amount;
        this.tier = tier;
        this.potionEffect = potionEffect;
        this.potionDuration = potionDuration;
        this.potionAmplifier = potionAmplifier;
    }

    public String id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    public ShopCategory category() {
        return category;
    }

    public int slot() {
        return slot;
    }

    public Material icon() {
        return icon;
    }

    public String name() {
        return name;
    }

    public List<String> description() {
        return description;
    }

    public Currency currency() {
        return currency;
    }

    public int price() {
        return price;
    }

    public void setPrice(int price) {
        // 供配置覆盖使用
        this.priceOverride = price;
    }

    public int effectivePrice() {
        return priceOverride > 0 ? priceOverride : price;
    }

    public int amount() {
        return amount;
    }

    public int tier() {
        return tier;
    }

    public String potionEffect() {
        return potionEffect;
    }

    public int potionDuration() {
        return potionDuration;
    }

    public int potionAmplifier() {
        return potionAmplifier;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPermanent() {
        return switch (kind) {
            case SWORD, PICKAXE, AXE, SHEARS, ARMOR, BOW -> true;
            default -> false;
        };
    }

    /** 生成 GUI 里展示的图标。 */
    public ItemStack buildIcon(TeamColor teamColor) {
        Material material = resolveMaterial(teamColor);
        int displayAmount = Math.max(1, Math.min(64, amount));
        ItemBuilder builder = ItemBuilder.of(material, displayAmount).name(name);
        builder.hideAttributes();
        if (description != null) {
            builder.lore(description);
        }
        return builder.build();
    }

    /** 售价描述，例如 "&f铁锭 ×4"。 */
    public String priceText() {
        return currency.color() + currency.display() + " &7×" + effectivePrice();
    }

    /** 经验模式下的售价（原始金额 × 该货币的经验价值）。 */
    public int xpPrice() {
        return effectivePrice() * currency.xpValue();
    }

    /** 按当前经济类型返回售价描述。 */
    public String priceText(boolean experienceMode) {
        return experienceMode ? "&a经验 &7×" + xpPrice() : priceText();
    }

    private Material resolveMaterial(TeamColor teamColor) {
        if (teamColor == null) {
            return icon;
        }
        return switch (kind) {
            case TEAM_WOOL -> teamColor.wool();
            case TEAM_TERRACOTTA -> teamColor.terracotta();
            case TEAM_GLASS -> teamColor.stainedGlass();
            default -> icon;
        };
    }
}
