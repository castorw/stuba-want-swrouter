package net.ctrdn.stuba.want.swrouter.module.routingripv2.api;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.InterfaceManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import net.ctrdn.stuba.want.swrouter.module.routingripv2.RIPv2NetworkInterfaceConfiguration;
import net.ctrdn.stuba.want.swrouter.module.routingripv2.RIPv2RoutingModule;

public class GetRIPv2InterfacesAPIMethod extends DefaultAPIMethod {

    public GetRIPv2InterfacesAPIMethod(RouterController routerController) {
        super(routerController, "get-ripv2-interfaces");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            RIPv2RoutingModule routingModule = this.getRouterController().getModule(RIPv2RoutingModule.class);
            InterfaceManagerModule interfaceManagerModule = this.getRouterController().getModule(InterfaceManagerModule.class);
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonArrayBuilder interfacesJab = Json.createArrayBuilder();

            for (NetworkInterface iface : interfaceManagerModule.getNetworkInterfaces()) {
                RIPv2NetworkInterfaceConfiguration ifaceConfig = routingModule.getNetworkInterfaceConfiguration(iface);
                JsonObjectBuilder ifaceJob = Json.createObjectBuilder();
                ifaceJob.add("Name", iface.getName());
                ifaceJob.add("Enabled", ifaceConfig.isEnabled());
                interfacesJab.add(ifaceJob);
            }
            responseJob.add("RIPv2Interfaces", interfacesJab);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to acquire RIPv2 routing module");
        }
    }
}
