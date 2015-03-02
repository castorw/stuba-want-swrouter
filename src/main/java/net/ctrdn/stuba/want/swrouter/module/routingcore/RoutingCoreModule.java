package net.ctrdn.stuba.want.swrouter.module.routingcore;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import net.ctrdn.stuba.want.swrouter.core.DefaultRouterModule;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.ModuleInitializationException;

public class RoutingCoreModule extends DefaultRouterModule {

    public RoutingCoreModule(RouterController controller) {
        super(controller);
    }

    @Override
    public void reloadConfiguration(JsonObject moduleConfiguration) {
    }

    @Override
    public JsonObjectBuilder dumpConfiguration() {
        return null;
    }

    @Override
    public void initialize() throws ModuleInitializationException {

    }

    @Override
    public void start() {
    }

    @Override
    public String getName() {
        return "Routing Core";
    }

    @Override
    public Integer getRevision() {
        return 1;
    }

}
