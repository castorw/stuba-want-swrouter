package net.ctrdn.stuba.want.swrouter.module.routingcore.api;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4Route;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4RouteFlag;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4RouteGateway;
import net.ctrdn.stuba.want.swrouter.module.routingcore.RoutingCoreModule;

public class GetIPRoutesAPIMethod extends DefaultAPIMethod {

    public GetIPRoutesAPIMethod(RouterController routerController) {
        super(routerController, "get-ip-routes");
    }

    @Override
    public JsonObjectBuilder execute(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonArrayBuilder routesJab = Json.createArrayBuilder();
            RoutingCoreModule rcm = this.getRouterController().getModule(RoutingCoreModule.class);
            for (IPv4Route route : rcm.getInstalledRoutes()) {
                JsonObjectBuilder routeJob = Json.createObjectBuilder();
                routeJob.add("TargetPrefix", route.getTargetPrefix().toString());

                JsonArrayBuilder gatewayJab = Json.createArrayBuilder();
                for (IPv4RouteGateway gw : route.getGateways()) {
                    if (gw.getGatewayAddress() != null) {
                        gatewayJab.add(gw.getGatewayAddress().toString());
                    } else if (gw.getGatewayInterface() != null) {
                        gatewayJab.add(gw.getGatewayInterface().getName());
                    }
                }
                routeJob.add("Gateways", gatewayJab);
                routeJob.add("AdministrativeDistance", route.getAdministrativeDistance());

                String flagString = "";
                for (IPv4RouteFlag flag : route.getFlags()) {
                    flagString += flag.getSymbol();
                }
                routeJob.add("Flags", flagString);
                routesJab.add(routeJob);
            }
            responseJob.add("InstalledRoutes", routesJab);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Internal error: " + ex.getMessage());
        }
    }

}
