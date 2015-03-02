package net.ctrdn.stuba.want.swrouter.api.core;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;

public class GetStartupConfigurationAPIMethod extends DefaultAPIMethod {

    public GetStartupConfigurationAPIMethod(RouterController routerController) {
        super(routerController, "get-startup-configuration");
    }

    @Override
    public JsonObjectBuilder execute(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        JsonObjectBuilder responseJob = Json.createObjectBuilder();
        responseJob.add("StartupConfiguration", this.getRouterController().getStartupConfiguration());
        return responseJob;
    }

}
