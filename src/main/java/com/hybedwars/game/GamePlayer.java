package com.hybedwars.game;

import com.hybedwars.util.Compat;
import com.hybedwars.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;

import java.util.UUID;

/** 对局中的单个玩家数据。 */
public class GamePlayer {

    private final UUID uuid;
    private final String name;
    private final Arena arena;

    private TeamColor teamColor;
    private boolean alive = true;
    private boolean spectator = true;
    private boolean respawning = false;

    private int kills;
    private int finalKills;
    private int deaths;
    private int bedsBroken;
    private int experience;
    private int experienceEarned;

    private int armorTier;   // 0 = 皮革, 1 = 锁链, 2 = 铁, 3 = 钻石
    private int swordTier;   // 0 = 木剑, 1 = 石剑, 2 = 铁剑, 3 = 钻石剑
    private int pickTier;    // 0 = 无, 1 = 木, 2 = 铁, 3 = 钻石, 4 = 钻石+效率I
    private int axeTier;     // 0 = 无, 1 = 木, 2 = 石, 3 = 铁, 4 = 钻石
    private boolean shears;
    private int bowTier;     // 0 = 无, 1 = 弓, 2 = 力量I, 3 = 冲击I

    private UUID lastDamager;
    private long lastDamageTime;
    private boolean voidDeath;

    private int respawnTaskId = -1;
    private int killStreak;
    private long lastPearlUse;
    private long lastFireballUse;
    private long lastRescueUse;
    private boolean magicMilkActive;
    private long magicMilkUntil;

    public GamePlayer(Player player, Arena arena) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.arena = arena;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public Arena arena() {
        return arena;
    }

    public Player bukkit() {
        return Bukkit.getPlayer(uuid);
    }

    public TeamColor teamColor() {
        return teamColor;
    }

    public Team team() {
        return teamColor == null ? null : arena.team(teamColor);
    }

    public void setTeamColor(TeamColor teamColor) {
        this.teamColor = teamColor;
    }

    // ---------- 状态 ----------

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isSpectator() {
        return spectator;
    }

    public void setSpectator(boolean spectator) {
        this.spectator = spectator;
    }

    public boolean isRespawning() {
        return respawning;
    }

    public void setRespawning(boolean respawning) {
        this.respawning = respawning;
    }

    // ---------- 统计 ----------

    public int kills() {
        return kills;
    }

    public int finalKills() {
        return finalKills;
    }

    public int deaths() {
        return deaths;
    }

    public int bedsBroken() {
        return bedsBroken;
    }

    public void addKill() {
        kills++;
        killStreak++;
    }

    public void addFinalKill() {
        finalKills++;
        killStreak++;
    }

    public void addDeath() {
        deaths++;
        killStreak = 0;
    }

    public void addBedBroken() {
        bedsBroken++;
    }

    public int killStreak() {
        return killStreak;
    }

    // ---------- 经验（经验模式下的货币） ----------

    public int experience() {
        return experience;
    }

    /** 本局累计获得过的经验总量（结算展示用）。 */
    public int experienceEarned() {
        return experienceEarned;
    }

    public void addExperience(int amount) {
        if (amount > 0) {
            this.experience += amount;
            this.experienceEarned += amount;
        }
    }

    public boolean takeExperience(int amount) {
        if (amount <= 0 || experience < amount) {
            return false;
        }
        experience -= amount;
        return true;
    }

    public void setExperience(int experience) {
        this.experience = Math.max(0, experience);
    }

    /** 本局是否为经验模式（资源转化为经验、商店用经验结算）。 */
    public boolean experienceMode() {
        return arena != null && arena.experienceMode();
    }

    // ---------- 装备等级 ----------

    public int armorTier() {
        return armorTier;
    }

    public void setArmorTier(int armorTier) {
        this.armorTier = Math.max(0, Math.min(3, armorTier));
    }

    public int swordTier() {
        return swordTier;
    }

    public void setSwordTier(int swordTier) {
        this.swordTier = Math.max(0, Math.min(3, swordTier));
    }

    public int pickTier() {
        return pickTier;
    }

    public void setPickTier(int pickTier) {
        this.pickTier = Math.max(0, Math.min(4, pickTier));
    }

    public int axeTier() {
        return axeTier;
    }

    public void setAxeTier(int axeTier) {
        this.axeTier = Math.max(0, Math.min(4, axeTier));
    }

    public boolean hasShears() {
        return shears;
    }

    public void setShears(boolean shears) {
        this.shears = shears;
    }

    public int bowTier() {
        return bowTier;
    }

    public void setBowTier(int bowTier) {
        this.bowTier = Math.max(0, Math.min(3, bowTier));
    }

    // ---------- 伤害记录 ----------

    public UUID lastDamager() {
        return lastDamager;
    }

