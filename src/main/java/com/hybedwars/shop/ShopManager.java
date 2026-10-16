package com.hybedwars.shop;

import com.hybedwars.HyBedWars;
import com.hybedwars.config.Messages;
import com.hybedwars.config.Settings;
import com.hybedwars.game.GamePlayer;
import com.hybedwars.game.Team;
import com.hybedwars.game.TeamColor;
import com.hybedwars.game.UpgradeType;
import com.hybedwars.item.Items;
import com.hybedwars.util.Chat;
import com.hybedwars.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 商店：商品目录、GUI 构建与购买逻辑。 */
public class ShopManager {

    private static final int MAIN_SLOT_INFO = 4;
    private static final int SLOT_UPGRADES = 49;
    private static final int SLOT_CLOSE = 45;
    private static final int SLOT_CLOSE_ALT = 8;

    private final HyBedWars plugin;
    private final Map<String, ShopItem> catalog = new LinkedHashMap<>();

    public ShopManager(HyBedWars plugin) {
        this.plugin = plugin;
    }

    // ==========================================================
    //  目录
    // ==========================================================

    public void load() {
        catalog.clear();
        buildCatalog();
        loadOverrides();
    }

    public Map<String, ShopItem> catalog() {
        return catalog;
    }

    public List<ShopItem> byCategory(ShopCategory category) {
        List<ShopItem> list = new ArrayList<>();
        for (ShopItem item : catalog.values()) {
            if (item.category() == category && item.enabled()) {
                list.add(item);
            }
        }
        list.sort(java.util.Comparator.comparingInt(ShopItem::slot));
        return list;
    }

    private void add(String id, ShopItem.Kind kind, ShopCategory category, int slot, Material icon,
                     String name, ShopItem.Currency currency, int price, int amount, int tier,
                     String potionEffect, int potionDuration, int potionAmplifier, String... lore) {
        catalog.put(id, new ShopItem(id, kind, category, slot, icon, name, List.of(lore),
                currency, price, amount, tier, potionEffect, potionDuration, potionAmplifier));
    }

