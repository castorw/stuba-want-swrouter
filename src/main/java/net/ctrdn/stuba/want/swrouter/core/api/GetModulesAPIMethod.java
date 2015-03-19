package net.ctrdn.stuba.want.swrouter.core.api;

import java.util.Collections;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.core.RouterModule;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;

public class GetModulesAPIMethod extends DefaultAPIMethod {

    public GetModulesAPIMethod(RouterController routerController) {
        super(routerController, "get-modules");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        JsonObjectBuilder responseJob = Json.createObjectBuilder();
        JsonArrayBuilder modulesJab = Json.createArrayBuilder();
        try {
            for (Class<? extends RouterModule> moduleClass : this.getRouterController().getModuleClasses()) {
                RouterModule module = this.getRouterController().getModule(moduleClass);
                JsonObjectBuilder moduleJob = Json.createObjectBuilder();
                moduleJob.add("Classpath", moduleClass.getName());
                moduleJob.add("Name", module.getName());
                moduleJob.add("Revision", module.getRevision());
                moduleJob.add("LoadPriority", module.getLoadPriority());
                modulesJab.add(moduleJob);
            }
            responseJob.add("Modules", modulesJab);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Internal error - " + ex.getMessage());
        }
    }

}
