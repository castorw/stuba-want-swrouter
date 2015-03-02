package net.ctrdn.stuba.want.swrouter.core;

abstract public class DefaultRouterModule implements RouterModule {

    protected final RouterController routerController;

    public DefaultRouterModule(RouterController controller) {
        this.routerController = controller;
    }
}