    private void buildCatalog() {
        // ---------------- 方块 ----------------
        ShopCategory blocks = ShopCategory.BLOCKS;
        add("wool", ShopItem.Kind.TEAM_WOOL, blocks, 10, Material.WHITE_WOOL,
                "&f羊毛 &7×16", ShopItem.Currency.IRON, 4, 16, 0, null, 0, 0,
                "&7最经济的搭桥与护床材料");
        add("terracotta", ShopItem.Kind.TEAM_TERRACOTTA, blocks, 11, Material.TERRACOTTA,
                "&f硬化黏土 &7×16", ShopItem.Currency.IRON, 12, 16, 0, null, 0, 0,
                "&7比羊毛更耐炸");
        add("glass", ShopItem.Kind.TEAM_GLASS, blocks, 12, Material.GLASS,
                "&f防爆玻璃 &7×4", ShopItem.Currency.IRON, 12, 4, 0, null, 0, 0,
                "&7不会被爆炸破坏（仍可被挖）");
        add("endstone", ShopItem.Kind.BLOCK, blocks, 19, Material.END_STONE,
                "&f末地石 &7×12", ShopItem.Currency.IRON, 24, 12, 0, null, 0, 0,
                "&7坚固的护床材料");
        add("planks", ShopItem.Kind.BLOCK, blocks, 20, Material.OAK_PLANKS,
                "&f木板 &7×16", ShopItem.Currency.GOLD, 4, 16, 0, null, 0, 0,
                "&7便宜的打折木结构");
        add("obsidian", ShopItem.Kind.BLOCK, blocks, 21, Material.OBSIDIAN,
                "&f黑曜石 &7×4", ShopItem.Currency.EMERALD, 4, 4, 0, null, 0, 0,
                "&7几乎无法被破坏");
        add("ladder", ShopItem.Kind.LADDER, blocks, 28, Material.LADDER,
                "&f梯子 &7×8", ShopItem.Currency.IRON, 4, 8, 0, null, 0, 0,
                "&7快速攀爬");

        // ---------------- 近战 ----------------
        ShopCategory melee = ShopCategory.MELEE;
        add("stone_sword", ShopItem.Kind.SWORD, melee, 10, Material.STONE_SWORD,
                "&f石剑", ShopItem.Currency.IRON, 10, 1, 1, null, 0, 0, "&7永久保留");
        add("iron_sword", ShopItem.Kind.SWORD, melee, 11, Material.IRON_SWORD,
                "&f铁剑", ShopItem.Currency.GOLD, 7, 1, 2, null, 0, 0, "&7永久保留");
        add("diamond_sword", ShopItem.Kind.SWORD, melee, 12, Material.DIAMOND_SWORD,
                "&f钻石剑", ShopItem.Currency.EMERALD, 4, 1, 3, null, 0, 0, "&7永久保留");
        add("knockback_stick", ShopItem.Kind.KNOCKBACK_STICK, melee, 19, Material.STICK,
                "&a击退木棍 &7(击退 I)", ShopItem.Currency.GOLD, 5, 1, 0, null, 0, 0,
                "&7把敌人打下虚空");

        // ---------------- 护甲 ----------------
        ShopCategory armor = ShopCategory.ARMOR;
        add("chain_armor", ShopItem.Kind.ARMOR, armor, 10, Material.CHAINMAIL_LEGGINGS,
                "&f锁链护甲", ShopItem.Currency.IRON, 40, 1, 1, null, 0, 0, "&7永久保留");
        add("iron_armor", ShopItem.Kind.ARMOR, armor, 11, Material.IRON_LEGGINGS,
                "&f铁护甲", ShopItem.Currency.GOLD, 12, 1, 2, null, 0, 0, "&7永久保留");
        add("diamond_armor", ShopItem.Kind.ARMOR, armor, 12, Material.DIAMOND_LEGGINGS,
                "&f钻石护甲", ShopItem.Currency.EMERALD, 6, 1, 3, null, 0, 0, "&7永久保留");

        // ---------------- 工具 ----------------
        ShopCategory tools = ShopCategory.TOOLS;
        add("shears", ShopItem.Kind.SHEARS, tools, 10, Material.SHEARS,
                "&f剪刀", ShopItem.Currency.IRON, 20, 1, 0, null, 0, 0, "&7快速清除羊毛");
        add("pickaxe_1", ShopItem.Kind.PICKAXE, tools, 11, Material.WOODEN_PICKAXE,
                "&f木镐", ShopItem.Currency.IRON, 10, 1, 1, null, 0, 0, "&7永久保留");
        add("pickaxe_2", ShopItem.Kind.PICKAXE, tools, 12, Material.IRON_PICKAXE,
                "&f铁镐", ShopItem.Currency.IRON, 10, 1, 2, null, 0, 0, "&7永久保留");
        add("pickaxe_3", ShopItem.Kind.PICKAXE, tools, 13, Material.DIAMOND_PICKAXE,
                "&f钻石镐", ShopItem.Currency.GOLD, 6, 1, 3, null, 0, 0, "&7永久保留");
        add("pickaxe_4", ShopItem.Kind.PICKAXE, tools, 14, Material.DIAMOND_PICKAXE,
                "&f钻石镐 &7(效率 I)", ShopItem.Currency.GOLD, 8, 1, 4, null, 0, 0, "&7永久保留");
        add("axe_1", ShopItem.Kind.AXE, tools, 19, Material.WOODEN_AXE,
                "&f木斧", ShopItem.Currency.IRON, 10, 1, 1, null, 0, 0, "&7永久保留");
        add("axe_2", ShopItem.Kind.AXE, tools, 20, Material.STONE_AXE,
                "&f石斧", ShopItem.Currency.IRON, 10, 1, 2, null, 0, 0, "&7永久保留");
        add("axe_3", ShopItem.Kind.AXE, tools, 21, Material.IRON_AXE,
                "&f铁斧", ShopItem.Currency.GOLD, 6, 1, 3, null, 0, 0, "&7永久保留");
        add("axe_4", ShopItem.Kind.AXE, tools, 22, Material.DIAMOND_AXE,
                "&f钻石斧", ShopItem.Currency.GOLD, 8, 1, 4, null, 0, 0, "&7永久保留");

        // ---------------- 远程 ----------------
        ShopCategory ranged = ShopCategory.RANGED;
        add("bow_1", ShopItem.Kind.BOW, ranged, 10, Material.BOW,
                "&f弓", ShopItem.Currency.GOLD, 12, 1, 1, null, 0, 0, "&7永久保留");
        add("bow_2", ShopItem.Kind.BOW, ranged, 11, Material.BOW,
                "&f力量 I 弓", ShopItem.Currency.GOLD, 20, 1, 2, null, 0, 0, "&7永久保留");
        add("bow_3", ShopItem.Kind.BOW, ranged, 12, Material.BOW,
                "&f冲击 I 弓", ShopItem.Currency.EMERALD, 6, 1, 3, null, 0, 0, "&7把敌人射下虚空");
        add("arrow", ShopItem.Kind.ARROW, ranged, 19, Material.ARROW,
                "&f箭 &7×6", ShopItem.Currency.GOLD, 2, 6, 0, null, 0, 0, "&7消耗品");

        // ---------------- 药水 ----------------
        ShopCategory potions = ShopCategory.POTIONS;
        add("potion_speed", ShopItem.Kind.POTION, potions, 10, Material.POTION,
                "&f速度 II 药水 &7(45 秒)", ShopItem.Currency.EMERALD, 1, 1, 0, "speed", 45, 1,
                "&7喝完立即生效");
        add("potion_jump", ShopItem.Kind.POTION, potions, 11, Material.POTION,
                "&f跳跃 V 药水 &7(45 秒)", ShopItem.Currency.EMERALD, 1, 1, 0, "jump_boost", 45, 4,
                "&7喝完立即生效");
        add("potion_invisibility", ShopItem.Kind.POTION, potions, 12, Material.POTION,
                "&f隐身药水 &7(30 秒)", ShopItem.Currency.EMERALD, 2, 1, 0, "invisibility", 30, 0,
                "&7脱下护甲才会完全隐身");

        // ---------------- 功能 ----------------
        ShopCategory utility = ShopCategory.UTILITY;
        add("golden_apple", ShopItem.Kind.GOLDEN_APPLE, utility, 10, Material.GOLDEN_APPLE,
                "&f金苹果", ShopItem.Currency.GOLD, 3, 1, 0, null, 0, 0, "&7瞬间回血与吸收");
        add("fireball", ShopItem.Kind.FIREBALL, utility, 11, Material.FIRE_CHARGE,
                "&f火球", ShopItem.Currency.IRON, 40, 1, 0, null, 0, 0, "&7炸开敌人的防御工事");
        add("tnt", ShopItem.Kind.TNT, utility, 12, Material.TNT,
                "&fTNT", ShopItem.Currency.GOLD, 4, 1, 0, null, 0, 0, "&7放下后立即点燃");
        add("ender_pearl", ShopItem.Kind.ENDER_PEARL, utility, 13, Material.ENDER_PEARL,
                "&f末影珍珠", ShopItem.Currency.EMERALD, 4, 1, 0, null, 0, 0, "&7瞬移，可越墙偷袭");
        add("water_bucket", ShopItem.Kind.WATER_BUCKET, utility, 14, Material.WATER_BUCKET,
                "&f水桶", ShopItem.Currency.GOLD, 4, 1, 0, null, 0, 0, "&7降低摔落伤害");
        add("sponge", ShopItem.Kind.SPONGE, utility, 15, Material.SPONGE,
                "&f海绵", ShopItem.Currency.GOLD, 4, 1, 0, null, 0, 0, "&7吸干敌人的水");
        add("bridge_egg", ShopItem.Kind.BRIDGE_EGG, utility, 19, Material.EGG,
                "&f搭桥蛋", ShopItem.Currency.EMERALD, 4, 1, 0, null, 0, 0, "&7投掷后自动铺桥");
        add("bed_bug", ShopItem.Kind.BED_BUG, utility, 20, Material.SNOWBALL,
                "&f床虱", ShopItem.Currency.EMERALD, 2, 1, 0, null, 0, 0, "&7投掷后召唤蠹虫骚扰敌人");
        add("dream_defender", ShopItem.Kind.DREAM_DEFENDER, utility, 21, Material.IRON_GOLEM_SPAWN_EGG,
                "&f梦境守卫", ShopItem.Currency.IRON, 120, 1, 0, null, 0, 0, "&7召唤铁傀儡守家");
        add("popup_tower", ShopItem.Kind.POPUP_TOWER, utility, 22, Material.CHEST,
                "&f紧凑弹出塔", ShopItem.Currency.IRON, 24, 1, 0, null, 0, 0, "&7放置后自动升起防御塔");
        add("magic_milk", ShopItem.Kind.MAGIC_MILK, utility, 23, Material.MILK_BUCKET,
                "&f魔法牛奶", ShopItem.Currency.GOLD, 4, 1, 0, null, 0, 0, "&730 秒内不会触发敌方陷阱");
        add("rescue_platform", ShopItem.Kind.RESCUE_PLATFORM, utility, 24, Material.STONE,
                "&f自救平台", ShopItem.Currency.IRON, 8, 2, 0, null, 0, 0,
                "&7坠落（空中）时右键，在脚下生成石板救你一命", "&7每次使用消耗 1 个");
    }

