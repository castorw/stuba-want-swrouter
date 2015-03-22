package net.ctrdn.stuba.want.swrouter.module.nat.api;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodUserException;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.nat.NATModule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATPool;

public class AddNATPoolAPIMethod extends DefaultAPIMethod {

    public AddNATPoolAPIMethod(RouterController routerController) {
        super(routerController, "add-nat-pool");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            NATModule natModule = this.getRouterController().getModule(NATModule.class);
            String name = request.getParameter("Name");
            IPv4Prefix prefix = IPv4Prefix.fromString(request.getParameter("Prefix"));
            NATPool pool = new NATPool(natModule, name, prefix);
            natModule.addNATPool(pool);
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            responseJob.add("Success", true);
            this.getRouterController().onConfigurationChanged();
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to obtain NAT module");
        } catch (IPv4MathException ex) {
            throw new APIMethodUserException(ex.getMessage());
        }
    }
}
