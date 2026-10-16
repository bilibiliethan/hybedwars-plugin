package com.hybedwars.util;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/** 世界目录复制 / 删除，用于「每局还原地图」。 */
public final class WorldUtil {

    private WorldUtil() {
    }

    /** 递归复制目录。 */
    public static boolean copyDirectory(Path source, Path target) {
        if (!Files.isDirectory(source)) {
            return false;
        }
        try {
            Files.createDirectories(target);
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(target.resolve(source.relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // 跳过会话锁，避免复制出损坏的世界
                    String name = file.getFileName().toString();
                    if (name.equals("session.lock")) {
                        return FileVisitResult.CONTINUE;
                    }
                    Files.copy(file, target.resolve(source.relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (IOException e) {
            Bukkit.getLogger().warning("[HyBedWars] 复制世界目录失败: " + e.getMessage());
            return false;
        }
    }

    /** 递归删除目录。 */
    public static boolean deleteDirectory(Path path) {
        if (!Files.exists(path)) {
            return true;
        }
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (IOException e) {
            Bukkit.getLogger().warning("[HyBedWars] 删除世界目录失败: " + e.getMessage());
            return false;
        }
    }

    public static File worldFolder(String worldName) {
        File container = Bukkit.getWorldContainer();
        return new File(container, worldName);
    }

    /** 卸载世界（不保存）。 */
    public static void unload(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            world.setAutoSave(false);
            Bukkit.unloadWorld(world, false);
        }
    }
}
