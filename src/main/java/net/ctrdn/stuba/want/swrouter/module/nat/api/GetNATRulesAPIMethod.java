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
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import net.ctrdn.stuba.want.swrouter.module.nat.NATModule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATRule;
import net.ctrdn.stuba.want.swrouter.module.nat.rule.DNATRule;
import net.ctrdn.stuba.want.swrouter.module.nat.rule.SNATInterfaceRule;
import net.ctrdn.stuba.want.swrouter.module.nat.rule.SNATPoolRule;

public class GetNATRulesAPIMethod extends DefaultAPIMethod {

    public GetNATRulesAPIMethod(RouterController routerController) {
        super(routerController, "get-nat-rules");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            NATModule natModule = this.getRouterController().getModule(NATModule.class);
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonArrayBuilder rulesJab = Json.createArrayBuilder();
            for (NATRule rule : natModule.getRuleList()) {
                JsonObjectBuilder ruleJob = Json.createObjectBuilder();
                ruleJob.add("ID", rule.getID().toString());
                ruleJob.add("Priority", rule.getPriority());
                ruleJob.add("Type", rule.getTypeString());
                ruleJob.add("Summary", rule.toString());

                JsonObjectBuilder ruleConfigurationJob = Json.createObjectBuilder();
                switch (rule.getTypeString()) {
                    case "SNAT_INTERFACE": {
                        SNATInterfaceRule ruleCast = (SNATInterfaceRule) rule;
                        ruleConfigurationJob.add("InsidePrefix", ruleCast.getInsidePrefix().toString());
                        ruleConfigurationJob.add("OutsideInterface", ruleCast.getOutsideInterface().getName());

                        if (ruleCast.getEcmpOutsideInterfaceList().isEmpty()) {
                            ruleConfigurationJob.addNull("ECMPOutsideInterfaces");
                        } else {
                            JsonArrayBuilder ecmpOutsideInterfaceJab = Json.createArrayBuilder();
                            for (NetworkInterface iface : ruleCast.getEcmpOutsideInterfaceList()) {
                                ecmpOutsideInterfaceJab.add(iface.getName());
                            }
                            ruleConfigurationJob.add("ECMPOutsideInterfaces", ecmpOutsideInterfaceJab);
                        }
                        break;
                    }
                    case "SNAT_POOL": {
                        SNATPoolRule ruleCast = (SNATPoolRule) rule;
                        ruleConfigurationJob.add("InsidePrefix", ruleCast.getInsidePrefix().toString());
                        ruleConfigurationJob.add("OutsidePool", ruleCast.getOutsidePool().getName());
                        ruleConfigurationJob.add("OverloadEnabled", ruleCast.isOverloadEnabled());

                        if (ruleCast.getEcmpOutsideInterfaceList().isEmpty()) {
                            ruleConfigurationJob.addNull("ECMPOutsideInterfaces");
                        } else {
                            JsonArrayBuilder ecmpOutsideInterfaceJab = Json.createArrayBuilder();
                            for (NetworkInterface iface : ruleCast.getEcmpOutsideInterfaceList()) {
                                ecmpOutsideInterfaceJab.add(iface.getName());
                            }
                            ruleConfigurationJob.add("ECMPOutsideInterfaces", ecmpOutsideInterfaceJab);
                        }
                        break;
                    }
                    case "DNAT": {
                        DNATRule ruleCast = (DNATRule) rule;
                        ruleConfigurationJob.add("InsideAddress", ruleCast.getInsideAddress().toString());
                        ruleConfigurationJob.add("OutsideAddress", ruleCast.getOutsideAddress().getAddress().toString());

                        if (ruleCast.getProtocol() == null) {
                            ruleConfigurationJob.addNull("ServiceConstraints");
                        } else {
                            JsonObjectBuilder serviceConstraintsJob = Json.createObjectBuilder();
                            serviceConstraintsJob.add("Protocol", ruleCast.getProtocol().name());
                            if (ruleCast.getInsideProtocolSpecificIdentifier() != null && ruleCast.getOutsideProtocolSpecificIdentifier() != null) {
                                serviceConstraintsJob.add("InsidePort", ruleCast.getInsideProtocolSpecificIdentifier());
                                serviceConstraintsJob.add("OutsidePort", ruleCast.getOutsideProtocolSpecificIdentifier());
                            } else {
                                serviceConstraintsJob.addNull("InsidePort");
                                serviceConstraintsJob.addNull("OutsidePort");
                            }
                            ruleConfigurationJob.add("ServiceConstraints", serviceConstraintsJob);
                        }

                        if (ruleCast.getEcmpOutsideInterfaceList().isEmpty()) {
                            ruleConfigurationJob.addNull("ECMPOutsideInterfaces");
                        } else {
                            JsonArrayBuilder ecmpOutsideInterfaceJab = Json.createArrayBuilder();
                            for (NetworkInterface iface : ruleCast.getEcmpOutsideInterfaceList()) {
                                ecmpOutsideInterfaceJab.add(iface.getName());
                            }
                            ruleConfigurationJob.add("ECMPOutsideInterfaces", ecmpOutsideInterfaceJab);
                        }
                        break;
                    }
                }

                ruleJob.add("Configuration", ruleConfigurationJob);
                rulesJab.add(ruleJob);
            }
            responseJob.add("NATRules", rulesJab);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to obtain NAT module");
        }
    }

}
