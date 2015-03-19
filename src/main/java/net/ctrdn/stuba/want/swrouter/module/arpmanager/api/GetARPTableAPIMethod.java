package net.ctrdn.stuba.want.swrouter.module.arpmanager.api;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.arpmanager.ARPManagerModule;
import net.ctrdn.stuba.want.swrouter.module.arpmanager.ARPTableEntry;

public class GetARPTableAPIMethod extends DefaultAPIMethod {

    public GetARPTableAPIMethod(RouterController routerController) {
        super(routerController, "get-arp-table");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonArrayBuilder tableJab = Json.createArrayBuilder();
            ARPManagerModule arpmm = this.getRouterController().getModule(ARPManagerModule.class);
            for (ARPTableEntry entry : arpmm.getEntries()) {
                JsonObjectBuilder entryJob = Json.createObjectBuilder();
                entryJob.add("IPv4Address", entry.getProtocolAddress().toString());
                entryJob.add("HardwareAddress", entry.getHardwareAddress().toString());
                entryJob.add("NetworkInterface", entry.getNetworkInterface().getName());
                tableJab.add(entryJob);
            }
            responseJob.add("ARPTable", tableJab);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Unable to access arp manager module");
        }
    }

}
