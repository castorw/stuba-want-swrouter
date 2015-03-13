package net.ctrdn.stuba.want.swrouter.module.nat;

import net.ctrdn.stuba.want.swrouter.core.processing.Packet;

public interface NATRule {

    public String getTypeString();

    public int getPriority();

    public NATRuleResult translate(Packet packet);

    public void clear();
}
