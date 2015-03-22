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
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.InterfaceManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import net.ctrdn.stuba.want.swrouter.module.nat.NATModule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATRule;

public class ConfigureNATRuleAPIMethod extends DefaultAPIMethod {

    public ConfigureNATRuleAPIMethod(RouterController routerController) {
        super(routerController, "configure-nat-rule");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            NATModule natModule = this.getRouterController().getModule(NATModule.class);
            InterfaceManagerModule interfaceManagetModule = this.getRouterController().getModule(InterfaceManagerModule.class);
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
            if (request.getParameter("Priority") != null) {
                Integer newPriority = Integer.parseInt(request.getParameter("Priority"));
                foundRule.setPriority(newPriority);
                natModule.sortNATRules();
                this.getRouterController().onConfigurationChanged();
            }

            if (request.getParameter("ECMPOutsideInterfaces") != null) {
                foundRule.getEcmpOutsideInterfaceList().clear();
                String[] ifaceStringArray = request.getParameter("ECMPOutsideInterfaces").split("\n");
                for (String ifaceString : ifaceStringArray) {
                    NetworkInterface iface = interfaceManagetModule.getNetworkInterfaceByName(ifaceString);
                    if (iface != null) {
                        foundRule.getEcmpOutsideInterfaceList().add(iface);
                    }
                }
                this.getRouterController().onConfigurationChanged();
            }
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            responseJob.add("Success", true);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to obtain required module");
        } catch (NumberFormatException ex) {
            throw new APIMethodUserException(ex.getMessage());
        }
    }

}
