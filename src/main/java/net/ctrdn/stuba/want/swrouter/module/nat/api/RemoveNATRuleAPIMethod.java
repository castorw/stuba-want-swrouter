package net.ctrdn.stuba.want.swrouter.module.nat.api;

import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodUserException;
import net.ctrdn.stuba.want.swrouter.exception.NATException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.nat.NATModule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATRule;

public class RemoveNATRuleAPIMethod extends DefaultAPIMethod {

    public RemoveNATRuleAPIMethod(RouterController routerController) {
        super(routerController, "remove-nat-rule");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            NATModule natModule = this.getRouterController().getModule(NATModule.class);
            UUID ruleId = UUID.fromString(request.getParameter("ID"));
            NATRule foundRule = null;
            for (NATRule rule : natModule.getRuleList()) {
                if (rule.getID().equals(ruleId)) {
                    foundRule = rule;
                    break;
                }
            }
            if (foundRule == null) {
                throw new APIMethodUserException("Rule not found");
            }
            natModule.uninstallNATRule(foundRule);
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            responseJob.add("Success", true);
            this.getRouterController().onConfigurationChanged();
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to obtain NAT module");
        } catch (NATException ex) {
            throw new APIMethodUserException(ex.getMessage());
        }
    }

}
