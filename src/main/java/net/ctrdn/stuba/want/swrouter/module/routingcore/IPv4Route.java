package net.ctrdn.stuba.want.swrouter.module.routingcore;

import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;

public interface IPv4Route {

    public IPv4Prefix getTargetPrefix();

    public IPv4Address getNextHopAddress();
}
