package net.ctrdn.stuba.want.swrouter.module.routingstatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4Route;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4RouteFlag;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4RouteGateway;

public class StaticIPv4Route implements IPv4Route {

    public class RouteGateway implements IPv4RouteGateway {

        private final IPv4Address gatewayAddress;

        public RouteGateway(IPv4Address address) {
            this.gatewayAddress = address;
        }

        @Override
        public IPv4Address getGatewayAddress() {
            return this.gatewayAddress;
        }

        @Override
        public NetworkInterface getGatewayInterface() {
            return null;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

    }

    private final IPv4Prefix targetPrefix;
    private final List<RouteGateway> gatewayList = Collections.synchronizedList(new ArrayList<RouteGateway>());
    private int nextGatewayIndex = 0;
    private final int administrativeDistance;
    private final List<IPv4RouteFlag> flagList = new ArrayList<>();

    public StaticIPv4Route(IPv4Prefix targetPrefix, int administrativeDistance) {
        this.targetPrefix = targetPrefix;
        this.administrativeDistance = administrativeDistance;
    }

    @Override
    public IPv4Prefix getTargetPrefix() {
        return this.targetPrefix;
    }

    @Override
    public IPv4RouteGateway getNextGateway() {
        IPv4RouteGateway gw = (IPv4RouteGateway) this.gatewayList.get(this.nextGatewayIndex);
        this.nextGatewayIndex++;
        if (this.nextGatewayIndex >= this.gatewayList.size()) {
            this.nextGatewayIndex = 0;
        }
        return gw;
    }

    @Override
    public IPv4RouteGateway[] getGateways() {
        return this.gatewayList.toArray(new IPv4RouteGateway[this.gatewayList.size()]);
    }

    @Override
    public int getAdministrativeDistance() {
        return administrativeDistance;
    }

    @Override
    public IPv4RouteFlag[] getFlags() {
        return new IPv4RouteFlag[]{new IPv4RouteFlag("S", "Static", "Administratively configured static route")};
    }

    protected void addGatewayAddress(IPv4Address address) {
        this.gatewayList.add(new RouteGateway(address));
    }
}
