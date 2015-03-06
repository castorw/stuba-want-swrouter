package net.ctrdn.stuba.want.swrouter.module.routingcore;

import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;

public interface IPv4RouteGateway {

    public IPv4Address getGatewayAddress();

    public NetworkInterface getGatewayInterface();

    public boolean isAvailable();
}
