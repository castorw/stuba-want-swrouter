package net.ctrdn.stuba.want.swrouter.module.interfacemanager.api;

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

public class GetNetworkInterfacesAPIMethod extends DefaultAPIMethod {

    public GetNetworkInterfacesAPIMethod(RouterController routerController) {
        super(routerController, "get-network-interfaces");
    }

    @Override
    public JsonObjectBuilder execute(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonArrayBuilder interfaceJab = Json.createArrayBuilder();
            for (NetworkInterface nic : this.getRouterController().getModule(InterfaceManagerModule.class).getNetworkInterfaces()) {
                JsonObjectBuilder nicJob = Json.createObjectBuilder();
                nicJob.add("Name", nic.getName());
                nicJob.add("MTU", nic.getMTU());
                nicJob.add("HardwareAddress", nic.getHardwareAddress().toString());
                if (nic.getIPv4InterfaceAddress() == null) {
                    nicJob.addNull("IPv4Address");
                    nicJob.addNull("IPv4NetworkMask");
                } else {
                    nicJob.add("IPv4Address", nic.getIPv4InterfaceAddress().getAddress().toString());
                    nicJob.add("IPv4NetworkMask", nic.getIPv4InterfaceAddress().getPrefix().getNetworkMask().getLength());
                }
                nicJob.add("Enabled", nic.isEnabled());
                interfaceJab.add(nicJob);
            }
            responseJob.add("NetworkInterfaces", interfaceJab);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to access interface manager module");
        }
    }

}
