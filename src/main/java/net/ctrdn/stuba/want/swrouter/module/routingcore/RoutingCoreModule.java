package net.ctrdn.stuba.want.swrouter.module.routingcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.DefaultRouterModule;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.ModuleInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingCoreModule extends DefaultRouterModule {

    private final Logger logger = LoggerFactory.getLogger(RoutingCoreModule.class);
    private final List<IPv4Route> routeList = new ArrayList<>();

    public RoutingCoreModule(RouterController controller) {
        super(controller);
    }

    @Override
    public void reloadConfiguration(JsonObject moduleConfiguration) {
    }

    @Override
    public JsonObjectBuilder dumpConfiguration() {
        return null;
    }

    @Override
    public void initialize() throws ModuleInitializationException {
        this.routerController.getPacketProcessor().addPipelineBranch(new RoutingPipelineBranch(this));
    }

    @Override
    public void start() {
    }

    @Override
    public String getName() {
        return "Routing Core";
    }

    @Override
    public Integer getRevision() {
        return 1;
    }

    @Override
    public int getLoadPriority() {
        return 512;
    }

    public void installRoute(IPv4Route route) {
        if (!this.routeList.contains(route)) {
            this.routeList.add(route);
            Collections.sort(this.routeList, new Comparator<IPv4Route>() {

                @Override
                public int compare(IPv4Route o1, IPv4Route o2) {
                    int prefixComp = o1.getTargetPrefix().getAddress().getDecimal() < o2.getTargetPrefix().getAddress().getDecimal() ? -1 : o1.getTargetPrefix().getAddress().getDecimal() == o2.getTargetPrefix().getAddress().getDecimal() ? 0 : 1;
                    if (prefixComp == 0) {
                        int adComp = o1.getAdministrativeDistance() < o2.getAdministrativeDistance() ? -1 : (o1.getAdministrativeDistance() == o2.getAdministrativeDistance()) ? 0 : 1;
                        if (adComp == 0) {
                            int nmlComp = o1.getTargetPrefix().getNetworkMask().getLength() < o2.getTargetPrefix().getNetworkMask().getLength() ? 1 : o1.getTargetPrefix().getNetworkMask().getLength() == o2.getTargetPrefix().getNetworkMask().getLength() ? 0 : -1;
                            return nmlComp;
                        }
                        return adComp;
                    }
                    return prefixComp;
                }
            });
            String hopString = "";
            for (IPv4RouteGateway gw : route.getGateways()) {
                if (!hopString.isEmpty()) {
                    hopString += ", ";
                }
                if (gw.getGatewayAddress() != null) {
                    hopString += gw.getGatewayAddress();
                } else if (gw.getGatewayInterface() != null) {
                    hopString += gw.getGatewayInterface().getName();
                }
            }
            this.logger.debug("Installed route to {} via {} with AD {}", route.getTargetPrefix(), hopString, route.getAdministrativeDistance());
        }
    }

    public void uninstallRoute(IPv4Route route) {
        if (this.routeList.contains(route)) {
            this.routeList.remove(route);
        }
    }

    public IPv4Route[] getInstalledRoutes() {
        return this.routeList.toArray(new IPv4Route[this.routeList.size()]);
    }

    protected IPv4Route lookupRoute(IPv4Address address) {
        for (IPv4Route route : this.routeList) {
            if (route.getTargetPrefix().containsAddress(address) && route.isAvailable()) {
                return route;
            }
        }
        return null;
    }

    protected RouterController getRouterController() {
        return this.routerController;
    }

    public IPv4Prefix getMulticastPrefix() {
        return IPv4Prefix.MULTICAST;
    }
}