    // ==========================================================
    //  shop.yml 覆盖
    // ==========================================================

    private void loadOverrides() {
        File file = new File(plugin.getDataFolder(), "shop.yml");
        if (!file.exists()) {
            writeDefaults(file);
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("items");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ShopItem item = catalog.get(id);
            ConfigurationSection section = root.getConfigurationSection(id);
            if (item == null || section == null) {
                continue;
            }
            item.setEnabled(section.getBoolean("enabled", true));
            int price = section.getInt("price", -1);
            if (price > 0) {
                item.setPrice(price);
            }
        }
    }

    private void writeDefaults(File file) {
        FileConfiguration config = new YamlConfiguration();
        config.set("# 说明", "修改 price 可调整价格，enabled: false 可下架商品。删除本文件会重新生成默认值。");
        for (ShopItem item : catalog.values()) {
            String path = "items." + item.id();
            config.set(path + ".enabled", true);
            config.set(path + ".slot", item.slot());
            config.set(path + ".price", item.price());
            config.set(path + ".currency", item.currency().name());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("无法生成 shop.yml: " + e.getMessage());
        }
    }

    // ==========================================================
    //  GUI
    // ==========================================================

    public void openMain(Player player) {
        GamePlayer gp = plugin.gameManager().findPlayer(player);
        TeamColor color = gp == null ? null : gp.teamColor();
        ShopGui gui = new ShopGui(ShopGui.Type.MAIN, null, player.getUniqueId(),
                Messages.get(xpMode(gp) ? "shop.title-main-xp" : "shop.title-main"), 6);

        for (ShopCategory category : ShopCategory.values()) {
            ItemStack icon = ItemBuilder.of(category.icon())
                    .name("&a" + category.display())
                    .lore("&7点击查看 &e" + byCategory(category).size() + " &7件商品")
                    .hideAttributes()
                    .build();
            gui.icon(category.slot(), icon);
        }
        gui.icon(SLOT_UPGRADES, upgradeEntryIcon(gp));
        gui.icon(MAIN_SLOT_INFO, resourceInfo(player, gp, color));
        gui.icon(SLOT_CLOSE, closeIcon());
        gui.icon(SLOT_CLOSE_ALT, closeIcon());
        player.openInventory(gui.getInventory());
    }

