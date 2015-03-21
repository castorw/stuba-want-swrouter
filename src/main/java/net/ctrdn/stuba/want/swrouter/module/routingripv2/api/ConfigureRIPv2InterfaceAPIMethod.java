package net.ctrdn.stuba.want.swrouter.module.routingripv2.api;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodUserException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.InterfaceManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import net.ctrdn.stuba.want.swrouter.module.routingripv2.RIPv2NetworkInterfaceConfiguration;
import net.ctrdn.stuba.want.swrouter.module.routingripv2.RIPv2RoutingModule;

public class ConfigureRIPv2InterfaceAPIMethod extends DefaultAPIMethod {

    public ConfigureRIPv2InterfaceAPIMethod(RouterController routerController) {
        super(routerController, "configure-ripv2-interface");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            RIPv2RoutingModule routingModule = this.getRouterController().getModule(RIPv2RoutingModule.class);
            InterfaceManagerModule interfaceManagerModule = this.getRouterController().getModule(InterfaceManagerModule.class);
            if (request.getParameter("InterfaceName") == null) {
                throw new APIMethodUserException("No interface provided");
            }
            NetworkInterface foundInterface = null;
            for (NetworkInterface iface : interfaceManagerModule.getNetworkInterfaces()) {
                if (iface.getName().equals(request.getParameter("InterfaceName"))) {
                    foundInterface = iface;
                    break;
                }
            }
            if (foundInterface == null) {
                throw new APIMethodUserException("Interface not found");
            }
            RIPv2NetworkInterfaceConfiguration ifaceConfig = routingModule.getNetworkInterfaceConfiguration(foundInterface);
            if (request.getParameter("Enabled") != null) {
                if (request.getParameter("Enabled").equals("toggle")) {
                    ifaceConfig.setEnabled(!ifaceConfig.isEnabled());
                } else {
                    ifaceConfig.setEnabled(Boolean.parseBoolean(request.getParameter("Enabled")));
                }
                this.getRouterController().onConfigurationChanged();
            }
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            responseJob.add("Success", true);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to acquire RIPv2 routing module");
        }
    }
}
