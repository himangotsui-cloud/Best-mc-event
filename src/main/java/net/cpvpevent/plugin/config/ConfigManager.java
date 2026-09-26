package net.cpvpevent.plugin.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Central place responsible for loading, saving and reloading every
 * configuration file the plugin uses. Each file is exposed as its own
 * FileConfiguration so managers can read the section relevant to them.
 */
public class ConfigManager {

    private final Plugin plugin;

    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration scoreboard;
    private FileConfiguration kits;
    private FileConfiguration events;
    private FileConfiguration parties;
    private FileConfiguration revfights;

    private File configFile;
    private File messagesFile;
    private File scoreboardFile;
    private File kitsFile;
    private File eventsFile;
    private File partiesFile;
    private File revfightsFile;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        config = loadOrCreate("config.yml");
        messages = loadOrCreate("messages.yml");
        scoreboard = loadOrCreate("scoreboard.yml");
        kits = loadOrCreate("kits.yml");
        events = loadOrCreate("events.yml");
        parties = loadOrCreate("parties.yml");
        revfights = loadOrCreate("revfights.yml");
    }

    public void reloadAll() {
        loadAll();
    }

    private FileConfiguration loadOrCreate(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource(fileName)) {
                if (in != null) {
                    plugin.saveResource(fileName, false);
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create " + fileName, e);
            }
        }

        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);

        // Merge defaults from the bundled resource so upgrades add new keys safely.
        InputStream defaultsStream = plugin.getResource(fileName);
        if (defaultsStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultsStream, StandardCharsets.UTF_8));
            loaded.setDefaults(defaults);
        }

        switch (fileName) {
            case "config.yml" -> configFile = file;
            case "messages.yml" -> messagesFile = file;
            case "scoreboard.yml" -> scoreboardFile = file;
            case "kits.yml" -> kitsFile = file;
            case "events.yml" -> eventsFile = file;
            case "parties.yml" -> partiesFile = file;
            case "revfights.yml" -> revfightsFile = file;
            default -> { }
        }

        return loaded;
    }

    public void saveEvents() {
        save(events, eventsFile);
    }

    public void saveParties() {
        save(parties, partiesFile);
    }

    public void saveRevFights() {
        save(revfights, revfightsFile);
    }

    private void save(FileConfiguration cfg, File file) {
        if (cfg == null || file == null) return;
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save " + file.getName(), e);
        }
    }

    public FileConfiguration config() {
        return config;
    }

    public FileConfiguration messages() {
        return messages;
    }

    public FileConfiguration scoreboard() {
        return scoreboard;
    }

    public FileConfiguration kits() {
        return kits;
    }

    public FileConfiguration events() {
        return events;
    }

    public FileConfiguration parties() {
        return parties;
    }

    public FileConfiguration revfights() {
        return revfights;
    }

    public String message(String path) {
        String prefix = messages.getString("prefix", "");
        String msg = messages.getString(path, path);
        return prefix + msg;
    }

    public String rawMessage(String path) {
        return messages.getString(path, path);
    }
}
