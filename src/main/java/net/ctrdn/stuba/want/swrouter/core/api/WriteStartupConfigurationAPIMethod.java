package net.ctrdn.stuba.want.swrouter.core.api;

import java.util.Date;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;

public class WriteStartupConfigurationAPIMethod extends DefaultAPIMethod {

    public WriteStartupConfigurationAPIMethod(RouterController routerController) {
        super(routerController, "write-startup-configuration");
    }

    @Override
    public JsonObjectBuilder execute(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        JsonObjectBuilder responseJob = Json.createObjectBuilder();
        Date sd = new Date();
        this.getRouterController().writeConfiguration();
        Date ed = new Date();
        responseJob.add("Time", ed.getTime() - sd.getTime());
        return responseJob;
    }

}
