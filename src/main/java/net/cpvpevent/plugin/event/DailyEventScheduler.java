package net.cpvpevent.plugin.event;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Drives configured daily events (events.daily-events schedule) from a
 * single centralized once-a-minute check rather than per-schedule delayed
 * tasks. Persists the last-run date to events.yml so a server restart or
 * reload never causes the same slot to fire twice in one day.
 */
public class DailyEventScheduler {

    private final CPVPEventPlus plugin;
    private BukkitTask checkTask;

    public DailyEventScheduler(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.configManager().config().getBoolean("daily-events.enabled", false)) return;

        // Check once a minute; cheap and precise enough for HH:mm schedules.
        checkTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkSchedule, 20L * 5, 20L * 60);
    }

    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    private void checkSchedule() {
        if (!plugin.configManager().config().getBoolean("daily-events.enabled", false)) return;
        if (plugin.eventManager().state() != EventState.IDLE) return;

        ZoneId zone;
        try {
            zone = ZoneId.of(plugin.configManager().config().getString("daily-events.timezone", "UTC"));
        } catch (Exception ex) {
            zone = ZoneId.of("UTC");
        }

        ZonedDateTime now = ZonedDateTime.now(zone);
        String today = now.toLocalDate().toString();

        List<Map<?, ?>> schedule = plugin.configManager().config().getMapList("daily-events.schedule");

        for (Map<?, ?> entry : schedule) {
            String timeStr = String.valueOf(entry.get("time"));
            LocalTime scheduledTime;
            try {
                scheduledTime = LocalTime.parse(timeStr);
            } catch (DateTimeParseException ex) {
                continue;
            }

            String runKey = "last-daily-run-date-" + timeStr;
            String lastRun = plugin.configManager().events().getString(runKey, "");

            boolean timeReached = !now.toLocalTime().isBefore(scheduledTime);
            boolean alreadyRanToday = today.equals(lastRun);

            if (timeReached && !alreadyRanToday) {
                runDailyEvent(entry);
                plugin.configManager().events().set(runKey, today);
                plugin.configManager().saveEvents();
                return; // only one daily event slot fires per check
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void runDailyEvent(Map<?, ?> raw) {
        Map<String, Object> entry = (Map<String, Object>) raw;
        boolean announce = Boolean.parseBoolean(String.valueOf(entry.getOrDefault("announce", true)));
        EventMode mode = EventMode.fromString(String.valueOf(entry.getOrDefault("mode", "AUTOMATIC")));
        int announceDelay = plugin.configManager().config().getInt("daily-events.announce-delay-seconds", 300);

        if (announce) {
            plugin.getServer().broadcastMessage(
                    net.cpvpevent.plugin.util.Text.color("&aA daily event will begin shortly!"));
        }

        int minutes = Math.max(0, announceDelay / 60);
        plugin.eventManager().startEvent(minutes, mode, announce);
    }
}
