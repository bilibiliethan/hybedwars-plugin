package com.hybedwars.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/** 坐标序列化 / 方块坐标压缩。 */
public final class Loc {

    private Loc() {
    }

    /** 序列化为 "world,x,y,z,yaw,pitch"。 */
    public static String serialize(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName()
                + "," + round(location.getX())
                + "," + round(location.getY())
                + "," + round(location.getZ())
                + "," + round(location.getYaw())
                + "," + round(location.getPitch());
    }

    public static Location deserialize(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String[] parts = text.split(",");
        if (parts.length < 4) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 只保留方块坐标。 */
    public static Location toBlock(Location location) {
        if (location == null) {
            return null;
        }
        return new Location(location.getWorld(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static String shortString(Location location) {
        if (location == null) {
            return "&c未设置";
        }
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private static String round(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    /** 同一世界内的方块坐标 → 唯一 long（x/z 支持 ±3.3e7，y 支持 0~4095）。 */
    public static long blockKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    public static long blockKey(Location location) {
        return blockKey(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
