package net.ctrdn.stuba.want.swrouter.module.routingstatic;

import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4NetworkMask;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.DefaultRouterModule;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.ModuleInitializationException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.routingcore.RoutingCoreModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticRoutingModule extends DefaultRouterModule {

    private final Logger logger = LoggerFactory.getLogger(StaticRoutingModule.class);
    private RoutingCoreModule routingCoreModule;
    private final List<StaticIPv4Route> routeList = new ArrayList<>();

    public StaticRoutingModule(RouterController controller) throws ModuleInitializationException {
        super(controller);
    }

    @Override
    public void reloadConfiguration(JsonObject moduleConfiguration) {
        if (moduleConfiguration != null) {
            JsonArray routesArray = moduleConfiguration.getJsonArray("StaticRoutes");
            if (routesArray != null) {
                for (JsonObject routeObj : routesArray.getValuesAs(JsonObject.class)) {
                    try {
                        IPv4Prefix prefix = new IPv4Prefix(IPv4Address.fromString(routeObj.getString("PrefixAddress")), new IPv4NetworkMask(routeObj.getInt("PrefixLength")));
                        IPv4Address nextHopAddress = IPv4Address.fromString(routeObj.getString("NextHopAddress"));
                        int administrativeDistance = routeObj.getInt("AdministrativeDistance");
                        StaticIPv4Route route = new StaticIPv4Route(prefix, nextHopAddress, administrativeDistance);
                        this.routeList.add(route);
                        this.logger.debug("Added route to {} via {} with AD of {}", prefix, nextHopAddress, administrativeDistance);
                    } catch (IPv4MathException ex) {
                        this.logger.warn("Failed to load route", ex);
                    }
                }
            }
        }
    }

    @Override
    public JsonObjectBuilder dumpConfiguration() {
        JsonObjectBuilder configJob = Json.createObjectBuilder();
        JsonArrayBuilder routesJab = Json.createArrayBuilder();

        for (StaticIPv4Route route : this.routeList) {
            JsonObjectBuilder routeJob = Json.createObjectBuilder();
            routeJob.add("PrefixAddress", route.getTargetPrefix().getAddress().toString());
            routeJob.add("PrefixLength", route.getTargetPrefix().getNetworkMask().getLength());
            routeJob.add("NextHopAddress", route.getNextHopAddress().toString());
            routeJob.add("AdministrativeDistance", route.getAdministrativeDistance());
            routesJab.add(routeJob);
        }
        configJob.add("StaticRoutes", routesJab);
        return configJob;
    }

    @Override
    public void initialize() throws ModuleInitializationException {
    }

    @Override
    public void start() {
        try {
            this.routingCoreModule = this.routerController.getModule(RoutingCoreModule.class);
            this.reinstallRoutes();
        } catch (NoSuchModuleException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String getName() {
        return "Static Routing";
    }

    @Override
    public Integer getRevision() {
        return 1;
    }

    public void reinstallRoutes() {
        for (StaticIPv4Route route : this.routeList) {
            this.routingCoreModule.uninstallRoute(route);
            this.routingCoreModule.installRoute(route);
        }
    }
}