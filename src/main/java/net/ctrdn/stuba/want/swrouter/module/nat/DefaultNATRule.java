package net.ctrdn.stuba.want.swrouter.module.nat;

import java.util.UUID;

abstract public class DefaultNATRule implements NATRule {

    private final UUID id;
    private final NATModule natModule;
    private final int priority;

    public DefaultNATRule(NATModule natModule, int priority) {
        this.natModule = natModule;
        this.priority = priority;
        this.id = UUID.randomUUID();
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    protected NATModule getNatModule() {
        return natModule;
    }

    @Override
    public UUID getID() {
        return this.id;
    }

}
