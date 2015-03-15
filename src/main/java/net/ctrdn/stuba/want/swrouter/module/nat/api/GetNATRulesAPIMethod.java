package net.ctrdn.stuba.want.swrouter.module.nat.api;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.nat.NATModule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATRule;

public class GetNATRulesAPIMethod extends DefaultAPIMethod {

    public GetNATRulesAPIMethod(RouterController routerController) {
        super(routerController, "get-nat-rules");
    }

    @Override
    public JsonObjectBuilder execute(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            NATModule natModule = this.getRouterController().getModule(NATModule.class);
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonArrayBuilder rulesJab = Json.createArrayBuilder();
            for (NATRule rule : natModule.getRuleList()) {
                JsonObjectBuilder ruleJob = Json.createObjectBuilder();
                ruleJob.add("Priority", rule.getPriority());
                ruleJob.add("Type", rule.getTypeString());
                ruleJob.add("Rule", rule.toString());
                rulesJab.add(ruleJob);
            }
            responseJob.add("NATRules", rulesJab);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to obtain NAT module");
        }
    }

}
