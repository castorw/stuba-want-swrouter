package net.ctrdn.stuba.want.swrouter.module.nat;

abstract public class DefaultNATRule implements NATRule {

    private final NATModule natModule;
    private final int priority;

    public DefaultNATRule(NATModule natModule, int priority) {
        this.natModule = natModule;
        this.priority = priority;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    protected NATModule getNatModule() {
        return natModule;
    }

}
