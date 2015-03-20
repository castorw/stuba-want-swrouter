package net.ctrdn.stuba.want.swrouter.module.routingstatic.api;

import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodUserException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.routingstatic.StaticIPv4Route;
import net.ctrdn.stuba.want.swrouter.module.routingstatic.StaticRoutingModule;

public class RemoveStaticIPRouteAPIMethod extends DefaultAPIMethod {

    public RemoveStaticIPRouteAPIMethod(RouterController routerController) {
        super(routerController, "remove-static-ip-route");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            StaticRoutingModule routingModule = this.getRouterController().getModule(StaticRoutingModule.class);
            JsonObjectBuilder responseJob = Json.createObjectBuilder();

            StaticIPv4Route foundRoute = null;
            for (StaticIPv4Route route : routingModule.getRoutes()) {
                if (route.getRouteUuid().equals(UUID.fromString(request.getParameter("ID")))) {
                    foundRoute = route;
                    break;
                }
            }
            if (foundRoute == null) {
                throw new APIMethodUserException("Route not found");
            }

            routingModule.removeRoute(foundRoute);
            this.getRouterController().onConfigurationChanged();
            responseJob.add("Success", true);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to acquire static IPv4 routing module");
        }
    }
}
