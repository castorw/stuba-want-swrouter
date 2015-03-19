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

public class GetARPConfigurationAPIMethod extends DefaultAPIMethod {

    public GetARPConfigurationAPIMethod(RouterController routerController) {
        super(routerController, "get-arp-configuration");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonObjectBuilder arpConfigJob = Json.createObjectBuilder();
            ARPManagerModule arpmm = this.getRouterController().getModule(ARPManagerModule.class);
            arpConfigJob.add("EntryTimeout", arpmm.getEntryTimeout());
            arpConfigJob.add("PipelineResolutionTimeout", arpmm.getPipelineResolutionTimeout());
            responseJob.add("ARPConfiguration", arpConfigJob);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Unable to access arp manager module");
        }
    }

}