    public void openCategory(Player player, ShopCategory category) {
        GamePlayer gp = plugin.gameManager().findPlayer(player);
        TeamColor color = gp == null ? null : gp.teamColor();
        ShopGui gui = new ShopGui(ShopGui.Type.CATEGORY, category, player.getUniqueId(),
                Messages.get(xpMode(gp) ? "shop.title-category-xp" : "shop.title-category",
                        "%category%", category.display()), 6);

        for (ShopItem item : byCategory(category)) {
            ItemStack icon = buildShopIcon(item, color, gp);
            gui.icon(item.slot(), icon);
            gui.put(item.slot(), item);
        }
        gui.icon(SLOT_UPGRADES, upgradeEntryIcon(gp));
        gui.icon(SLOT_CLOSE, backIcon());
        gui.icon(SLOT_CLOSE_ALT, closeIcon());
        player.openInventory(gui.getInventory());
    }

    public void openUpgrades(Player player) {
        GamePlayer gp = plugin.gameManager().findPlayer(player);
        ShopGui gui = new ShopGui(ShopGui.Type.UPGRADES, null, player.getUniqueId(),
                Messages.get("shop.title-upgrades"), 6);

        int slot = 20;
        Team team = gp == null ? null : gp.team();
        for (UpgradeType type : UpgradeType.normal()) {
            gui.icon(slot, buildUpgradeIcon(type, team, gp));
            gui.putUpgrade(slot, type);
            slot++;
        }
        slot = 30;
        for (UpgradeType type : UpgradeType.traps()) {
            gui.icon(slot, buildTrapIcon(type, team, gp));
            gui.putUpgrade(slot, type);
            slot++;
        }
        if (team != null) {
            gui.icon(4, ItemBuilder.of(Material.DIAMOND)
                    .name("&b队伍升级")
                    .lore("&7当前陷阱队列：&e" + describeTraps(team),
                            xpMode(gp)
                                    ? "&7捡到的钻石会转化为经验，可直接用来升级"
                                    : "&7钻石可通过地图中央的生成器获取")
                    .hideAttributes().build());
        }
        gui.icon(SLOT_CLOSE, backIcon());
        gui.icon(SLOT_CLOSE_ALT, closeIcon());
        player.openInventory(gui.getInventory());
    }

