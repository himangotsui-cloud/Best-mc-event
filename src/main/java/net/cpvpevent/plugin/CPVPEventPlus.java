package net.cpvpevent.plugin;

import net.cpvpevent.plugin.border.BorderManager;
import net.cpvpevent.plugin.chat.ChatManager;
import net.cpvpevent.plugin.commands.CommandRegistrar;
import net.cpvpevent.plugin.config.ConfigManager;
import net.cpvpevent.plugin.drop.DropManager;
import net.cpvpevent.plugin.event.DailyEventScheduler;
import net.cpvpevent.plugin.event.EventManager;
import net.cpvpevent.plugin.gui.GUIManager;
import net.cpvpevent.plugin.kits.KitManager;
import net.cpvpevent.plugin.listeners.CombatListener;
import net.cpvpevent.plugin.listeners.ChatListener;
import net.cpvpevent.plugin.listeners.PlayerConnectionListener;
import net.cpvpevent.plugin.listeners.ProtectionListener;
import net.cpvpevent.plugin.party.PartyManager;
import net.cpvpevent.plugin.pvp.PvPManager;
import net.cpvpevent.plugin.regen.RegenerationManager;
import net.cpvpevent.plugin.rekit.RekitManager;
import net.cpvpevent.plugin.revfight.RevFightManager;
import net.cpvpevent.plugin.revive.ReviveManager;
import net.cpvpevent.plugin.scoreboard.CPVPPlaceholderExpansion;
import net.cpvpevent.plugin.scoreboard.ScoreboardManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for CPVPEventPlus. Wires together every manager and is
 * responsible for clean startup/shutdown so no scheduled tasks are ever
 * left running after a reload or server stop.
 */
public class CPVPEventPlus extends JavaPlugin {

    private ConfigManager configManager;
    private EventManager eventManager;
    private BorderManager borderManager;
    private DropManager dropManager;
    private PvPManager pvpManager;
    private RekitManager rekitManager;
    private ReviveManager reviveManager;
    private PartyManager partyManager;
    private RevFightManager revFightManager;
    private ChatManager chatManager;
    private KitManager kitManager;
    private RegenerationManager regenerationManager;
    private ScoreboardManager scoreboardManager;
    private GUIManager guiManager;
    private DailyEventScheduler dailyEventScheduler;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.loadAll();

        // Order matters only in that managers referencing others via
        // plugin.xxxManager() do so lazily inside method bodies, not in
        // their constructors, so construction order here is safe.
        eventManager = new EventManager(this);
        borderManager = new BorderManager(this);
        dropManager = new DropManager(this);
        pvpManager = new PvPManager(this);
        rekitManager = new RekitManager(this);
        reviveManager = new ReviveManager(this);
        partyManager = new PartyManager(this);
        revFightManager = new RevFightManager(this);
        chatManager = new ChatManager(this);
        kitManager = new KitManager(this);
        regenerationManager = new RegenerationManager(this);
        scoreboardManager = new ScoreboardManager(this);
        guiManager = new GUIManager(this);
        dailyEventScheduler = new DailyEventScheduler(this);

        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        new CommandRegistrar(this).registerAll();

        scoreboardManager.start();
        dailyEventScheduler.start();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CPVPPlaceholderExpansion(this).register();
        }

        getLogger().info("CPVPEventPlus enabled.");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) eventManager.shutdown();
        if (borderManager != null) borderManager.stopBorder();
        if (scoreboardManager != null) scoreboardManager.stop();
        if (dailyEventScheduler != null) dailyEventScheduler.stop();
        getServer().getScheduler().cancelTasks(this);
        getLogger().info("CPVPEventPlus disabled - all tasks cancelled.");
    }

    public void reload() {
        configManager.reloadAll();
        rekitManager.reloadFromConfig();
    }

    // ------------------------------------------------------------------
    // Manager accessors
    // ------------------------------------------------------------------

    public ConfigManager configManager() {
        return configManager;
    }

    public EventManager eventManager() {
        return eventManager;
    }

    public BorderManager borderManager() {
        return borderManager;
    }

    public DropManager dropManager() {
        return dropManager;
    }

    public PvPManager pvpManager() {
        return pvpManager;
    }

    public RekitManager rekitManager() {
        return rekitManager;
    }

    public ReviveManager reviveManager() {
        return reviveManager;
    }

    public PartyManager partyManager() {
        return partyManager;
    }

    public RevFightManager revFightManager() {
        return revFightManager;
    }

    public ChatManager chatManager() {
        return chatManager;
    }

    public KitManager kitManager() {
        return kitManager;
    }

    public RegenerationManager regenerationManager() {
        return regenerationManager;
    }

    public ScoreboardManager scoreboardManager() {
        return scoreboardManager;
    }

    public GUIManager guiManager() {
        return guiManager;
    }
}
