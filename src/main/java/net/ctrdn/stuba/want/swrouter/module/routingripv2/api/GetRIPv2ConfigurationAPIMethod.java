package net.ctrdn.stuba.want.swrouter.module.routingripv2.api;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.routingripv2.RIPv2RoutingModule;

public class GetRIPv2ConfigurationAPIMethod extends DefaultAPIMethod {

    public GetRIPv2ConfigurationAPIMethod(RouterController routerController) {
        super(routerController, "get-ripv2-configuration");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            RIPv2RoutingModule routingModule = this.getRouterController().getModule(RIPv2RoutingModule.class);
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonObjectBuilder ripv2ConfigJob = Json.createObjectBuilder();
            ripv2ConfigJob.add("UpdateInterval", routingModule.getUpdateInterval());
            ripv2ConfigJob.add("HoldDownTimeout", routingModule.getHoldDownTimeout());
            ripv2ConfigJob.add("FlushTimeout", routingModule.getFlushTimeout());
            responseJob.add("RIPv2Configuration", ripv2ConfigJob);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to acquire RIPv2 routing module");
        }
    }

}
