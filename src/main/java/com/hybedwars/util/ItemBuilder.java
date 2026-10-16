package com.hybedwars.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 物品构建器（链式调用）。 */
@SuppressWarnings("deprecation")
public final class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;
    private final List<String> lore = new ArrayList<>();

    private ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, Math.max(1, Math.min(64, amount)));
        this.meta = item.getItemMeta();
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material, 1);
    }

    public static ItemBuilder of(Material material, int amount) {
        return new ItemBuilder(material, amount);
    }

    /** 基于已有物品创建构建器（会复制其名称、Lore、附魔、旗帜与自定义数据）。 */
    public static ItemBuilder of(ItemStack stack) {
        ItemBuilder builder = new ItemBuilder(stack.getType(), stack.getAmount());
        ItemMeta source = stack.getItemMeta();
        if (source == null) {
            return builder;
        }
        ItemMeta target = builder.meta;
        if (source.hasDisplayName()) {
            target.setDisplayName(source.getDisplayName());
        }
        if (source.hasLore()) {
            target.setLore(source.getLore());
        }
        for (ItemFlag flag : source.getItemFlags()) {
            target.addItemFlags(flag);
        }
        for (java.util.Map.Entry<Enchantment, Integer> entry : source.getEnchants().entrySet()) {
            target.addEnchant(entry.getKey(), entry.getValue(), true);
        }
        if (source.isUnbreakable()) {
            target.setUnbreakable(true);
        }
        if (source instanceof LeatherArmorMeta sourceLeather
                && target instanceof LeatherArmorMeta targetLeather) {
            targetLeather.setColor(sourceLeather.getColor());
        }
        if (source instanceof PotionMeta sourcePotion && target instanceof PotionMeta targetPotion) {
            targetPotion.setColor(sourcePotion.getColor());
            for (org.bukkit.potion.PotionEffect effect : sourcePotion.getCustomEffects()) {
                targetPotion.addCustomEffect(effect, true);
            }
        }
        if (!source.getPersistentDataContainer().isEmpty()) {
            source.getPersistentDataContainer().copyTo(target.getPersistentDataContainer(), true);
        }
        return builder;
    }

    public ItemBuilder name(String name) {
        meta.setDisplayName(Chat.color(name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder lore(List<String> lines) {
        for (String line : lines) {
            lore.add(Chat.color(line));
        }
        return this;
    }

    public ItemBuilder addLore(String line) {
        lore.add(Chat.color(line));
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (enchantment != null && level > 0) {
            meta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    public ItemBuilder enchant(String key, int level) {
        Enchantment enchantment = Compat.enchOrNull(key);
        return enchant(enchantment, level);
    }

    public ItemBuilder enchantGlow(boolean glow) {
        if (glow) {
            Enchantment enchantment = Compat.enchOrNull("unbreaking");
            if (enchantment != null) {
                meta.addEnchant(enchantment, 1, true);
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        if (unbreakable) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemBuilder hideAttributes() {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder leatherColor(org.bukkit.Color color) {
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(color);
        }
        return this;
    }

    public ItemBuilder potionColor(org.bukkit.Color color) {
        if (meta instanceof PotionMeta potionMeta) {
            potionMeta.setColor(color);
        }
        return this;
    }

    public ItemStack build() {
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
}
