package com.hybedwars.listener;

import com.hybedwars.HyBedWars;
import com.hybedwars.game.Arena;
import com.hybedwars.shop.ShopCategory;
import com.hybedwars.shop.ShopGui;
import com.hybedwars.shop.ShopItem;
import com.hybedwars.shop.ShopManager;
import com.hybedwars.game.UpgradeType;
import com.hybedwars.util.Chat;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.InventoryHolder;
import java.util.UUID;

/** 商店 GUI 与商店 NPC 的交互。 */
public class ShopListener implements Listener {

    private static final int SLOT_UPGRADES = 49;
    private static final int SLOT_BACK = 45;

    private final HyBedWars plugin;

    public ShopListener(HyBedWars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Arena arena = plugin.gameManager().arenaOf(player);
        if (arena == null) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Villager) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof ShopGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (gui.viewer() != null && !gui.viewer().equals(player.getUniqueId())) {
            return;
        }
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int slot = event.getRawSlot();
        ShopManager shop = plugin.shopManager();

        if (slot == SLOT_UPGRADES && gui.type() != ShopGui.Type.UPGRADES) {
            shop.openUpgrades(player);
            return;
        }
        if (slot == SLOT_BACK || slot == 8) {
            if (gui.type() == ShopGui.Type.MAIN) {
                player.closeInventory();
            } else if (gui.type() == ShopGui.Type.CATEGORY) {
                shop.openMain(player);
            } else {
                shop.openMain(player);
            }
            return;
        }

        switch (gui.type()) {
            case MAIN -> {
                for (ShopCategory category : ShopCategory.values()) {
                    if (category.slot() == slot) {
                        shop.openCategory(player, category);
                        return;
                    }
                }
            }
            case CATEGORY -> {
                ShopItem item = gui.items().get(slot);
                if (item != null) {
                    shop.purchase(player, item, event.isShiftClick());
                }
            }
            case UPGRADES -> {
                UpgradeType upgrade = gui.upgrades().get(slot);
                if (upgrade != null) {
                    shop.purchaseUpgrade(player, upgrade);
                }
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof ShopGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractNpc(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!(event.getRightClicked() instanceof Villager)) {
            return;
        }
        UUID npcId = event.getRightClicked().getUniqueId();
        Arena arena = plugin.gameManager().arenaOf(player);
        Boolean upgrade = arena == null ? null : arena.npcType(npcId);
        if (upgrade != null) {
            event.setCancelled(true);
            if (upgrade) {
                plugin.shopManager().openUpgrades(player);
            } else {
                plugin.shopManager().openMain(player);
            }
            return;
        }
        // 大厅 NPC：点击加入对应地图（不要求玩家正在对局中）
        String arenaName = plugin.lobby() == null ? null : plugin.lobby().npcArena(npcId);
        if (arenaName != null) {
            event.setCancelled(true);
            Arena target = plugin.arenaManager().get(arenaName);
            if (target == null) {
                Chat.raw(player, "&c找不到地图 &e" + arenaName + "&c。");
                return;
            }
            String reason = target.join(player, false);
            if (reason != null) {
                Chat.raw(player, "&c" + reason);
            }
        }
    }
}
