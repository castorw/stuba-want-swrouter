package net.ctrdn.stuba.want.swrouter.module.nat.api;

import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.ctrdn.stuba.want.swrouter.api.DefaultAPIMethod;
import net.ctrdn.stuba.want.swrouter.common.net.IPv4Address;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodException;
import net.ctrdn.stuba.want.swrouter.exception.APIMethodUserException;
import net.ctrdn.stuba.want.swrouter.exception.IPv4MathException;
import net.ctrdn.stuba.want.swrouter.exception.NATException;
import net.ctrdn.stuba.want.swrouter.exception.NoSuchModuleException;
import net.ctrdn.stuba.want.swrouter.module.nat.NATModule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATPool;
import net.ctrdn.stuba.want.swrouter.module.nat.NATRule;
import net.ctrdn.stuba.want.swrouter.module.nat.rule.SNATPoolRule;

public class ConfigureNATPoolAddressesAPIMethod extends DefaultAPIMethod {

    public ConfigureNATPoolAddressesAPIMethod(RouterController routerController) {
        super(routerController, "configure-nat-pool-addresses");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            NATModule natModule = this.getRouterController().getModule(NATModule.class);
            NATPool foundPool = null;
            for (NATPool pool : natModule.getPoolList()) {
                if (pool.getID().equals(UUID.fromString(request.getParameter("ID")))) {
                    foundPool = pool;
                    break;
                }
            }
            if (foundPool == null) {
                throw new APIMethodUserException("Pool not found");
            }
            foundPool.getAddressList().clear();
            String[] addressStringArray = request.getParameter("Addresses").split("\n");
            for (String addressString : addressStringArray) {
                IPv4Address address = IPv4Address.fromString(addressString);
                foundPool.addAddress(address);
            }
            for (NATRule rule : natModule.getRuleList()) {
                if (rule.getTypeString().equals("SNAT_POOL")) {
                    SNATPoolRule ruleCast = (SNATPoolRule) rule;
                    if (ruleCast.getOutsidePool().getName().equals(foundPool.getName())) {
                        ruleCast.evaluatePool();
                    }
                }
            }
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            responseJob.add("Success", true);
            this.getRouterController().onConfigurationChanged();
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to obtain NAT module");
        } catch (IPv4MathException | NATException ex) {
            throw new APIMethodUserException(ex.getMessage());
        }
    }
}