    private String describeTraps(Team team) {
        if (team.traps().isEmpty()) {
            return "空";
        }
        StringBuilder sb = new StringBuilder();
        for (UpgradeType trap : team.traps()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(trap.display());
        }
        return sb.toString();
    }

    private ItemStack buildShopIcon(ShopItem item, TeamColor color, GamePlayer gp) {
        ItemBuilder builder = ItemBuilder.of(item.buildIcon(color));
        builder.addLore("");
        builder.addLore("&7价格：" + item.priceText(xpMode(gp)));
        if (item.isPermanent()) {
            builder.addLore("&7死亡后保留");
        }
        builder.addLore("&e点击购买");
        if (Settings.bulkBuyAmount > 1) {
            builder.addLore("&7潜行点击可一次购买 &e" + Settings.bulkBuyAmount + " &7次");
        }
        if (gp != null && item.kind() == ShopItem.Kind.SWORD) {
            builder.addLore("&7当前等级：&e" + gp.swordTier());
        }
        return builder.build();
    }

    private ItemStack upgradeEntryIcon(GamePlayer gp) {
        ItemBuilder builder = ItemBuilder.of(Material.DIAMOND)
                .name("&b队伍升级")
                .hideAttributes();
        if (gp != null && gp.team() != null) {
            Team team = gp.team();
            builder.addLore(xpMode(gp)
                    ? "&7用经验购买全队增益与陷阱"
                    : "&7用钻石购买全队增益与陷阱");
            builder.addLore("");
            for (UpgradeType type : UpgradeType.normal()) {
                builder.addLore("&7" + type.display() + "：&e" + team.upgradeLevel(type) + "&7/" + type.maxLevel());
            }
            builder.addLore("&7陷阱队列：&e" + describeTraps(team));
        } else {
            builder.addLore("&c需要先加入对局");
        }
        builder.addLore("");
        builder.addLore("&e点击打开");
        return builder.build();
    }

    private ItemStack resourceInfo(Player player, GamePlayer gp, TeamColor color) {
        boolean xp = xpMode(gp);
        ItemBuilder builder = ItemBuilder.of(xp ? Material.EXPERIENCE_BOTTLE : Material.CHEST)
                .name("&a我的资源")
                .hideAttributes();
        if (xp) {
            builder.addLore("&a经验 &7× " + (gp == null ? 0 : gp.experience()));
            builder.addLore("");
            builder.addLore("&7捡到的 &f铁 &7/ &6金 &7/ &b钻石 &7/ &a绿宝石");
            builder.addLore("&7会自动转化为经验值。");
            builder.addLore("");
            builder.addLore("&7换算：&f铁 " + Settings.xpValueIron
                    + " &8| &6金 " + Settings.xpValueGold
                    + " &8| &b钻石 " + Settings.xpValueDiamond
                    + " &8| &a绿宝石 " + Settings.xpValueEmerald);
        } else {
            for (ShopItem.Currency currency : ShopItem.Currency.values()) {
                builder.addLore(currency.color() + currency.display() + " &7× "
                        + countCurrency(player, currency));
            }
        }
        builder.addLore("");
        builder.addLore("&7队伍：" + (color == null ? "&7无" : color.code() + color.display()));
        return builder.build();
    }

