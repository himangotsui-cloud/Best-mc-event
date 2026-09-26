package net.cpvpevent.plugin.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.cpvpevent.plugin.CPVPEventPlus;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Uses Paper's AsyncChatEvent (rather than the deprecated Bukkit
 * AsyncPlayerChatEvent) so the plugin stays aligned with modern Paper APIs.
 */
public class ChatListener implements Listener {

    private final CPVPEventPlus plugin;

    public ChatListener(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        boolean handled = plugin.chatManager().handleChat(event.getPlayer(), plainMessage);
        if (handled) {
            event.setCancelled(true);
        }
    }
}
