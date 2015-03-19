package net.ctrdn.stuba.want.swrouter.core.api;

import java.util.Date;
import javax.json.Json;
import javax.json.JsonObject;
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
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        JsonObjectBuilder responseJob = Json.createObjectBuilder();
        responseJob.add("RunningConfiguration", this.getRouterController().getRunningConfiguration());
        return responseJob;
    }

    @Override
    public JsonObject executeGet(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        response.setHeader("Content-disposition", "attachment; filename=\"swrouter-config-" + new Date().getTime() + ".conf.json\"");
        return this.getRouterController().getRunningConfiguration();
    }

}
