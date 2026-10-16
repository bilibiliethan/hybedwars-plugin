package com.hybedwars.generator;

import com.hybedwars.config.Settings;
import com.hybedwars.game.Team;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/** 资源生成器。队伍生成器产出铁/金，公共生成器产出钻石/绿宝石。 */
public class Generator {

    public enum Type {
        IRON("铁", Material.IRON_INGOT),
        GOLD("金", Material.GOLD_INGOT),
        DIAMOND("钻石", Material.DIAMOND),
        EMERALD("绿宝石", Material.EMERALD);

        private final String display;
        private final Material material;

        Type(String display, Material material) {
            this.display = display;
            this.material = material;
        }

        public String display() {
            return display;
        }

        public Material material() {
            return material;
        }

        public boolean isGlobal() {
            return this == DIAMOND || this == EMERALD;
        }
    }

    private final Type type;
    private final Location location;
    private final Team owner;

    private int tier = 1;
    private double counter;
    /** 自定义刷新间隔（秒/个）。0 表示使用默认速率表。 */
    private double customRate = 0;

    public Generator(Type type, Location location, Team owner) {
        this.type = type;
        this.location = location;
        this.owner = owner;
        this.counter = 0;
    }

    public Type type() {
        return type;
    }

    public Location location() {
        return location;
    }

    public Team owner() {
        return owner;
    }

    public int tier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = Math.max(1, tier);
    }

    /** 设置该生成器的自定义刷新间隔（秒/个）。传 0 恢复默认速率。 */
    public void setCustomRate(double seconds) {
        this.customRate = Math.max(0, seconds);
    }

    public void reset() {
        counter = 0;
    }

    /** 每 tick 调用一次。 */
    public void tick() {
        if (location == null || location.getWorld() == null) {
            return;
        }
        double seconds = rate();
        double perTick = 1.0 / Math.max(0.05, seconds);
        counter += perTick;
        if (counter >= 1.0) {
            counter -= 1.0;
            spawnItem();
        }
    }

    private double rate() {
        if (customRate > 0) {
            return customRate;
        }
        if (owner != null) {
            int forge = owner.upgradeLevel(com.hybedwars.game.UpgradeType.FORGE);
            return switch (type) {
                case IRON -> Settings.rate(Settings.ironRate, forge + 1);
                case GOLD -> Settings.rate(Settings.goldRate, forge + 1);
                default -> 30.0;
            };
        }
        return switch (type) {
            case DIAMOND -> Settings.rate(Settings.diamondRate, tier);
            case EMERALD -> Settings.rate(Settings.emeraldRate, tier);
            default -> 30.0;
        };
    }

    private void spawnItem() {
        World world = location.getWorld();
        if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return;
        }
        Material material = type.material();
        if (countNearby(world, material) >= Settings.generatorItemLimit) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location drop = location.clone().add(
                random.nextDouble(-0.4, 0.4), 0.2, random.nextDouble(-0.4, 0.4));
        Item item = world.dropItem(drop, new ItemStack(material));
        item.setVelocity(new Vector(0, 0, 0));
        item.setPickupDelay(6);
        item.setUnlimitedLifetime(true);
        item.setCanMobPickup(false);
    }

    private int countNearby(World world, Material material) {
        int count = 0;
        for (Entity entity : world.getNearbyEntities(location, 3.5, 4, 3.5)) {
            if (entity instanceof Item item && item.getItemStack().getType() == material) {
                count += item.getItemStack().getAmount();
                if (count >= Settings.generatorItemLimit) {
                    return count;
                }
            }
        }
        return count;
    }
}
