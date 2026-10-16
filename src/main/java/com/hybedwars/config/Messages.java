package com.hybedwars.config;

import com.hybedwars.HyBedWars;
import com.hybedwars.util.Chat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** messages.yml 的访问入口。 */
public final class Messages {

    private static final Map<String, Object> VALUES = new HashMap<>();
    private static String prefix = "&8[&c&lBW&8] &r";

    private Messages() {
    }

    public static String prefix() {
        return Chat.color(prefix);
    }

    public static void setPrefix(String value) {
        prefix = value == null ? "" : value;
    }

    public static void load() {
        VALUES.clear();
        HyBedWars plugin = HyBedWars.get();
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // 用 jar 内置文件兜底，避免升级后缺失新增键
        YamlConfiguration defaults = null;
        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in != null) {
                defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                config.setDefaults(defaults);
            }
        } catch (Exception ignored) {
            // 忽略：使用磁盘内容
        }

        // 先收集磁盘上的键（用户自定义优先）
        for (String key : config.getKeys(true)) {
            Object value = config.get(key);
            if (value != null && !(value instanceof org.bukkit.configuration.ConfigurationSection)) {
                VALUES.put(key, value);
            }
        }
        // 再用 jar 内置默认值补齐磁盘上缺失的键（老版本配置文件升级后可立即用上新文案）
        if (defaults != null) {
            for (String key : defaults.getKeys(true)) {
                if (VALUES.containsKey(key)) {
                    continue;
                }
                Object value = defaults.get(key);
                if (value != null && !(value instanceof org.bukkit.configuration.ConfigurationSection)) {
                    VALUES.put(key, value);
                }
            }
        }
    }

    /** 取消息并替换 %key% 占位符，参数成对传入："%time%", "10"。 */
    public static String get(String key, Object... replacements) {
        Object value = VALUES.get(key);
        String text;
        if (value == null) {
            text = "&c[missing:" + key + "]";
        } else if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append('\n');
                }
                sb.append(list.get(i));
            }
            text = sb.toString();
        } else {
            text = String.valueOf(value);
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            if (replacements[i] == null) {
                continue;
            }
            text = text.replace(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
        }
        return text;
    }

    /** 取列表型消息。 */
    public static List<String> getList(String key) {
        Object value = VALUES.get(key);
        List<String> out = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object o : list) {
                out.add(String.valueOf(o));
            }
        } else if (value != null) {
            out.add(String.valueOf(value));
        }
        return out;
    }

    public static boolean has(String key) {
        return VALUES.containsKey(key);
    }
}
