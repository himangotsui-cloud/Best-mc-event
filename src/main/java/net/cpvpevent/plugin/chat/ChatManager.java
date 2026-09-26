package net.cpvpevent.plugin.chat;

import net.cpvpevent.plugin.CPVPEventPlus;
import net.cpvpevent.plugin.party.Party;
import net.cpvpevent.plugin.util.Text;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {

    private final CPVPEventPlus plugin;

    private boolean globalChatEnabled = true;
    private final Map<UUID, Boolean> staffChatToggled = new ConcurrentHashMap<>();
    private final Map<UUID, ChatChannel> activeChannel = new ConcurrentHashMap<>();

    public ChatManager(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    public void setGlobalChatEnabled(boolean enabled) {
        this.globalChatEnabled = enabled;
    }

    public boolean isGlobalChatEnabled() {
        return globalChatEnabled;
    }

    public void toggleStaffChat(Player player, boolean enabled) {
        staffChatToggled.put(player.getUniqueId(), enabled);
        player.sendMessage(plugin.configManager().message(enabled ? "chat.staffchat-on" : "chat.staffchat-off"));
    }

    public boolean isStaffChatToggled(Player player) {
        return staffChatToggled.getOrDefault(player.getUniqueId(), false);
    }

    public void setChannel(Player player, ChatChannel channel) {
        activeChannel.put(player.getUniqueId(), channel);
    }

    public ChatChannel channelOf(Player player) {
        return activeChannel.getOrDefault(player.getUniqueId(), ChatChannel.GLOBAL);
    }

    /**
     * Routes a chat message according to the player's staff-chat toggle and
     * active channel selection. Returns true if the message was handled
     * (i.e. the listener should cancel default chat handling).
     */
    public boolean handleChat(Player sender, String message) {
        if (isStaffChatToggled(sender)) {
            broadcastToStaff(sender, message);
            return true;
        }

        ChatChannel channel = channelOf(sender);
        switch (channel) {
            case STAFF -> broadcastToStaff(sender, message);
            case PARTY -> broadcastToParty(sender, message);
            case GLOBAL -> {
                if (!globalChatEnabled) {
                    sender.sendMessage(plugin.configManager().message("chat.disabled"));
                    return true;
                }
                String prefix = plugin.configManager().config().getString("chat.prefixes.global", "");
                plugin.getServer().broadcastMessage(Text.color(prefix) + sender.getName() + ": " + message);
            }
        }
        return true;
    }

    private void broadcastToStaff(Player sender, String message) {
        String prefix = plugin.configManager().config().getString("chat.prefixes.staff", "&c[STAFF] ");
        String formatted = Text.color(prefix) + sender.getName() + ": " + message;
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission("cpvpevent.staffchat")) {
                online.sendMessage(formatted);
            }
        }
    }

    private void broadcastToParty(Player sender, String message) {
        plugin.partyManager().partyOf(sender).ifPresentOrElse(party -> {
            String prefix = plugin.configManager().config().getString("chat.prefixes.party", "&a[PARTY] ");
            String formatted = Text.color(prefix) + sender.getName() + ": " + message;
            for (UUID memberId : party.members()) {
                Player member = plugin.getServer().getPlayer(memberId);
                if (member != null) member.sendMessage(formatted);
            }
        }, () -> sender.sendMessage(plugin.configManager().message("party.not-in-party")));
    }
}
