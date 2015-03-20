package net.ctrdn.stuba.want.swrouter.module.routingstatic.api;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodUserException;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.routingstatic.StaticIPv4Route;
import net.ctrdn.stuba.want.swrouter.module.routingstatic.StaticRoutingModule;

public class AddStaticIPRouteAPIMethod extends DefaultAPIMethod {

    public AddStaticIPRouteAPIMethod(RouterController routerController) {
        super(routerController, "add-static-ip-route");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            StaticRoutingModule routingModule = this.getRouterController().getModule(StaticRoutingModule.class);
            JsonObjectBuilder responseJob = Json.createObjectBuilder();

            IPv4Prefix targetPrefix = IPv4Prefix.fromString(request.getParameter("TargetPrefix"));
            int administrativeDistance = Integer.parseInt(request.getParameter("AdministrativeDistance"));
            String gatewaysString = request.getParameter("Gateways");
            String[] gatewaysStringSplit = gatewaysString.split(",");

            StaticIPv4Route route = new StaticIPv4Route(targetPrefix, administrativeDistance);
            for (String gwString : gatewaysStringSplit) {
                route.addGatewayAddress(IPv4Address.fromString(gwString));
            }
            routingModule.addRoute(route);
            responseJob.add("Success", true);
            this.getRouterController().onConfigurationChanged();
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to acquire static IPv4 routing module");
        } catch (IPv4MathException | NumberFormatException ex) {
            throw new APIMethodUserException(ex.getMessage());
        }
    }

}
