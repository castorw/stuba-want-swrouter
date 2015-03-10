package net.ctrdn.stuba.want.swrouter.module.routingripv2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4Route;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4RouteFlag;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4RouteGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final public class RIPv2Route implements IPv4Route {

    private class RouteGateway implements IPv4RouteGateway {

        private final RIPv2RouteEntry routeEntry;

        public RouteGateway(RIPv2RouteEntry entry) {
            this.routeEntry = entry;
        }

        @Override
        public IPv4Address getGatewayAddress() {
            try {
                return (this.routeEntry.getNextHopAddress().equals(IPv4Address.fromString("0.0.0.0"))) ? this.routeEntry.getSenderAddress() : this.routeEntry.getNextHopAddress();
            } catch (IPv4MathException ex) {
                RIPv2Route.this.logger.warn("Failed to return gateway address", ex);
            }
            return null;
        }

        @Override
        public NetworkInterface getGatewayInterface() {
            return null;
        }

        @Override
        public boolean isAvailable() {
            return this.routeEntry.getMetric() < 16;
        }

    }

    private final Logger logger = LoggerFactory.getLogger(RIPv2Route.class);
    private final IPv4Prefix targetPrefix;
    private final List<RIPv2RouteEntry> routeEntryList = Collections.synchronizedList(new CopyOnWriteArrayList<RIPv2RouteEntry>());
    private RouteGateway[] activeGateways = new RouteGateway[0];
    private int nextGatewayIndex = 0;
    private final IPv4RouteFlag ripFlag = new IPv4RouteFlag("r", "RIPv2", "Route learned through RIPv2 protocol");

    protected RIPv2Route(RIPv2RouteEntry initialRouteEntry) {
        this.targetPrefix = initialRouteEntry.getTargetPrefix();
        this.newRouteEntryReceived(initialRouteEntry);
    }

    protected void newRouteEntryReceived(RIPv2RouteEntry entry) {
        this.routeEntryList.add(entry);
        this.calculateActiveGateways();
    }

    protected void calculateActiveGateways() {
        List<RIPv2RouteEntry> orderedEntryList = new ArrayList<>(this.routeEntryList);
        Collections.sort(orderedEntryList, new Comparator<RIPv2RouteEntry>() {

            @Override
            public int compare(RIPv2RouteEntry o1, RIPv2RouteEntry o2) {
                return o1.getMetric() < o2.getMetric() ? -1 : o1.getMetric() == o2.getMetric() ? 0 : 1;
            }
        });
        List<RouteGateway> gwList = new ArrayList<>();
        int bestMetric;
        if (orderedEntryList.size() > 0) {
            bestMetric = orderedEntryList.get(0).getMetric();
        } else {
            bestMetric = 16;
        }
        if (bestMetric < 16) {
            for (RIPv2RouteEntry entry2 : orderedEntryList) {
                if (entry2.getMetric() == bestMetric) {
                    gwList.add(new RouteGateway(entry2));
                }
            }
            this.activeGateways = gwList.toArray(new RouteGateway[gwList.size()]);
            this.nextGatewayIndex = 0;

            String prefixString = "";
            for (RouteGateway gw : this.activeGateways) {
                if (!prefixString.isEmpty()) {
                    prefixString += ", ";
                }
                prefixString += gw.getGatewayAddress();
            }

            this.logger.info("Adjacency changed: Prefix {} is now available via {}", this.getTargetPrefix(), prefixString);
        } else {
            this.activeGateways = new RouteGateway[0];
            this.logger.info("Adjacency changed: No routes to destination prefix {}", this.getTargetPrefix());
        }
    }

    protected List<RIPv2RouteEntry> getRouteEntryList() {
        return this.routeEntryList;
    }

    protected int getBestMetric() {
        return (this.activeGateways.length > 0) ? this.activeGateways[0].routeEntry.getMetric() : 16;
    }

    @Override
    public IPv4Prefix getTargetPrefix() {
        return this.targetPrefix;
    }

    @Override
    public IPv4RouteGateway getNextGateway() {
        RouteGateway gw = (RouteGateway) this.activeGateways[this.nextGatewayIndex];
        this.nextGatewayIndex++;
        if (this.nextGatewayIndex >= this.activeGateways.length) {
            this.nextGatewayIndex = 0;
        }
        return gw;
    }

    @Override
    public IPv4RouteGateway[] getGateways() {
        return this.activeGateways;
    }

    @Override
    public int getAdministrativeDistance() {
        return 120;
    }

    @Override
    public IPv4RouteFlag[] getFlags() {
        return new IPv4RouteFlag[]{this.ripFlag};
    }

    @Override
    public boolean isAvailable() {
        return this.activeGateways.length > 0;
    }

}
