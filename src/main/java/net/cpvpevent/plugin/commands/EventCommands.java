package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.event.EventMode;
import net.cpvpevent.plugin.event.EventState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class EventCommands implements CommandExecutor, TabCompleter {

    private final CPVPEventPlus plugin;

    public EventCommands(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cpvpevent.admin")) {
            sender.sendMessage(plugin.configManager().message("general.no-permission"));
            return true;
        }

        switch (label.toLowerCase()) {
            case "startevent" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.configManager().message("general.invalid-usage")
                            .replace("%usage%", "/startevent <min> <mode> <announce>"));
                    return true;
                }
                if (plugin.eventManager().state() != EventState.IDLE) {
                    sender.sendMessage(plugin.configManager().message("event.already-running"));
                    return true;
                }
                int minutes;
                try {
                    minutes = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.configManager().message("general.invalid-number"));
                    return true;
                }
                EventMode mode = EventMode.fromString(args[1]);
                boolean announce = Boolean.parseBoolean(args[2]);
                plugin.eventManager().startEvent(minutes, mode, announce);
            }
            case "startforce" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.configManager().message("general.invalid-usage")
                            .replace("%usage%", "/startforce <mode> <announce>"));
                    return true;
                }
                if (plugin.eventManager().state() != EventState.IDLE) {
                    sender.sendMessage(plugin.configManager().message("event.already-running"));
                    return true;
                }
                EventMode mode = EventMode.fromString(args[0]);
                boolean announce = Boolean.parseBoolean(args[1]);
                plugin.eventManager().startForce(mode, announce);
            }
            case "stopevent" -> {
                if (plugin.eventManager().state() == EventState.IDLE) {
                    sender.sendMessage(plugin.configManager().message("event.none-running"));
                    return true;
                }
                plugin.eventManager().stopEvent();
            }
            case "pauseevent" -> plugin.eventManager().pauseEvent();
            case "resumeevent" -> plugin.eventManager().resumeEvent();
            case "eventstatus" -> {
                sender.sendMessage(plugin.configManager().message("event.status-header"));
                sender.sendMessage(plugin.configManager().message("event.status-line")
                        .replace("%state%", plugin.eventManager().state().name())
                        .replace("%mode%", plugin.eventManager().mode().name())
                        .replace("%stage%", plugin.eventManager().currentStageNumber() + "/" + plugin.eventManager().totalStages()));
            }
            default -> { }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> options = new ArrayList<>();
        if (label.equalsIgnoreCase("startevent") && args.length == 2) {
            options.addAll(List.of("AUTOMATIC", "HIGH_PLAYERS", "LOW_PLAYERS", "MANUALLY"));
        } else if (label.equalsIgnoreCase("startforce") && args.length == 1) {
            options.addAll(List.of("AUTOMATIC", "HIGH_PLAYERS", "LOW_PLAYERS", "MANUALLY"));
        } else if ((label.equalsIgnoreCase("startevent") && args.length == 3)
                || (label.equalsIgnoreCase("startforce") && args.length == 2)) {
            options.addAll(List.of("true", "false"));
        }
        return options;
    }
}
