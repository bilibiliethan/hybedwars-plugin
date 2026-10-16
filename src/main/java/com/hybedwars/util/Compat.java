package com.hybedwars.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 版本兼容层。
 * <p>
 * 1.20.5 起附魔与药水效果全部迁移到 {@link Registry}，同时大量字段被重命名
 * （如 DAMAGE_ALL → SHARPNESS、FAST_DIGGING → HASTE）。为保证在不同小版本上都能工作，
 * 这里统一走注册表按 key 查询，并用反射兜底字段名。
 */
@SuppressWarnings("unchecked")
public final class Compat {

    private static final Registry<Enchantment> ENCHANTMENTS = (Registry<Enchantment>) findRegistry(
            "ENCHANTMENT", "ENCHANTMENTS");
    private static final Registry<PotionEffectType> EFFECTS = (Registry<PotionEffectType>) findRegistry(
            "EFFECT", "POTION_EFFECT_TYPE", "POTION_EFFECTS");

    private static final Map<String, Enchantment> ENCH_CACHE = new HashMap<>();
    private static final Map<String, PotionEffectType> EFFECT_CACHE = new HashMap<>();

    private Compat() {
    }

    private static Object findRegistry(String... names) {
        for (String name : names) {
            try {
                Field field = Registry.class.getField(name);
                Object value = field.get(null);
                if (value instanceof Registry) {
                    return value;
                }
            } catch (Throwable ignored) {
                // 尝试下一个候选名
            }
        }
        return null;
    }

    /** 按 minecraft key 获取附魔，例如 "sharpness"、"efficiency"。 */
    public static Enchantment ench(String key) {
        String id = key.toLowerCase(Locale.ROOT);
        Enchantment cached = ENCH_CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        Enchantment result = null;
        NamespacedKey nk = NamespacedKey.minecraft(id);
        if (ENCHANTMENTS != null) {
            result = ENCHANTMENTS.get(nk);
        }
        if (result == null) {
            result = Enchantment.getByKey(nk);
        }
        if (result == null) {
            throw new IllegalArgumentException("未知附魔: " + key);
        }
        ENCH_CACHE.put(id, result);
        return result;
    }

    /** 按 minecraft key 获取药水效果，例如 "speed"、"haste"、"jump_boost"。 */
    public static PotionEffectType effect(String key) {
        String id = key.toLowerCase(Locale.ROOT);
        PotionEffectType cached = EFFECT_CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        PotionEffectType result = null;
        NamespacedKey nk = NamespacedKey.minecraft(id);
        if (EFFECTS != null) {
            result = EFFECTS.get(nk);
        }
        if (result == null) {
            result = PotionEffectType.getByName(id.toUpperCase(Locale.ROOT));
        }
        if (result == null) {
            throw new IllegalArgumentException("未知药水效果: " + key);
        }
        EFFECT_CACHE.put(id, result);
        return result;
    }

    /** 附魔是否可用（用于容错处理）。 */
    public static Enchantment enchOrNull(String key) {
        try {
            return ench(key);
        } catch (Throwable t) {
            return null;
        }
    }

    public static PotionEffectType effectOrNull(String key) {
        try {
            return effect(key);
        } catch (Throwable t) {
            return null;
        }
    }
}
