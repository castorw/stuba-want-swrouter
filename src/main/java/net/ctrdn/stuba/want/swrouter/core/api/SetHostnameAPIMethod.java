package net.ctrdn.stuba.want.swrouter.core.api;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;

public class SetHostnameAPIMethod extends DefaultAPIMethod {

    public SetHostnameAPIMethod(RouterController routerController) {
        super(routerController, "set-hostname");
    }

    @Override
    public JsonObjectBuilder execute(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        String newHostname = request.getParameter("hostname");
        this.getRouterController().setHostname(newHostname);
        JsonObjectBuilder responseJob = Json.createObjectBuilder();
        responseJob.add("Success", true);
        this.getRouterController().onConfigurationChanged();
        return responseJob;
    }

}
