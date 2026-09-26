package net.cpvpevent.plugin.event;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.util.SafeLocationFinder;
import net.cpvpevent.plugin.util.Text;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central state machine driving the event lifecycle. Rather than scattering
 * independent delayed tasks, all timing is driven from a single repeating
 * scheduler tick, which makes cancel/pause/resume trivial and avoids
 * leaking tasks.
 */
public class EventManager {

    private final CPVPEventPlus plugin;

    private final Map<UUID, PlayerEventState> playerStates = new ConcurrentHashMap<>();
    private String storedWinner = "";

    private EventState state = EventState.IDLE;
    private EventMode mode = EventMode.AUTOMATIC;
    private boolean announceEnabled = true;

    private List<EventStage> stages = new ArrayList<>();
    private int stageIndex = 0;
    private long secondsElapsedInRun = 0;
    private long countdownSecondsRemaining = 0;

    private BukkitTask tickTask;
    private BukkitTask countdownTask;

    private final SafeLocationFinder locationFinder = new SafeLocationFinder();

    public EventManager(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    public void startEvent(int minutes, EventMode requestedMode, boolean announce) {
        if (state != EventState.IDLE) {
            return;
        }
        this.mode = requestedMode;
        this.announceEnabled = announce;
        this.state = EventState.COUNTDOWN;
        this.countdownSecondsRemaining = Math.max(0, minutes) * 60L;

        if (announce) {
            plugin.getServer().broadcastMessage(
                    Text.color(plugin.configManager().message("event.starting-countdown")
                            .replace("%seconds%", String.valueOf(countdownSecondsRemaining))));
        }

        countdownTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (countdownSecondsRemaining <= 0) {
                countdownTask.cancel();
                beginRun();
                return;
            }
            countdownSecondsRemaining--;
        }, 20L, 20L);
    }

    public void startForce(EventMode requestedMode, boolean announce) {
        if (state != EventState.IDLE) {
            return;
        }
        this.mode = requestedMode;
        this.announceEnabled = announce;
        if (announce) {
            plugin.getServer().broadcastMessage(plugin.configManager().message("event.force-started"));
        }
        beginRun();
    }

    private void beginRun() {
        state = EventState.RUNNING;
        stageIndex = 0;
        secondsElapsedInRun = 0;
        stages = loadStages();

        preparePlayersForEvent();

        if (announceEnabled) {
            plugin.getServer().broadcastMessage(plugin.configManager().message("event.started"));
        }

        startTickTask();
    }

    public void stopEvent() {
        if (state == EventState.IDLE) return;
        cancelTasks();
        state = EventState.IDLE;
        playerStates.clear();
        plugin.borderManager().stopBorder();
        plugin.getServer().broadcastMessage(plugin.configManager().message("event.stopped"));
    }

    public void pauseEvent() {
        if (state != EventState.RUNNING) return;
        state = EventState.PAUSED;
        plugin.borderManager().pauseBorder();
        plugin.getServer().broadcastMessage(plugin.configManager().message("event.paused"));
    }

    public void resumeEvent() {
        if (state != EventState.PAUSED) return;
        state = EventState.RUNNING;
        plugin.borderManager().resumeBorder();
        plugin.getServer().broadcastMessage(plugin.configManager().message("event.resumed"));
    }

    public void shutdown() {
        cancelTasks();
    }

    private void cancelTasks() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    // ------------------------------------------------------------------
    // Automatic stage progression - single centralized 1-second tick
    // ------------------------------------------------------------------

    private void startTickTask() {
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (state != EventState.RUNNING) {
                return; // paused - simply do not progress time
            }
            secondsElapsedInRun++;

            checkWinner();

            if (mode != EventMode.AUTOMATIC) {
                return;
            }

            while (stageIndex < stages.size() && stages.get(stageIndex).delaySeconds() <= secondsElapsedInRun) {
                runStage(stages.get(stageIndex));
                stageIndex++;
            }
        }, 20L, 20L);
    }

    private void runStage(EventStage stage) {
        switch (stage.type()) {
            case ANNOUNCE -> plugin.getServer().broadcastMessage(Text.color(stage.message()));
            case BORDER -> plugin.borderManager().shrinkBorder(stage.distance(), stage.timeSeconds());
            case DROP -> plugin.dropManager().triggerDrop(stage.dropMode());
            case PVP_ENABLE -> plugin.pvpManager().setPvpEnabled(true);
            case PVP_DISABLE -> plugin.pvpManager().setPvpEnabled(false);
            case CUSTOM -> { /* reserved for future extension */ }
        }
    }

    private List<EventStage> loadStages() {
        List<EventStage> loaded = new ArrayList<>();
        List<Map<?, ?>> list = plugin.configManager().config().getMapList("event.automatic.stages");
        for (Map<?, ?> raw : list) {
            loaded.add(parseStageMap(raw));
        }
        return loaded;
    }

    @SuppressWarnings("unchecked")
    private EventStage parseStageMap(Map<?, ?> raw) {
        Map<String, Object> map = (Map<String, Object>) raw;
        EventStageType type = EventStageType.valueOf(String.valueOf(map.getOrDefault("type", "CUSTOM")).toUpperCase());
        long delay = Long.parseLong(String.valueOf(map.getOrDefault("delay-seconds", 0)));
        String message = String.valueOf(map.getOrDefault("message", ""));
        int distance = Integer.parseInt(String.valueOf(map.getOrDefault("distance", 0)));
        int time = Integer.parseInt(String.valueOf(map.getOrDefault("time-seconds", 0)));
        String dropMode = String.valueOf(map.getOrDefault("mode", "DEEPSLATE"));
        return new EventStage(type, delay, message, distance, time, dropMode);
    }

    // ------------------------------------------------------------------
    // Player participation
    // ------------------------------------------------------------------

    private void preparePlayersForEvent() {
        World world = plugin.getServer().getWorlds().get(0);
        List<Material> allowed = new ArrayList<>();
        for (String name : plugin.configManager().config().getStringList("event.safe-spawn-blocks")) {
            Material material = Material.matchMaterial(name);
            if (material != null) allowed.add(material);
        }

        int radius = plugin.configManager().config().getInt("event.spawn-search-radius", 200);
        int clearance = plugin.configManager().config().getInt("event.min-clearance-blocks", 3);
        int attempts = plugin.configManager().config().getInt("event.spawn-search-attempts", 50);
        boolean spectatorJoin = plugin.configManager().config().getBoolean("event.join-spectator-by-default", true);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Location safe = locationFinder.findSafeLocation(world, radius, allowed, clearance, attempts);
            if (safe != null) {
                player.teleport(safe);
                playerStates.put(player.getUniqueId(), PlayerEventState.ALIVE);
                player.setGameMode(GameMode.SURVIVAL);
            } else if (spectatorJoin) {
                playerStates.put(player.getUniqueId(), PlayerEventState.SPECTATOR);
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
    }

    public void onPlayerJoinDuringEvent(Player player) {
        if (state == EventState.IDLE) return;
        boolean spectatorJoin = plugin.configManager().config().getBoolean("event.join-spectator-by-default", true);
        if (spectatorJoin) {
            playerStates.put(player.getUniqueId(), PlayerEventState.SPECTATOR);
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    public void markDead(Player player) {
        playerStates.put(player.getUniqueId(), PlayerEventState.SPECTATOR);
    }

    public void markAlive(Player player) {
        playerStates.put(player.getUniqueId(), PlayerEventState.ALIVE);
    }

    public PlayerEventState stateOf(Player player) {
        return playerStates.getOrDefault(player.getUniqueId(), PlayerEventState.SPECTATOR);
    }

    public long aliveCount() {
        return playerStates.values().stream().filter(s -> s == PlayerEventState.ALIVE).count();
    }

    private void checkWinner() {
        if (state != EventState.RUNNING) return;
        long alive = aliveCount();
        long totalTracked = playerStates.size();
        if (totalTracked > 1 && alive == 1) {
            playerStates.entrySet().stream()
                    .filter(e -> e.getValue() == PlayerEventState.ALIVE)
                    .findFirst()
                    .ifPresent(e -> {
                        Player winner = plugin.getServer().getPlayer(e.getKey());
                        endEventWithWinner(winner != null ? winner.getName() : "Unknown");
                    });
        }
    }

    private void endEventWithWinner(String winnerName) {
        storedWinner = winnerName;
        plugin.configManager().events().set("last-winner", winnerName);
        plugin.configManager().saveEvents();

        state = EventState.ENDING;
        cancelTasks();
        plugin.borderManager().stopBorder();

        plugin.getServer().broadcastMessage(
                plugin.configManager().message("event.winner").replace("%player%", winnerName));

        state = EventState.IDLE;
        playerStates.clear();
    }

    public void announceStoredWinner() {
        String winner = plugin.configManager().events().getString("last-winner", "");
        if (winner == null || winner.isBlank()) {
            plugin.getServer().broadcastMessage(plugin.configManager().message("event.no-winner-stored"));
            return;
        }
        plugin.getServer().broadcastMessage(
                plugin.configManager().message("event.winner").replace("%player%", winner));
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public EventState state() {
        return state;
    }

    public EventMode mode() {
        return mode;
    }

    public int currentStageNumber() {
        return stageIndex;
    }

    public int totalStages() {
        return stages.size();
    }
}
