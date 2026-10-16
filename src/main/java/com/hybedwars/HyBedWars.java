package com.hybedwars;

import com.hybedwars.command.BedWarsCommand;
import com.hybedwars.config.Messages;
import com.hybedwars.config.Settings;
import com.hybedwars.game.Arena;
import com.hybedwars.game.ArenaManager;
import com.hybedwars.game.GameManager;
import com.hybedwars.lobby.Lobby;
import com.hybedwars.listener.BlockListener;
import com.hybedwars.listener.CombatListener;
import com.hybedwars.listener.ItemListener;
import com.hybedwars.listener.PlayerListener;
import com.hybedwars.listener.ShopListener;
import com.hybedwars.shop.ShopManager;
import com.hybedwars.stats.StatsManager;
import com.hybedwars.ui.ScoreboardManager;
import com.hybedwars.util.Chat;
import com.hybedwars.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * HyBedWars 主类。
 * <p>
 * 仿 Hypixel 起床战争，支持四种模式：
 * 单人八队、双人八队、四人四队、四队八人（体验）。
 */
public final class HyBedWars extends JavaPlugin {

    private static HyBedWars instance;

    private ArenaManager arenaManager;
    private ShopManager shopManager;
    private StatsManager statsManager;
    private GameManager gameManager;
    private ScoreboardManager scoreboardManager;
    private Lobby lobby;

    public static HyBedWars get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        Messages.load();
        Settings.load();

        this.shopManager = new ShopManager(this);
        this.shopManager.load();

        this.arenaManager = new ArenaManager(this);
        this.arenaManager.load();

        this.lobby = new Lobby(this);
        this.lobby.load();

        this.statsManager = new StatsManager(this);
        this.statsManager.load();
        this.statsManager.startAutoSave();

        this.scoreboardManager = new ScoreboardManager(this);
        this.gameManager = new GameManager(this);
        this.gameManager.start();

        registerListeners();
        registerCommand();

        createFolders();

        getLogger().info("HyBedWars v" + getDescription().getVersion() + " 已启用，共 "
                + arenaManager.all().size() + " 张地图。");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdownAll();
            gameManager.stop();
        }
        if (scoreboardManager != null) {
            scoreboardManager.clear();
        }
        if (arenaManager != null) {
            for (Arena arena : arenaManager.all()) {
                if (arena.worldName() != null && arena.world() != null) {
                    WorldUtil.unload(arena.worldName());
                }
            }
            arenaManager.save();
        }
        if (lobby != null) {
            lobby.unload();
        }
        if (statsManager != null) {
            statsManager.stopAutoSave();
            statsManager.save();
        }
        instance = null;
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ItemListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ShopListener(this), this);
    }

    private void registerCommand() {
        BedWarsCommand executor = new BedWarsCommand(this);
        PluginCommand command = getCommand("bedwars");
        if (command == null) {
            getLogger().severe("无法注册 /bedwars 命令，请检查 plugin.yml。");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void createFolders() {
        File data = getDataFolder();
        if (!data.exists() && !data.mkdirs()) {
            getLogger().warning("无法创建插件数据目录。");
        }
        File templates = new File(data, Settings.templateFolder);
        if (!templates.exists() && !templates.mkdirs()) {
            getLogger().warning("无法创建地图模板目录。");
        }
    }

    /** 重载配置。 */
    public void reloadAll() {
        Settings.load();
        Messages.load();
        shopManager.load();
        if (lobby != null) {
            lobby.unload();
        }
        lobby = new Lobby(this);
        lobby.load();
        Chat.raw(Bukkit.getConsoleSender(), "[HyBedWars] 配置已重载。");
    }

    // ---------------- 管理器访问 ----------------

    public ArenaManager arenaManager() {
        return arenaManager;
    }

    public ShopManager shopManager() {
        return shopManager;
    }

    public StatsManager statsManager() {
        return statsManager;
    }

    public GameManager gameManager() {
        return gameManager;
    }

    public ScoreboardManager scoreboardManager() {
        return scoreboardManager;
    }

    public Lobby lobby() {
        return lobby;
    }
}
