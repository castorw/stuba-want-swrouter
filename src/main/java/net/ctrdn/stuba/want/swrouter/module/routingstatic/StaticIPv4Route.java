package net.ctrdn.stuba.want.swrouter.module.routingstatic;

import java.util.ArrayList;
import java.util.List;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.exception.RoutingException;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4Route;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4RouteFlag;

public class StaticIPv4Route implements IPv4Route {

    private final IPv4Prefix targetPrefix;
    private final IPv4Address nextHopAddress;
    private final int administrativeDistance;
    private final List<IPv4RouteFlag> flagList = new ArrayList<>();

    public StaticIPv4Route(IPv4Prefix targetPrefix, IPv4Address nextHopAddress, int administrativeDistance) {
        this.targetPrefix = targetPrefix;
        this.nextHopAddress = nextHopAddress;
        this.administrativeDistance = administrativeDistance;
    }

    @Override
    public IPv4Prefix getTargetPrefix() {
        return this.targetPrefix;
    }

    @Override
    public IPv4Address getNextHopAddress() {
        return this.nextHopAddress;
    }

    @Override
    public int getAdministrativeDistance() {
        return administrativeDistance;
    }

    @Override
    public IPv4RouteFlag[] getFlags() {
        return new IPv4RouteFlag[]{new IPv4RouteFlag("S", "Static", "Administratively configured static route")};
    }

    @Override
    public void addFlag(IPv4RouteFlag flag) throws RoutingException {
        if (flag.getSymbol().equals("S")) {
            throw new RoutingException("Flag \"S\" is reserved by static routing and cannot be applied.");
        }
        if (!this.flagList.contains(flag)) {
            this.flagList.add(flag);
        }
    }

    @Override
    public void removeFlag(IPv4RouteFlag flag) throws RoutingException {
        if (flag.getSymbol().equals("S")) {
            throw new RoutingException("Flag \"S\" is reserved by static routing and cannot be removed.");
        }
        if (this.flagList.contains(flag)) {
            this.flagList.remove(flag);
        }
    }

}
