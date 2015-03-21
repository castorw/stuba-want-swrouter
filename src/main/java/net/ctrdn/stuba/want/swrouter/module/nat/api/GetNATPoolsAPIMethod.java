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
import net.ctrdn.stuba.want.swrouter.module.nat.NATAddress;
import net.ctrdn.stuba.want.swrouter.module.nat.NATModule;
import net.ctrdn.stuba.want.swrouter.module.nat.NATPool;

public class GetNATPoolsAPIMethod extends DefaultAPIMethod {

    public GetNATPoolsAPIMethod(RouterController routerController) {
        super(routerController, "get-nat-pools");
    }

    @Override
    public JsonObjectBuilder executePost(RouterController routerController, HttpServletRequest request, HttpServletResponse response) throws APIMethodException {
        try {
            NATModule natModule = this.getRouterController().getModule(NATModule.class);
            JsonObjectBuilder responseJob = Json.createObjectBuilder();
            JsonArrayBuilder poolsJab = Json.createArrayBuilder();
            for (NATPool pool : natModule.getPoolList()) {
                JsonObjectBuilder poolJob = Json.createObjectBuilder();
                poolJob.add("ID", pool.getID().toString());
                poolJob.add("Name", pool.getName());
                poolJob.add("Prefix", pool.getPrefix().toString());
                JsonArrayBuilder addressesJab = Json.createArrayBuilder();
                for (NATAddress na : pool.getAddressList()) {
                    addressesJab.add(na.getAddress().toString());
                }
                poolJob.add("Addresses", addressesJab);
                poolsJab.add(poolJob);
            }
            responseJob.add("NATPools", poolsJab);
            return responseJob;
        } catch (NoSuchModuleException ex) {
            throw new APIMethodException("Failed to obtain NAT module");
        }
    }

}
