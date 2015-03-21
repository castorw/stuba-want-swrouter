package net.ctrdn.stuba.want.swrouter.module.nat.api;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.nat.NATModule;

public class GetNATConfigurationAPIMethod extends DefaultAPIMethod {

    public GetNATConfigurationAPIMethod(RouterController routerController) {
        super(routerController, "get-nat-configuration");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            NATModule natModule = this.getRouterController().getModule(NATModule.class);
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonObjectBuilder configurationJob = Json.createObjectBuilder();
            configurationJob.add("AddressTranslationTimeout", natModule.getAddressTranslationTimeout());
            configurationJob.add("PortTranslationTimeout", natModule.getPortTranslationTimeout());
            configurationJob.add("TranslationHoldDownTimeout", natModule.getTranslationHoldDownTimeout());
            responseJob.add("NATConfiguration", configurationJob);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to obtain NAT module");
        }
    }

}
