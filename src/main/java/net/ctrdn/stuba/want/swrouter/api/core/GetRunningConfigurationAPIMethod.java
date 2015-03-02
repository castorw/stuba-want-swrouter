package net.ctrdn.stuba.want.swrouter.api.core;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;

public class GetRunningConfigurationAPIMethod extends DefaultAPIMethod {

    public GetRunningConfigurationAPIMethod(RouterController routerController) {
        super(routerController, "get-running-configuration");
    }

    @Override
    public JsonObjectBuilder execute(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        JsonObjectBuilder responseJob = Json.createObjectBuilder();
        responseJob.add("RunningConfiguration", this.getRouterController().getRunningConfiguration());
        return responseJob;
    }

}
