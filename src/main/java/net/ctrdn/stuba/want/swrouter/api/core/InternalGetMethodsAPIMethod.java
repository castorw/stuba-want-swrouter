package net.ctrdn.stuba.want.swrouter.api.core;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.APIMethodRegistry;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;

public class InternalGetMethodsAPIMethod extends DefaultAPIMethod {

    public InternalGetMethodsAPIMethod(RouterController routerController) {
        super(routerController, "internal.get-methods");
    }

    @Override
    public JsonObjectBuilder execute(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        JsonObjectBuilder responseJob = Json.createObjectBuilder();
        JsonArrayBuilder listJab = Json.createArrayBuilder();
        for (String methodName : APIMethodRegistry.getInstance().getMethodNames()) {
            listJab.add(methodName);
        }
        responseJob.add("Methods", listJab);
        return responseJob;
    }
}
