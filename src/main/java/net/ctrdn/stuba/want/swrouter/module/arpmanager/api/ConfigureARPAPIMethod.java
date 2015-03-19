package net.ctrdn.stuba.want.swrouter.module.arpmanager.api;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.arpmanager.ARPManagerModule;

public class ConfigureARPAPIMethod extends DefaultAPIMethod {

    public ConfigureARPAPIMethod(RouterController routerController) {
        super(routerController, "configure-arp");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            ARPManagerModule arpmm = this.getRouterController().getModule(ARPManagerModule.class);
            if (request.getParameter("EntryTimeout") != null) {
                String entryTimeout = request.getParameter("EntryTimeout").toLowerCase().trim();
                if (entryTimeout.trim().endsWith("ms")) {
                    entryTimeout = entryTimeout.substring(0, entryTimeout.length() - 2);
                }
                arpmm.setEntryTimeout(Integer.parseInt(entryTimeout));
                this.getRouterController().onConfigurationChanged();
            }
            if (request.getParameter("PipelineResolutionTimeout") != null) {
                String pipelineResolutionTimeout = request.getParameter("PipelineResolutionTimeout").toLowerCase().trim();
                if (pipelineResolutionTimeout.trim().endsWith("ms")) {
                    pipelineResolutionTimeout = pipelineResolutionTimeout.substring(0, pipelineResolutionTimeout.length() - 2);
                }
                arpmm.setPipelineResolutionTimeout(Integer.parseInt(pipelineResolutionTimeout));
                this.getRouterController().onConfigurationChanged();
            }
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            responseJob.add("Success", true);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Unable to access arp manager module");
        }
    }

}
