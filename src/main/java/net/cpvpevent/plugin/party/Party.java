package net.cpvpevent.plugin.party;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A group of players sharing friendly fire immunity, glow visibility and
 * a shared revive-kill counter.
 */
public class Party {

    private final UUID id = UUID.randomUUID();
    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final AtomicInteger sharedReviveKills = new AtomicInteger(0);

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID id() {
        return id;
    }

    public UUID leader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
    }

    public Set<UUID> members() {
        return members;
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public int size() {
        return members.size();
    }

    public int reviveKills() {
        return sharedReviveKills.get();
    }

    public int addReviveKill() {
        return sharedReviveKills.incrementAndGet();
    }

    public void resetReviveKills() {
        sharedReviveKills.set(0);
    }
}
