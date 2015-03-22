package net.ctrdn.stuba.want.swrouter.module.interfacemanager.api;

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

public class ResetNetworkInterfaceStatsAPIMethod extends DefaultAPIMethod {

    public ResetNetworkInterfaceStatsAPIMethod(RouterController routerController) {
        super(routerController, "reset-network-interface-stats");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            InterfaceManagerModule imm = this.getRouterController().getModule(InterfaceManagerModule.class);
            String ifaceName = request.getParameter("InterfaceName");
            if (ifaceName == null || ifaceName.trim().isEmpty()) {
                for (NetworkInterface iface : imm.getNetworkInterfaces()) {
                    iface.resetStats();
                }
            } else {
                boolean found = false;
                for (NetworkInterface iface : imm.getNetworkInterfaces()) {
                    if (iface.getName().equals(ifaceName)) {
                        iface.resetStats();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new APIMethodUserException("Interface not found");
                }
            }
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            responseJob.add("Success", true);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to acquire required module");
        }
    }

}
