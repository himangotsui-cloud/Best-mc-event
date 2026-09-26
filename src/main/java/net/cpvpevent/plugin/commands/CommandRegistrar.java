package net.cpvpevent.plugin.commands;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.command.PluginCommand;

/**
 * Registers every command's executor/tab-completer. Centralized here so
 * plugin.yml stays the single source of truth for usage/permission text
 * while the Java side only worries about wiring behaviour.
 */
public class CommandRegistrar {

    private final CPVPEventPlus plugin;

    public CommandRegistrar(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        EventCommands eventCommands = new EventCommands(plugin);
        register("startevent", eventCommands);
        register("startforce", eventCommands);
        register("stopevent", eventCommands);
        register("pauseevent", eventCommands);
        register("resumeevent", eventCommands);
        register("eventstatus", eventCommands);

        BorderCommands borderCommands = new BorderCommands(plugin);
        register("border", borderCommands);
        register("stopborder", borderCommands);
        register("resumeborder", borderCommands);

        register("drop", new DropCommand(plugin));

        ReviveCommands reviveCommands = new ReviveCommands(plugin);
        register("revive", reviveCommands);
        register("reviveall", reviveCommands);
        register("revivedisable", reviveCommands);
        register("reviveenable", reviveCommands);

        RekitCommands rekitCommands = new RekitCommands(plugin);
        register("rekit", rekitCommands);
        register("rekitset", rekitCommands);
        register("rekitstatus", rekitCommands);

        PvpCommands pvpCommands = new PvpCommands(plugin);
        register("pvp", pvpCommands);
        register("protection", pvpCommands);

        ChatCommands chatCommands = new ChatCommands(plugin);
        register("chat", chatCommands);
        register("staffchat", chatCommands);

        MiscCommands miscCommands = new MiscCommands(plugin);
        register("announce", miscCommands);
        register("announcewinner", miscCommands);
        register("joinspectator", miscCommands);
        register("clearblock", miscCommands);
        register("clearspawn", miscCommands);
        register("setnetherite", miscCommands);
        register("setbedrock", miscCommands);
        register("kitall", miscCommands);
        register("tpspawn", miscCommands);
        register("cpvpeventplusreload", miscCommands);
        register("help", miscCommands);

        register("party", new PartyCommand(plugin));
        register("revfights", new RevFightsCommand(plugin));

        GUICommands guiCommands = new GUICommands(plugin);
        register("eventsettings", guiCommands);
        register("eventools", guiCommands);

        register("regen", new RegenCommand(plugin));
        register("event", new EventDispatchCommand(plugin));
    }

    private void register(String name, Object executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().warning("Command not found in plugin.yml: " + name);
            return;
        }
        if (executor instanceof org.bukkit.command.CommandExecutor exec) {
            command.setExecutor(exec);
        }
        if (executor instanceof org.bukkit.command.TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }
}
