package net.ctrdn.stuba.want.swrouter.module.routingstatic.api;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.routingcore.IPv4RouteGateway;
import net.ctrdn.stuba.want.swrouter.module.routingstatic.StaticIPv4Route;
import net.ctrdn.stuba.want.swrouter.module.routingstatic.StaticRoutingModule;

public class GetStaticIPRoutesAPIMethod extends DefaultAPIMethod {

    public GetStaticIPRoutesAPIMethod(RouterController routerController) {
        super(routerController, "get-static-ip-routes");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            StaticRoutingModule routingModule = this.getRouterController().getModule(StaticRoutingModule.class);

            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonArrayBuilder routesJab = Json.createArrayBuilder();

            for (StaticIPv4Route route : routingModule.getRoutes()) {
                JsonObjectBuilder routeJob = Json.createObjectBuilder();
                routeJob.add("ID", route.getRouteUuid().toString());
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
                routesJab.add(routeJob);
            }
            responseJob.add("StaticRoutes", routesJab);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to acquire static IPv4 routing module");
        }
    }

}