    public void setLastDamager(UUID lastDamager) {
        this.lastDamager = lastDamager;
        this.lastDamageTime = System.currentTimeMillis();
    }

    public boolean wasRecentlyDamaged(int seconds) {
        return System.currentTimeMillis() - lastDamageTime <= seconds * 1000L;
    }

    public void clearDamager() {
        this.lastDamager = null;
    }

    public boolean isVoidDeath() {
        return voidDeath;
    }

    public void setVoidDeath(boolean voidDeath) {
        this.voidDeath = voidDeath;
    }

    // ---------- 冷却 ----------

    public boolean canUsePearl() {
        return System.currentTimeMillis() - lastPearlUse >= com.hybedwars.config.Settings.pearlCooldown * 1000L;
    }

    public void markPearlUsed() {
        this.lastPearlUse = System.currentTimeMillis();
    }

    public boolean canUseFireball() {
        return System.currentTimeMillis() - lastFireballUse >= 900L;
    }

    public void markFireballUsed() {
        this.lastFireballUse = System.currentTimeMillis();
    }

    public boolean canUseRescue() {
        return System.currentTimeMillis() - lastRescueUse >= 1500L;
    }

    public void markRescueUsed() {
        this.lastRescueUse = System.currentTimeMillis();
    }

    public boolean hasMagicMilk() {
        return magicMilkActive && System.currentTimeMillis() < magicMilkUntil;
    }

    public void giveMagicMilk(int seconds) {
        this.magicMilkActive = true;
        this.magicMilkUntil = System.currentTimeMillis() + seconds * 1000L;
    }

    // ---------- 重生任务 ----------

    public int respawnTaskId() {
        return respawnTaskId;
    }

    public void setRespawnTaskId(int respawnTaskId) {
        this.respawnTaskId = respawnTaskId;
    }

    public void cancelRespawnTask() {
        if (respawnTaskId != -1) {
            Bukkit.getScheduler().cancelTask(respawnTaskId);
            respawnTaskId = -1;
        }
    }

    // ---------- 出生装备 ----------

    /** 发放出生套装（含队伍升级带来的附魔）。 */
    public void giveSpawnKit() {
        Player player = bukkit();
        if (player == null) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(null);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setLevel(0);
        player.setExp(0f);

        applyArmor();
        applySword();

        // 镐 / 斧 / 剪刀
        if (pickTier > 0) {
            inv.setItem(1, buildPickaxe());
        }
        if (axeTier > 0) {
            inv.setItem(2, buildAxe());
        }
        if (shears) {
            inv.setItem(3, ItemBuilder.of(Material.SHEARS).unbreakable(true).build());
        }
        if (bowTier > 0) {
            inv.setItem(4, buildBow());
        }

        player.setGameMode(GameMode.SURVIVAL);
        applyTeamEffects(player);
        applyLegacyPvp();
    }

