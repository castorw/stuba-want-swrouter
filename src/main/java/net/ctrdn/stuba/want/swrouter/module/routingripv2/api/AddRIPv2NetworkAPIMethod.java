package net.ctrdn.stuba.want.swrouter.module.routingripv2.api;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodUserException;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.routingripv2.RIPv2RoutingModule;

public class AddRIPv2NetworkAPIMethod extends DefaultAPIMethod {

    public AddRIPv2NetworkAPIMethod(RouterController routerController) {
        super(routerController, "add-ripv2-network");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            RIPv2RoutingModule routingModule = this.getRouterController().getModule(RIPv2RoutingModule.class);
            IPv4Prefix prefix = IPv4Prefix.fromString(request.getParameter("IPv4Prefix"));
            routingModule.getNetworkPrefixList().add(prefix);
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            this.getRouterController().onConfigurationChanged();
            responseJob.add("Success", true);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to acquire RIPv2 routing module");
        } catch (IPv4MathException ex) {
            throw new APIMethodUserException(ex.getMessage());
        }
    }
}