    private ItemStack buildUpgradeIcon(UpgradeType type, Team team, GamePlayer gp) {
        int level = team == null ? 0 : team.upgradeLevel(type);
        boolean maxed = team != null && team.isMaxed(type);
        ItemBuilder builder = ItemBuilder.of(type.icon())
                .name("&b" + type.display())
                .hideAttributes();
        builder.addLore("&7" + type.description());
        builder.addLore("");
        builder.addLore("&7当前等级：&e" + level + "&7/&e" + type.maxLevel());
        if (maxed) {
            builder.addLore(Messages.get("shop.lore.maxed"));
        } else {
            builder.addLore("&7下一等级：&e" + (level + 1));
            builder.addLore("&7价格：" + upgradePriceText(gp, type.price(level)));
            builder.addLore("");
            builder.addLore("&e点击购买");
        }
        return builder.build();
    }

    private ItemStack buildTrapIcon(UpgradeType type, Team team, GamePlayer gp) {
        boolean maxed = team != null && team.isMaxed(type);
        ItemBuilder builder = ItemBuilder.of(type.icon())
                .name("&b" + type.display())
                .hideAttributes();
        builder.addLore("&7" + type.description());
        builder.addLore("");
        builder.addLore("&7价格：" + upgradePriceText(gp, type.price(0)));
        builder.addLore("&7最多同时排队 &e3 &7个陷阱");
        if (maxed) {
            builder.addLore(Messages.get("shop.lore.maxed"));
        } else {
            builder.addLore("");
            builder.addLore("&e点击购买");
        }
        return builder.build();
    }

    private ItemStack closeIcon() {
        return ItemBuilder.of(Material.BARRIER)
                .name("&c关闭")
                .hideAttributes().build();
    }

    private ItemStack backIcon() {
        return ItemBuilder.of(Material.ARROW)
                .name("&a返回商店")
                .hideAttributes().build();
    }

    // ==========================================================
    //  购买
    // ==========================================================

    public void purchase(Player player, ShopItem item, boolean bulk) {
        GamePlayer gp = plugin.gameManager().findPlayer(player);
        if (gp == null) {
            return;
        }
        if (item.kind() == ShopItem.Kind.UPGRADES) {
            openUpgrades(player);
            return;
        }
        if (!canUpgrade(gp, item)) {
            Chat.send(player, "shop.insufficient", "%cost%", item.priceText(xpMode(gp)));
            return;
        }
        int times = bulk && !item.isPermanent() ? Settings.bulkBuyAmount : 1;
        int done = 0;
        for (int i = 0; i < times; i++) {
            if (!hasCurrency(player, item)) {
                break;
            }
            takeCurrency(player, item);
            if (!give(player, gp, item)) {
                if (Settings.refundOnFail) {
                    refund(player, item);
                }
                break;
            }
            done++;
        }
        if (done == 0) {
            Chat.send(player, "shop.insufficient", "%cost%", item.priceText(xpMode(gp)));
        } else {
            Chat.send(player, "shop.purchased", "%item%", Chat.strip(item.name()) + " ×" + done);
            if (item.kind() == ShopItem.Kind.ARMOR) {
                gp.applyArmor();
            }
        }
    }

    /** 是否会购买到比当前更低等级的永久装备。 */
    private boolean canUpgrade(GamePlayer gp, ShopItem item) {
        return switch (item.kind()) {
            case SWORD -> gp.swordTier() < item.tier();
            case PICKAXE -> gp.pickTier() < item.tier();
            case AXE -> gp.axeTier() < item.tier();
            case ARMOR -> gp.armorTier() < item.tier();
            case BOW -> gp.bowTier() < item.tier();
            case SHEARS -> !gp.hasShears();
            default -> true;
        };
    }

