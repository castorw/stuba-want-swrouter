package net.ctrdn.stuba.want.swrouter.module.icmp;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import net.ctrdn.stuba.want.swrouter.core.DefaultRouterModule;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.ModuleInitializationException;

public class ICMPModule extends DefaultRouterModule {

    public ICMPModule(RouterController controller) {
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
        this.routerController.getPacketProcessor().addPipelineBranch(new ICMPEchoResponderPipelineBranch(this));
    }

    @Override
    public void start() {

    }

    @Override
    public String getName() {
        return "ICMP";
    }

    @Override
    public Integer getRevision() {
        return 1;
    }

    protected RouterController getRouterController() {
        return this.routerController;
    }
}
