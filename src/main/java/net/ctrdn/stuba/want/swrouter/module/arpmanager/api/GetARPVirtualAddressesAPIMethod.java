package net.ctrdn.stuba.want.swrouter.module.arpmanager.api;

import java.util.Map;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.arpmanager.ARPManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;

public class GetARPVirtualAddressesAPIMethod extends DefaultAPIMethod {

    public GetARPVirtualAddressesAPIMethod(RouterController routerController) {
        super(routerController, "get-arp-virtual-addresses");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonArrayBuilder addressesJab = Json.createArrayBuilder();
            ARPManagerModule arpmm = this.getRouterController().getModule(ARPManagerModule.class);
            for (Map.Entry<IPv4Address, NetworkInterface> entry : arpmm.getVirtualAddressMap().entrySet()) {
                JsonObjectBuilder entryJob = Json.createObjectBuilder();
                entryJob.add("IPv4Address", entry.getKey().toString());
                entryJob.add("NetworkInterface", entry.getValue().getName());
                addressesJab.add(entryJob);
            }
            responseJob.add("ARPVirtualAddresses", addressesJab);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Unable to access arp manager module");
        }
    }

}
