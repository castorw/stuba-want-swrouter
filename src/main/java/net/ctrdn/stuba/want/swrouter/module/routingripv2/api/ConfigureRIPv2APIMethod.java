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
import net.ctrdn.stuba.want.swrouter.module.routingripv2.RIPv2RoutingModule;

public class ConfigureRIPv2APIMethod extends DefaultAPIMethod {

    public ConfigureRIPv2APIMethod(RouterController routerController) {
        super(routerController, "configure-ripv2");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            RIPv2RoutingModule routingModule = this.getRouterController().getModule(RIPv2RoutingModule.class);
            if (request.getParameter("UpdateInterval") != null) {
                String updateInterval = request.getParameter("UpdateInterval").toLowerCase().trim();
                if (updateInterval.trim().endsWith("ms")) {
                    updateInterval = updateInterval.substring(0, updateInterval.length() - 2);
                }
                routingModule.setUpdateInterval(Integer.parseInt(updateInterval));
                this.getRouterController().onConfigurationChanged();
            }
            if (request.getParameter("HoldDownTimeout") != null) {
                String holdDownTimeout = request.getParameter("HoldDownTimeout").toLowerCase().trim();
                if (holdDownTimeout.trim().endsWith("ms")) {
                    holdDownTimeout = holdDownTimeout.substring(0, holdDownTimeout.length() - 2);
                }
                routingModule.setHoldDownTimeout(Integer.parseInt(holdDownTimeout));
                this.getRouterController().onConfigurationChanged();
            }
            if (request.getParameter("FlushTimeout") != null) {
                String flushTimeout = request.getParameter("FlushTimeout").toLowerCase().trim();
                if (flushTimeout.trim().endsWith("ms")) {
                    flushTimeout = flushTimeout.substring(0, flushTimeout.length() - 2);
                }
                routingModule.setFlushTimeout(Integer.parseInt(flushTimeout));
                this.getRouterController().onConfigurationChanged();
            }
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            responseJob.add("Success", true);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to acquire RIPv2 routing module");
        } catch (NumberFormatException ex) {
            throw new APIMethodUserException("Invalid value provided");
        }
    }
}
