package net.ctrdn.stuba.want.swrouter.module.routingripv2.api;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.routingripv2.RIPv2RoutingModule;

public class GetRIPv2NetworksAPIMethod extends DefaultAPIMethod {

    public GetRIPv2NetworksAPIMethod(RouterController routerController) {
        super(routerController, "get-ripv2-networks");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            RIPv2RoutingModule routingModule = this.getRouterController().getModule(RIPv2RoutingModule.class);
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonArrayBuilder networksJab = Json.createArrayBuilder();

            for (IPv4Prefix prefix : routingModule.getNetworkPrefixList()) {
                networksJab.add(prefix.toString());
            }
            responseJob.add("RIPv2Networks", networksJab);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to acquire RIPv2 routing module");
        }
    }
}
