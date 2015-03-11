package net.ctrdn.stuba.want.swrouter.module.routingripv2;

import net.ctrdn.stuba.want.swrouter.module.interfacemanager.NetworkInterface;

public class RIPv2NetworkInterfaceConfiguration {

    private final NetworkInterface networkInterface;
    private boolean enabled = false;

    public RIPv2NetworkInterfaceConfiguration(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }
}
