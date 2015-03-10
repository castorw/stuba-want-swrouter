package net.ctrdn.stuba.want.swrouter.core.api;

import java.util.Date;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.InterfaceManagerModule;

public class GetSystemInformationAPIMethod extends DefaultAPIMethod {

    public GetSystemInformationAPIMethod(RouterController routerController) {
        super(routerController, "get-system-information");
    }

    @Override
    public JsonObjectBuilder execute(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            responseJob.add("Hostname", this.getRouterController().getHostname());
            responseJob.add("BootDate", this.getRouterController().getBootDate().toString());
            responseJob.add("BootFinishDate", this.getRouterController().getBootFinishDate().toString());
            responseJob.add("BootTime", this.getRouterController().getBootFinishDate().getTime() - this.getRouterController().getBootDate().getTime());
            responseJob.add("Uptime", new Date().getTime() - this.getRouterController().getBootDate().getTime());
            responseJob.add("ModuleCount", this.getRouterController().getModuleClasses().length);
            responseJob.add("PipelineBranchCount", this.getRouterController().getPacketProcessor().getPipelineBranches().length);
            responseJob.add("InterfaceCount", this.getRouterController().getModule(InterfaceManagerModule.class).getNetworkInterfaces().length);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Internal error - " + ex.getMessage());
        }
    }
}