    private boolean give(Player player, GamePlayer gp, ShopItem item) {
        switch (item.kind()) {
            case BLOCK -> player.getInventory().addItem(new ItemStack(item.icon(), item.amount()));
            case TEAM_WOOL -> player.getInventory().addItem(new ItemStack(
                    gp.teamColor() == null ? Material.WHITE_WOOL : gp.teamColor().wool(), item.amount()));
            case TEAM_TERRACOTTA -> player.getInventory().addItem(new ItemStack(
                    gp.teamColor() == null ? Material.TERRACOTTA : gp.teamColor().terracotta(), item.amount()));
            case TEAM_GLASS -> player.getInventory().addItem(new ItemStack(
                    gp.teamColor() == null ? Material.GLASS : gp.teamColor().stainedGlass(), item.amount()));
            case LADDER -> player.getInventory().addItem(Items.ladder(item.amount()));
            case SWORD -> {
                gp.setSwordTier(Math.max(gp.swordTier(), item.tier()));
                gp.applySword();
            }
            case PICKAXE -> {
                gp.setPickTier(Math.max(gp.pickTier(), item.tier()));
                player.getInventory().setItem(1, gp.buildPickaxe());
            }
            case AXE -> {
                gp.setAxeTier(Math.max(gp.axeTier(), item.tier()));
                player.getInventory().setItem(2, gp.buildAxe());
            }
            case SHEARS -> {
                gp.setShears(true);
                player.getInventory().setItem(3, Items.shears());
            }
            case ARMOR -> gp.setArmorTier(Math.max(gp.armorTier(), item.tier()));
            case BOW -> {
                gp.setBowTier(Math.max(gp.bowTier(), item.tier()));
                player.getInventory().setItem(4, gp.buildBow());
            }
            case ARROW -> player.getInventory().addItem(Items.arrows(item.amount()));
            case KNOCKBACK_STICK -> player.getInventory().addItem(Items.knockbackStick());
            case GOLDEN_APPLE -> player.getInventory().addItem(Items.goldenApple());
            case POTION -> player.getInventory().addItem(Items.potion(item.potionEffect(),
                    item.potionAmplifier(), item.potionDuration(),
                    Chat.strip(item.name()), ""));
            case FIREBALL -> player.getInventory().addItem(Items.fireball());
            case TNT -> player.getInventory().addItem(Items.tnt());
            case ENDER_PEARL -> player.getInventory().addItem(Items.enderPearl());
            case WATER_BUCKET -> player.getInventory().addItem(Items.waterBucket());
            case SPONGE -> player.getInventory().addItem(Items.sponge());
            case BRIDGE_EGG -> player.getInventory().addItem(Items.bridgeEgg());
            case BED_BUG -> player.getInventory().addItem(Items.bedBug());
            case DREAM_DEFENDER -> player.getInventory().addItem(Items.dreamDefender(gp.teamColor()));
            case POPUP_TOWER -> player.getInventory().addItem(Items.popupTower());
            case MAGIC_MILK -> player.getInventory().addItem(Items.magicMilk());
            case RESCUE_PLATFORM -> {
                ItemStack platform = Items.rescuePlatform();
                platform.setAmount(Math.max(1, item.amount()));
                player.getInventory().addItem(platform);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** 购买队伍升级。 */
    public void purchaseUpgrade(Player player, UpgradeType type) {
        GamePlayer gp = plugin.gameManager().findPlayer(player);
        if (gp == null) {
            return;
        }
        Team team = gp.team();
        if (team == null) {
            return;
        }
        if (type.isTrap()) {
            if (team.isMaxed(type)) {
                Chat.send(player, "shop.upgrade-maxed");
                return;
            }
            if (team.trapCount() >= 3) {
                Chat.send(player, "shop.trap-queue-full");
                return;
            }
            int price = type.price(0);
            if (!hasCurrency(player, ShopItem.Currency.DIAMOND, price)) {
                Chat.send(player, "shop.insufficient", "%cost%", upgradePriceText(gp, price));
                return;
            }
            takeCurrency(player, ShopItem.Currency.DIAMOND, price);
            team.addTrap(type);
            team.setUpgradeLevel(type, team.upgradeLevel(type) + 1);
            Chat.send(player, "shop.trap-queued",
                    "%trap%", type.display(), "%queue%", describeTraps(team));
            for (Player member : team.onlinePlayers()) {
                member.sendMessage(Chat.color(Messages.prefix() + Messages.get("shop.trap-queued",
                        "%trap%", type.display(), "%queue%", describeTraps(team))));
            }
            plugin.scoreboardManager().update(gp.arena());
            openUpgrades(player);
            return;
        }
        if (team.isMaxed(type)) {
            Chat.send(player, "shop.upgrade-maxed");
            return;
        }
        int price = team.nextPrice(type);
        if (!hasCurrency(player, ShopItem.Currency.DIAMOND, price)) {
            Chat.send(player, "shop.insufficient", "%cost%", upgradePriceText(gp, price));
            return;
        }
        takeCurrency(player, ShopItem.Currency.DIAMOND, price);
        int newLevel = team.upgradeLevel(type) + 1;
        team.setUpgradeLevel(type, newLevel);
        applyTeamUpgrade(team, type);
        for (Player member : team.onlinePlayers()) {
            member.sendMessage(Chat.color(Messages.prefix() + Messages.get("shop.upgrade-purchased",
                    "%upgrade%", type.display(), "%level%", String.valueOf(newLevel))));
        }
        plugin.scoreboardManager().update(gp.arena());
        openUpgrades(player);
    }

    private void applyTeamUpgrade(Team team, UpgradeType type) {
        for (Player member : team.onlinePlayers()) {
            GamePlayer gp = team.arena().player(member);
            if (gp == null) {
                continue;
            }
            switch (type) {
                case SHARPNESS -> gp.applySword();
                case PROTECTION -> gp.applyArmor();
                case HASTE -> gp.applyTeamEffects(member);
                default -> {
                }
            }
        }
    }

    // ==========================================================
    //  货币（普通模式 = 背包里的实物资源；经验模式 = 经验值）
    // ==========================================================

    /** 本局是否以经验作为货币。 */
    public boolean xpMode(GamePlayer gp) {
        return gp != null && gp.experienceMode();
    }

    /** 某件商品在当前对局中的实际花费。 */
    public int cost(GamePlayer gp, ShopItem item) {
        return xpMode(gp) ? item.xpPrice() : item.effectivePrice();
    }

    /** 队伍升级的实际花费（经验模式下按钻石价 × 钻石经验价值折算）。 */
    public int upgradeCost(GamePlayer gp, int diamondPrice) {
        return xpMode(gp) ? diamondPrice * ShopItem.Currency.DIAMOND.xpValue() : diamondPrice;
    }

    private String upgradePriceText(GamePlayer gp, int diamondPrice) {
        return xpMode(gp)
                ? "&a经验 &7×" + upgradeCost(gp, diamondPrice)
                : "&b钻石 &7×" + diamondPrice;
    }

    public boolean hasCurrency(Player player, ShopItem item) {
        GamePlayer gp = plugin.gameManager().findPlayer(player);
        int price = cost(gp, item);
        if (xpMode(gp)) {
            return gp.experience() >= price;
        }
        return countCurrency(player, item.currency()) >= price;
    }

    public boolean hasCurrency(Player player, ShopItem.Currency currency, int amount) {
        GamePlayer gp = plugin.gameManager().findPlayer(player);
        if (xpMode(gp)) {
            return gp.experience() >= amount * currency.xpValue();
        }
        return countCurrency(player, currency) >= amount;
    }

    public int countCurrency(Player player, ShopItem.Currency currency) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == currency.material()) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    public void takeCurrency(Player player, ShopItem item) {
        GamePlayer gp = plugin.gameManager().findPlayer(player);
        if (xpMode(gp)) {
            gp.takeExperience(item.xpPrice());
            return;
        }
        takeCurrency(player, item.currency(), item.effectivePrice());
    }

    public void takeCurrency(Player player, ShopItem.Currency currency, int amount) {
        GamePlayer gp = plugin.gameManager().findPlayer(player);
        if (xpMode(gp)) {
            gp.takeExperience(amount * currency.xpValue());
            return;
        }
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != currency.material()) {
                continue;
            }
            int take = Math.min(remaining, stack.getAmount());
            remaining -= take;
            if (take >= stack.getAmount()) {
                player.getInventory().setItem(i, null);
            } else {
                stack.setAmount(stack.getAmount() - take);
            }
        }
    }

    public void refund(Player player, ShopItem item) {
        GamePlayer gp = plugin.gameManager().findPlayer(player);
        if (xpMode(gp)) {
            gp.addExperience(item.xpPrice());
            return;
        }
        player.getInventory().addItem(new ItemStack(item.currency().material(), item.effectivePrice()));
    }
}
