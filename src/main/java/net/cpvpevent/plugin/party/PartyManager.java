package net.cpvpevent.plugin.party;

import net.cpvpevent.plugin.CPVPEventPlus;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages parties: creation, invites, membership, glow visibility (via a
 * per-party scoreboard team so only teammates see the glow) and the shared
 * revive-kill counter used by the revive system.
 */
public class PartyManager {

    private final CPVPEventPlus plugin;

    private final Map<UUID, Party> partiesById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToParty = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>(); // invitee -> inviter

    public PartyManager(CPVPEventPlus plugin) {
        this.plugin = plugin;
    }

    public boolean isInParty(Player player) {
        return playerToParty.containsKey(player.getUniqueId());
    }

    public Optional<Party> partyOf(Player player) {
        UUID partyId = playerToParty.get(player.getUniqueId());
        return partyId == null ? Optional.empty() : Optional.ofNullable(partiesById.get(partyId));
    }

    public Party createParty(Player leader) {
        Party party = new Party(leader.getUniqueId());
        partiesById.put(party.id(), party);
        playerToParty.put(leader.getUniqueId(), party.id());
        return party;
    }

    public void invite(Player inviter, Player target) {
        pendingInvites.put(target.getUniqueId(), inviter.getUniqueId());
    }

    public boolean acceptInvite(Player player) {
        UUID inviterId = pendingInvites.remove(player.getUniqueId());
        if (inviterId == null) return false;

        Player inviter = plugin.getServer().getPlayer(inviterId);
        if (inviter == null) return false;

        Party party = partyOf(inviter).orElseGet(() -> createParty(inviter));

        int maxSize = plugin.configManager().config().getInt("party.max-size", 8);
        if (party.size() >= maxSize) return false;

        party.addMember(player.getUniqueId());
        playerToParty.put(player.getUniqueId(), party.id());
        applyGlow(party);
        return true;
    }

    public void leaveParty(Player player) {
        partyOf(player).ifPresent(party -> {
            party.removeMember(player.getUniqueId());
            playerToParty.remove(player.getUniqueId());
            removeGlow(player, party);

            if (party.members().isEmpty()) {
                partiesById.remove(party.id());
            } else if (party.leader().equals(player.getUniqueId())) {
                party.setLeader(party.members().iterator().next());
            }
        });
    }

    public boolean kick(Player leader, Player target) {
        Optional<Party> partyOpt = partyOf(leader);
        if (partyOpt.isEmpty()) return false;
        Party party = partyOpt.get();
        if (!party.leader().equals(leader.getUniqueId())) return false;
        if (!party.isMember(target.getUniqueId())) return false;

        party.removeMember(target.getUniqueId());
        playerToParty.remove(target.getUniqueId());
        removeGlow(target, party);
        return true;
    }

    public void disband(Party party) {
        for (UUID member : party.members()) {
            playerToParty.remove(member);
            Player online = plugin.getServer().getPlayer(member);
            if (online != null) removeGlow(online, party);
        }
        partiesById.remove(party.id());
    }

    public void resetAll() {
        for (Party party : partiesById.values()) {
            disband(party);
        }
        partiesById.clear();
        playerToParty.clear();
        pendingInvites.clear();
    }

    public boolean isFriendlyFire(Player a, Player b) {
        return partyOf(a).map(p -> p.isMember(b.getUniqueId())).orElse(false);
    }

    public void addSharedReviveKill(Player killer) {
        partyOf(killer).ifPresent(Party::addReviveKill);
    }

    public int reviveProgress(Player player) {
        return partyOf(player).map(Party::reviveKills).orElse(0);
    }

    private void applyGlow(Party party) {
        if (!plugin.configManager().config().getBoolean("party.glow-enabled", true)) return;

        Team team = teamFor(party);
        for (UUID memberId : party.members()) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null) {
                team.addEntry(member.getName());
                member.setGlowing(true);
            }
        }
    }

    private void removeGlow(Player player, Party party) {
        player.setGlowing(false);
        Team team = plugin.getServer().getScoreboardManager().getMainScoreboard().getTeam("party_" + party.id().toString().substring(0, 8));
        if (team != null) {
            team.removeEntry(player.getName());
        }
    }

    private Team teamFor(Party party) {
        String name = "party_" + party.id().toString().substring(0, 8);
        Team team = plugin.getServer().getScoreboardManager().getMainScoreboard().getTeam(name);
        if (team == null) {
            team = plugin.getServer().getScoreboardManager().getMainScoreboard().registerNewTeam(name);
            team.setCanSeeFriendlyInvisibles(true);
        }
        return team;
    }
}