    /** 启用低版本（1.8 风格）PVP：把攻击速度拉到极高，使攻击冷却几乎为 0。 */
    public void applyLegacyPvp() {
        Player player = bukkit();
        if (player == null) {
            return;
        }
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attr == null) {
            return;
        }
        // 移除旧的同 UUID 修饰符，避免重复叠加
        attr.getModifiers().stream()
                .filter(m -> m.getUniqueId().equals(LEGACY_PVP_UUID))
                .forEach(attr::removeModifier);
        attr.addModifier(new AttributeModifier(LEGACY_PVP_UUID, "hybedwars-legacy-attack-speed",
                1024.0, AttributeModifier.Operation.ADD_NUMBER));
    }

    /** 移除低版本 PVP 的 attack_speed 加成，恢复正常攻击冷却。 */
    public void removeLegacyPvp() {
        Player player = bukkit();
        if (player == null) {
            return;
        }
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attr == null) {
            return;
        }
        attr.getModifiers().stream()
                .filter(m -> m.getUniqueId().equals(LEGACY_PVP_UUID))
                .forEach(attr::removeModifier);
    }

    private static final UUID LEGACY_PVP_UUID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    /** 即时穿上当前等级的护甲。 */
    public void applyArmor() {
        Player player = bukkit();
        if (player == null) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        Team team = team();
        int protection = team == null ? 0 : team.upgradeLevel(UpgradeType.PROTECTION);
        org.bukkit.Color color = teamColor == null ? org.bukkit.Color.WHITE : teamColor.bukkitColor();

        Material legs = switch (armorTier) {
            case 3 -> Material.DIAMOND_LEGGINGS;
            case 2 -> Material.IRON_LEGGINGS;
            case 1 -> Material.CHAINMAIL_LEGGINGS;
            default -> null;
        };
        Material boots = switch (armorTier) {
            case 3 -> Material.DIAMOND_BOOTS;
            case 2 -> Material.IRON_BOOTS;
            case 1 -> Material.CHAINMAIL_BOOTS;
            default -> null;
        };

        ItemStack helmet = ItemBuilder.of(Material.LEATHER_HELMET)
                .leatherColor(color).unbreakable(true).hideAttributes().build();
        ItemStack chestplate = ItemBuilder.of(Material.LEATHER_CHESTPLATE)
                .leatherColor(color).unbreakable(true).hideAttributes().build();
        ItemStack legStack = legs == null
                ? ItemBuilder.of(Material.LEATHER_LEGGINGS).leatherColor(color)
                .unbreakable(true).hideAttributes().build()
                : ItemBuilder.of(legs).unbreakable(true).hideAttributes().build();
        ItemStack bootStack = boots == null
                ? ItemBuilder.of(Material.LEATHER_BOOTS).leatherColor(color)
                .unbreakable(true).hideAttributes().build()
                : ItemBuilder.of(boots).unbreakable(true).hideAttributes().build();

        if (protection > 0) {
            applyProtection(helmet, protection);
            applyProtection(chestplate, protection);
            applyProtection(legStack, protection);
            applyProtection(bootStack, protection);
        }
        inv.setHelmet(helmet);
        inv.setChestplate(chestplate);
        inv.setLeggings(legStack);
        inv.setBoots(bootStack);
    }

    /** 即时换上当前等级的剑。 */
    public void applySword() {
        Player player = bukkit();
        if (player == null) {
            return;
        }
        player.getInventory().setItem(0, buildSword());
    }

    public ItemStack buildSword() {
        Material material = switch (swordTier) {
            case 3 -> Material.DIAMOND_SWORD;
            case 2 -> Material.IRON_SWORD;
            case 1 -> Material.STONE_SWORD;
            default -> Material.WOODEN_SWORD;
        };
        Team team = team();
        int sharpness = team == null ? 0 : team.upgradeLevel(UpgradeType.SHARPNESS);
        ItemBuilder builder = ItemBuilder.of(material).unbreakable(true).hideAttributes();
        if (sharpness > 0) {
            builder.enchant(Compat.ench("sharpness"), sharpness);
        }
        return builder.build();
    }

    public ItemStack buildPickaxe() {
        Material material = switch (pickTier) {
            case 4, 3 -> Material.DIAMOND_PICKAXE;
            case 2 -> Material.IRON_PICKAXE;
            default -> Material.WOODEN_PICKAXE;
        };
        ItemBuilder builder = ItemBuilder.of(material).unbreakable(true).hideAttributes();
        if (pickTier >= 4) {
            builder.enchant(Compat.ench("efficiency"), 1);
        }
        return builder.build();
    }

    public ItemStack buildAxe() {
        Material material = switch (axeTier) {
            case 4 -> Material.DIAMOND_AXE;
            case 3 -> Material.IRON_AXE;
            case 2 -> Material.STONE_AXE;
            default -> Material.WOODEN_AXE;
        };
        return ItemBuilder.of(material).unbreakable(true).hideAttributes().build();
    }

    public ItemStack buildBow() {
        ItemBuilder builder = ItemBuilder.of(Material.BOW).unbreakable(true);
        if (bowTier >= 2) {
            builder.enchant(Compat.ench("power"), 1);
        }
        if (bowTier >= 3) {
            builder.enchant(Compat.ench("punch"), 1);
        }
        return builder.build();
    }

    private void applyProtection(ItemStack item, int level) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.addEnchant(Compat.ench("protection"), Math.min(4, level), true);
        org.bukkit.inventory.ItemFlag flag = org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES;
        meta.addItemFlags(flag);
        item.setItemMeta(meta);
    }

    /** 持续性的队伍升级效果（急迫）。 */
    public void applyTeamEffects(Player player) {
        Team team = team();
        if (team == null) {
            return;
        }
        int haste = team.upgradeLevel(UpgradeType.HASTE);
        if (haste > 0) {
            org.bukkit.potion.PotionEffectType type = Compat.effect("haste");
            player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, haste - 1, false, false, true));
        } else {
            org.bukkit.potion.PotionEffectType type = Compat.effectOrNull("haste");
            if (type != null) {
                player.removePotionEffect(type);
            }
        }
    }

    /** 统计当前资源（铁/金/钻/绿）。 */
    public int countResource(Material material) {
        Player player = bukkit();
        if (player == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    public void reset() {
        cancelRespawnTask();
        kills = 0;
        finalKills = 0;
        deaths = 0;
        bedsBroken = 0;
        experience = 0;
        experienceEarned = 0;
        armorTier = 0;
        swordTier = 0;
        pickTier = 0;
        axeTier = 0;
        shears = false;
        bowTier = 0;
        alive = true;
        spectator = true;
        respawning = false;
        killStreak = 0;
        voidDeath = false;
        lastDamager = null;
        magicMilkActive = false;
        lastRescueUse = 0;
    }
}
