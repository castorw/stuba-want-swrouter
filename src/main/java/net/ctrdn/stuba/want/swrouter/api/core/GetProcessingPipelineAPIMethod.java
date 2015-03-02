package net.ctrdn.stuba.want.swrouter.api.core;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.core.processing.PipelineBranch;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;

public class GetProcessingPipelineAPIMethod extends DefaultAPIMethod {

    public GetProcessingPipelineAPIMethod(RouterController routerController) {
        super(routerController, "get-processing-pipeline");
    }

    @Override
    public JsonObjectBuilder execute(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        JsonObjectBuilder responseJob = Json.createObjectBuilder();
        JsonArrayBuilder branchJab = Json.createArrayBuilder();

        for (PipelineBranch branch : this.getRouterController().getPacketProcessor().getPipelineBranches()) {
            JsonObjectBuilder branchJob = Json.createObjectBuilder();
            branchJob.add("Classpath", branch.getClass().getName());
            branchJob.add("Name", branch.getName());
            branchJob.add("Description", branch.getDescription());
            branchJob.add("Priority", branch.getPriority());
            branchJob.add("Enabled", branch.isEnabled());
            branchJob.add("InstallerClass", this.getRouterController().getPacketProcessor().getPipelineBranchInstallerClass(branch));
            branchJab.add(branchJob);
        }
        responseJob.add("PipelineBranches", branchJab);
        return responseJob;
    }

}
