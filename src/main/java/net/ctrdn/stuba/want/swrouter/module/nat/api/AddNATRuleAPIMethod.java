package net.ctrdn.stuba.want.swrouter.module.nat.api;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.common.IPv4Protocol;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Prefix;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodUserException;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.NATException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.InterfaceManagerModule;
import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;
import net.ctrdn.stuba.want.swrouter.module.nat.NATModule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATPool;
import net.ctrdn.stuba.want.swrouter.module.nat.rule.DNATRule;
import net.ctrdn.stuba.want.swrouter.module.nat.rule.SNATInterfaceRule;
import net.ctrdn.stuba.want.swrouter.module.nat.rule.SNATPoolRule;

public class AddNATRuleAPIMethod extends DefaultAPIMethod {

    public AddNATRuleAPIMethod(RouterController routerController) {
        super(routerController, "add-nat-rule");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            NATModule natModule = this.getRouterController().getModule(NATModule.class);
            InterfaceManagerModule interfaceManagetModule = this.getRouterController().getModule(InterfaceManagerModule.class);
            String type = this.parseInputParameter(request, "Type");
            Integer priority = Integer.parseInt(this.parseInputParameter(request, "Priority"));

            String insideAddressString = this.parseInputParameter(request, "InsideAddress");
            String outsideAddressString = this.parseInputParameter(request, "OutsideAddress");
            String insidePrefixString = this.parseInputParameter(request, "InsidePrefix");
            String outsideInterfaceString = this.parseInputParameter(request, "OutsideInterface");
            String outsidePoolString = this.parseInputParameter(request, "OutsidePool");
            String overloadEnabledString = this.parseInputParameter(request, "OverloadEnabled");
            String protocolString = this.parseInputParameter(request, "Protocol");
            String insideProtocolSpecificIdentifierString = this.parseInputParameter(request, "InsideProtocolSpecificIdentifier");
            String outsideProtocolSpecificIdentifierString = this.parseInputParameter(request, "OutsideProtocolSpecificIdentifier");

            switch (type) {
                case "SNAT_INTERFACE": {
                    IPv4Prefix insidePrefix = IPv4Prefix.fromString(insidePrefixString);
                    NetworkInterface outsideInterface = interfaceManagetModule.getNetworkInterfaceByName(outsideInterfaceString);
                    if (outsideInterface == null) {
                        throw new APIMethodUserException("Interface does not exist");
                    }
                    SNATInterfaceRule rule = new SNATInterfaceRule(natModule, priority, insidePrefix, outsideInterface);
                    natModule.installNATRule(rule);
                    break;
                }
                case "SNAT_POOL": {
                    IPv4Prefix insidePrefix = IPv4Prefix.fromString(insidePrefixString);
                    NATPool outsidePool = natModule.getNATPool(outsidePoolString);
                    if (outsidePool == null) {
                        throw new APIMethodUserException("Pool does not exist");
                    }
                    boolean overloadEnabled = Boolean.parseBoolean(overloadEnabledString);
                    SNATPoolRule rule = new SNATPoolRule(natModule, priority, insidePrefix, outsidePool, overloadEnabled);
                    natModule.installNATRule(rule);
                    break;
                }
                case "DNAT": {
                    IPv4Address insideAddress = IPv4Address.fromString(insideAddressString);
                    IPv4Address outsideAddress = IPv4Address.fromString(outsideAddressString);
                    IPv4Protocol protocol = null;
                    Integer insideProtocolSpecificIdentifier = null;
                    Integer outsideProtocolSpecificIdentifier = null;
                    if (!protocolString.trim().isEmpty()) {
                        protocol = IPv4Protocol.valueOf(protocolString);
                        if (protocol == IPv4Protocol.UNKNOWN) {
                            throw new APIMethodUserException("Unknown IPv4 protocol");
                        }
                        if (protocol == IPv4Protocol.TCP || protocol == IPv4Protocol.UDP) {
                            insideProtocolSpecificIdentifier = Integer.parseInt(insideProtocolSpecificIdentifierString);
                            outsideProtocolSpecificIdentifier = Integer.parseInt(outsideProtocolSpecificIdentifierString);
                        }
                    }
                    DNATRule rule = new DNATRule(natModule, priority, natModule.getNATAddress(outsideAddress), insideAddress, protocol, outsideProtocolSpecificIdentifier, insideProtocolSpecificIdentifier);
                    natModule.installNATRule(rule);
                    break;
                }
                default: {
                    throw new APIMethodUserException("Unsupported NAT rule type");
                }
            }

            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            responseJob.add("Success", true);
            this.getRouterController().onConfigurationChanged();
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to obtain required module");
        } catch (NumberFormatException | IPv4MathException | NATException ex) {
            throw new APIMethodUserException(ex.getMessage());
        }
    }

    private String parseInputParameter(HttpServletRequest request, String parameterName) throws APIMethodUserException {
        if (request.getParameter(parameterName) == null) {
            throw new APIMethodUserException("Input is missing required field \"" + parameterName + "\"");
        }
        return request.getParameter(parameterName);
    }
}
