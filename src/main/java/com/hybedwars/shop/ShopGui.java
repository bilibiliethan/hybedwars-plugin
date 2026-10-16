package com.hybedwars.shop;

import com.hybedwars.game.UpgradeType;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 商店界面容器，用于识别点击归属。 */
public class ShopGui implements InventoryHolder {

    public enum Type {
        MAIN, CATEGORY, UPGRADES
    }

    private final Type type;
    private final ShopCategory category;
    private final UUID viewer;
    private final Inventory inventory;
    private final Map<Integer, ShopItem> items = new HashMap<>();
    private final Map<Integer, UpgradeType> upgrades = new HashMap<>();

    public ShopGui(Type type, ShopCategory category, UUID viewer, String title, int rows) {
        this.type = type;
        this.category = category;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, rows * 9,
                com.hybedwars.util.Chat.comp(title));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Type type() {
        return type;
    }

    public ShopCategory category() {
        return category;
    }

    public UUID viewer() {
        return viewer;
    }

    public Map<Integer, ShopItem> items() {
        return items;
    }

    public Map<Integer, UpgradeType> upgrades() {
        return upgrades;
    }

    public void put(int slot, ShopItem item) {
        items.put(slot, item);
    }

    public void putUpgrade(int slot, UpgradeType upgrade) {
        upgrades.put(slot, upgrade);
    }

    public void icon(int slot, org.bukkit.inventory.ItemStack stack) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, stack);
        }
    }
}
