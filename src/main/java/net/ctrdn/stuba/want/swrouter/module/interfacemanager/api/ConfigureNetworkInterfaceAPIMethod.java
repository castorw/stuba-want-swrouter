package net.ctrdn.stuba.want.swrouter.module.interfacemanager.api;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4InterfaceAddress;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4NetworkMask;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodUserException;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.InterfaceManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;

public class ConfigureNetworkInterfaceAPIMethod extends DefaultAPIMethod {

    public ConfigureNetworkInterfaceAPIMethod(RouterController routerController) {
        super(routerController, "configure-network-interface");
    }

    @Override
    public JsonObjectBuilder execute(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            NetworkInterface nic = this.getRouterController().getModule(InterfaceManagerModule.class).getNetworkInterfaceByName(request.getParameter("InterfaceName"));
            if (request.getParameter("IPv4Address") != null) {
                IPv4Address address = IPv4Address.fromString(request.getParameter("IPv4Address"));
                IPv4NetworkMask netmask = new IPv4NetworkMask(Integer.parseInt(request.getParameter("IPv4NetworkMask")));
                nic.setIPv4InterfaceAddress(new IPv4InterfaceAddress(address, netmask));
                this.getRouterController().onConfigurationChanged();
            }
            if (request.getParameter("Enabled") != null) {
                boolean enabled;
                if (request.getParameter("Enabled").equals("toggle")) {
                    enabled = !nic.isEnabled();
                } else {
                    enabled = Boolean.parseBoolean(request.getParameter("Enabled"));
                }
                nic.setEnabled(enabled);
                this.getRouterController().onConfigurationChanged();
            }
            responseJob.add("Success", true);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to access interface manager module");
        } catch (IPv4MathException ex) {
            throw new APIMethodUserException(ex.getMessage());
        }
    }

}
