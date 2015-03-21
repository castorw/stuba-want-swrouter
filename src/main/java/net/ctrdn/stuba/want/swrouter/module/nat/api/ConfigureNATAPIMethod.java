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

public class ConfigureNATAPIMethod extends DefaultAPIMethod {

    public ConfigureNATAPIMethod(RouterController routerController) {
        super(routerController, "configure-nat");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            NATModule natModule = this.getRouterController().getModule(NATModule.class);
            if (request.getParameter("AddressTranslationTimeout") != null) {
                String addressTranslationTimeout = request.getParameter("AddressTranslationTimeout").toLowerCase().trim();
                if (addressTranslationTimeout.trim().endsWith("ms")) {
                    addressTranslationTimeout = addressTranslationTimeout.substring(0, addressTranslationTimeout.length() - 2);
                }
                natModule.setAddressTranslationTimeout(Integer.parseInt(addressTranslationTimeout));
                this.getRouterController().onConfigurationChanged();
            }
            if (request.getParameter("PortTranslationTimeout") != null) {
                String portTranslationTimeout = request.getParameter("PortTranslationTimeout").toLowerCase().trim();
                if (portTranslationTimeout.trim().endsWith("ms")) {
                    portTranslationTimeout = portTranslationTimeout.substring(0, portTranslationTimeout.length() - 2);
                }
                natModule.setPortTranslationTimeout(Integer.parseInt(portTranslationTimeout));
                this.getRouterController().onConfigurationChanged();
            }
            if (request.getParameter("TranslationHoldDownTimeout") != null) {
                String translationHoldDownTimeout = request.getParameter("TranslationHoldDownTimeout").toLowerCase().trim();
                if (translationHoldDownTimeout.trim().endsWith("ms")) {
                    translationHoldDownTimeout = translationHoldDownTimeout.substring(0, translationHoldDownTimeout.length() - 2);
                }
                natModule.setTranslationHoldDownTimeout(Integer.parseInt(translationHoldDownTimeout));
                this.getRouterController().onConfigurationChanged();
            }
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            responseJob.add("Success", true);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to obtain NAT module");
        }
    }

}
